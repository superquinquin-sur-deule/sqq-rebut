<script setup lang="ts">
import { computed } from 'vue';
import Icon from '../Icon.vue';
import { URG, fmtShort, parseISO, type Urgency } from '../../lib/dates';
import { fmtQty } from '../../lib/qty';
import type { ReleveLineDto } from '../../api';

const props = defineProps<{ lines: ReleveLineDto[] }>();
const emit = defineEmits<{ (e: 'select', line: ReleveLineDto): void }>();
const sorted = computed(() => [...props.lines].sort((a, b) => (b.id ?? 0) - (a.id ?? 0)));
const groups = computed(() => {
  const sent = sorted.value.filter((l) => l.sent);
  const reassort = sorted.value.filter((l) => !l.sent && l.type === 'REASSORT');
  const dlc = sorted.value.filter((l) => !l.sent && l.type !== 'REASSORT');
  return [
    { key: 'sent', title: 'Envoyé au rebut', lines: sent },
    { key: 'reassort', title: 'Réassort', lines: reassort },
    { key: 'dlc', title: 'Suivi DLC', lines: dlc },
  ].filter((g) => g.lines.length);
});
</script>

<template>
  <div v-if="!props.lines.length" class="sess-empty">
    <Icon name="list" :size="40" />
    <div>
      <b style="font-family:var(--font-display);font-size:15px;text-transform:uppercase;letter-spacing:0.04em">Aucune ligne</b><br />
      Scanne ton premier produit.
    </div>
  </div>
  <div v-else class="sess-scroll">
    <template v-for="g in groups" :key="g.key">
      <div class="sess-group-head">{{ g.title }} · {{ g.lines.length }}</div>
      <component
        :is="l.sent ? 'div' : 'button'"
        v-for="l in g.lines"
        :key="l.id"
        :type="l.sent ? undefined : 'button'"
        :class="['sess-line', { 'is-sent': l.sent }]"
        @click="!l.sent && emit('select', l)"
      >
        <span :class="['urgdot', l.type === 'PERTE' ? 'perte' : l.type === 'REASSORT' ? 'reassort' : l.urgency]" />
        <div class="nm">
          <b>{{ l.name }}</b>
          <span v-if="l.type === 'PERTE'">{{ l.motifLabel }} · {{ l.rayon }}</span>
          <span v-else-if="l.type === 'REASSORT'">Réassort · {{ l.rayon }}</span>
          <span v-else>{{ URG[l.urgency as Urgency].tag }} · {{ fmtShort(parseISO(l.dlc as string)) }} · {{ l.rayon }}</span>
        </div>
        <span v-if="l.type !== 'REASSORT'" class="q">{{ fmtQty(l.qty, l.uom) }}</span>
        <Icon v-if="l.sent" name="checkCircle" :size="18" class="sess-sent" />
        <Icon v-else name="chevR" :size="18" class="sess-chev" />
      </component>
    </template>
  </div>
</template>
