<script setup lang="ts">
import { computed } from 'vue';
import Icon from '../Icon.vue';
import { isWeightUom } from '../../lib/qty';
import type { Product } from '../../api';

const props = defineProps<{ product: Product }>();
const emit = defineEmits<{ (e: 'again'): void }>();

const stockLabel = computed(() => {
  const q = props.product.qtyAvailable ?? 0;
  if (isWeightUom(props.product.uom)) {
    return `${q.toLocaleString('fr-FR', { maximumFractionDigits: 3 })} kg`;
  }
  return `${q.toLocaleString('fr-FR', { maximumFractionDigits: 0 })} ${props.product.uom}`;
});
</script>

<template>
  <div class="entry">
    <div class="entry-scroll">
      <div class="prod-card">
        <span class="prod-thumb"><Icon name="box" :size="26" /></span>
        <div class="prod-info">
          <div class="prod-name">{{ props.product.name }}</div>
          <div class="prod-meta">
            <span class="prod-rayon">{{ props.product.rayon }}</span>
            <span class="code">{{ props.product.barcode || 'sans code-barres' }}</span>
          </div>
        </div>
      </div>

      <div class="stock-box">
        <span class="stock-box-label">Stock disponible</span>
        <span class="stock-box-value">{{ stockLabel }}</span>
      </div>
    </div>

    <div class="entry-foot">
      <button class="btn btn-primary btn-md btn-block" @click="emit('again')">
        <Icon name="scan" :size="18" />Scanner un autre produit
      </button>
    </div>
  </div>
</template>
