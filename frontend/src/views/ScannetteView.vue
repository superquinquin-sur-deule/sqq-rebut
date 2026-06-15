<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import Icon from '../components/Icon.vue';
import ScanReady from '../components/scannette/ScanReady.vue';
import ModeMenu from '../components/scannette/ModeMenu.vue';
import EntryScreen, { type ValidatePayload } from '../components/scannette/EntryScreen.vue';
import SessionList from '../components/scannette/SessionList.vue';
import LineEditor from '../components/scannette/LineEditor.vue';
import FlashConfirm from '../components/scannette/FlashConfirm.vue';
import { useReleveStore } from '../store/releve';
import { beep, errorBeep } from '../lib/sound';
import type { Urgency } from '../lib/dates';
import type { Product, ReleveLineDto } from '../api';

const store = useReleveStore();

const tab = ref<'scan' | 'liste'>('scan');
const mode = ref<'menu' | 'dlc' | 'perte'>('menu');
const phase = ref<'ready' | 'entry' | 'flash'>('ready');
const product = ref<Product | null>(null);
const editingLine = ref<ReleveLineDto | null>(null);
const scanError = ref<string | null>(null);
const busy = ref(false);
const flashData = ref<{ name: string; qty: number; uom?: string; urg?: Urgency; motifLabel?: string } | null>(null);

let flashTimer: number | undefined;
let poll: number | undefined;

function pickMode(m: 'dlc' | 'perte') {
  mode.value = m;
  phase.value = 'ready';
  product.value = null;
  scanError.value = null;
  tab.value = 'scan';
}

function backToMenu() {
  mode.value = 'menu';
  phase.value = 'ready';
  product.value = null;
  scanError.value = null;
}

async function onScanned(code: string) {
  scanError.value = null;
  busy.value = true;
  try {
    product.value = await store.lookup(code);
    beep();
    phase.value = 'entry';
    tab.value = 'scan';
  } catch (e) {
    const status = (e as { response?: { status?: number } })?.response?.status;
    scanError.value = status === 404 ? `Code inconnu : ${code}` : 'Erreur de recherche produit';
    errorBeep();
  } finally {
    busy.value = false;
  }
}

async function onValidate(payload: ValidatePayload) {
  if (!product.value) return;
  const name = product.value.name ?? '';
  const barcode = product.value.barcode ?? '';
  const productId = product.value.id;
  const uom = product.value.uom;
  try {
    if (payload.type === 'DLC') {
      await store.addLine({ barcode, productId, dlc: payload.dlc, qty: payload.qty, type: 'DLC' });
    } else {
      await store.addLine({ barcode, productId, qty: payload.qty, type: 'PERTE', motifId: payload.motifId });
    }
  } catch {
    scanError.value = payload.type === 'PERTE' ? "Échec d'envoi au rebut" : "Échec d'enregistrement de la ligne";
    phase.value = 'ready';
    product.value = null;
    return;
  }
  beep();
  flashData.value =
    payload.type === 'DLC'
      ? { name, qty: payload.qty, uom, urg: payload.urg }
      : { name, qty: payload.qty, uom, motifLabel: payload.motifLabel };
  phase.value = 'flash';
  window.clearTimeout(flashTimer);
  flashTimer = window.setTimeout(() => {
    phase.value = 'ready';
    product.value = null;
    flashData.value = null;
  }, 1250);
}

function onPicked(p: Product) {
  scanError.value = null;
  product.value = p;
  beep();
  phase.value = 'entry';
  tab.value = 'scan';
}

function cancel() {
  phase.value = 'ready';
  product.value = null;
}

function openScan() {
  tab.value = 'scan';
  editingLine.value = null;
}

async function onEditSave(patch: { qty: number }) {
  if (!editingLine.value) return;
  try {
    await store.updateLine(editingLine.value.id, patch);
  } catch {
    scanError.value = 'Échec de modification de la ligne';
  }
  editingLine.value = null;
}

async function onEditDelete() {
  if (!editingLine.value) return;
  try {
    await store.remove(editingLine.value.id);
  } catch {
    scanError.value = 'Échec de suppression de la ligne';
  }
  editingLine.value = null;
}

onMounted(() => {
  store.fetchToday();
  store.fetchMotifs();
  poll = window.setInterval(() => {
    if (phase.value !== 'entry' && !editingLine.value) store.fetchToday();
  }, 5000);
});
onUnmounted(() => {
  window.clearInterval(poll);
  window.clearTimeout(flashTimer);
});
</script>

<template>
  <div class="app-scannette">
    <div class="device">
      <div class="sc">

        <div class="sc-body">
          <template v-if="tab === 'scan'">
            <ModeMenu v-if="mode === 'menu'" @select="pickMode" />
            <template v-else>
              <div class="sc-modebar">
                <button class="sc-back" @click="backToMenu" aria-label="Retour au menu">
                  <Icon name="arrowLeft" :size="20" />
                </button>
                <span class="sc-mode-label" :class="mode">{{ mode === 'dlc' ? 'Relevé DLC' : 'Relevé Pertes' }}</span>
              </div>
              <EntryScreen
                v-if="phase === 'entry' && product"
                :product="product"
                :mode="mode === 'perte' ? 'perte' : 'dlc'"
                :motifs="store.motifs"
                @validate="onValidate"
                @cancel="cancel"
              />
              <ScanReady
                v-else
                :mode="mode === 'perte' ? 'perte' : 'dlc'"
                :error="scanError"
                :busy="busy"
                @scanned="onScanned"
                @picked="onPicked"
              />
            </template>
          </template>
          <LineEditor
            v-else-if="editingLine"
            :line="editingLine"
            @save="onEditSave"
            @delete="onEditDelete"
            @back="editingLine = null"
          />
          <SessionList v-else :lines="store.lines" @select="editingLine = $event" />
        </div>

        <FlashConfirm
          v-if="phase === 'flash' && flashData"
          :name="flashData.name"
          :qty="flashData.qty"
          :uom="flashData.uom"
          :urg="flashData.urg"
          :motif-label="flashData.motifLabel"
        />

        <div class="sc-tabs">
          <button :class="['sc-tab', { 'is-on': tab === 'scan' }]" @click="openScan">
            <Icon name="scan" :size="22" /><span class="lbl">Scanner</span>
          </button>
          <button :class="['sc-tab', { 'is-on': tab === 'liste' }]" @click="tab = 'liste'">
            <span v-if="store.counts.total > 0" class="badge">{{ store.counts.total }}</span>
            <Icon name="list" :size="22" /><span class="lbl">Le relevé</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
