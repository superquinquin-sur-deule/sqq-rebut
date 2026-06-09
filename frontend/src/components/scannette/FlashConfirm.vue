<script setup lang="ts">
import Icon from '../Icon.vue';
import { URG, type Urgency } from '../../lib/dates';

defineProps<{ name: string; qty: number; urg: Urgency }>();
</script>

<template>
  <div class="flash">
    <div class="ring"><Icon name="check" :size="62" :stroke="3" /></div>
    <h2>Ligne ajoutée</h2>
    <div class="det">
      <div class="nm">{{ name }}</div>
      <div class="qd">{{ URG[urg].tag }} · ×{{ qty }} pièce{{ qty > 1 ? 's' : '' }}</div>
    </div>
  </div>
</template>
