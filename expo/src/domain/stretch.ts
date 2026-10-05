/**
 * Stretching (data/StretchDemo.kt, data/Stretching.kt, the routine helpers of StretchScreen.kt and the
 * stretch editors): demonstration playback, routine steps, the player's words and the editor rules.
 */
import guides from './stretchGuides.json';
import { isPreparing, mmss, phase, PhaseKind, Phase, TimerState, workRemaining } from './timer';
import { Defaults, RoutineStretch, Stretch, StretchMode, StretchRoutine, StretchSession } from './types';

// ---------------------------------------------------------------- demonstrations

export interface DemoStep {
  title: string;
  instruction: string;
  frame: number;
}

/** One guide: a sprite sheet of `frameCount` frames, `columns` wide, with numbered steps. */
export interface StretchDemo {
  name: string;
  frameCount: number;
  columns: number;
  frameWidth: number;
  frameHeight: number;
  steps: DemoStep[];
  /** First frame of the repeated movement; the frames before it set the pose up. */
  loopStart: number;
  moving: boolean;
  /** Share of each half-cycle a reps stretch pauses at its ends (0 = none). */
  repHoldFraction: number;
}

/** Stable figure keys used by saved stretches and backups, with their names (Figures in Figure.kt). */
export const FIGURES: Record<string, string> = {
  elephant: 'Elephant walks',
  fold: 'Hamstring stretch',
  wgs: "World's greatest stretch",
  needle: 'Thread the needle',
  hipflexor: 'Hip flexor stretch',
  ninety: '90/90 stretch',
  shoulderir: 'Shoulder internal rotation',
  catcow: 'Cat cow',
  plow: 'Plow pose',
};
export const figureName = (key: string) => FIGURES[key] ?? 'No demonstration';

const GUIDES: Record<string, StretchDemo> = guides;
export const guideFor = (figure: string): StretchDemo | null => GUIDES[figure] ?? null;

/** How long the preview shows each step, and how much of that it spends moving into it. */
const SLOT_MS = 2600;
const MOVE_MS = 1600;

/** Before Start: first the setup, then move into each step's position and hold it long enough to inspect. */
export function previewFrame(g: StretchDemo, elapsedMs: number): number {
  const ms = Math.max(0, elapsedMs);
  const slot = Math.floor(ms / SLOT_MS) % g.steps.length;
  if (slot === 0) return g.steps[0].frame;
  const progress = Math.min(1, Math.max(0, (ms % SLOT_MS) / MOVE_MS));
  const from = g.steps[slot - 1].frame;
  return Math.round(from + (g.steps[slot].frame - from) * progress);
}

/** The step whose movement a frame belongs to. */
export function stepAt(g: StretchDemo, frame: number): number {
  const i = g.steps.findIndex((s) => frame <= s.frame);
  return i < 0 ? g.steps.length - 1 : i;
}

/** During the exercise: a hold shows the held pose; reps follow the clock, pausing at each end for `repHoldFraction`. */
export function exerciseFrame(g: StretchDemo, progress: number | null): number {
  if (progress == null) return g.frameCount - 1;
  const p = Math.min(1, Math.max(0, progress));
  const movement = Math.min(1, Math.max(0, (p - g.repHoldFraction) / (1 - 2 * g.repHoldFraction)));
  return Math.round(g.loopStart + movement * (g.frameCount - 1 - g.loopStart));
}

/** During the exercise the tabs are hidden; the instruction follows the step nearest the shown frame. */
export function nearestStep(g: StretchDemo, frame: number): number {
  let best = 0;
  g.steps.forEach((s, i) => {
    if (Math.abs(s.frame - frame) < Math.abs(g.steps[best].frame - frame)) best = i;
  });
  return best;
}

/** Maps display frame times to demo time from one origin, so rounding per frame never accumulates into drift. */
export class DemoClock {
  private origin: number | null = null;
  constructor(private readonly startMs: number) {}

  elapsedMs(frameTimeMs: number): number {
    if (this.origin == null) this.origin = frameTimeMs;
    return this.startMs + Math.floor(frameTimeMs - this.origin);
  }
}

/** Mirrors the instruction for the right side as well as the image, without touching other words. */
export function forDemoSide(text: string, rightSide: boolean): string {
  if (!rightSide) return text;
  return text.replace(/\b(left|right)\b/gi, (word) => {
    const swapped = word.toLowerCase() === 'left' ? 'right' : 'left';
    return word[0] === word[0].toUpperCase() ? swapped[0].toUpperCase() + swapped.slice(1) : swapped;
  });
}

// ---------------------------------------------------------------- routines

/** The player's phases for a routine in order; a sided stretch becomes a left and a right phase. */
export function routineSteps(routineId: string, routineStretches: RoutineStretch[], stretches: Stretch[]): Phase[] {
  return routineStretches
    .filter((rs) => rs.routineId === routineId)
    .sort((a, b) => a.sortOrder - b.sortOrder)
    .flatMap((rs) => {
      const st = stretches.find((s) => s.id === rs.stretchId);
      if (!st) return [];
      const seconds = rs.seconds ?? st.seconds;
      const reps = st.mode === StretchMode.REPS ? st.reps : 0;
      return (st.sided ? ['Left', 'Right'] : ['']).map((side) =>
        phase(PhaseKind.WORK, seconds, 0, { label: st.name, side, figure: st.figure, reps, stretchId: st.id }),
      );
    });
}

export const routineSeconds = (routineId: string, routineStretches: RoutineStretch[], stretches: Stretch[]) =>
  routineSteps(routineId, routineStretches, stretches).reduce((t, p) => t + p.seconds, 0);

/** "12 min · 9 stretches", minutes rounded to the nearest. */
export function routineSummary(routineId: string, routineStretches: RoutineStretch[], stretches: Stretch[]): string {
  const count = routineStretches.filter((rs) => rs.routineId === routineId).length;
  return `${Math.floor((routineSeconds(routineId, routineStretches, stretches) + 30) / 60)} min · ${count} ${count === 1 ? 'stretch' : 'stretches'}`;
}

/** The routine Today offers: the active one, else the first. */
export const activeRoutine = (routines: StretchRoutine[]) => routines.find((r) => r.active) ?? routines[0] ?? null;

/** Library line: "per side · 45 s" or "10 reps". */
export function stretchLine(st: Stretch): string {
  return [st.sided ? 'per side' : null, st.mode === StretchMode.REPS ? `${st.reps} reps` : `${st.seconds} s`].filter(Boolean).join(' · ');
}

// ---------------------------------------------------------------- the player

/** Where a reps stretch is in its current rep: 0 at one end, 1 at the other, back to 0. */
export function repMotion(p: Phase, remaining: number): number {
  const cycle = ((p.seconds - remaining) / (p.seconds / p.reps)) % 1;
  return cycle < 0.5 ? cycle * 2 : (1 - cycle) * 2;
}

/** "Left · Hold", "10 reps", "Hold". */
export const sideLine = (p: Phase) => [p.side || null, p.reps > 0 ? `${p.reps} reps` : 'Hold'].filter(Boolean).join(' · ');

/** The line under the demonstration. */
export function playerStatus(s: TimerState, p: Phase): string {
  if (s.paused) return 'Paused';
  if (s.awaitingStart) return `${s.index === 0 ? 'Ready' : 'Next stretch'} · ${mmss(s.remaining)}`;
  if (isPreparing(s)) return 'Get ready';
  return p.reps > 0 ? 'Move' : 'Hold';
}

/** The bottom button: "Start left side", "Start stretch", "Start next stretch", "Resume", "Pause". */
export function playerAction(s: TimerState, p: Phase): string {
  if (s.awaitingStart && p.side) return `Start ${p.side.toLowerCase()} side`;
  if (s.awaitingStart) return s.index === 0 ? 'Start stretch' : 'Start next stretch';
  return s.paused ? 'Resume' : 'Pause';
}

/** "3 of 12 · 6:30 remaining". */
export const playerProgress = (s: TimerState) => `${s.index + 1} of ${s.phases.length} · ${mmss(workRemaining(s))} remaining`;

/** Stretch timers carry "routineId:runId". */
export const routineOf = (sessionId: string) => sessionId.split(':')[0];

/** A run counts once a stretch was completed or 30 s were stretched; skipping through the list is not a session. */
export function stretchSessionFor(s: TimerState, date: string): StretchSession | null {
  if (!s.stretch || (s.completed === 0 && s.elapsed < 30)) return null;
  return {
    id: s.sessionId,
    routineId: routineOf(s.sessionId),
    routineName: s.title,
    date,
    startedAt: s.startedAt,
    totalSec: Math.floor(s.elapsed),
    completed: s.completed,
    skipped: s.skipped,
  };
}

/** The timer if it is a run of this routine (also while it ends), else null. */
export const routineRun = (s: TimerState | null, routineId: string): TimerState | null =>
  s != null && s.stretch && routineOf(s.sessionId) === routineId ? s : null;

// ---------------------------------------------------------------- editors

const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));

/** Hold or total seconds: 5..600, 45 when cleared. */
export const clampStretchSeconds = (v: number | null) => clamp(Math.trunc(v ?? 45), 5, 600);
/** Rep count: 1..50. */
export const clampReps = (v: number | null) => clamp(Math.trunc(v ?? 1), 1, 50);

/** A new custom stretch: a 45 s hold with the hip flexor guide (StretchEditorScreen's draft). */
export const newStretch = (id: string): Stretch => ({ ...Defaults.stretch, id, name: '', figure: 'hipflexor' });

/** Reps on gives 10 reps when there were none; off goes back to a hold. */
export const withRepsMode = (st: Stretch, on: boolean): Stretch => ({
  ...st,
  mode: on ? StretchMode.REPS : StretchMode.HOLD,
  reps: on && st.reps === 0 ? 10 : st.reps,
});

/** "45 s per side" for a routine row; the routine's own seconds win over the stretch's. */
export const entrySeconds = (rs: RoutineStretch, st: Stretch | undefined) => rs.seconds ?? st?.seconds ?? 45;
