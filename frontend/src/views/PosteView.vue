<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import Icon from '../components/Icon.vue';
import ReleveTable from '../components/poste/ReleveTable.vue';
import RebutMotifModal from '../components/scannette/RebutMotifModal.vue';
import { useReleveStore } from '../store/releve';
import { URG, fmtLong, parseISO, type Urgency } from '../lib/dates';
import logo from '../assets/sqq-logo.svg';

const store = useReleveStore();
const route = useRoute();

const typeFilter = ref<'all' | 'DLC' | 'PERTE' | 'REASSORT'>('all');
const rayonFilter = ref('all');
const query = ref('');
const toastMsg = ref<string | null>(null);
const rebutModalOpen = ref(false);
const rebutBusy = ref(false);
const selectedIds = ref<Set<number>>(new Set());

let toastTimer: number | undefined;
let poll: number | undefined;

const dateLabel = computed(() => (store.date ? fmtLong(parseISO(store.date)) : ''));

const filtered = computed(() =>
  store.lines.filter(
    (l) =>
      (rayonFilter.value === 'all' || l.rayon === rayonFilter.value) &&
      (!query.value ||
        (l.name ?? '').toLowerCase().includes(query.value.toLowerCase()) ||
        (l.barcode ?? '').includes(query.value)),
  ),
);

const groups = computed(() =>
  (['j0', 'j1', 'j2'] as Urgency[])
    .map((k) => ({
      key: k,
      lines: filtered.value.filter((l) => l.urgency === k).slice().sort((a, b) => Number(a.sent) - Number(b.sent)),
    }))
    .filter((g) => g.lines.length),
);

const perteLines = computed(() =>
  filtered.value.filter((l) => l.type === 'PERTE').slice().sort((a, b) => Number(a.sent) - Number(b.sent)),
);

const reassortLines = computed(() =>
  filtered.value.filter((l) => l.type === 'REASSORT').slice().sort((a, b) => Number(a.sent) - Number(b.sent)),
);

const unsentPerte = computed(() => perteLines.value.filter((l) => !l.sent));

const allUnsentPerteSelected = computed(
  () => unsentPerte.value.length > 0 && unsentPerte.value.every((l) => selectedIds.value.has(l.id!)),
);

function toggleLine(id: number) {
  const next = new Set(selectedIds.value);
  if (next.has(id)) next.delete(id);
  else next.add(id);
  selectedIds.value = next;
}

function toggleAll() {
  selectedIds.value = allUnsentPerteSelected.value
    ? new Set()
    : new Set(unsentPerte.value.map((l) => l.id!));
}

watch(unsentPerte, (lines) => {
  const live = new Set(lines.map((l) => l.id!));
  const kept = [...selectedIds.value].filter((id) => live.has(id));
  if (kept.length !== selectedIds.value.size) selectedIds.value = new Set(kept);
});

const showDlc = computed(() => typeFilter.value === 'all' || typeFilter.value === 'DLC');
const showPerte = computed(() => typeFilter.value === 'all' || typeFilter.value === 'PERTE');
const showReassort = computed(() => typeFilter.value === 'all' || typeFilter.value === 'REASSORT');

const visibleCount = computed(
  () =>
    (showDlc.value ? groups.value.reduce((n, g) => n + g.lines.length, 0) : 0) +
    (showPerte.value ? perteLines.value.length : 0) +
    (showReassort.value ? reassortLines.value.length : 0),
);

function toast(m: string) {
  toastMsg.value = m;
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => (toastMsg.value = null), 2600);
}

async function setQty(id: number, qty: number) {
  await store.setQty(id, qty);
}
async function del(id: number) {
  await store.remove(id);
}
async function refresh() {
  await store.fetch();
  toast('Relevé actualisé');
}

async function doRebut(motifId: number) {
  const ids = [...selectedIds.value].filter((id) => unsentPerte.value.some((l) => l.id === id));
  if (!ids.length) {
    rebutModalOpen.value = false;
    return;
  }
  rebutBusy.value = true;
  try {
    const r = await store.rebut(motifId, ids);
    toast(`${r.created} perte${r.created > 1 ? 's' : ''} envoyée${r.created > 1 ? 's' : ''} au rebut`);
    selectedIds.value = new Set();
  } catch {
    toast("Échec de l'envoi au rebut");
  } finally {
    rebutBusy.value = false;
    rebutModalOpen.value = false;
  }
}

watch(
  () => route.params.releveId,
  (id) => {
    if (id) store.fetchById(Number(id));
  },
  { immediate: true },
);
onMounted(() => {
  store.fetchMotifs();
  poll = window.setInterval(() => store.fetch(), 5000);
});
onUnmounted(() => {
  window.clearInterval(poll);
  window.clearTimeout(toastTimer);
});
</script>

<template>
  <div class="app-poste">
    <div class="poste-head">
      <img :src="logo" alt="SuperQuinquin" />
      <div class="brand">SuperQuinquin<small>Rebut</small></div>
      <nav class="poste-nav">
        <router-link class="nav-link" :to="{ name: 'historique' }"><Icon name="clock" :size="16" />Historique</router-link>
        <router-link class="nav-link" :to="{ name: 'scannette' }"><Icon name="scan" :size="16" />Scannette</router-link>
      </nav>
    </div>

    <div class="dk">
      <div class="dk-top">
        <div class="dk-title-row">
          <h1 class="dk-title">Relevé {{ dateLabel ? ` du ${dateLabel}` : '' }}</h1>
          <div class="dk-actions">
            <button class="btn btn-ghost btn-md" @click="refresh"><Icon name="refresh" :size="18" />Actualiser</button>
          </div>
        </div>
      </div>

      <div class="dk-toolbar">
        <div class="dk-seg">
          <button :class="{ 'is-on': typeFilter === 'all' }" @click="typeFilter = 'all'">Tous</button>
          <button :class="{ 'is-on': typeFilter === 'DLC' }" @click="typeFilter = 'DLC'">DLC</button>
          <button :class="{ 'is-on': typeFilter === 'PERTE' }" @click="typeFilter = 'PERTE'">Pertes</button>
          <button :class="{ 'is-on': typeFilter === 'REASSORT' }" @click="typeFilter = 'REASSORT'">Réassort</button>
        </div>
        <div class="dk-search">
          <Icon name="search" :size="16" style="color:var(--sqq-mute)" />
          <input v-model="query" placeholder="Rechercher un produit…" />
        </div>
        <span class="dk-filter">Rayon
          <select v-model="rayonFilter">
            <option value="all">Tous</option>
            <option v-for="r in store.rayons" :key="r" :value="r">{{ r }}</option>
          </select>
        </span>
      </div>

      <div class="dk-scroll">
        <div v-if="!visibleCount" class="dk-empty">
          <Icon name="search" :size="36" /><h3>Aucun produit</h3>
          <p>Ajuste les filtres ou scanne des produits depuis la scannette.</p>
        </div>
        <template v-else>
          <template v-if="showDlc">
            <div v-for="g in groups" :key="g.key">
              <div class="dk-group-head">
                <span class="pill" :class="g.key">
                  <span style="width:7px;height:7px;border-radius:50%;background:currentColor" />{{ URG[g.key].tag }} · {{ URG[g.key].label }}
                </span>
                <span class="line" />
              </div>
              <ReleveTable :lines="g.lines" @qty="setQty" @delete="del" />
            </div>
          </template>
          <div v-if="showPerte && perteLines.length">
            <div class="dk-group-head">
              <span class="pill perte">
                <span style="width:7px;height:7px;border-radius:50%;background:currentColor" />Pertes
              </span>
              <span class="line" />
              <label v-if="unsentPerte.length" class="dk-selall">
                <input type="checkbox" :checked="allUnsentPerteSelected" @change="toggleAll" />Tout sélectionner
              </label>
              <button
                v-if="unsentPerte.length"
                class="btn btn-danger btn-sm"
                :disabled="rebutBusy || !selectedIds.size"
                @click="rebutModalOpen = true"
              >
                <Icon name="trash" :size="16" />Envoyer au rebut · {{ selectedIds.size }}
              </button>
            </div>
            <ReleveTable :lines="perteLines" selectable :selected="selectedIds" @qty="setQty" @delete="del" @toggle="toggleLine" />
          </div>
          <div v-if="showReassort && reassortLines.length">
            <div class="dk-group-head">
              <span class="pill reassort">
                <span style="width:7px;height:7px;border-radius:50%;background:currentColor" />Réassort
              </span>
              <span class="line" />
            </div>
            <ReleveTable :lines="reassortLines" @qty="setQty" @delete="del" />
          </div>
        </template>
      </div>
    </div>

    <RebutMotifModal
      v-if="rebutModalOpen"
      :motifs="store.motifs"
      :count="selectedIds.size"
      :busy="rebutBusy"
      @confirm="doRebut"
      @close="rebutModalOpen = false"
    />
    <div v-if="toastMsg" class="toast"><Icon name="checkCircle" :size="18" />{{ toastMsg }}</div>
  </div>
</template>
