<script setup lang="ts">
import { URG, fmtShort, type Urgency } from '../../lib/dates';

defineProps<{ value: Urgency | null }>();
const emit = defineEmits<{ (e: 'select', k: Urgency): void }>();

const keys: Urgency[] = ['j0', 'j1', 'j2'];
const sub = (k: Urgency) => (k === 'j0' ? 'Auj.' : k === 'j1' ? 'Demain' : '+2 j');
</script>

<template>
  <div class="dlc-grid">
    <button
      v-for="k in keys"
      :key="k"
      :class="['dlc-opt', k, { 'is-on': value === k }]"
      @click="emit('select', k)"
    >
      <span class="tag">{{ URG[k].tag }}</span>
      <span class="date">{{ fmtShort(URG[k].date()) }}</span>
      <span class="urg">{{ sub(k) }}</span>
    </button>
  </div>
</template>
