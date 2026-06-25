export function isWeightUom(uom?: string | null): boolean {
  return !!uom && uom.toLowerCase().startsWith('kg');
}

export function fmtQty(qty: number, uom?: string | null): string {
  if (isWeightUom(uom)) {
    return `${qty.toLocaleString('fr-FR', { maximumFractionDigits: 3 })} kg`;
  }
  return `×${qty}`;
}

/** Quantité de stock (qty_available Odoo), avec son unité : « 2,248 kg » ou « 12 Unité(s) ». */
export function fmtStock(qty: number, uom?: string | null): string {
  return isWeightUom(uom)
    ? `${qty.toLocaleString('fr-FR', { maximumFractionDigits: 3 })} kg`
    : `${qty.toLocaleString('fr-FR', { maximumFractionDigits: 0 })} ${uom ?? ''}`.trim();
}

export const kgToG = (kg: number) => Math.round(kg * 1000);
export const gToKg = (g: number) => g / 1000;
export const round3 = (v: number) => Math.round(v * 1000) / 1000;
