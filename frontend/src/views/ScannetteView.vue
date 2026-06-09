<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import Icon from '../components/Icon.vue';
import ScanReady from '../components/scannette/ScanReady.vue';
import EntryScreen from '../components/scannette/EntryScreen.vue';
import SessionList from '../components/scannette/SessionList.vue';
import FlashConfirm from '../components/scannette/FlashConfirm.vue';
import { useReleveStore } from '../store/releve';
import { fmtLong, today, type Urgency } from '../lib/dates';
import { beep } from '../lib/sound';
import type { Product } from '../api';

const store = useReleveStore();

const tab = ref<'scan' | 'liste'>('scan');
const phase = ref<'ready' | 'entry' | 'flash'>('ready');
const product = ref<Product | null>(null);
const scanError = ref<string | null>(null);
const busy = ref(false);
const flashData = ref<{ name: string; qty: number; urg: Urgency } | null>(null);

let flashTimer: number | undefined;
let poll: number | undefined;

const dateLabel = computed(() => fmtLong(today()));

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

async function onValidate(payload: { dlc: string; qty: number; urg: Urgency }) {
  if (!product.value) return;
  const name = product.value.name ?? '';
  try {
    await store.addLine(product.value.barcode ?? '', payload.dlc, payload.qty);
  } catch {
    scanError.value = "Échec d'enregistrement de la ligne";
    phase.value = 'ready';
    product.value = null;
    return;
  }
  beep();
  flashData.value = { name, qty: payload.qty, urg: payload.urg };
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
            <EntryScreen
              v-if="phase === 'entry' && product"
              :product="product"
              @validate="onValidate"
              @cancel="cancel"
            />
            <ScanReady v-else :error="scanError" :busy="busy" @scanned="onScanned" />
          </template>
          <SessionList v-else :lines="store.lines" />
        </div>

        <FlashConfirm
          v-if="phase === 'flash' && flashData"
          :name="flashData.name"
          :qty="flashData.qty"
          :urg="flashData.urg"
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
