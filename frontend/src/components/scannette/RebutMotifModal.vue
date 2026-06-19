<script setup lang="ts">
import { ref } from 'vue';
import Icon from '../Icon.vue';
import MotifGrid from './MotifGrid.vue';
import type { Motif } from '../../api';

const props = defineProps<{ motifs: Motif[]; count: number; busy?: boolean }>();
const emit = defineEmits<{ (e: 'confirm', motifId: number): void; (e: 'close'): void }>();

const motifId = ref<number | null>(null);

function confirm() {
  if (motifId.value != null && !props.busy) emit('confirm', motifId.value);
}
</script>

<template>
  <div class="modal-overlay" @click.self="emit('close')">
    <div class="modal">
      <div class="modal-head">
        <span class="ic"><Icon name="trash" :size="22" /></span>
        <div>
          <h3>Envoyer au rebut</h3>
          <p>{{ count }} produit{{ count > 1 ? 's' : '' }} · choisis un motif unique</p>
        </div>
      </div>
      <div class="modal-body">
        <MotifGrid :motifs="motifs" :value="motifId" @select="motifId = $event" />
      </div>
      <div class="modal-foot">
        <button class="btn btn-ghost btn-md" @click="emit('close')"><Icon name="x" :size="18" />Annuler</button>
        <button class="btn btn-danger btn-md" :disabled="motifId == null || busy" @click="confirm">
          <Icon name="check" :size="18" />Confirmer le rebut
        </button>
      </div>
    </div>
  </div>
</template>
