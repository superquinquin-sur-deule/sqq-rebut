<script setup lang="ts">
import { ref, onMounted } from 'vue';
import Icon from '../components/Icon.vue';
import { api, type ReleveSummaryDto } from '../api';
import { fmtLong, fmtISO, parseISO, today } from '../lib/dates';
import logo from '../assets/sqq-logo.svg';

const releves = ref<ReleveSummaryDto[]>([]);
const loading = ref(true);
const error = ref<string | null>(null);
const todayIso = fmtISO(today());

onMounted(async () => {
  try {
    releves.value = await api.getReleves();
  } catch {
    error.value = 'Impossible de charger les relevés';
  } finally {
    loading.value = false;
  }
});
</script>

<template>
  <div class="app-poste">
    <div class="poste-head">
      <img :src="logo" alt="SuperQuinquin" />
      <div class="brand">SuperQuinquin<small>Rebut</small></div>
      <nav class="poste-nav">
        <router-link class="nav-link" to="/releves"><Icon name="calendar" :size="16" />Relevé du jour</router-link>
        <router-link class="nav-link" :to="{ name: 'scannette' }"><Icon name="scan" :size="16" />Scannette</router-link>
      </nav>
    </div>

    <div class="dk">
      <div class="dk-top">
        <div class="dk-title-row">
          <h1 class="dk-title">Historique des relevés</h1>
        </div>
      </div>

      <div class="dk-scroll">
        <div v-if="loading" class="dk-empty">
          <Icon name="clock" :size="36" /><h3>Chargement…</h3>
        </div>
        <div v-else-if="error" class="dk-empty">
          <Icon name="alert" :size="36" /><h3>Erreur</h3><p>{{ error }}</p>
        </div>
        <div v-else-if="!releves.length" class="dk-empty">
          <Icon name="calendar" :size="36" /><h3>Aucun relevé</h3>
          <p>Scanne des produits depuis la scannette pour démarrer un relevé.</p>
        </div>
        <div v-else class="hist-list">
          <router-link
            v-for="r in releves"
            :key="r.id"
            class="hist-row"
            :to="{ name: 'poste', params: { releveId: r.id } }"
          >
            <Icon name="calendar" :size="20" />
            <span class="hist-date">
              {{ fmtLong(parseISO(r.date)) }}
              <span v-if="r.date === todayIso" class="hist-today">Aujourd'hui</span>
            </span>
            <span class="hist-count">{{ r.lineCount }} ligne{{ r.lineCount > 1 ? 's' : '' }}</span>
            <Icon name="chevR" :size="18" />
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hist-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 640px;
}

.hist-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  background: #fff;
  border: 1px solid var(--sqq-line);
  border-radius: 14px;
  color: var(--sqq-brown);
  transition: background 0.12s, border-color 0.12s, transform 0.1s;
}

.hist-row:hover {
  background: var(--sqq-cream);
  border-color: var(--sqq-brown);
}

.hist-row:active {
  transform: scale(0.99);
}

.hist-date {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 15px;
  text-transform: capitalize;
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.hist-today {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--sqq-brown);
  background: var(--sqq-yellow);
  padding: 3px 8px;
  border-radius: 999px;
}

.hist-count {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 12.5px;
  color: var(--sqq-brown-soft);
}
</style>
