<script lang="ts">
import type { Urgency } from '../../lib/dates';

export type ValidatePayload =
  | { type: 'DLC'; dlc: string; qty: number; urg: Urgency }
  | { type: 'PERTE'; motifId: number; motifLabel: string; qty: number };
</script>

<script setup lang="ts">
import { ref, computed } from 'vue';
import Icon from '../Icon.vue';
import DlcGrid from './DlcGrid.vue';
import MotifGrid from './MotifGrid.vue';
import QtyStepper from './QtyStepper.vue';
import WeightInput from './WeightInput.vue';
import { URG, fmtShort, fmtISO, parseISO, urgFromDate, today } from '../../lib/dates';
import { gToKg, kgToG } from '../../lib/qty';
import type { Motif, Product } from '../../api';

const props = defineProps<{ product: Product; mode: 'dlc' | 'perte'; motifs?: Motif[] }>();
const emit = defineEmits<{
  (e: 'validate', payload: ValidatePayload): void;
  (e: 'cancel'): void;
}>();

const urgency = ref<Urgency | null>(null);
const quantity = ref(1);
const byWeight = computed(() => props.product.soldByWeight);
const grams = ref<number | null>(props.product.scannedWeight != null ? kgToG(props.product.scannedWeight) : null);
const exactDate = ref('');
const showDate = ref(false);
const minDate = fmtISO(today());

const motifId = ref<number | null>(null);

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

const qtyForLine = computed(() => (byWeight.value ? (grams.value != null ? gToKg(grams.value) : null) : quantity.value));

const canValidate = computed(
  () =>
    qtyForLine.value != null &&
    qtyForLine.value > 0 &&
    (props.mode === 'dlc' ? !!urgency.value && !!dlcForLine.value : motifId.value != null),
);

function validate() {
  if (!canValidate.value) return;
  if (props.mode === 'dlc') {
    emit('validate', { type: 'DLC', dlc: dlcForLine.value!, qty: qtyForLine.value!, urg: urgency.value! });
  } else {
    const m = (props.motifs ?? []).find((x) => x.id === motifId.value);
    if (!m) return;
    emit('validate', { type: 'PERTE', motifId: m.id, motifLabel: m.label, qty: qtyForLine.value! });
  }
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

      <div v-if="props.mode === 'dlc'">
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

      <div v-else>
        <div class="field-label"><span>Motif de rupture</span></div>
        <MotifGrid :motifs="props.motifs ?? []" :value="motifId" @select="motifId = $event" />
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
