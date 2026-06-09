import { defineStore } from 'pinia';
import { api, type Product, type RebutResult, type ReleveLineDto } from '../api';

interface State {
  lines: ReleveLineDto[];
  date: string;
  loading: boolean;
  error: string | null;
}

/** État partagé du relevé du soir — lu par la scannette ET le poste responsable. */
export const useReleveStore = defineStore('releve', {
  state: (): State => ({
    lines: [],
    date: '',
    loading: false,
    error: null,
  }),

  getters: {
    counts: (s) => ({
      j0: s.lines.filter((l) => l.urgency === 'j0' && !l.sent).length,
      j1: s.lines.filter((l) => l.urgency === 'j1' && !l.sent).length,
      j2: s.lines.filter((l) => l.urgency === 'j2' && !l.sent).length,
      total: s.lines.length,
    }),
    rayons: (s) => [...new Set(s.lines.map((l) => l.rayon).filter(Boolean))] as string[],
    j0Active: (s) => s.lines.filter((l) => l.urgency === 'j0' && !l.sent),
  },

  actions: {
    async fetch() {
      this.loading = true;
      this.error = null;
      try {
        const r = await api.getReleve();
        this.date = r.date;
        this.lines = r.lines ?? [];
      } catch (e) {
        this.error = errMsg(e);
      } finally {
        this.loading = false;
      }
    },

    async lookup(barcode: string): Promise<Product> {
      return api.getProductByBarcode(barcode);
    },

    async addLine(barcode: string, dlc: string, qty: number): Promise<ReleveLineDto> {
      const line = await api.addLine({ barcode, dlc, qty });
      await this.fetch();
      return line;
    },

    async setQty(id: number, qty: number) {
      await api.updateLineQty(id, { qty });
      await this.fetch();
    },

    async remove(id: number) {
      await api.deleteLine(id);
      await this.fetch();
    },

    async sendRebut(lineIds?: number[]): Promise<RebutResult> {
      const res = await api.sendRebut({ lineIds });
      await this.fetch();
      return res;
    },
  },
});

function errMsg(e: unknown): string {
  if (e && typeof e === 'object' && 'message' in e) {
    return String((e as { message: unknown }).message);
  }
  return 'Erreur inconnue';
}
