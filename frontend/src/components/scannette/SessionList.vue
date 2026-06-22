<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue';
import Icon from '../Icon.vue';
import RebutMotifModal from './RebutMotifModal.vue';
import SessionLine from './SessionLine.vue';
import { useReleveStore } from '../../store/releve';
import type { ReleveLineDto } from '../../api';

const props = defineProps<{ lines: ReleveLineDto[]; mode: 'menu' | 'dlc' | 'perte' | 'stock' }>();
const emit = defineEmits<{ (e: 'select', line: ReleveLineDto): void }>();

const store = useReleveStore();

const modeType = computed(() =>
  props.mode === 'perte' ? 'PERTE' : props.mode === 'stock' ? 'REASSORT' : props.mode === 'dlc' ? 'DLC' : null,
);
const visible = computed(() => (modeType.value ? props.lines.filter((l) => l.type === modeType.value) : props.lines));
const sorted = computed(() => [...visible.value].sort((a, b) => (b.id ?? 0) - (a.id ?? 0)));

const groups = computed(() => {
  if (!modeType.value) {
    // Mode menu : on garde la vue globale en trois sections.
    const sent = sorted.value.filter((l) => l.sent);
    const reassort = sorted.value.filter((l) => !l.sent && l.type === 'REASSORT');
    const dlc = sorted.value.filter((l) => !l.sent && l.type !== 'REASSORT');
    return [
      { key: 'sent', title: 'Envoyé au rebut', lines: sent },
      { key: 'reassort', title: 'Réassort', lines: reassort },
      { key: 'dlc', title: 'Suivi DLC', lines: dlc },
    ].filter((g) => g.lines.length);
  }
  const pendingTitle = props.mode === 'perte' ? 'Pertes' : props.mode === 'stock' ? 'Réassort' : 'Suivi DLC';
  return [
    { key: 'pending', title: pendingTitle, lines: sorted.value.filter((l) => !l.sent) },
    { key: 'sent', title: 'Envoyé au rebut', lines: sorted.value.filter((l) => l.sent) },
  ].filter((g) => g.lines.length);
});

const unsentPerte = computed(() => sorted.value.filter((l) => l.type === 'PERTE' && !l.sent));
const showRebut = computed(() => props.mode === 'perte' && unsentPerte.value.length > 0);

const modalOpen = ref(false);
const rebutBusy = ref(false);
const rebutMsg = ref<string | null>(null);
let msgTimer: number | undefined;

function toast(m: string) {
  rebutMsg.value = m;
  window.clearTimeout(msgTimer);
  msgTimer = window.setTimeout(() => (rebutMsg.value = null), 2600);
}

async function removeLine(l: ReleveLineDto) {
  if (l.id == null) return;
  try {
    await store.remove(l.id);
  } catch {
    toast('Échec de suppression de la ligne');
  }
}

async function doRebut(motifId: number) {
  const ids = unsentPerte.value.map((l) => l.id).filter((id): id is number => id != null);
  if (!ids.length) {
    modalOpen.value = false;
    return;
  }
  rebutBusy.value = true;
  try {
    const r = await store.rebut(motifId, ids);
    toast(`${r.created} produit${r.created > 1 ? 's' : ''} envoyé${r.created > 1 ? 's' : ''} au rebut`);
  } catch {
    toast("Échec de l'envoi au rebut");
  } finally {
    rebutBusy.value = false;
    modalOpen.value = false;
  }
}

onUnmounted(() => window.clearTimeout(msgTimer));
</script>

<template>
  <div v-if="!visible.length" class="sess-empty">
    <Icon name="list" :size="40" />
    <div>
      <b style="font-family:var(--font-display);font-size:15px;text-transform:uppercase;letter-spacing:0.04em">Aucune ligne</b><br />
      Scanne ton premier produit.
    </div>
  </div>
  <template v-else>
    <div class="sess-scroll">
      <template v-for="g in groups" :key="g.key">
        <div class="sess-group-head">{{ g.title }} · {{ g.lines.length }}</div>
        <SessionLine
          v-for="l in g.lines"
          :key="l.id"
          :line="l"
          @select="emit('select', l)"
          @remove="removeLine(l)"
        />
      </template>
    </div>

    <div v-if="showRebut" class="sess-rebut">
      <button class="btn btn-primary btn-md btn-block" :disabled="rebutBusy" @click="modalOpen = true">
        <Icon name="trash" :size="18" />Envoyer au rebut · {{ unsentPerte.length }}
      </button>
    </div>
  </template>

  <RebutMotifModal
    v-if="modalOpen"
    :motifs="store.motifs"
    :count="unsentPerte.length"
    :busy="rebutBusy"
    @confirm="doRebut"
    @close="modalOpen = false"
  />
  <div v-if="rebutMsg" class="toast"><Icon name="checkCircle" :size="18" />{{ rebutMsg }}</div>
</template>
