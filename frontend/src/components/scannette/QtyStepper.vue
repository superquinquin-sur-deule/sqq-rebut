<script setup lang="ts">
import Icon from '../Icon.vue';

const props = defineProps<{ qty: number; compact?: boolean }>();
const emit = defineEmits<{ (e: 'update', v: number): void }>();

const set = (v: number) => emit('update', Math.max(1, v));
</script>

<template>
  <div>
    <div class="qty">
      <button aria-label="moins" @click="set(props.qty - 1)"><Icon name="minus" :size="26" /></button>
      <div class="val"><b>{{ props.qty }}</b><span>pièce{{ props.qty > 1 ? 's' : '' }}</span></div>
      <button aria-label="plus" @click="set(props.qty + 1)"><Icon name="plus" :size="26" /></button>
    </div>
    <div v-if="!props.compact" class="qty-chips">
      <button v-for="n in [1, 2, 5, 10]" :key="n" @click="set(n)">{{ n }}</button>
    </div>
  </div>
</template>
