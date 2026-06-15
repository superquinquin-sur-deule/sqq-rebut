<script setup lang="ts">
import Icon from '../Icon.vue';

const emit = defineEmits<{ (e: 'select', mode: 'dlc' | 'perte' | 'stock'): void }>();
</script>

<template>
  <div class="mode-menu">
    <p class="mode-intro">Que veux-tu relever&nbsp;?</p>
    <div class="mode-grid">
      <button class="mode-btn dlc" @click="emit('select', 'dlc')">
        <span class="mode-ic"><Icon name="calendar" :size="40" /></span>
        <span class="mode-title">DLC</span>
      </button>
      <button class="mode-btn perte" @click="emit('select', 'perte')">
        <span class="mode-ic"><Icon name="alert" :size="40" /></span>
        <span class="mode-title">Pertes</span>
      </button>
      <button class="mode-btn stock" @click="emit('select', 'stock')">
        <span class="mode-ic"><Icon name="box" :size="40" /></span>
        <span class="mode-title">Stock</span>
      </button>
    </div>
  </div>
</template>
