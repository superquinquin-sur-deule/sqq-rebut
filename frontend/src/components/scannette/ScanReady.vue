<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';
import Icon from '../Icon.vue';

const props = defineProps<{ error?: string | null; busy?: boolean }>();
const emit = defineEmits<{ (e: 'scanned', code: string): void }>();

const code = ref('');
const inp = ref<HTMLInputElement | null>(null);
const bars = [16, 9, 22, 6, 14, 20, 8, 18, 11, 24, 7, 15, 19];

function focus() {
  nextTick(() => inp.value?.focus());
}

function submit() {
  const v = code.value.trim();
  code.value = '';
  if (v) emit('scanned', v);
  focus();
}

// La scannette Zebra agit en "wedge clavier" : elle injecte le code touche par touche
// puis envoie Entrée. On l'écoute au niveau de window (sans champ focalisé, donc sans
// ouvrir le clavier virtuel du téléphone). La saisie manuelle passe, elle, par le <form>
// quand l'utilisateur tape dans l'input — le listener s'efface alors pour ne pas doubler.
let buf = '';
let last = 0;
const GAP_MS = 50; // un wedge enchaîne les frappes < ~50ms ; au-delà on reset le buffer

function onKeydown(e: KeyboardEvent) {
  const a = document.activeElement;
  if (a instanceof HTMLInputElement || a instanceof HTMLTextAreaElement) return;
  if (props.busy) return;

  const now = e.timeStamp;
  if (now - last > GAP_MS) buf = '';
  last = now;

  if (e.key === 'Enter') {
    const v = buf.trim();
    buf = '';
    if (v) emit('scanned', v);
    return;
  }
  if (e.key.length === 1) buf += e.key;
}

onMounted(() => window.addEventListener('keydown', onKeydown));
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown));
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
  </div>
</template>
