<script setup lang="ts">
import { ref, computed } from 'vue';
import Icon from '../Icon.vue';
import QtyStepper from './QtyStepper.vue';
import WeightInput from './WeightInput.vue';
import MotifGrid from './MotifGrid.vue';
import { URG, fmtShort, parseISO, type Urgency } from '../../lib/dates';
import { isWeightUom, gToKg, kgToG } from '../../lib/qty';
import type { Motif, ReleveLineDto } from '../../api';

const props = defineProps<{ line: ReleveLineDto; motifs?: Motif[] }>();
const emit = defineEmits<{
  (e: 'save', patch: { qty: number; motifId?: number }): void;
  (e: 'delete'): void;
  (e: 'back'): void;
}>();

const isPerte = computed(() => props.line.type === 'PERTE');
const byWeight = computed(() => isWeightUom(props.line.uom));
const quantity = ref(byWeight.value ? 1 : props.line.qty);
const grams = ref<number | null>(byWeight.value ? kgToG(props.line.qty) : null);
const motifId = ref<number | null>(props.line.motifId ?? null);
const confirming = ref(false);

const qtyForLine = computed(() =>
  byWeight.value ? (grams.value != null ? gToKg(grams.value) : null) : quantity.value,
);
const canSave = computed(
  () => qtyForLine.value != null && qtyForLine.value > 0 && (!isPerte.value || motifId.value != null),
);

function save() {
  if (!canSave.value) return;
  emit('save', { qty: qtyForLine.value!, motifId: isPerte.value ? motifId.value! : undefined });
}
</script>

<template>
  <div class="entry">
    <div class="sc-modebar">
      <button class="sc-back" @click="emit('back')" aria-label="Retour au relevé">
        <Icon name="arrowLeft" :size="20" />
      </button>
      <span class="sc-mode-label">Modifier la ligne</span>
    </div>

    <div class="entry-scroll">
      <div class="prod-card">
        <span class="prod-thumb"><Icon name="box" :size="26" /></span>
        <div class="prod-info">
          <div class="prod-name">{{ props.line.name }}</div>
          <div class="prod-meta">
            <span class="prod-rayon">{{ props.line.rayon }}</span>
            <span class="code">{{ props.line.barcode }}</span>
          </div>
        </div>
      </div>

      <div v-if="isPerte">
        <div class="field-label"><span>Motif de rupture</span></div>
        <MotifGrid :motifs="props.motifs ?? []" :value="motifId" @select="motifId = $event" />
      </div>
      <div v-else class="edit-tag">
        <span :class="['urgdot', props.line.urgency]" />
        <span>{{ URG[props.line.urgency as Urgency].tag }} · {{ fmtShort(parseISO(props.line.dlc as string)) }}</span>
      </div>

      <div>
        <div class="field-label"><span>Quantité concernée</span></div>
        <WeightInput v-if="byWeight" :grams="grams" @update="grams = $event" />
        <QtyStepper v-else :qty="quantity" @update="quantity = $event" />
      </div>
    </div>

    <div v-if="confirming" class="entry-foot">
      <button class="btn btn-ghost btn-md" @click="confirming = false"><Icon name="x" :size="18" />Annuler</button>
      <button class="btn btn-danger btn-md btn-block" @click="emit('delete')">
        <Icon name="trash" :size="18" />Supprimer la ligne
      </button>
    </div>
    <div v-else class="entry-foot">
      <button class="btn btn-ghost btn-md edit-del" @click="confirming = true" aria-label="Supprimer la ligne">
        <Icon name="trash" :size="18" />
      </button>
      <button class="btn btn-primary btn-md btn-block" :disabled="!canSave" @click="save">
        <Icon name="check" :size="18" />Enregistrer
      </button>
    </div>
  </div>
</template>
