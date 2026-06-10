<script setup lang="ts">
import Icon from '../Icon.vue';
import { URG, type Urgency } from '../../lib/dates';
import { fmtQty, isWeightUom } from '../../lib/qty';

const props = defineProps<{ name: string; qty: number; uom?: string; urg?: Urgency; motifLabel?: string }>();
const tag = props.urg ? URG[props.urg].tag : (props.motifLabel ?? '');
</script>

<template>
  <div class="flash">
    <div class="ring"><Icon name="check" :size="62" :stroke="3" /></div>
    <h2>Ligne ajoutée</h2>
    <div class="det">
      <div class="nm">{{ name }}</div>
      <div class="qd">
        {{ tag }} ·
        <template v-if="isWeightUom(props.uom)">{{ fmtQty(qty, props.uom) }}</template>
        <template v-else>×{{ qty }} pièce{{ qty > 1 ? 's' : '' }}</template>
      </div>
    </div>
  </div>
</template>
