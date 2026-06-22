<script setup lang="ts">
import { computed, ref } from 'vue';
import Icon from '../Icon.vue';
import { URG, fmtShort, parseISO, type Urgency } from '../../lib/dates';
import { fmtQty } from '../../lib/qty';
import type { ReleveLineDto } from '../../api';

const props = defineProps<{ line: ReleveLineDto }>();
const emit = defineEmits<{ (e: 'select'): void; (e: 'remove'): void }>();

const THRESHOLD = 90;

const dx = ref(0);
const dragging = ref(false);
let startX = 0;
let moved = false;

function onDown(e: PointerEvent) {
  if (props.line.sent) return;
  startX = e.clientX;
  moved = false;
  dragging.value = true;
  (e.currentTarget as HTMLElement).setPointerCapture?.(e.pointerId);
}

function onMove(e: PointerEvent) {
  if (!dragging.value) return;
  dx.value = e.clientX - startX;
  if (Math.abs(dx.value) > 4) moved = true;
}

function settle() {
  if (!dragging.value) return;
  dragging.value = false;
  if (Math.abs(dx.value) >= THRESHOLD) {
    emit('remove');
    return;
  }
  dx.value = 0;
}

function onCancel() {
  dragging.value = false;
  dx.value = 0;
}

function onClick() {
  if (moved) return;
  if (!props.line.sent) emit('select');
}

const bgOpacity = computed(() => Math.min(1, Math.abs(dx.value) / THRESHOLD));
</script>

<template>
  <div class="sess-row">
    <div v-if="!line.sent" class="sess-row-bg" :style="{ opacity: bgOpacity }">
      <Icon name="trash" :size="20" />
      <Icon name="trash" :size="20" />
    </div>
    <component
      :is="line.sent ? 'div' : 'button'"
      :type="line.sent ? undefined : 'button'"
      :class="['sess-line', { 'is-sent': line.sent, 'is-swiping': dragging }]"
      :style="line.sent ? undefined : { transform: `translateX(${dx}px)`, touchAction: 'pan-y' }"
      @pointerdown="onDown"
      @pointermove="onMove"
      @pointerup="settle"
      @pointercancel="onCancel"
      @click="onClick"
    >
      <span :class="['urgdot', line.type === 'PERTE' ? 'perte' : line.type === 'REASSORT' ? 'reassort' : line.urgency]" />
      <div class="nm">
        <b>{{ line.name }}</b>
        <span v-if="line.type === 'PERTE'">{{ line.motifLabel ? `${line.motifLabel} · ` : 'Perte · ' }}{{ line.rayon }}</span>
        <span v-else-if="line.type === 'REASSORT'">Réassort · {{ line.rayon }}</span>
        <span v-else>{{ URG[line.urgency as Urgency].tag }} · {{ fmtShort(parseISO(line.dlc as string)) }} · {{ line.rayon }}</span>
      </div>
      <span v-if="line.type !== 'REASSORT'" class="q">{{ fmtQty(line.qty, line.uom) }}</span>
      <Icon v-if="line.sent" name="checkCircle" :size="18" class="sess-sent" />
      <Icon v-else name="chevR" :size="18" class="sess-chev" />
    </component>
  </div>
</template>
