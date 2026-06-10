// Bips de retour (succès / erreur) + vibration (port du prototype).

let actx: AudioContext | null = null;

function ctx(): AudioContext | null {
  try {
    const Ctx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    actx = actx || new Ctx();
    return actx;
  } catch {
    return null; // audio indisponible — on ignore
  }
}

function tone(c: AudioContext, freq: number, start: number, dur: number): void {
  const o = c.createOscillator();
  const g = c.createGain();
  o.connect(g);
  g.connect(c.destination);
  o.type = 'square';
  o.frequency.value = freq;
  const t = c.currentTime + start;
  g.gain.setValueAtTime(0.0001, t);
  g.gain.exponentialRampToValueAtTime(0.12, t + 0.01);
  g.gain.exponentialRampToValueAtTime(0.0001, t + dur);
  o.start(t);
  o.stop(t + dur + 0.01);
}

export function beep(): void {
  const c = ctx();
  if (c) tone(c, 1180, 0, 0.13);
  if (navigator.vibrate) navigator.vibrate(90);
}

// Grave + double bip : nettement distinct du succès pour un code inconnu.
export function errorBeep(): void {
  const c = ctx();
  if (c) {
    tone(c, 320, 0, 0.16);
    tone(c, 320, 0.2, 0.16);
  }
  if (navigator.vibrate) navigator.vibrate([140, 70, 140]);
}
