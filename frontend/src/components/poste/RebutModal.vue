<script setup lang="ts">
import { computed } from 'vue';
import Icon from '../Icon.vue';
import type { ReleveLineDto } from '../../api';

const props = defineProps<{ lines: ReleveLineDto[] }>();
const emit = defineEmits<{ (e: 'confirm'): void; (e: 'close'): void }>();

const total = computed(() => props.lines.reduce((s, l) => s + l.qty, 0));
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
          <span class="urgdot j0" style="width:10px;height:10px;border-radius:50%;background:var(--sqq-terracotta)" />
          <div class="nm">{{ l.name }}<small>{{ l.rayon }} · {{ l.barcode }}</small></div>
          <span class="qd">×{{ l.qty }}</span>
        </div>
        <div class="recap-total">
          <span>Total au rebut</span>
          <b>{{ total }} pièce{{ total > 1 ? 's' : '' }}</b>
        </div>
        <div class="odoo-note">
          <Icon name="box" :size="18" style="flex-shrink:0;color:var(--sqq-teal)" />
          <div>
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
