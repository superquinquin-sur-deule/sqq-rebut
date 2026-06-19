<script lang="ts">
import type { Urgency } from '../../lib/dates';

export type ValidatePayload = { dlc: string; qty: number; urg: Urgency };
</script>

<script setup lang="ts">
import { ref, computed } from 'vue';
import Icon from '../Icon.vue';
import DlcGrid from './DlcGrid.vue';
import QtyStepper from './QtyStepper.vue';
import WeightInput from './WeightInput.vue';
import { URG, fmtISO } from '../../lib/dates';
import { gToKg, kgToG } from '../../lib/qty';
import type { Product } from '../../api';

const props = defineProps<{ product: Product }>();
const emit = defineEmits<{
  (e: 'validate', payload: ValidatePayload): void;
  (e: 'cancel'): void;
}>();

const urgency = ref<Urgency | null>(null);
const quantity = ref(1);
const byWeight = computed(() => props.product.soldByWeight);
const grams = ref<number | null>(props.product.scannedWeight != null ? kgToG(props.product.scannedWeight) : null);

function pickUrg(k: Urgency) {
  urgency.value = k;
}

const dlcForLine = computed(() => (urgency.value ? fmtISO(URG[urgency.value].date()) : null));
const qtyForLine = computed(() => (byWeight.value ? (grams.value != null ? gToKg(grams.value) : null) : quantity.value));
const canValidate = computed(
  () => qtyForLine.value != null && qtyForLine.value > 0 && !!urgency.value && !!dlcForLine.value,
);

function validate() {
  if (!canValidate.value) return;
  emit('validate', { dlc: dlcForLine.value!, qty: qtyForLine.value!, urg: urgency.value! });
}
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

      <div>
        <div class="field-label"><span>Date limite de consommation</span></div>
        <DlcGrid :value="urgency" @select="pickUrg" />
      </div>

      <div v-if="byWeight">
        <div class="field-label"><span>Poids concerné</span></div>
        <WeightInput :grams="grams" @update="grams = $event" />
      </div>
      <div v-else>
        <div class="field-label"><span>Quantité concernée</span></div>
        <QtyStepper :qty="quantity" @update="quantity = $event" />
      </div>
    </div>

    <div class="entry-foot">
      <button class="btn btn-ghost btn-md" @click="emit('cancel')"><Icon name="x" :size="18" />Annuler</button>
      <button class="btn btn-primary btn-md btn-block" :disabled="!canValidate" @click="validate">
        <Icon name="check" :size="18" />Valider la ligne
      </button>
    </div>
  </div>
</template>
