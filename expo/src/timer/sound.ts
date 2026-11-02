/** Cue output: the two beeps, speech through a gate that keeps rapid cues from piling up, and vibration. */
import { AudioPlayer, createAudioPlayer, setAudioModeAsync } from 'expo-audio';
import * as Speech from 'expo-speech';
import { Vibration } from 'react-native';
import { Cue, CueGate } from '@/domain/timer';

/** A beep also vibrates, as in TimerService.kt: 60 ms for a short one, 500 ms for a long one. */
const BEEP_VIBRATION = { short: 60, long: 500 } as const;

let players: Record<'short' | 'long', AudioPlayer> | null = null;
const gate = new CueGate();
let waiting: ReturnType<typeof setTimeout> | null = null;

/** Loads the beeps once. Beeps play in silent mode and mix with the user's music instead of stopping it. */
export async function prepareSound() {
  if (players) return;
  await setAudioModeAsync({ playsInSilentMode: true, interruptionMode: 'mixWithOthers', shouldPlayInBackground: false });
  players = {
    short: createAudioPlayer(require('../../assets/sounds/beep-short.wav')),
    long: createAudioPlayer(require('../../assets/sounds/beep-long.wav')),
  };
}

export function releaseSound() {
  players?.short.remove();
  players?.long.remove();
  players = null;
}

export function play(cue: Cue) {
  if ('beep' in cue) {
    const player = players?.[cue.beep];
    player?.seekTo(0);
    player?.play();
    Vibration.vibrate(BEEP_VIBRATION[cue.beep]);
  } else if ('say' in cue) {
    say(cue.say);
  } else {
    Vibration.vibrate(cue.vibrate);
  }
}

/** Speaks now, or after the gap when another cue just started; a newer cue replaces a waiting one. */
function say(text: string) {
  const now = Date.now();
  const ready = gate.offer(text, now);
  if (ready) return speak(ready);
  const due = gate.dueAt();
  if (waiting || due == null) return;
  waiting = setTimeout(() => {
    waiting = null;
    const later = gate.poll(Date.now());
    if (later) speak(later);
  }, Math.max(0, due - now));
}

/** A new cue cuts the one still speaking, like QUEUE_FLUSH on Android. */
function speak(text: string) {
  Speech.stop();
  Speech.speak(text, { language: 'en-US' });
}

/** Stops speech and drops a waiting cue: on pause, stop and the end. */
export function silence() {
  if (gate.cancel()) Speech.stop();
  if (waiting) clearTimeout(waiting);
  waiting = null;
}
