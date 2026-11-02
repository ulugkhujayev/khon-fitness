/**
 * The running timer (TimerService.kt without the Android service): a cardio interval timer or a stretch routine,
 * one at a time. A 50 ms clock while the app is open, cues and a snapshot on each transition, alerts while in the
 * background, and the result written at the end: the cardio session's time, or a stretch session.
 * Screens read it through `useTimer`.
 */
import { AppState, AppStateStatus, NativeEventSubscription } from 'react-native';
import { cancelAlerts, prepareNotifications, scheduleAlerts } from '@/alerts/notifications';
import { today } from '@/domain/dates';
import { routineOf, stretchSessionFor } from '@/domain/stretch';
import {
  advanced, begin, buildPhases, Cue, expired, finished, isDone, isPreparing, pause, Phase, phaseSpeech, resume, skip, tick, TimerState, timerState,
  transitionCues, upcomingAlerts,
} from '@/domain/timer';
import { Exercise, Session } from '@/domain/types';
import { newId } from '@/db/id';
import * as repo from '@/db/repository';
import { clearSnapshot, loadSnapshot, saveSnapshot } from './snapshots';
import { play, prepareSound, silence } from './sound';

/** How a timer ended, for the screen that shows it. */
export interface FinishedTimer {
  sessionId: string;
  elapsedSec: number;
  early: boolean;
}

export interface TimerView {
  state: TimerState | null;
  /** How the last timer ended; kept until the next one starts, so its screen never starts it again. */
  finished: FinishedTimer | null;
}

const TICK_MS = 50;

let view: TimerView = { state: null, finished: null };
const listeners = new Set<() => void>();
let loop: ReturnType<typeof setInterval> | null = null;
let lastTick = 0;
let ending = false;
let appState: NativeEventSubscription | null = null;

export const subscribe = (listener: () => void) => {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
};
export const getView = () => view;

/** What a snapshot must keep up with; the time left follows from the wall clock. */
const transitionKey = (s: TimerState) => [s.sessionId, s.index, s.paused, s.awaitingStart, isPreparing(s), s.speed].join('|');

function setState(state: TimerState | null, finishedTimer: FinishedTimer | null = view.finished) {
  const before = view.state;
  view = { state, finished: finishedTimer };
  if (state && !isDone(state) && (!before || transitionKey(before) !== transitionKey(state))) saveSnapshot(state);
  listeners.forEach((l) => l());
}

const playAll = (cues: Cue[]) => cues.forEach(play);

// ---------------------------------------------------------------- clock

function step() {
  const before = view.state;
  if (!before) return stopLoop();
  const now = Date.now();
  const next = tick(before, Math.max(0, now - lastTick) / 1000);
  lastTick = now;
  if (next === before) return;
  const cues = transitionCues(before, next);
  setState(next);
  // The end silences any speech first, then plays its own beep and words.
  if (isDone(next)) void end(false, cues);
  else playAll(cues);
}

/** Starts the clock from now. */
function runLoop() {
  lastTick = Date.now();
  startInterval();
}

/** Starts the clock from the last tick, so the first step covers the time it was stopped. */
function startInterval() {
  stopLoop();
  loop = setInterval(step, TICK_MS);
  appState ??= AppState.addEventListener('change', onAppState);
}

function stopLoop() {
  if (loop) clearInterval(loop);
  loop = null;
}

/**
 * Leaving the app: save where the timer is, stop the clock and schedule its phase changes as notifications,
 * so cues never come twice. Coming back: drop them, catch up over the gap, and run the clock again.
 */
function onAppState(status: AppStateStatus) {
  const s = view.state;
  if (!s || isDone(s)) return;
  if (status === 'background') {
    stopLoop();
    saveSnapshot(s);
    void scheduleAlerts('timer', upcomingAlerts(s, Date.now()), { sessionId: s.sessionId, stretch: s.stretch });
  } else if (status === 'active') {
    void cancelAlerts('timer');
    step();
    if (view.state && !isDone(view.state)) startInterval();
  }
}

// ---------------------------------------------------------------- actions

/** Opens the timer for a cardio session, or continues the one already running or saved for it. */
export function startTimer(session: Session, exercise: Exercise) {
  void prepareSound();
  void prepareNotifications();
  if (continueExisting(session.id, false)) return;
  const phases = buildPhases(exercise.warmupSec, exercise.workSec, exercise.restSec, exercise.rounds);
  if (phases.length === 0) return;
  setState(timerState({ sessionId: session.id, title: exercise.name, phases, remaining: phases[0].seconds, awaitingStart: true, startedAt: Date.now() }), null);
  runLoop();
}

/**
 * Opens a stretch routine at its first ready screen, or continues the run of this routine already going or saved.
 * Each run gets its own id, "routineId:runId", which becomes the id of its stretch session.
 */
export function startStretch(routineId: string, title: string, phases: Phase[]) {
  void prepareSound();
  void prepareNotifications();
  const sessionId = `${routineId}:${newId()}`;
  if (continueExisting(sessionId, true) || view.state || phases.length === 0) return;
  setState(timerState({ sessionId, title, phases, remaining: phases[0].seconds, stretch: true, startedAt: Date.now() }), null);
  runLoop();
}

/** A live or saved timer of the same kind for the same session continues; stretch runs match by routine. */
function continueExisting(sessionId: string, stretch: boolean): boolean {
  const same = (id: string) => (stretch ? routineOf(id) === routineOf(sessionId) : id === sessionId);
  const live = view.state;
  if (live && !isDone(live) && live.stretch === stretch && same(live.sessionId)) return true;
  if (live) return false;
  const snap = loadSnapshot();
  const now = Date.now();
  if (!snap || snap.state.stretch !== stretch || !same(snap.state.sessionId) || expired(snap, now)) return false;
  const s = advanced(snap, now);
  setState(s, null);
  runLoop();
  if (isDone(s)) void end(false);
  return true;
}

/** Start timer: the three-second countdown, with its first beep. */
export function beginTimer() {
  const s = view.state;
  if (!s) return;
  const next = begin(s);
  if (next === s) return;
  silence();
  setState(next);
  play({ beep: 'short' });
}

export function pauseTimer() {
  const s = view.state;
  if (!s) return;
  silence();
  setState(pause(s));
}

export function resumeTimer() {
  const s = view.state;
  if (!s) return;
  lastTick = Date.now();
  setState(resume(s));
}

/** Skip: a stretch moves on to the next ready screen, or ends the routine after the last one. */
export function skipTimer() {
  const s = view.state;
  if (!s || isDone(s)) return;
  silence();
  const next = skip(s);
  setState(next);
  if (isDone(next)) void end(false, transitionCues(s, next));
  else play({ say: `Ready for ${phaseSpeech(next)}` });
}

/** Stop: ends the timer early; the time done so far is still written. */
export const stopTimer = () => end(true);

/** Debug speed for testing a whole workout in seconds. */
export function setTimerSpeed(speed: number) {
  const s = view.state;
  if (s) setState({ ...s, speed });
}

/** Writes the result (TimerService.recordTimer), clears the snapshot, and reports how the timer ended. `cues` play after the silence. */
async function end(early: boolean, cues: Cue[] = []) {
  const s = view.state;
  if (ending || !s) return;
  ending = true;
  stopLoop();
  silence();
  playAll(cues);
  const done = finished(s);
  setState(done);
  // A done snapshot until the result is written: if the app dies in between, the restore writes it.
  saveSnapshot(done);
  void cancelAlerts('timer');
  const elapsedSec = Math.floor(s.elapsed);
  try {
    await record(s, elapsedSec);
    clearSnapshot(s.sessionId);
  } finally {
    ending = false;
    if (view.state?.sessionId === s.sessionId) setState(null, { sessionId: s.sessionId, elapsedSec, early });
  }
}

/** A stretch run becomes a stretch session once it counts; a cardio timer writes its time to the session. */
async function record(s: TimerState, elapsedSec: number) {
  if (!s.stretch) {
    await repo.patchSession(s.sessionId, { timeSec: elapsedSec });
    return;
  }
  const run = stretchSessionFor(s, today());
  if (run) await repo.saveStretchSession(run);
}

// ---------------------------------------------------------------- restore

/** A snapshot is stale once its cardio session is gone or saved, or its routine is gone or the run already recorded. */
async function stillOpen(s: TimerState): Promise<boolean> {
  if (!s.stretch) {
    const session = await repo.session(s.sessionId);
    return session != null && session.finishedAt == null;
  }
  const [routines, runs] = await Promise.all([repo.stretchRoutines(), repo.stretchSessions()]);
  return routines.some((r) => r.id === routineOf(s.sessionId)) && !runs.some((r) => r.id === s.sessionId);
}

/**
 * App start: brings back a timer the system closed mid-workout, advanced by the time the app was gone.
 * One that ran out meanwhile writes its result; a stale one (see `stillOpen`, or over an hour past its end) is dropped.
 */
export async function restoreTimer() {
  if (view.state) return;
  const snap = loadSnapshot();
  if (!snap) return;
  const open = await stillOpen(snap.state);
  if (!open || expired(snap, Date.now())) {
    clearSnapshot(snap.state.sessionId);
    return;
  }
  if (view.state) return;
  const now = Date.now();
  void cancelAlerts('timer');
  void prepareSound();
  const s = advanced(snap, now);
  setState(s, null);
  if (isDone(s)) void end(false);
  else runLoop();
}
