<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import Icon from '../components/Icon.vue';
import { api, type ReportSettingsDto } from '../api';
import { fmtLong } from '../lib/dates';
import logo from '../assets/sqq-logo.svg';

const enabled = ref(false);
const sendTime = ref('18:00');
const recipients = ref<string[]>(['']);
const thresholdPieces = ref(5);
const thresholdKg = ref(1);
const lastSentAt = ref<string | null>(null);
const lastStatus = ref<string | null>(null);
const lastError = ref<string | null>(null);

const loading = ref(true);
const loadError = ref<string | null>(null);
const saving = ref(false);
const sending = ref(false);
const toastMsg = ref<string | null>(null);
let toastTimer: number | undefined;

const cleanRecipients = computed(() => recipients.value.map((m) => m.trim()).filter(Boolean));

const lastSentLabel = computed(() => {
  if (!lastSentAt.value) return null;
  const d = new Date(lastSentAt.value);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${fmtLong(d)} à ${hh}:${mm}`;
});

function apply(s: ReportSettingsDto) {
  enabled.value = s.enabled;
  sendTime.value = s.sendTime;
  recipients.value = s.recipients.length ? [...s.recipients] : [''];
  thresholdPieces.value = s.thresholdPieces;
  thresholdKg.value = s.thresholdKg;
  lastSentAt.value = s.lastSentAt ?? null;
  lastStatus.value = s.lastStatus ?? null;
  lastError.value = s.lastError ?? null;
}

function message(e: unknown, fallback: string): string {
  const data = (e as { response?: { data?: { message?: string } } })?.response?.data;
  return data?.message ?? fallback;
}

function toast(m: string) {
  toastMsg.value = m;
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => (toastMsg.value = null), 3200);
}

onMounted(async () => {
  try {
    apply(await api.getReportSettings());
  } catch {
    loadError.value = 'Impossible de charger les réglages';
  } finally {
    loading.value = false;
  }
});

onUnmounted(() => window.clearTimeout(toastTimer));

function addRecipient() {
  recipients.value.push('');
}

function removeRecipient(i: number) {
  recipients.value.splice(i, 1);
  if (!recipients.value.length) recipients.value = [''];
}

async function save() {
  saving.value = true;
  try {
    apply(
      await api.updateReportSettings({
        enabled: enabled.value,
        sendTime: sendTime.value,
        recipients: cleanRecipients.value,
        thresholdPieces: Number(thresholdPieces.value),
        thresholdKg: Number(thresholdKg.value),
      }),
    );
    toast('Réglages enregistrés');
  } catch (e) {
    toast(message(e, "Échec de l'enregistrement"));
  } finally {
    saving.value = false;
  }
}

async function sendNow() {
  sending.value = true;
  try {
    const r = await api.sendReportNow();
    toast(r.lineCount ? `Rapport envoyé (${r.lineCount} produits)` : 'Rapport envoyé (rien à signaler)');
    apply(await api.getReportSettings());
  } catch (e) {
    toast(message(e, "Échec de l'envoi"));
    try {
      apply(await api.getReportSettings());
    } catch {
      // Réglages illisibles : le toast d'erreur suffit.
    }
  } finally {
    sending.value = false;
  }
}
</script>

<template>
  <div class="app-poste">
    <div class="poste-head">
      <img :src="logo" alt="SuperQuinquin" />
      <div class="brand">SuperQuinquin<small>Rebut</small></div>
      <nav class="poste-nav">
        <router-link class="nav-link" to="/releves"><Icon name="calendar" :size="16" />Relevé du jour</router-link>
        <router-link class="nav-link" :to="{ name: 'historique' }"><Icon name="clock" :size="16" />Historique</router-link>
        <router-link class="nav-link" :to="{ name: 'scannette' }"><Icon name="scan" :size="16" />Scannette</router-link>
      </nav>
    </div>

    <div class="dk">
      <div class="dk-top">
        <div class="dk-title-row">
          <h1 class="dk-title">Rapport quotidien</h1>
          <div class="dk-actions">
            <button class="btn btn-ghost btn-md" :disabled="loading || sending" @click="sendNow">
              <Icon name="send" :size="18" />{{ sending ? 'Envoi…' : 'Envoyer maintenant' }}
            </button>
            <button class="btn btn-primary btn-md" :disabled="loading || saving" @click="save">
              <Icon name="check" :size="18" />{{ saving ? 'Enregistrement…' : 'Enregistrer' }}
            </button>
          </div>
        </div>
      </div>

      <div class="dk-scroll">
        <div v-if="loading" class="dk-empty">
          <Icon name="clock" :size="36" /><h3>Chargement…</h3>
        </div>
        <div v-else-if="loadError" class="dk-empty">
          <Icon name="alert" :size="36" /><h3>Erreur</h3><p>{{ loadError }}</p>
        </div>

        <div v-else class="rg-form">
          <section class="rg-card">
            <p class="rg-intro">
              Chaque jour à l'heure choisie, un e-mail liste les produits <strong>DLC</strong> du relevé du
              jour dont la quantité dépasse les seuils, groupés par J-0 / J-1 / J-2. Le détail complet est
              joint au format Excel.
            </p>
          </section>

          <section class="rg-card">
            <label class="rg-switch">
              <input v-model="enabled" type="checkbox" />
              <span>Activer l'envoi automatique</span>
            </label>
            <p class="rg-hint">
              Désactivé, aucun e-mail n'est envoyé automatiquement — le bouton « Envoyer maintenant »
              reste utilisable.
            </p>

            <div class="rg-field rg-narrow">
              <span class="field-label">Heure d'envoi (heure de Paris)</span>
              <input v-model="sendTime" type="time" class="rg-input" />
            </div>
          </section>

          <section class="rg-card">
            <span class="field-label">Destinataires</span>
            <div v-for="(_, i) in recipients" :key="i" class="rg-mail-row">
              <input
                v-model="recipients[i]"
                type="email"
                class="rg-input"
                inputmode="email"
                placeholder="prenom.nom@superquinquin.fr"
              />
              <button class="btn btn-ghost btn-sm" title="Retirer" @click="removeRecipient(i)">
                <Icon name="trash" :size="16" />
              </button>
            </div>
            <button class="btn btn-ghost btn-sm rg-add" @click="addRecipient">
              <Icon name="plus" :size="16" />Ajouter une adresse
            </button>
          </section>

          <section class="rg-card">
            <span class="field-label">Seuils</span>
            <div class="rg-grid">
              <div class="rg-field">
                <label class="rg-sub">Produits à la pièce</label>
                <div class="rg-unit">
                  <input v-model.number="thresholdPieces" type="number" min="0" step="1" class="rg-input" />
                  <span>pièces</span>
                </div>
              </div>
              <div class="rg-field">
                <label class="rg-sub">Produits au poids</label>
                <div class="rg-unit">
                  <input v-model.number="thresholdKg" type="number" min="0" step="0.1" class="rg-input" />
                  <span>kg</span>
                </div>
              </div>
            </div>
            <p class="rg-hint">
              Seuils stricts : un produit à exactement {{ thresholdPieces }} pièces ou
              {{ thresholdKg }} kg n'est pas listé.
            </p>
          </section>

          <section class="rg-card">
            <span class="field-label">Dernier envoi</span>
            <div v-if="!lastSentLabel" class="rg-hint">Aucun envoi pour le moment.</div>
            <template v-else>
              <div class="rg-last">
                <span class="rg-badge" :class="lastStatus === 'OK' ? 'is-ok' : 'is-ko'">
                  {{ lastStatus === 'OK' ? 'Envoyé' : 'Erreur' }}
                </span>
                <span>{{ lastSentLabel }}</span>
              </div>
              <p v-if="lastError" class="rg-error">{{ lastError }}</p>
            </template>
            <p class="rg-hint">
              « Envoyer maintenant » utilise les réglages <strong>enregistrés</strong> et ne remplace pas
              l'envoi automatique du jour.
            </p>
          </section>
        </div>
      </div>
    </div>

    <div v-if="toastMsg" class="toast"><Icon name="checkCircle" :size="18" />{{ toastMsg }}</div>
  </div>
</template>

<style scoped>
.rg-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 640px;
}

.rg-card {
  background: #fff;
  border: 1px solid var(--sqq-line);
  border-radius: 14px;
  padding: 18px;
}

.rg-intro {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--sqq-brown-soft);
}

.rg-switch {
  display: flex;
  align-items: center;
  gap: 10px;
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 14px;
  color: var(--sqq-brown);
  cursor: pointer;
}

.rg-switch input {
  width: 18px;
  height: 18px;
  accent-color: var(--sqq-brown);
}

.rg-hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--sqq-mute);
}

.rg-field {
  display: flex;
  flex-direction: column;
}

.rg-field + .rg-field,
.rg-hint + .rg-field {
  margin-top: 16px;
}

.rg-sub {
  font-size: 12px;
  color: var(--sqq-brown-soft);
  margin-bottom: 6px;
}

.rg-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--sqq-line);
  border-radius: 10px;
  background: var(--sqq-cream);
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--sqq-brown);
}

.rg-input:focus {
  outline: none;
  border-color: var(--sqq-brown);
}

.rg-narrow .rg-input {
  max-width: 160px;
}

.rg-mail-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.rg-add {
  margin-top: 2px;
}

.rg-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.rg-grid .rg-field + .rg-field {
  margin-top: 0;
}

.rg-unit {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rg-unit span {
  font-size: 12.5px;
  color: var(--sqq-brown-soft);
}

.rg-last {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--sqq-brown);
}

.rg-badge {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  padding: 3px 8px;
  border-radius: 999px;
}

.rg-badge.is-ok {
  color: var(--sqq-brown);
  background: var(--sqq-yellow);
}

.rg-badge.is-ko {
  color: #fff;
  background: #b3261e;
}

.rg-error {
  margin: 8px 0 0;
  font-family: var(--font-mono);
  font-size: 11.5px;
  line-height: 1.45;
  color: #b3261e;
  word-break: break-word;
}

@media (max-width: 560px) {
  .rg-grid {
    grid-template-columns: 1fr;
  }
}
</style>
