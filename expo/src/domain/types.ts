/**
 * Entities, field for field the same as the Kotlin data classes (data/Entities.kt, data/Stretching.kt).
 * Field names are the SQLite column names and the backup JSON keys, so a backup from the Kotlin app
 * imports here unchanged and the other way round.
 */

export const Kind = { STRENGTH: 'strength', CARDIO: 'cardio' } as const;
export const Intensity = { LOW: 'low', MODERATE: 'moderate', HIGH: 'high' } as const;
export const SetStatus = { PENDING: 'pending', DONE: 'done', SKIPPED: 'skipped' } as const;
export const ItemType = { PROGRAM: 'program', CARDIO: 'cardio', REST: 'rest' } as const;
export const StretchMode = { HOLD: 'hold', REPS: 'reps' } as const;

export type KindValue = (typeof Kind)[keyof typeof Kind];
export type ItemTypeValue = (typeof ItemType)[keyof typeof ItemType];
export type SetStatusValue = (typeof SetStatus)[keyof typeof SetStatus];

export const MUSCLE_GROUPS = [
  'chest', 'upper back', 'lats', 'shoulders', 'biceps', 'triceps', 'forearms',
  'core', 'lower back', 'glutes', 'quads', 'hamstrings', 'calves',
] as const;

export interface Exercise {
  id: string;
  name: string;
  kind: string;
  notes: string;
  archived: boolean;
  builtin: boolean;
  // strength
  primaryMuscle: string | null;
  /** Comma separated. */
  secondaryMuscles: string;
  stepKg: number;
  // cardio
  modalityId: string | null;
  intensity: string | null;
  intervals: boolean;
  warmupSec: number;
  workSec: number;
  restSec: number;
  rounds: number;
  /** Comma separated: time,distance,avgHr,watts,laps,poolLength,stroke. */
  fields: string;
}

export interface Modality {
  id: string;
  name: string;
  /** ARGB as in Kotlin, e.g. 0xFFB39DFF. Use argbToHex for display. */
  color: number;
  archived: boolean;
}

export interface Program {
  id: string;
  name: string;
  active: boolean;
  sortOrder: number;
}

export interface Block {
  id: string;
  programId: string;
  sortOrder: number;
  superset: boolean;
}

export interface BlockExercise {
  id: string;
  blockId: string;
  /** 0 = A, 1 = B. */
  slot: number;
  exerciseId: string;
  sets: number;
  minReps: number;
  maxReps: number;
}

export interface Session {
  id: string;
  /** yyyy-MM-dd */
  date: string;
  startedAt: number;
  finishedAt: number | null;
  /** program | cardio */
  itemType: string;
  programId: string | null;
  programName: string | null;
  exerciseId: string | null;
  exerciseName: string | null;
  note: string;
  effort: number | null;
  timeSec: number | null;
  distanceM: number | null;
  avgHr: number | null;
  watts: number | null;
  laps: number | null;
  poolLength: number | null;
  stroke: string | null;
  /** Health Connect / HealthKit record ids when imported, comma separated. */
  sourceId: string | null;
}

/** One set inside a program session. Carries a snapshot of names so history never changes. */
export interface SetLog {
  id: string;
  sessionId: string;
  blockIndex: number;
  blockSuperset: boolean;
  slot: number;
  setIndex: number;
  /** Execution order within the session. */
  order: number;
  exerciseId: string;
  exerciseName: string;
  minReps: number;
  maxReps: number;
  stepKg: number;
  weightKg: number | null;
  reps: number | null;
  status: string;
}

export interface IntervalLog {
  id: string;
  sessionId: string;
  index: number;
  avgHr: number | null;
  watts: number | null;
}

export interface Bodyweight {
  date: string;
  kg: number;
}

/** Template: one row per weekday, 1 = Monday .. 7 = Sunday. */
export interface WeekPlan {
  weekday: number;
  itemType: string;
  itemId: string | null;
}

/** A date that leaves the weekly template. Auto rows only freeze a past date before a template edit. */
export interface DayOverride {
  date: string;
  itemType: string;
  itemId: string | null;
  auto: boolean;
}

export interface Stretch {
  id: string;
  name: string;
  sided: boolean;
  mode: string;
  /** Hold seconds per side, or the total seconds for a reps stretch. */
  seconds: number;
  /** Reps per side when mode is reps. */
  reps: number;
  figure: string;
  muscles: string;
  cue: string;
  link: string;
  builtin: boolean;
  archived: boolean;
}

export interface StretchRoutine {
  id: string;
  name: string;
  active: boolean;
  sortOrder: number;
}

export interface RoutineStretch {
  id: string;
  routineId: string;
  stretchId: string;
  sortOrder: number;
  /** Overrides the stretch default when set. */
  seconds: number | null;
}

export interface StretchSession {
  id: string;
  routineId: string;
  routineName: string;
  date: string;
  startedAt: number;
  totalSec: number;
  completed: number;
  skipped: number;
}

/** Single settings row, id = 1. Minutes since midnight. */
export interface EatingWindow {
  id: number;
  enabled: boolean;
  startMinute: number;
  endMinute: number;
  remindBeforeMin: number;
}

export interface WindowDay {
  date: string;
  /** Epoch millis of a manual open, or null. */
  openedAt: number | null;
  /** Epoch millis of a manual close, or null. */
  closedAt: number | null;
  note: string;
}

/** Defaults of the Kotlin constructors. kotlinx.serialization leaves default values out of the backup JSON, so import fills them back from here. */
export const Defaults = {
  exercise: {
    notes: '', archived: false, builtin: false, primaryMuscle: null, secondaryMuscles: '', stepKg: 2.5,
    modalityId: null, intensity: null, intervals: false, warmupSec: 0, workSec: 0, restSec: 0, rounds: 0, fields: '',
  } satisfies Partial<Exercise>,
  modality: { archived: false } satisfies Partial<Modality>,
  program: { active: false, sortOrder: 0 } satisfies Partial<Program>,
  block: { superset: true } satisfies Partial<Block>,
  blockExercise: { sets: 3, minReps: 8, maxReps: 12 } satisfies Partial<BlockExercise>,
  session: {
    finishedAt: null, programId: null, programName: null, exerciseId: null, exerciseName: null, note: '', effort: null,
    timeSec: null, distanceM: null, avgHr: null, watts: null, laps: null, poolLength: null, stroke: null, sourceId: null,
  } satisfies Partial<Session>,
  setLog: { weightKg: null, reps: null, status: SetStatus.PENDING } satisfies Partial<SetLog>,
  intervalLog: { avgHr: null, watts: null } satisfies Partial<IntervalLog>,
  bodyweight: {} satisfies Partial<Bodyweight>,
  weekPlan: { itemId: null } satisfies Partial<WeekPlan>,
  dayOverride: { itemId: null, auto: false } satisfies Partial<DayOverride>,
  stretch: {
    sided: false, mode: StretchMode.HOLD, seconds: 45, reps: 0, figure: '', muscles: '', cue: '', link: '', builtin: false, archived: false,
  } satisfies Partial<Stretch>,
  stretchRoutine: { active: false, sortOrder: 0 } satisfies Partial<StretchRoutine>,
  routineStretch: { seconds: null } satisfies Partial<RoutineStretch>,
  stretchSession: {} satisfies Partial<StretchSession>,
  eatingWindow: { id: 1, enabled: false, startMinute: 12 * 60, endMinute: 20 * 60, remindBeforeMin: 15 } satisfies EatingWindow,
  windowDay: { openedAt: null, closedAt: null, note: '' } satisfies Partial<WindowDay>,
};

export const splitList = (s: string): string[] => s.split(',').map((x) => x.trim()).filter((x) => x.length > 0);

/** Kotlin stores colors as ARGB longs. 0xFFB39DFF -> "#B39DFF". */
export function argbToHex(argb: number): string {
  return '#' + (argb >>> 0).toString(16).padStart(8, '0').slice(2).toUpperCase();
}

const rgb = (hex: string) => {
  const n = parseInt(hex.slice(1, 7), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
};

/** "#FF9F0A" at 0.2 -> "rgba(255,159,10,0.2)": a soft fill in a modality's color. */
export function hexAlpha(hex: string, alpha: number): string {
  const [r, g, b] = rgb(hex);
  return `rgba(${r},${g},${b},${alpha})`;
}

/** `over` laid on `base` at `alpha`, as one opaque color: a tint on a card that is not transparent. */
export function blend(base: string, over: string, alpha: number): string {
  const a = rgb(base);
  const b = rgb(over);
  return '#' + a.map((v, i) => Math.round(v + (b[i] - v) * alpha).toString(16).padStart(2, '0')).join('').toUpperCase();
}
