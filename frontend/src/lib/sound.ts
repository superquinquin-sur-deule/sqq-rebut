// Bip de confirmation + vibration (port du prototype).

let actx: AudioContext | null = null;

export function beep(): void {
  try {
    const Ctx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    actx = actx || new Ctx();
    const o = actx.createOscillator();
    const g = actx.createGain();
    o.connect(g);
    g.connect(actx.destination);
    o.type = 'square';
    o.frequency.value = 1180;
    g.gain.setValueAtTime(0.0001, actx.currentTime);
    g.gain.exponentialRampToValueAtTime(0.12, actx.currentTime + 0.01);
    g.gain.exponentialRampToValueAtTime(0.0001, actx.currentTime + 0.13);
    o.start();
    o.stop(actx.currentTime + 0.14);
  } catch {
    /* audio indisponible — on ignore */
  }
  if (navigator.vibrate) {
    navigator.vibrate(90);
  }
}
