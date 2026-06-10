<script setup lang="ts">
const props = defineProps<{ grams: number | null }>();
const emit = defineEmits<{ (e: 'update', v: number | null): void }>();

function onInput(e: Event) {
  const v = Math.round(parseFloat((e.target as HTMLInputElement).value));
  emit('update', Number.isFinite(v) && v > 0 ? v : null);
}
</script>

<template>
  <div>
    <div class="qty weight">
      <div class="val">
        <input
          type="number"
          inputmode="numeric"
          min="1"
          step="1"
          placeholder="—"
          :value="props.grams ?? ''"
          @input="onInput"
        />
        <span>grammes</span>
      </div>
    </div>
    <div class="qty-chips">
      <button v-for="g in [100, 250, 500, 1000]" :key="g" @click="emit('update', g)">{{ g }} g</button>
    </div>
  </div>
</template>
