<script setup lang="ts">
import { computed } from 'vue';
import Icon from '../Icon.vue';
import { URG, fmtShort, parseISO, type Urgency } from '../../lib/dates';
import type { ReleveLineDto } from '../../api';

const props = defineProps<{ lines: ReleveLineDto[] }>();
const sorted = computed(() => [...props.lines].sort((a, b) => (b.id ?? 0) - (a.id ?? 0)));
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
    <div v-for="l in sorted" :key="l.id" class="sess-line">
      <span :class="['urgdot', l.urgency]" />
      <div class="nm">
        <b>{{ l.name }}</b>
        <span>{{ URG[l.urgency as Urgency].tag }} · {{ fmtShort(parseISO(l.dlc)) }} · {{ l.rayon }}</span>
      </div>
      <span class="q">×{{ l.qty }}</span>
    </div>
  </div>
</template>
