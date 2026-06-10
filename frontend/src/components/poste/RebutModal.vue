<script setup lang="ts">
import { computed } from 'vue';
import Icon from '../Icon.vue';
import { fmtQty, isWeightUom, round3 } from '../../lib/qty';
import type { ReleveLineDto } from '../../api';

const props = withDefaults(defineProps<{ lines: ReleveLineDto[]; kind?: 'dlc' | 'perte' }>(), {
  kind: 'dlc',
});
const emit = defineEmits<{ (e: 'confirm'): void; (e: 'close'): void }>();

const pieces = computed(() => props.lines.filter((l) => !isWeightUom(l.uom)).reduce((s, l) => s + l.qty, 0));
const kilos = computed(() => props.lines.filter((l) => isWeightUom(l.uom)).reduce((s, l) => s + l.qty, 0));
const totalLabel = computed(() => {
  const parts: string[] = [];
  if (pieces.value > 0) parts.push(`${pieces.value} pièce${pieces.value > 1 ? 's' : ''}`);
  if (kilos.value > 0) parts.push(fmtQty(round3(kilos.value), 'kg'));
  return parts.join(' · ') || '0 pièce';
});
const isPerte = computed(() => props.kind === 'perte');
</script>

<template>
  <div class="modal-overlay" @click="emit('close')">
    <div class="modal" @click.stop>
      <div class="modal-head">
        <span class="ic"><Icon name="alert" :size="22" /></span>
        <div>
          <h3>Envoyer au rebut</h3>
          <p v-if="isPerte">{{ props.lines.length }} perte{{ props.lines.length > 1 ? 's' : '' }} · motif choisi à la saisie</p>
          <p v-else>{{ props.lines.length }} produit{{ props.lines.length > 1 ? 's' : '' }} en J-0 · périment aujourd'hui</p>
        </div>
      </div>
      <div class="modal-body">
        <div v-for="l in props.lines" :key="l.id" class="recap-line">
          <span
            class="urgdot"
            :class="isPerte ? 'perte' : 'j0'"
            style="width:10px;height:10px;border-radius:50%"
          />
          <div class="nm">
            {{ l.name }}<small>{{ l.rayon }} · {{ isPerte ? l.motifLabel : l.barcode }}</small>
          </div>
          <span class="qd">{{ fmtQty(l.qty, l.uom) }}</span>
        </div>
        <div class="recap-total">
          <span>Total au rebut</span>
          <b>{{ totalLabel }}</b>
        </div>
        <div class="odoo-note">
          <Icon name="box" :size="18" style="flex-shrink:0;color:var(--sqq-teal)" />
          <div v-if="isPerte">
            Crée <b>{{ props.lines.length }} ligne{{ props.lines.length > 1 ? 's' : '' }} de rebut</b>
            dans Odoo (motif de chaque ligne), valide le mouvement de stock et retire ces lignes du relevé.
          </div>
          <div v-else>
            Crée <b>{{ props.lines.length }} ligne{{ props.lines.length > 1 ? 's' : '' }} de rebut</b>
            dans Odoo (motif : <i>DLC Dépassée</i>), valide le mouvement de stock et retire ces lignes du relevé.
          </div>
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
