/**
 * The interval timer as pure state (timer/TimerState.kt, TimerSnapshot.kt, CueGate.kt and the cue rules of
 * TimerService.kt). The app's clock calls `tick`; everything else here is a function of the state.
 */

export const PhaseKind = { WARMUP: 'WARMUP', WORK: 'WORK', REST: 'REST', DONE: 'DONE' } as const;
export type PhaseKindValue = (typeof PhaseKind)[keyof typeof PhaseKind];

export interface Phase {
  kind: PhaseKindValue;
  seconds: number;
  round: number;
  /** Stretch fields: the stretch name, side ("" / "Left" / "Right"), figure key, rep count for a reps stretch. */
  label: string;
  side: string;
  figure: string;
  reps: number;
  stretchId: string;
}

export interface TimerState {
  sessionId: string;
  title: string;
  phases: Phase[];
  index: number;
  remaining: number;
  elapsed: number;
  paused: boolean;
  speed: number;
  /** A stretch routine: the phases carry names and figures, the end writes a StretchSession. */
  stretch: boolean;
  skipped: number;
  awaitingStart: boolean;
  /** Preparation uses real seconds and never counts as exercise time. */
  preparation: number;
  completed: number;
  startedAt: number;
}

/** A phase with the Kotlin defaults for the stretch fields. */
export const phase = (kind: PhaseKindValue, seconds: number, round: number, extra: Partial<Phase> = {}): Phase => ({
  kind, seconds, round, label: '', side: '', figure: '', reps: 0, stretchId: '', ...extra,
});

/** A timer with the Kotlin constructor defaults; `awaitingStart` defaults to `stretch`. */
export function timerState(init: Pick<TimerState, 'sessionId' | 'title' | 'phases'> & Partial<TimerState>): TimerState {
  const stretch = init.stretch ?? false;
  return {
    index: 0, remaining: 0, elapsed: 0, paused: false, speed: 1, skipped: 0, preparation: 0, completed: 0, startedAt: 0,
    awaitingStart: stretch, ...init, stretch,
  };
}

/** Warm-up, then work and rest per round, no rest after the last round. */
export function buildPhases(warmupSec: number, workSec: number, restSec: number, rounds: number): Phase[] {
  const out: Phase[] = [];
  if (warmupSec > 0) out.push(phase(PhaseKind.WARMUP, warmupSec, 0));
  for (let r = 1; r <= rounds; r++) {
    out.push(phase(PhaseKind.WORK, workSec, r));
    if (r < rounds && restSec > 0) out.push(phase(PhaseKind.REST, restSec, r));
  }
  return out;
}

// ---------------------------------------------------------------- derived values

export const currentPhase = (s: TimerState): Phase => s.phases[Math.min(Math.max(s.index, 0), s.phases.length - 1)];
export const isDone = (s: TimerState) => s.phases.length === 0 || s.index >= s.phases.length;
export const roundsOf = (s: TimerState) => s.phases.filter((p) => p.kind === PhaseKind.WORK).length;
export const totalOf = (s: TimerState) => s.phases.reduce((t, p) => t + p.seconds, 0);
const laterSeconds = (s: TimerState) => s.phases.slice(s.index + 1).reduce((t, p) => t + p.seconds, 0);
export const workRemaining = (s: TimerState) => (isDone(s) ? 0 : s.remaining + laterSeconds(s));
export const isPreparing = (s: TimerState) => s.preparation > 0;
export const isActive = (s: TimerState) => !isDone(s) && !s.paused && !s.awaitingStart && !isPreparing(s);

// ---------------------------------------------------------------- transitions

/** Start: a three-second preparation. A second Start changes nothing. */
export function begin(s: TimerState): TimerState {
  if (isDone(s) || !s.awaitingStart) return s;
  return { ...s, awaitingStart: false, paused: false, preparation: 3 };
}

export const pause = (s: TimerState): TimerState => (isDone(s) || s.awaitingStart ? s : { ...s, paused: true });
export const resume = (s: TimerState): TimerState => (s.paused && !s.awaitingStart && !isDone(s) ? { ...s, paused: false } : s);

function advance(s: TimerState, skippedPhase = false): TimerState {
  const next = s.index + 1;
  return {
    ...s,
    index: next,
    remaining: s.phases[next]?.seconds ?? 0,
    preparation: 0,
    awaitingStart: s.stretch && next < s.phases.length,
    paused: s.stretch ? false : s.paused,
    skipped: s.skipped + (skippedPhase ? 1 : 0),
    completed: s.completed + (skippedPhase ? 0 : 1),
  };
}

export const skip = (s: TimerState): TimerState => (isDone(s) ? s : advance(s, true));

/** Runs the clock forward by `seconds` of real time. Preparation is real time; the rest runs at `speed`. A stretch boundary discards overshoot. */
export function tick(s: TimerState, seconds: number): TimerState {
  if (isDone(s) || s.paused || s.awaitingStart || seconds <= 0) return s;
  let delta = seconds;
  let next = s;
  if (isPreparing(next)) {
    const used = Math.min(delta, next.preparation);
    next = { ...next, preparation: Math.max(0, next.preparation - used) };
    delta -= used;
    if (delta <= 0) return next;
  }
  delta *= next.speed;
  while (delta > 0 && !isDone(next)) {
    const used = Math.min(delta, next.remaining);
    next = { ...next, remaining: Math.max(0, next.remaining - used), elapsed: next.elapsed + used };
    delta -= used;
    if (next.remaining > 0) break;
    next = advance(next);
    if (next.stretch) break;
  }
  return next;
}

/** The state when the timer ends: every phase passed, nothing remaining. */
export const finished = (s: TimerState): TimerState => ({ ...s, index: s.phases.length, remaining: 0 });

// ---------------------------------------------------------------- snapshot

/** The timer as it stood at wall-clock `savedAt` (ms). Saved on every transition, so it survives the app being closed. */
export interface TimerSnapshot {
  state: TimerState;
  savedAt: number;
}

/** What the live timer would show at `now`: the same tick runs over the gap. A clock set backwards never rewinds it. */
export const advanced = (snap: TimerSnapshot, now: number) => tick(snap.state, Math.max(0, now - snap.savedAt) / 1000);

/** Too old to trust: more than an hour past the moment the timer would have ended. A finished one never expires. */
export const expired = (snap: TimerSnapshot, now: number) => !isDone(snap.state) && now - snap.savedAt > workRemaining(snap.state) * 1000 + 3_600_000;

export const encodeSnapshot = (snap: TimerSnapshot) => JSON.stringify(snap);

const isPhaseKind = (v: unknown): v is PhaseKindValue => typeof v === 'string' && Object.values<string>(PhaseKind).includes(v);
const num = (v: unknown, fallback: number) => (typeof v === 'number' && Number.isFinite(v) ? v : fallback);
const bool = (v: unknown, fallback: boolean) => (typeof v === 'boolean' ? v : fallback);
const str = (v: unknown, fallback: string) => (typeof v === 'string' ? v : fallback);
const field = (o: object, key: string): unknown => (key in o ? Object.getOwnPropertyDescriptor(o, key)?.value : undefined);

function readPhase(p: unknown): Phase | null {
  if (typeof p !== 'object' || p === null) return null;
  const kind = field(p, 'kind');
  const seconds = field(p, 'seconds');
  const round = field(p, 'round');
  if (!isPhaseKind(kind) || typeof seconds !== 'number' || typeof round !== 'number') return null;
  return phase(kind, seconds, round, {
    label: str(field(p, 'label'), ''),
    side: str(field(p, 'side'), ''),
    figure: str(field(p, 'figure'), ''),
    reps: num(field(p, 'reps'), 0),
    stretchId: str(field(p, 'stretchId'), ''),
  });
}

/** Reads a saved snapshot; null for junk or a timer without phases. Missing fields take their defaults. */
export function decodeSnapshot(text: string | null): TimerSnapshot | null {
  if (!text) return null;
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    return null;
  }
  if (typeof raw !== 'object' || raw === null) return null;
  const state = field(raw, 'state');
  const savedAt = field(raw, 'savedAt');
  if (typeof state !== 'object' || state === null || typeof savedAt !== 'number') return null;
  const sessionId = field(state, 'sessionId');
  const rawPhases = field(state, 'phases');
  if (typeof sessionId !== 'string' || !Array.isArray(rawPhases)) return null;
  const phases = rawPhases.map(readPhase);
  if (phases.length === 0 || phases.some((p) => p == null)) return null;
  const stretch = bool(field(state, 'stretch'), false);
  return {
    savedAt,
    state: timerState({
      sessionId,
      title: str(field(state, 'title'), ''),
      phases: phases.filter((p): p is Phase => p != null),
      index: num(field(state, 'index'), 0),
      remaining: num(field(state, 'remaining'), 0),
      elapsed: num(field(state, 'elapsed'), 0),
      paused: bool(field(state, 'paused'), false),
      speed: num(field(state, 'speed'), 1),
      stretch,
      skipped: num(field(state, 'skipped'), 0),
      awaitingStart: bool(field(state, 'awaitingStart'), stretch),
      preparation: num(field(state, 'preparation'), 0),
      completed: num(field(state, 'completed'), 0),
      startedAt: num(field(state, 'startedAt'), 0),
    }),
  };
}

// ---------------------------------------------------------------- cue gate

/**
 * Lets speech start at most one cue per `gapMs`. A cue that comes sooner waits for the gap, and a newer cue
 * replaces it, so a burst of phase changes speaks only the latest one.
 */
export class CueGate {
  private spokenAt: number | null = null;
  private pending: string | null = null;
  private talking = false;

  constructor(private readonly gapMs = 300) {}

  /** The cue to speak now, or null when it must wait until `dueAt()`. */
  offer(cue: string, nowMs: number): string | null {
    this.pending = cue;
    return this.poll(nowMs);
  }

  poll(nowMs: number): string | null {
    const cue = this.pending;
    if (cue == null) return null;
    if (this.spokenAt != null && nowMs - this.spokenAt < this.gapMs) return null;
    this.pending = null;
    this.spokenAt = nowMs;
    this.talking = true;
    return cue;
  }

  dueAt(): number | null {
    return this.pending == null ? null : (this.spokenAt ?? 0) + this.gapMs;
  }

  /** Drops the waiting cue. True only when a cue was spoken since the last cancel, so speech needs a stop. */
  cancel(): boolean {
    this.pending = null;
    const was = this.talking;
    this.talking = false;
    return was;
  }
}

// ---------------------------------------------------------------- words and cues

/** 75.2 -> "1:16": whole seconds rounded up, as the countdown shows them. */
export function mmss(seconds: number): string {
  const s = Math.max(0, Math.ceil(seconds));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

export const minutesWords = (sec: number) => (sec % 60 === 0 ? `${sec / 60} minute${sec / 60 === 1 ? '' : 's'}` : `${sec} seconds`);

/** What the voice says as a phase starts. */
export function phaseSpeech(s: TimerState): string {
  const p = currentPhase(s);
  if (s.stretch) return p.label + (p.side ? `, ${p.side.toLowerCase()}` : '') + (p.reps > 0 ? `, ${p.reps} reps` : '');
  switch (p.kind) {
    case PhaseKind.WARMUP:
      return `Warm up, ${minutesWords(p.seconds)}`;
    case PhaseKind.WORK:
      return `Round ${p.round} of ${roundsOf(s)}. Go`;
    case PhaseKind.REST:
      return `Rest, ${minutesWords(p.seconds)}`;
    default:
      return 'Done';
  }
}

/** One thing the timer does at a moment: a beep (which also vibrates), a word, or a vibration alone. */
export type Cue = { beep: 'short' | 'long' } | { say: string } | { vibrate: number };

/** Cues between two consecutive states of a running timer (the tick loop of TimerService.kt). */
export function transitionCues(before: TimerState, next: TimerState): Cue[] {
  if (isDone(next)) return isDone(before) ? [] : [{ beep: 'long' }, { say: next.stretch ? 'Routine done' : 'All rounds done' }];
  if (next.index !== before.index) {
    return [{ beep: 'long' }, { say: next.awaitingStart ? `Stretch complete. Next, ${phaseSpeech(next)}` : phaseSpeech(next) }];
  }
  if (isPreparing(before)) {
    if (!isPreparing(next)) return [{ vibrate: 200 }, { say: 'Go' }];
    return Math.ceil(next.preparation) !== Math.ceil(before.preparation) ? [{ beep: 'short' }] : [];
  }
  if (!isActive(next)) return [];
  const whole = Math.ceil(next.remaining);
  if (whole === Math.ceil(before.remaining)) return [];
  const p = currentPhase(next);
  const cues: Cue[] = [];
  if (whole >= 1 && whole <= 3) cues.push({ beep: 'short' });
  if (whole === 10 && p.seconds > 20 && p.reps === 0) cues.push({ say: 'ten seconds' });
  if (p.reps > 0) {
    const per = p.seconds / p.reps;
    const rep = Math.floor((p.seconds - next.remaining) / per);
    const previousRep = Math.floor((currentPhase(before).seconds - before.remaining) / per);
    if (rep >= 1 && rep <= p.reps && rep !== previousRep) cues.push({ say: String(rep) });
  }
  return cues;
}

/** Short name of the current phase: "Warm-up", "Work 2/4", "Rest", "Done"; a stretch shows its name and side. */
export function phaseTitle(s: TimerState): string {
  if (isDone(s)) return 'Done';
  const p = currentPhase(s);
  if (s.stretch) return p.label + (p.side ? ` · ${p.side}` : '');
  if (p.kind === PhaseKind.WORK) return `Work ${p.round}/${roundsOf(s)}`;
  return p.kind === PhaseKind.WARMUP ? 'Warm-up' : 'Rest';
}

/** One alert for a phase change ahead, for when the app is in the background. */
export interface Alert {
  /** Wall-clock ms. */
  at: number;
  title: string;
  body: string;
}

/**
 * The phase changes still ahead of a running cardio timer, as wall-clock alerts: each next phase, then the end.
 * A paused or waiting timer has none; a stretch waits for a tap at each boundary, so it gets only the next one.
 */
export function upcomingAlerts(s: TimerState, now: number): Alert[] {
  if (isDone(s) || s.paused || s.awaitingStart) return [];
  const alerts: Alert[] = [];
  let at = now + (s.preparation + s.remaining / s.speed) * 1000;
  let state = s;
  while (!isDone(state)) {
    state = advanceForAlert(state);
    if (isDone(state)) {
      alerts.push({ at, title: s.title, body: s.stretch ? 'Routine done' : 'All rounds done' });
      break;
    }
    alerts.push({ at, title: `${phaseTitle(state)} · ${s.title}`, body: phaseSpeech(state) });
    if (s.stretch) break;
    at += (currentPhase(state).seconds / s.speed) * 1000;
  }
  return alerts;
}

const advanceForAlert = (s: TimerState): TimerState => ({ ...s, index: s.index + 1, remaining: s.phases[s.index + 1]?.seconds ?? 0, preparation: 0 });

// ---------------------------------------------------------------- the timer screen

/** Background of the timer screen: dark while waiting, counting down, paused or done; else the phase color. */
export type PhaseTone = 'idle' | 'warmup' | 'work' | 'rest';

export function phaseTone(s: TimerState | null): PhaseTone {
  if (!s || isDone(s) || s.awaitingStart || isPreparing(s) || s.paused) return 'idle';
  const kind = currentPhase(s).kind;
  return kind === PhaseKind.WORK ? 'work' : kind === PhaseKind.REST ? 'rest' : 'warmup';
}

/** The word above the countdown. */
export function timerLabel(s: TimerState | null): string {
  if (!s) return 'Starting';
  if (isDone(s)) return 'Done';
  if (s.awaitingStart) return 'Ready';
  if (isPreparing(s)) return 'Get ready';
  if (s.paused) return 'Paused';
  const kind = currentPhase(s).kind;
  return kind === PhaseKind.WARMUP ? 'Warm-up' : kind === PhaseKind.WORK ? 'Work' : 'Rest';
}

/** The big number: the 3-2-1 countdown, else the time left in the phase. */
export const countdownText = (s: TimerState | null) => (s && isPreparing(s) ? String(Math.ceil(s.preparation)) : mmss(s?.remaining ?? 0));

/** "Round 2 of 4", or null for the warm-up and the end. */
export function roundLine(s: TimerState): string | null {
  if (isDone(s) || currentPhase(s).kind === PhaseKind.WARMUP) return null;
  return `Round ${currentPhase(s).round} of ${roundsOf(s)}`;
}

/** "Next: rest 3:00", or "Last one". */
export function nextLine(s: TimerState): string {
  const next = s.phases[s.index + 1];
  if (!next) return 'Last one';
  const word = next.kind === PhaseKind.WORK ? 'work' : next.kind === PhaseKind.REST ? 'rest' : 'warm-up';
  return `Next: ${word} ${mmss(next.seconds)}`;
}

export const elapsedLine = (s: TimerState) => `Elapsed ${mmss(s.elapsed)} of ${mmss(totalOf(s))}`;
