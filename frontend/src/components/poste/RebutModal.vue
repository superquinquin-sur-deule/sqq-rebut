<script setup lang="ts">
import { computed } from 'vue';
import Icon from '../Icon.vue';
import { fmtQty, isWeightUom, round3 } from '../../lib/qty';
import type { ReleveLineDto } from '../../api';

const props = defineProps<{ lines: ReleveLineDto[] }>();
const emit = defineEmits<{ (e: 'confirm'): void; (e: 'close'): void }>();

const pieces = computed(() => props.lines.filter((l) => !isWeightUom(l.uom)).reduce((s, l) => s + l.qty, 0));
const kilos = computed(() => props.lines.filter((l) => isWeightUom(l.uom)).reduce((s, l) => s + l.qty, 0));
const totalLabel = computed(() => {
  const parts: string[] = [];
  if (pieces.value > 0) parts.push(`${pieces.value} pièce${pieces.value > 1 ? 's' : ''}`);
  if (kilos.value > 0) parts.push(fmtQty(round3(kilos.value), 'kg'));
  return parts.join(' · ') || '0 pièce';
});
</script>

<template>
  <div class="modal-overlay" @click="emit('close')">
    <div class="modal" @click.stop>
      <div class="modal-head">
        <span class="ic"><Icon name="alert" :size="22" /></span>
        <div>
          <h3>Envoyer au rebut</h3>
          <p>{{ props.lines.length }} produit{{ props.lines.length > 1 ? 's' : '' }} en J-0 · périment aujourd'hui</p>
        </div>
      </div>
      <div class="modal-body">
        <div v-for="l in props.lines" :key="l.id" class="recap-line">
          <span class="urgdot j0" style="width:10px;height:10px;border-radius:50%" />
          <div class="nm">
            {{ l.name }}<small>{{ l.rayon }} · {{ l.barcode }}</small>
          </div>
          <span class="qd">{{ fmtQty(l.qty, l.uom) }}</span>
        </div>
        <div class="recap-total">
          <span>Total au rebut</span>
          <b>{{ totalLabel }}</b>
        </div>
      </div>
      <div class="modal-foot">
        <button class="btn btn-ghost btn-md" @click="emit('close')">Annuler</button>
        <button class="btn btn-danger btn-md" @click="emit('confirm')">
          <Icon name="upload" :size="18" />Confirmer l'envoi vers Odoo
        </button>
      </div>
    </div>
  </div>
</template>
