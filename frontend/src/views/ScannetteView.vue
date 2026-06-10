<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import Icon from '../components/Icon.vue';
import ScanReady from '../components/scannette/ScanReady.vue';
import ModeMenu from '../components/scannette/ModeMenu.vue';
import EntryScreen, { type ValidatePayload } from '../components/scannette/EntryScreen.vue';
import SessionList from '../components/scannette/SessionList.vue';
import FlashConfirm from '../components/scannette/FlashConfirm.vue';
import { useReleveStore } from '../store/releve';
import { beep } from '../lib/sound';
import type { Urgency } from '../lib/dates';
import type { Product } from '../api';

const store = useReleveStore();

const tab = ref<'scan' | 'liste'>('scan');
const mode = ref<'menu' | 'dlc' | 'perte'>('menu');
const phase = ref<'ready' | 'entry' | 'flash'>('ready');
const product = ref<Product | null>(null);
const scanError = ref<string | null>(null);
const busy = ref(false);
const flashData = ref<{ name: string; qty: number; urg?: Urgency; motifLabel?: string } | null>(null);

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
    phase.value = 'entry';
    tab.value = 'scan';
  } catch (e) {
    const status = (e as { response?: { status?: number } })?.response?.status;
    scanError.value = status === 404 ? `Code inconnu : ${code}` : 'Erreur de recherche produit';
  } finally {
    busy.value = false;
  }
}

async function onValidate(payload: ValidatePayload) {
  if (!product.value) return;
  const name = product.value.name ?? '';
  const barcode = product.value.barcode ?? '';
  try {
    if (payload.type === 'DLC') {
      await store.addLine({ barcode, dlc: payload.dlc, qty: payload.qty, type: 'DLC' });
    } else {
      await store.addLine({ barcode, qty: payload.qty, type: 'PERTE', motifId: payload.motifId });
    }
  } catch {
    scanError.value = "Échec d'enregistrement de la ligne";
    phase.value = 'ready';
    product.value = null;
    return;
  }
  beep();
  flashData.value =
    payload.type === 'DLC'
      ? { name, qty: payload.qty, urg: payload.urg }
      : { name, qty: payload.qty, motifLabel: payload.motifLabel };
  phase.value = 'flash';
  window.clearTimeout(flashTimer);
  flashTimer = window.setTimeout(() => {
    phase.value = 'ready';
    product.value = null;
    flashData.value = null;
  }, 1250);
}

function cancel() {
  phase.value = 'ready';
  product.value = null;
}

onMounted(() => {
  store.fetchToday();
  store.fetchMotifs();
  poll = window.setInterval(() => {
    if (phase.value !== 'entry') store.fetchToday();
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
              <ScanReady v-else :error="scanError" :busy="busy" @scanned="onScanned" />
            </template>
          </template>
          <SessionList v-else :lines="store.lines" />
        </div>

        <FlashConfirm
          v-if="phase === 'flash' && flashData"
          :name="flashData.name"
          :qty="flashData.qty"
          :urg="flashData.urg"
          :motif-label="flashData.motifLabel"
        />

        <div class="sc-tabs">
          <button :class="['sc-tab', { 'is-on': tab === 'scan' }]" @click="tab = 'scan'">
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
