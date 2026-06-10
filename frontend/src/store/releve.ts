import { defineStore } from 'pinia';
import { api, type Motif, type NewLineRequest, type Product, type RebutResult, type ReleveLineDto } from '../api';

interface State {
  lines: ReleveLineDto[];
  date: string;
  /** Relevé courant : null = celui du jour (scannette), sinon un relevé précis (poste). */
  releveId: number | null;
  /** Motifs de rupture (Odoo), chargés une fois puis gardés en cache côté front. */
  motifs: Motif[];
  loading: boolean;
  error: string | null;
}

/** État partagé du relevé du soir — lu par la scannette ET le poste responsable. */
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
    j0Active: (s) => s.lines.filter((l) => l.urgency === 'j0' && !l.sent),
    perteActive: (s) => s.lines.filter((l) => l.type === 'PERTE' && !l.sent),
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

    async addLine(req: NewLineRequest): Promise<ReleveLineDto> {
      const line = await api.addLine(req);
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
