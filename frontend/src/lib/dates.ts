export const MOIS = ['janv.', 'févr.', 'mars', 'avril', 'mai', 'juin',
  'juill.', 'août', 'sept.', 'oct.', 'nov.', 'déc.'];
export const JOURS = ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi'];

export type Urgency = 'j0' | 'j1' | 'j2';

export function today(): Date {
  const n = new Date();
  return new Date(n.getFullYear(), n.getMonth(), n.getDate());
}

export function addDays(base: Date, n: number): Date {
  const d = new Date(base);
  d.setDate(d.getDate() + n);
  return d;
}

/** "2026-06-09" → Date locale (sans décalage de fuseau). */
export function parseISO(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, (m ?? 1) - 1, d ?? 1);
}

export function fmtShort(d: Date): string {
  return `${d.getDate()} ${MOIS[d.getMonth()]}`;
}

export function fmtLong(d: Date): string {
  return `${JOURS[d.getDay()]} ${d.getDate()} ${MOIS[d.getMonth()]} ${d.getFullYear()}`;
}

export function fmtISO(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function urgFromDate(d: Date): Urgency {
  const day = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const diff = Math.round((day.getTime() - today().getTime()) / 86_400_000);
  if (diff <= 0) return 'j0';
  if (diff === 1) return 'j1';
  return 'j2';
}

export const URG: Record<Urgency, { tag: string; label: string; date: () => Date }> = {
  j0: { tag: 'J-0', label: "Périme aujourd'hui", date: () => today() },
  j1: { tag: 'J-1', label: 'Périme demain', date: () => addDays(today(), 1) },
  j2: { tag: 'J-2', label: 'Périme dans 2 jours', date: () => addDays(today(), 2) },
};
