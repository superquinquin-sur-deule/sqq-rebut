<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import Icon from '../components/Icon.vue';
import ScanReady from '../components/scannette/ScanReady.vue';
import ModeMenu from '../components/scannette/ModeMenu.vue';
import EntryScreen, { type ValidatePayload } from '../components/scannette/EntryScreen.vue';
import SessionList from '../components/scannette/SessionList.vue';
import LineEditor from '../components/scannette/LineEditor.vue';
import FlashConfirm from '../components/scannette/FlashConfirm.vue';
import { useReleveStore } from '../store/releve';
import { beep, errorBeep } from '../lib/sound';
import { fmtQty, isWeightUom } from '../lib/qty';
import type { Urgency } from '../lib/dates';
import type { Product, ReleveLineDto } from '../api';

const store = useReleveStore();

const tab = ref<'scan' | 'liste'>('scan');
const mode = ref<'menu' | 'dlc' | 'perte' | 'stock'>('menu');
const phase = ref<'ready' | 'entry' | 'flash'>('ready');
const product = ref<Product | null>(null);
const editingLine = ref<ReleveLineDto | null>(null);
const scanError = ref<string | null>(null);
const busy = ref(false);
const flashData = ref<{ name: string; qty: number; uom?: string; urg?: Urgency } | null>(null);
/** Dernier produit ajouté en scan-en-rafale (pertes / réassort) : affiché jusqu'au prochain scan. */
const lastAdded = ref<{ name: string; detail: string } | null>(null);

let flashTimer: number | undefined;
let poll: number | undefined;

/** Nombre de lignes du mode courant (badge de l'onglet « Le relevé »). */
const modeCount = computed(() => {
  const t = mode.value === 'perte' ? 'PERTE' : mode.value === 'stock' ? 'REASSORT' : mode.value === 'dlc' ? 'DLC' : null;
  return t ? store.lines.filter((l) => l.type === t).length : store.lines.length;
});

function pickMode(m: 'dlc' | 'perte' | 'stock') {
  mode.value = m;
  phase.value = 'ready';
  product.value = null;
  lastAdded.value = null;
  scanError.value = null;
  tab.value = 'scan';
}

function backToMenu() {
  mode.value = 'menu';
  phase.value = 'ready';
  product.value = null;
  lastAdded.value = null;
  scanError.value = null;
}

function stockLabel(p: Product): string {
  const q = p.qtyAvailable ?? 0;
  return isWeightUom(p.uom)
    ? `${q.toLocaleString('fr-FR', { maximumFractionDigits: 3 })} kg`
    : `${q.toLocaleString('fr-FR', { maximumFractionDigits: 0 })} ${p.uom}`;
}

/** Pertes / réassort : on ajoute directement à la liste et on affiche le produit, sans validation. */
async function addToList(p: Product) {
  const type = mode.value === 'perte' ? 'PERTE' : 'REASSORT';
  // Perte d'un produit au poids : on remonte le poids précis du code-barres balance
  // (réassort = simple liste, le backend ignore la quantité).
  const qty = type === 'PERTE' && p.soldByWeight ? p.scannedWeight : undefined;
  let line: ReleveLineDto;
  try {
    line = await store.addLine({ barcode: p.barcode ?? '', productId: p.id, type, qty });
  } catch {
    scanError.value = mode.value === 'perte' ? "Échec d'ajout à la liste pertes" : "Échec d'ajout au réassort";
    errorBeep();
    return;
  }
  lastAdded.value = {
    name: line.name ?? p.name,
    detail: type === 'PERTE' ? `Ajouté · ${fmtQty(line.qty, line.uom ?? p.uom)}` : `Réassort · stock ${stockLabel(p)}`,
  };
  beep();
}

async function onScanned(code: string) {
  scanError.value = null;
  busy.value = true;
  try {
    const p = await store.lookup(code);
    if (mode.value === 'dlc') {
      product.value = p;
      beep();
      phase.value = 'entry';
    } else {
      await addToList(p);
    }
    tab.value = 'scan';
  } catch (e) {
    const status = (e as { response?: { status?: number } })?.response?.status;
    scanError.value = status === 404 ? `Code inconnu : ${code}` : 'Erreur de recherche produit';
    errorBeep();
  } finally {
    busy.value = false;
  }
}

function onValidate(payload: ValidatePayload) {
  if (!product.value) return;
  const name = product.value.name ?? '';
  const barcode = product.value.barcode ?? '';
  const productId = product.value.id;
  const uom = product.value.uom;
  store
    .addLine({ barcode, productId, dlc: payload.dlc, qty: payload.qty, type: 'DLC' })
    .then(() => {
      beep();
      flashData.value = { name, qty: payload.qty, uom, urg: payload.urg };
      phase.value = 'flash';
      window.clearTimeout(flashTimer);
      flashTimer = window.setTimeout(() => {
        phase.value = 'ready';
        product.value = null;
        flashData.value = null;
      }, 1250);
    })
    .catch(() => {
      scanError.value = "Échec d'enregistrement de la ligne";
      phase.value = 'ready';
      product.value = null;
    });
}

function onPicked(p: Product) {
  scanError.value = null;
  tab.value = 'scan';
  if (mode.value === 'dlc') {
    product.value = p;
    beep();
    phase.value = 'entry';
  } else {
    addToList(p);
  }
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
                <span class="sc-mode-label" :class="mode">{{
                  mode === 'dlc' ? 'Relevé DLC' : mode === 'perte' ? 'Relevé Pertes' : 'Réassort'
                }}</span>
              </div>
              <EntryScreen
                v-if="mode === 'dlc' && phase === 'entry' && product"
                :product="product"
                @validate="onValidate"
                @cancel="cancel"
              />
              <ScanReady
                v-else
                :mode="mode"
                :error="scanError"
                :busy="busy"
                :last="mode === 'dlc' ? null : lastAdded"
                :count="modeCount"
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
          <SessionList v-else :lines="store.lines" :mode="mode" @select="editingLine = $event" />
        </div>

        <FlashConfirm
          v-if="phase === 'flash' && flashData"
          :name="flashData.name"
          :qty="flashData.qty"
          :uom="flashData.uom"
          :urg="flashData.urg"
        />

        <div class="sc-tabs">
          <button :class="['sc-tab', { 'is-on': tab === 'scan' }]" @click="openScan">
            <Icon name="scan" :size="22" /><span class="lbl">Scanner</span>
          </button>
          <button :class="['sc-tab', { 'is-on': tab === 'liste' }]" @click="tab = 'liste'">
            <span v-if="modeCount > 0" class="badge">{{ modeCount }}</span>
            <Icon name="list" :size="22" /><span class="lbl">Le relevé</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
