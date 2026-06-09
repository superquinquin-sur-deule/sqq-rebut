<script setup lang="ts">
import { ref, computed } from 'vue';
import Icon from '../Icon.vue';
import DlcGrid from './DlcGrid.vue';
import QtyStepper from './QtyStepper.vue';
import { URG, fmtShort, fmtISO, parseISO, urgFromDate, today, type Urgency } from '../../lib/dates';
import type { Product } from '../../api';

const props = defineProps<{ product: Product }>();
const emit = defineEmits<{
  (e: 'validate', payload: { dlc: string; qty: number; urg: Urgency }): void;
  (e: 'cancel'): void;
}>();

const urgency = ref<Urgency | null>(null);
const quantity = ref(1);
const exactDate = ref('');
const showDate = ref(false);
const minDate = fmtISO(today());

function pickUrg(k: Urgency) {
  urgency.value = k;
  exactDate.value = '';
  showDate.value = false;
}

function onExact(e: Event) {
  const v = (e.target as HTMLInputElement).value;
  exactDate.value = v;
  if (v) urgency.value = urgFromDate(parseISO(v));
}

const dlcForLine = computed(() => exactDate.value || (urgency.value ? fmtISO(URG[urgency.value].date()) : null));

function validate() {
  if (!urgency.value || !dlcForLine.value) return;
  emit('validate', { dlc: dlcForLine.value, qty: quantity.value, urg: urgency.value });
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
            <span class="code">{{ props.product.barcode }}</span>
          </div>
        </div>
      </div>

      <div>
        <div class="field-label"><span>Date limite de consommation</span></div>
        <DlcGrid :value="urgency" @select="pickUrg" />
        <div class="exact-date">
          <button v-if="!showDate" :class="{ 'has-date': exactDate }" @click="showDate = true">
            <Icon name="calendar" :size="16" />
            {{ exactDate ? `DLC : ${fmtShort(parseISO(exactDate))}` : 'Choisir une date exacte' }}
          </button>
          <input
            v-else
            type="date"
            :value="exactDate"
            :min="minDate"
            @change="onExact"
            @blur="showDate = false"
          />
          <span v-if="urgency" class="dlc-readout">{{ URG[urgency].tag }} — {{ URG[urgency].label.toLowerCase() }}</span>
        </div>
      </div>

      <div>
        <div class="field-label"><span>Quantité concernée</span></div>
        <QtyStepper :qty="quantity" @update="quantity = $event" />
      </div>
    </div>

    <div class="entry-foot">
      <button class="btn btn-ghost btn-md" @click="emit('cancel')"><Icon name="x" :size="18" />Annuler</button>
      <button class="btn btn-primary btn-md btn-block" :disabled="!urgency" @click="validate">
        <Icon name="check" :size="18" />Valider la ligne
      </button>
    </div>
  </div>
</template>
