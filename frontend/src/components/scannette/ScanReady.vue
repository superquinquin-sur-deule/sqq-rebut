<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue';
import Icon from '../Icon.vue';

const props = defineProps<{ error?: string | null; busy?: boolean }>();
const emit = defineEmits<{ (e: 'scanned', code: string): void }>();

const code = ref('');
const inp = ref<HTMLInputElement | null>(null);
const bars = [16, 9, 22, 6, 14, 20, 8, 18, 11, 24, 7, 15, 19];

function focus() {
  nextTick(() => inp.value?.focus());
}

// La scannette Zebra agit en "wedge clavier" : elle tape le code dans le champ focalisé
// et envoie Entrée. Le même champ gère donc le scan ET la saisie manuelle.
function submit() {
  const v = code.value.trim();
  code.value = '';
  if (v) emit('scanned', v);
  focus();
}

onMounted(focus);
watch(() => props.error, focus);
defineExpose({ focus });
</script>

<template>
  <div class="scan-ready">
    <div class="scan-target">
      <span class="corner tl" /><span class="corner tr" /><span class="corner bl" /><span class="corner br" />
      <div class="barcode">
        <i v-for="(h, i) in bars" :key="i" :style="{ height: '38px', width: h > 15 ? '3px' : '2px' }" />
      </div>
      <div class="laser" />
    </div>
    <div>
      <h2>Prêt à scanner</h2>
      <p>Vise le code-barres d'un produit frais à relever (DLC à J-2 ou moins).</p>
    </div>
    <form class="scan-manual" @submit.prevent="submit">
      <input
        ref="inp"
        v-model="code"
        inputmode="numeric"
        autocomplete="off"
        placeholder="Code-barres EAN…"
        :disabled="props.busy"
      />
      <button class="btn btn-primary btn-md" type="submit" :disabled="props.busy">
        <Icon name="scan" :size="18" />
      </button>
    </form>
    <div v-if="props.error" class="scan-error">{{ props.error }}</div>
    <div class="scan-trigger-hint">
      <Icon name="barcode" :size="16" />scanne ou tape le code puis <kbd>Entrée</kbd>
    </div>
  </div>
</template>
