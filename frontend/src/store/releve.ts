import { defineStore } from 'pinia';
import { api, type Motif, type NewLineRequest, type Product, type ReleveLineDto } from '../api';

interface State {
  lines: ReleveLineDto[];
  date: string;
  releveId: number | null;
  motifs: Motif[];
  loading: boolean;
  error: string | null;
}

export const useReleveStore = defineStore('releve', {
  state: (): State => ({
    lines: [],
    date: '',
    releveId: null,
    motifs: [],
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
  },

  actions: {
    async fetch() {
      this.loading = true;
      this.error = null;
      try {
        const r = this.releveId != null ? await api.getReleveById(this.releveId) : await api.getReleve();
        this.date = r.date;
        this.lines = r.lines ?? [];
        if (r.id != null) this.releveId = r.id;
      } catch (e) {
        this.error = errMsg(e);
      } finally {
        this.loading = false;
      }
    },

    /** Cible le relevé du jour (scannette). */
    async fetchToday() {
      this.releveId = null;
      await this.fetch();
    },

    /** Cible un relevé précis par id (poste). */
    async fetchById(id: number) {
      this.releveId = id;
      await this.fetch();
    },

    /** Charge les motifs de rupture une seule fois (cache front). */
    async fetchMotifs() {
      if (this.motifs.length) return;
      this.motifs = await api.getMotifs();
    },

    async lookup(barcode: string): Promise<Product> {
      return api.getProductByBarcode(barcode);
    },

    async searchProducts(q: string): Promise<Product[]> {
      return api.searchProducts({ q });
    },

    async addLine(req: NewLineRequest): Promise<ReleveLineDto> {
      const line = await api.addLine(req);
      await this.fetch();
      return line;
    },

    async updateLine(id: number, patch: { qty?: number }) {
      await api.updateLine(id, patch);
      await this.fetch();
    },

    async setQty(id: number, qty: number) {
      await this.updateLine(id, { qty });
    },

    async remove(id: number) {
      const idx = this.lines.findIndex((l) => l.id === id);
      const backup = idx >= 0 ? this.lines[idx] : null;
      if (idx >= 0) this.lines.splice(idx, 1);
      try {
        await api.deleteLine(id);
      } catch (e) {
        if (backup) this.lines.splice(idx, 0, backup);
        throw e;
      }
      await this.fetch();
    },

    async rebut(motifId: number, lineIds?: number[]) {
      const r = await api.sendRebut({ motifId, lineIds });
      await this.fetch();
      return r;
    },
  },
});

function errMsg(e: unknown): string {
  if (e && typeof e === 'object' && 'message' in e) {
    return String((e as { message: unknown }).message);
  }
  return 'Erreur inconnue';
}
