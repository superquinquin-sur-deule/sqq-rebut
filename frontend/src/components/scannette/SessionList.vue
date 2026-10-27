<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue';
import Icon from '../Icon.vue';
import RebutMotifModal from './RebutMotifModal.vue';
import SessionLine from './SessionLine.vue';
import { useReleveStore } from '../../store/releve';
import { URG, type Urgency } from '../../lib/dates';
import type { ReleveLineDto } from '../../api';

const props = defineProps<{ lines: ReleveLineDto[]; mode: 'menu' | 'dlc' | 'perte' | 'stock' }>();
const emit = defineEmits<{ (e: 'select', line: ReleveLineDto): void }>();

const store = useReleveStore();

const modeType = computed(() =>
  props.mode === 'perte' ? 'PERTE' : props.mode === 'stock' ? 'REASSORT' : props.mode === 'dlc' ? 'DLC' : null,
);
const visible = computed(() => (modeType.value ? props.lines.filter((l) => l.type === modeType.value) : props.lines));
const sorted = computed(() => [...visible.value].sort((a, b) => (b.id ?? 0) - (a.id ?? 0)));

type Group = { key: string; title?: string; urg?: Urgency; lines: ReleveLineDto[] };
type Section = { key: string; title: string; count: number; groups: Group[] };

function flat(key: string, title: string, lines: ReleveLineDto[]): Section {
  return { key, title, count: lines.length, groups: [{ key, lines }] };
}

function dlcSection(lines: ReleveLineDto[]): Section {
  const groups = (['j0', 'j1', 'j2'] as Urgency[])
    .map((k) => ({
      key: k,
      title: `${URG[k].tag} · ${URG[k].label}`,
      urg: k,
      lines: lines.filter((l) => l.urgency === k),
    }))
    .filter((g) => g.lines.length);
  return { key: 'dlc', title: 'Suivi DLC', count: lines.length, groups };
}

const sections = computed<Section[]>(() => {
  const pending = sorted.value.filter((l) => !l.sent);
  const sent = flat('sent', 'Envoyé au rebut', sorted.value.filter((l) => l.sent));
  const list = !modeType.value
    ? // Mode menu : vue globale, le suivi DLC sous-groupé par urgence.
      [
        sent,
        flat('reassort', 'Réassort', pending.filter((l) => l.type === 'REASSORT')),
        flat('perte', 'Pertes', pending.filter((l) => l.type === 'PERTE')),
        dlcSection(pending.filter((l) => l.type === 'DLC')),
      ]
    : [
        props.mode === 'dlc'
          ? dlcSection(pending)
          : flat('pending', props.mode === 'perte' ? 'Pertes' : 'Réassort', pending),
        sent,
      ];
  return list.filter((s) => s.count);
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
      <template v-for="s in sections" :key="s.key">
        <div class="sess-group-head">{{ s.title }} · {{ s.count }}</div>
        <template v-for="g in s.groups" :key="g.key">
          <div v-if="g.urg" class="sess-group-sub">
            <span class="urgdot" :class="g.urg" />{{ g.title }} · {{ g.lines.length }}
          </div>
          <SessionLine
            v-for="l in g.lines"
            :key="l.id"
            :line="l"
            @select="emit('select', l)"
            @remove="removeLine(l)"
          />
        </template>
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
