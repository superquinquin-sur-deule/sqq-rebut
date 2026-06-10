<script setup lang="ts">
import Icon from '../Icon.vue';
import { URG, fmtShort, parseISO, type Urgency } from '../../lib/dates';
import { fmtQty, isWeightUom, round3 } from '../../lib/qty';
import type { ReleveLineDto } from '../../api';

const props = defineProps<{
  lines: ReleveLineDto[];
  sortable?: boolean;
  sortKey?: string;
  sortDir?: 'asc' | 'desc';
}>();
const emit = defineEmits<{
  (e: 'qty', id: number, qty: number): void;
  (e: 'delete', id: number): void;
  (e: 'sort', key: string): void;
}>();

const cols = [
  { id: 'name', label: 'Produit' },
  { id: 'rayon', label: 'Rayon' },
  { id: 'urg', label: 'DLC / Motif' },
  { id: 'qty', label: 'Quantité' },
];
const arrow = (id: string) => (props.sortKey === id ? (props.sortDir === 'asc' ? '▲' : '▼') : '↕');

function onWeight(id: number, e: Event) {
  const input = e.target as HTMLInputElement;
  const v = parseFloat(input.value);
  if (Number.isFinite(v) && v > 0) {
    emit('qty', id, round3(v));
  } else {
    input.value = '';
  }
}
</script>

<template>
  <table class="dk-table">
    <thead>
      <tr v-if="props.sortable">
        <th
          v-for="c in cols"
          :key="c.id"
          class="sortable"
          :class="{ 'is-sorted': props.sortKey === c.id }"
          @click="emit('sort', c.id)"
        >
          {{ c.label }}<span class="sortarrow">{{ arrow(c.id) }}</span>
        </th>
        <th style="text-align:right">Action</th>
      </tr>
      <tr v-else>
        <th>Produit</th><th>Rayon</th><th>DLC / Motif</th><th>Quantité</th><th style="text-align:right">Action</th>
      </tr>
    </thead>
    <tbody>
      <tr v-for="l in props.lines" :key="l.id" :class="{ 'row-sent': l.sent }">
        <td>
          <div class="td-prod">{{ l.name }}</div>
          <div class="td-code">{{ l.barcode }}</div>
        </td>
        <td><span class="td-rayon">{{ l.rayon }}</span></td>
        <td>
          <span v-if="l.type === 'PERTE'" class="td-urg perte"><span class="dot" />{{ l.motifLabel }}</span>
          <template v-else>
            <span class="td-urg" :class="l.urgency"><span class="dot" />{{ URG[l.urgency as Urgency].tag }}</span>
            <div class="td-urg date" style="margin-top:3px">{{ fmtShort(parseISO(l.dlc as string)) }}</div>
          </template>
        </td>
        <td>
          <b v-if="l.sent" style="font-family:var(--font-display);font-size:15px">{{ fmtQty(l.qty, l.uom) }}</b>
          <div v-else-if="isWeightUom(l.uom)" class="dk-qty dk-weight">
            <input type="number" step="0.001" min="0.001" :value="l.qty" @change="onWeight(l.id!, $event)" />
            <span>kg</span>
          </div>
          <div v-else class="dk-qty">
            <button @click="emit('qty', l.id!, Math.max(1, l.qty - 1))"><Icon name="minus" :size="15" /></button>
            <b>{{ l.qty }}</b>
            <button @click="emit('qty', l.id!, l.qty + 1)"><Icon name="plus" :size="15" /></button>
          </div>
        </td>
        <td class="td-actions">
          <span v-if="l.sent" class="sent-badge"><Icon name="checkCircle" :size="15" />Au rebut</span>
          <button v-else class="icon-btn danger" title="Supprimer la ligne" @click="emit('delete', l.id!)">
            <Icon name="trash" :size="17" />
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>
