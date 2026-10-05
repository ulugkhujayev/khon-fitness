/** The cardio session form (CardioScreen.kt): which fields it shows, their values, and the computed pace. */
import { CARDIO_FIELDS } from './editors';
import { Exercise, Session, splitList } from './types';

export type NumberField = Exclude<CardioField, 'stroke'>;

export type CardioField = (typeof CARDIO_FIELDS)[number];

/** Pool length when none is set, in metres. */
export const DEFAULT_POOL = 25;
export const STROKES = ['Freestyle', 'Breaststroke', 'Backstroke', 'Butterfly', 'Mixed'];

const isField = (f: string): f is CardioField => CARDIO_FIELDS.some((c) => c === f);

/** The value a session stores for a field, or null. */
function stored(s: Session, f: CardioField): number | string | null {
  switch (f) {
    case 'time':
      return s.timeSec;
    case 'distance':
      return s.distanceM;
    case 'avgHr':
      return s.avgHr;
    case 'watts':
      return s.watts;
    case 'laps':
      return s.laps;
    case 'poolLength':
      return s.poolLength;
    case 'stroke':
      return s.stroke;
  }
}

/**
 * The exercise's fields, then any field this session already stores: a field removed from the exercise later
 * must not hide old data. Without an exercise: time, distance, heart rate.
 */
export function formFields(exercise: Exercise | null, s: Session): CardioField[] {
  const current = exercise ? splitList(exercise.fields).filter(isField) : (['time', 'distance', 'avgHr'] as const).slice();
  return [...current, ...CARDIO_FIELDS.filter((f) => !current.includes(f) && stored(s, f) != null)];
}

/** A number field as the form shows it: minutes, kilometres, or the stored number; pool length defaults to 25. */
export function fieldValue(s: Session, f: NumberField): number | null {
  switch (f) {
    case 'time':
      return s.timeSec == null ? null : s.timeSec / 60;
    case 'distance':
      return s.distanceM == null ? null : s.distanceM / 1000;
    case 'poolLength':
      return s.poolLength ?? DEFAULT_POOL;
    default:
      return s[f];
  }
}

/** The session after the form sets a number field; minutes and kilometres go back to whole seconds and metres. */
export function withFieldValue(s: Session, f: NumberField, v: number | null): Session {
  const whole = (x: number | null, scale = 1) => (x == null ? null : Math.round(x * scale));
  switch (f) {
    case 'time':
      return { ...s, timeSec: whole(v, 60) };
    case 'distance':
      return { ...s, distanceM: whole(v, 1000) };
    default:
      return { ...s, [f]: whole(v) };
  }
}

/** How each number field reads and steps in the form (CardioScreen.kt). */
export const NUMBER_FIELDS: Record<NumberField, { label: string; unit: string; step: number; decimals: number }> = {
  time: { label: 'Time', unit: 'min', step: 1, decimals: 1 },
  distance: { label: 'Distance', unit: 'km', step: 0.1, decimals: 1 },
  avgHr: { label: 'Avg heart rate', unit: 'bpm', step: 1, decimals: 0 },
  watts: { label: 'Avg watts', unit: 'W', step: 5, decimals: 0 },
  laps: { label: 'Laps', unit: '', step: 1, decimals: 0 },
  poolLength: { label: 'Pool length', unit: 'm', step: 5, decimals: 0 },
};

/** What Save writes: the form's fields, and the finish time the first time it is saved. */
export function cardioPatch(draft: Session, now: number) {
  const { date, timeSec, distanceM, avgHr, watts, laps, poolLength, stroke, effort, note } = draft;
  return { date, timeSec, distanceM, avgHr, watts, laps, poolLength, stroke, effort, note, finishedAt: draft.finishedAt ?? now };
}

/** "5:07" from seconds, cut to whole seconds like the Kotlin form. */
const clock = (sec: number) => `${Math.floor(sec / 60)}:${String(Math.floor(sec % 60)).padStart(2, '0')}`;

/** Pace from time and distance, "5:07 /km"; null until both are filled. */
export function runPace(timeSec: number | null, distanceM: number | null): string | null {
  if (!timeSec || !distanceM || timeSec <= 0 || distanceM <= 0) return null;
  return `${clock(timeSec / (distanceM / 1000))} /km`;
}

/** A swim from laps: the distance in metres and the pace per 100 m once there is a time. */
export function swimFromLaps(laps: number | null, poolLength: number | null, timeSec: number | null): { distance: string; pace: string | null } | null {
  if (!laps || laps <= 0) return null;
  const metres = laps * (poolLength ?? DEFAULT_POOL);
  return { distance: `${metres} m`, pace: timeSec && timeSec > 0 ? `${clock(timeSec / (metres / 100))} /100 m` : null };
}
