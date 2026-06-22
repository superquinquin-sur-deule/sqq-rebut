<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';
import Icon from '../Icon.vue';
import ProductSearchModal from './ProductSearchModal.vue';
import type { Product } from '../../api';

const props = defineProps<{
  mode: 'dlc' | 'perte' | 'stock';
  error?: string | null;
  busy?: boolean;
  last?: { name: string; detail: string } | null;
}>();
const emit = defineEmits<{
  (e: 'scanned', code: string): void;
  (e: 'picked', product: Product): void;
  (e: 'delete'): void;
}>();

const code = ref('');
const inp = ref<HTMLInputElement | null>(null);
const bars = [16, 9, 22, 6, 14, 20, 8, 18, 11, 24, 7, 15, 19];

const showSearch = ref(false);
const searchQuery = ref('');

const isBarcode = (v: string) => /^\d+$/.test(v);

function focus() {
  nextTick(() => inp.value?.focus());
}

function submit() {
  const v = code.value.trim();
  if (!v || isBarcode(v)) {
    code.value = '';
    if (v) emit('scanned', v);
    focus();
    return;
  }
  searchQuery.value = v;
  showSearch.value = true;
}

function closeSearch() {
  showSearch.value = false;
  focus();
}

function onPick(p: Product) {
  showSearch.value = false;
  code.value = '';
  emit('picked', p);
}

let buf = '';
let last = 0;
const GAP_MS = 50;

function onKeydown(e: KeyboardEvent) {
  const a = document.activeElement;
  if (a instanceof HTMLInputElement || a instanceof HTMLTextAreaElement) return;
  if (props.busy || showSearch.value) return;

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
    <div v-if="props.last" class="scan-last">
      <Icon name="checkCircle" :size="22" class="scan-last-ic" />
      <div class="scan-last-info">
        <b>{{ props.last.name }}</b>
        <span>{{ props.last.detail }}</span>
      </div>
      <button class="scan-last-del" type="button" aria-label="Supprimer du relevé" @click="emit('delete')">
        <Icon name="trash" :size="18" />
      </button>
    </div>
    <div>
      <h2>Prêt à scanner</h2>
      <p>
        {{
          props.mode === 'perte'
            ? 'Enchaîne les scans des produits à mettre en perte.'
            : props.mode === 'stock'
              ? 'Enchaîne les scans des produits à réassortir.'
              : "Vise le code-barres d'un produit frais à relever (DLC à J-2 ou moins)."
        }}
      </p>
    </div>
    <form class="scan-manual" @submit.prevent="submit">
      <input
        ref="inp"
        v-model="code"
        inputmode="text"
        enterkeyhint="search"
        autocomplete="off"
        placeholder="Code-barres ou nom du produit…"
        :disabled="props.busy"
      />
      <button class="btn btn-primary btn-md" type="submit" :disabled="props.busy" aria-label="Scanner ou chercher">
        <Icon name="search" :size="18" />
      </button>
    </form>
    <div v-if="props.error" class="scan-error">{{ props.error }}</div>
    <ProductSearchModal
      v-if="showSearch"
      :initial-query="searchQuery"
      @pick="onPick"
      @close="closeSearch"
    />
  </div>
</template>
