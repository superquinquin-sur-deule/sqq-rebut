<script setup lang="ts">
import Icon from '../Icon.vue';
import type { Motif } from '../../api';

defineProps<{ motifs: Motif[]; value: number | null }>();
const emit = defineEmits<{ (e: 'select', id: number): void }>();
</script>

<template>
  <div class="motif-grid">
    <button
      v-for="m in motifs"
      :key="m.id"
      :class="['motif-opt', { 'is-on': value === m.id }]"
      @click="emit('select', m.id)"
    >
      <span class="motif-label">{{ m.label }}</span>
      <Icon v-if="value === m.id" name="check" :size="20" />
    </button>
    <p v-if="!motifs.length" class="motif-empty">Aucun motif disponible</p>
  </div>
</template>
