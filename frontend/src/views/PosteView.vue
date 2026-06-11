<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import Icon from '../components/Icon.vue';
import ReleveTable from '../components/poste/ReleveTable.vue';
import RebutModal from '../components/poste/RebutModal.vue';
import { useReleveStore } from '../store/releve';
import { URG, fmtLong, parseISO, type Urgency } from '../lib/dates';
import logo from '../assets/sqq-logo.svg';
import type { ReleveLineDto } from '../api';

const store = useReleveStore();
const route = useRoute();

const view = ref<'urgence' | 'tableau'>('urgence');
const rayonFilter = ref('all');
const query = ref('');
const sort = ref<{ key: string; dir: 'asc' | 'desc' }>({ key: 'urg', dir: 'asc' });
const modal = ref(false);
const toastMsg = ref<string | null>(null);

let toastTimer: number | undefined;
let poll: number | undefined;

const URG_ORDER: Record<string, number> = { j0: 0, j1: 1, j2: 2 };
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

const urgRank = (l: ReleveLineDto) => (l.type === 'PERTE' ? 3 : URG_ORDER[l.urgency ?? ''] ?? 3);

const sortedTable = computed(() => {
  const arr = filtered.value.slice();
  const s = sort.value;
  arr.sort((a, b) => {
    let av: number | string;
    let bv: number | string;
    if (s.key === 'urg') {
      av = urgRank(a);
      bv = urgRank(b);
    } else if (s.key === 'qty') {
      av = a.qty;
      bv = b.qty;
    } else {
      av = String((a as Record<string, unknown>)[s.key] ?? '').toLowerCase();
      bv = String((b as Record<string, unknown>)[s.key] ?? '').toLowerCase();
    }
    const r = av < bv ? -1 : av > bv ? 1 : 0;
    return s.dir === 'asc' ? r : -r;
  });
  return arr;
});

function setSort(key: string) {
  sort.value = { key, dir: sort.value.key === key && sort.value.dir === 'asc' ? 'desc' : 'asc' };
}

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
async function confirmRebut() {
  const ids = store.j0Active.map((l: ReleveLineDto) => l.id!).filter((x): x is number => x != null);
  const res = await store.sendRebut(ids);
  modal.value = false;
  toast(res.dryRun ? `${res.created} ligne(s) simulées (dry-run)` : `${res.created} ligne(s) de rebut créées dans Odoo`);
}
async function refresh() {
  await store.fetch();
  toast('Relevé actualisé');
}

watch(
  () => route.params.releveId,
  (id) => {
    if (id) store.fetchById(Number(id));
  },
  { immediate: true },
);
onMounted(() => {
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
            <button class="btn btn-danger btn-md" :disabled="!store.j0Active.length" @click="modal = true">
              <Icon name="upload" :size="18" />Envoyer les J-0 au rebut{{ store.j0Active.length ? ` (${store.j0Active.length})` : '' }}
            </button>
          </div>
        </div>
      </div>

      <div class="dk-toolbar">
        <div class="dk-seg">
          <button :class="{ 'is-on': view === 'urgence' }" @click="view = 'urgence'">Par urgence</button>
          <button :class="{ 'is-on': view === 'tableau' }" @click="view = 'tableau'">Tableau triable</button>
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
        <div v-if="!filtered.length" class="dk-empty">
          <Icon name="search" :size="36" /><h3>Aucun produit</h3>
          <p>Ajuste les filtres ou scanne des produits depuis la scannette.</p>
        </div>
        <ReleveTable
          v-else-if="view === 'tableau'"
          :lines="sortedTable"
          sortable
          :sort-key="sort.key"
          :sort-dir="sort.dir"
          @qty="setQty"
          @delete="del"
          @sort="setSort"
        />
        <template v-else>
          <div v-for="g in groups" :key="g.key">
            <div class="dk-group-head">
              <span class="pill" :class="g.key">
                <span style="width:7px;height:7px;border-radius:50%;background:currentColor" />{{ URG[g.key].tag }} · {{ URG[g.key].label }}
              </span>
              <span class="line" />
            </div>
            <ReleveTable :lines="g.lines" @qty="setQty" @delete="del" />
          </div>
          <div v-if="perteLines.length">
            <div class="dk-group-head">
              <span class="pill perte">
                <span style="width:7px;height:7px;border-radius:50%;background:currentColor" />Pertes
              </span>
              <span class="line" />
            </div>
            <ReleveTable :lines="perteLines" @qty="setQty" @delete="del" />
          </div>
        </template>
      </div>
    </div>

    <RebutModal v-if="modal" :lines="store.j0Active" @close="modal = false" @confirm="confirmRebut" />
    <div v-if="toastMsg" class="toast"><Icon name="checkCircle" :size="18" />{{ toastMsg }}</div>
  </div>
</template>
