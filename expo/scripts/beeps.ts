/**
 * Writes the timer beeps as 16-bit mono WAV files: `bun scripts/beeps.ts`.
 * Short: the three beeps before a phase change (150 ms). Long: the phase change and the end (900 ms).
 * The Kotlin app used Android ToneGenerator; these stand in for it on both platforms.
 */
const RATE = 44_100;
const FADE_SEC = 0.01;

function beep(frequency: number, seconds: number, volume = 0.6): Uint8Array {
  const samples = Math.round(RATE * seconds);
  const fade = Math.round(RATE * FADE_SEC);
  const data = new DataView(new ArrayBuffer(44 + samples * 2));
  const text = (offset: number, s: string) => [...s].forEach((c, i) => data.setUint8(offset + i, c.charCodeAt(0)));
  text(0, 'RIFF');
  data.setUint32(4, 36 + samples * 2, true);
  text(8, 'WAVE');
  text(12, 'fmt ');
  data.setUint32(16, 16, true);
  data.setUint16(20, 1, true); // PCM
  data.setUint16(22, 1, true); // mono
  data.setUint32(24, RATE, true);
  data.setUint32(28, RATE * 2, true);
  data.setUint16(32, 2, true);
  data.setUint16(34, 16, true);
  text(36, 'data');
  data.setUint32(40, samples * 2, true);
  for (let i = 0; i < samples; i++) {
    // Short ramps at both ends keep the speaker from clicking.
    const envelope = Math.min(1, i / fade, (samples - 1 - i) / fade);
    data.setInt16(44 + i * 2, Math.round(Math.sin((2 * Math.PI * frequency * i) / RATE) * envelope * volume * 32767), true);
  }
  return new Uint8Array(data.buffer);
}

await Bun.write('assets/sounds/beep-short.wav', beep(1000, 0.15));
await Bun.write('assets/sounds/beep-long.wav', beep(740, 0.9));
console.log('assets/sounds: beep-short.wav, beep-long.wav');
