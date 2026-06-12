<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue';
import Icon from '../Icon.vue';
import { debounce } from '../../lib/debounce';
import { useReleveStore } from '../../store/releve';
import type { Product } from '../../api';

const props = defineProps<{ initialQuery?: string }>();
const emit = defineEmits<{ (e: 'pick', product: Product): void; (e: 'close'): void }>();

const store = useReleveStore();

const MIN_QUERY = 3;
const DEBOUNCE_MS = 300;

const query = ref(props.initialQuery ?? '');
const inp = ref<HTMLInputElement | null>(null);
const results = ref<Product[]>([]);
const searching = ref(false);
const searched = ref(false);
const searchError = ref<string | null>(null);

async function search(q: string) {
  searching.value = true;
  searchError.value = null;
  try {
    const found = await store.searchProducts(q);
    if (query.value.trim() !== q) return;
    results.value = found;
    searched.value = true;
  } catch {
    if (query.value.trim() === q) searchError.value = 'Erreur de recherche produit';
  } finally {
    searching.value = false;
  }
}

const runSearch = debounce(search, DEBOUNCE_MS);

watch(query, (v) => {
  const q = v.trim();
  searched.value = false;
  if (q.length < MIN_QUERY) {
    runSearch.cancel();
    results.value = [];
    searchError.value = null;
    return;
  }
  runSearch(q);
});

function submit() {
  const q = query.value.trim();
  if (q.length >= MIN_QUERY) {
    runSearch.cancel();
    search(q);
  }
}

onMounted(() => {
  nextTick(() => inp.value?.focus());
  const q = query.value.trim();
  if (q.length >= MIN_QUERY) search(q);
});
onBeforeUnmount(() => runSearch.cancel());
</script>

<template>
  <div class="search-modal">
    <div class="sc-modebar">
      <button class="sc-back" @click="emit('close')" aria-label="Fermer la recherche">
        <Icon name="arrowLeft" :size="20" />
      </button>
      <span class="sc-mode-label">Recherche produit</span>
    </div>
    <form class="scan-manual search-modal-form" @submit.prevent="submit">
      <input
        ref="inp"
        v-model="query"
        type="search"
        enterkeyhint="search"
        autocomplete="off"
        placeholder="Nom du produit…"
      />
      <button class="btn btn-primary btn-md" type="submit">
        <Icon name="search" :size="18" />
      </button>
    </form>
    <div class="sess-scroll">
      <button v-for="p in results" :key="p.id" type="button" class="sess-line" @click="emit('pick', p)">
        <div class="nm">
          <b>{{ p.name }}</b>
          <span>{{ p.rayon }}<template v-if="p.uom"> · {{ p.price.toFixed(2) }} €/{{ p.uom }}</template></span>
        </div>
        <Icon name="chevR" :size="18" class="sess-chev" />
      </button>
      <div v-if="searching" class="scan-results-hint">Recherche…</div>
      <div v-else-if="query.trim().length < MIN_QUERY" class="scan-results-hint">Tape au moins 3 lettres…</div>
      <div v-else-if="searched && !results.length && !searchError" class="scan-results-hint">Aucun produit trouvé</div>
      <div v-if="searchError" class="scan-error">{{ searchError }}</div>
    </div>
  </div>
</template>
