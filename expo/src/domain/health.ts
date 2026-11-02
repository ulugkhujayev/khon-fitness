/**
 * Band and watch workouts from Health Connect (Android) or HealthKit (iOS) as import drafts (HealthImport.kt):
 * the platform reads become `HealthWorkout`s, overlapping copies merge, each draft gets a default exercise,
 * and an import builds a finished cardio session with per-interval heart rate.
 */
import { iso, shortDate } from './dates';
import { fmt } from './format';
import { Defaults, Exercise, IntervalLog, ItemType, Session, splitList } from './types';

/** The workout types the app tells apart; both platforms map their own type ids onto these. */
export type WorkoutKind = 'run' | 'swim' | 'bike' | 'row' | 'hiit' | 'walk' | 'hike' | 'elliptical' | 'strength' | 'other';

/** Name and seeded modality per kind (HealthImport.modalityFor). */
export const WORKOUT_KINDS: Record<WorkoutKind, { name: string; modalityId: string | null }> = {
  run: { name: 'Run', modalityId: 'run' },
  swim: { name: 'Swim', modalityId: 'swim' },
  bike: { name: 'Bike', modalityId: 'bike' },
  row: { name: 'Row', modalityId: 'row' },
  hiit: { name: 'HIIT', modalityId: null },
  walk: { name: 'Walk', modalityId: null },
  hike: { name: 'Hike', modalityId: null },
  elliptical: { name: 'Elliptical', modalityId: null },
  strength: { name: 'Strength', modalityId: null },
  other: { name: 'Workout', modalityId: null },
};

/** One workout as a platform reads it, before merging. Times are epoch milliseconds. */
export interface HealthWorkout {
  /** Health Connect record id or HealthKit uuid. */
  sourceId: string;
  start: number;
  end: number;
  kind: WorkoutKind;
  distanceM: number | null;
  /** [time ms, bpm], oldest first. */
  hrSamples: [number, number][];
  /** The writing app's name, e.g. "Mi Fitness". */
  origin: string;
  laps: number | null;
  poolLength: number | null;
}

/** One band session that is not in the app yet (ImportDraft in HealthImport.kt). */
export interface ImportDraft {
  sourceId: string;
  start: number;
  end: number;
  typeName: string;
  modalityId: string | null;
  distanceM: number | null;
  avgHr: number | null;
  hrSamples: [number, number][];
  origins: string[];
  laps: number | null;
  poolLength: number | null;
  /** Every record id merged into this draft, so none of them shows again. */
  mergedIds: string[];
  /** Gym workouts are logged in the app itself; a watch copy never becomes a cardio session. */
  strength: boolean;
}

/** Display names of the apps that usually write workouts (HealthImport.appName). */
export function appName(pkg: string): string {
  switch (pkg) {
    case 'com.xiaomi.wearable':
    case 'com.mi.health':
      return 'Mi Fitness';
    case 'com.sec.android.app.shealth':
      return 'Samsung Health';
    case 'com.google.android.apps.fitness':
      return 'Google Fit';
    case 'com.garmin.android.apps.connectmobile':
      return 'Garmin';
    case 'com.strava':
      return 'Strava';
    default: {
      const last = pkg.slice(pkg.lastIndexOf('.') + 1);
      return last.charAt(0).toUpperCase() + last.slice(1);
    }
  }
}

const METERS_PER: Record<string, number> = { m: 1, meters: 1, km: 1000, kilometers: 1000, mi: 1609.344, miles: 1609.344, yd: 0.9144, ft: 0.3048, feet: 0.3048, in: 0.0254, inches: 0.0254 };

/** A length in metres as either platform hands it over: `{ inMeters }` (Health Connect results) or a value with a unit. */
export function lengthInMeters(length: unknown): number | null {
  if (typeof length !== 'object' || length === null) return null;
  if ('inMeters' in length && typeof length.inMeters === 'number') return length.inMeters;
  if ('unit' in length && typeof length.unit === 'string') {
    const factor = METERS_PER[length.unit];
    const value = 'value' in length ? length.value : 'quantity' in length ? length.quantity : null;
    if (factor !== undefined && typeof value === 'number') return value * factor;
  }
  return null;
}

/** Laps and the pool length as the median lap, from lap lengths in metres (zero-length laps carry no pool length). */
export function lapsOf(lapMeters: (number | null)[]): { laps: number | null; poolLength: number | null } {
  const lengths = lapMeters.filter((m): m is number => m != null && m > 0).sort((a, b) => a - b);
  return { laps: lapMeters.length > 0 ? lapMeters.length : null, poolLength: lengths.length > 0 ? Math.trunc(lengths[Math.trunc(lengths.length / 2)]) : null };
}

export const draftDate = (d: ImportDraft) => iso(new Date(d.start));
export const draftSeconds = (d: ImportDraft) => Math.trunc((d.end - d.start) / 1000);
export const originLabel = (d: ImportDraft) => d.origins.join(' + ');

/** A read workout as a draft: Kotlin truncates the average heart rate and drops a zero distance. */
export function toDraft(w: HealthWorkout): ImportDraft {
  const kind = WORKOUT_KINDS[w.kind];
  const distance = w.distanceM == null ? 0 : Math.trunc(w.distanceM);
  return {
    sourceId: w.sourceId,
    start: w.start,
    end: w.end,
    typeName: kind.name,
    modalityId: kind.modalityId,
    distanceM: distance > 0 ? distance : null,
    avgHr: w.hrSamples.length > 0 ? Math.trunc(w.hrSamples.reduce((t, [, bpm]) => t + bpm, 0) / w.hrSamples.length) : null,
    hrSamples: w.hrSamples,
    origins: [w.origin],
    laps: w.laps,
    poolLength: w.poolLength,
    mergedIds: [w.sourceId],
    strength: w.kind === 'strength',
  };
}

/** The band and the watch both record the same workout: drafts of one modality that overlap in time become one. */
export function mergeOverlaps(drafts: ImportDraft[]): ImportDraft[] {
  const out: ImportDraft[] = [];
  for (const d of drafts) {
    const i = out.findIndex((o) => o.modalityId === d.modalityId && d.start < o.end && d.end > o.start);
    if (i < 0) {
      out.push(d);
      continue;
    }
    const o = out[i];
    // The copy with more heart-rate samples wins; the other fills its gaps.
    const base = d.hrSamples.length > o.hrSamples.length ? d : o;
    const other = base === d ? o : d;
    out[i] = {
      ...base,
      start: Math.min(o.start, d.start),
      end: Math.max(o.end, d.end),
      distanceM: base.distanceM ?? other.distanceM,
      avgHr: base.avgHr ?? other.avgHr,
      laps: base.laps ?? other.laps,
      poolLength: base.poolLength ?? other.poolLength,
      origins: [...new Set([...o.origins, ...d.origins])],
      mergedIds: [...new Set([...o.mergedIds, ...d.mergedIds])],
    };
  }
  return out;
}

/** Workouts not imported yet, newest first, with overlapping copies merged (HealthImport.drafts). */
export function importDrafts(workouts: HealthWorkout[], imported: Set<string>): ImportDraft[] {
  return mergeOverlaps(
    workouts
      .filter((w) => !imported.has(w.sourceId))
      .map(toDraft)
      .sort((a, b) => b.start - a.start),
  );
}

/** How many sessions wait on Today: strength copies are left out before merging (HealthImport.pendingCount). */
export const pendingCount = (workouts: HealthWorkout[], imported: Set<string>) =>
  importDrafts(
    workouts.filter((w) => w.kind !== 'strength'),
    imported,
  ).length;

/** Record ids already imported: a session's `sourceId` holds every id merged into it. */
export const importedIds = (sessions: Pick<Session, 'sourceId'>[]) => new Set(sessions.flatMap((s) => (s.sourceId ? splitList(s.sourceId) : [])));

/**
 * The exercise a draft imports as without asking: the day's planned cardio when its modality matches, else the
 * plainest exercise of that modality. A workout with no modality, or none for it, gets none; the user picks.
 */
export function defaultExercise(modalityId: string | null, planned: Exercise | null, cardio: Exercise[]): Exercise | null {
  if (modalityId == null) return null;
  if (planned && planned.modalityId === modalityId) return planned;
  // Stable sort: plain exercises before interval ones, otherwise in library order.
  return cardio.filter((e) => e.modalityId === modalityId).sort((a, b) => Number(a.intervals) - Number(b.intervals))[0] ?? null;
}

/** Average heart rate inside each work interval, from the session start and the exercise's structure. */
export function intervalHeartRates(d: ImportDraft, ex: Exercise): (number | null)[] {
  if (!ex.intervals || ex.rounds <= 0) return [];
  return Array.from({ length: ex.rounds }, (_, k) => {
    const from = d.start + (ex.warmupSec + k * (ex.workSec + ex.restSec)) * 1000;
    const to = from + ex.workSec * 1000;
    const inside = d.hrSamples.filter(([t]) => t >= from && t < to).map(([, bpm]) => bpm);
    return inside.length > 0 ? Math.trunc(inside.reduce((t, b) => t + b, 0) / inside.length) : null;
  });
}

/** Swimming pool length when the draft has none (KhonRepository.importDraft). */
const DEFAULT_POOL_M = 25;

/**
 * The finished cardio session a draft becomes (KhonRepository.importDraft): laps only when the exercise logs
 * them, from the draft or its distance; the merged record ids as `sourceId`, so they never show again.
 */
export function importedSession(d: ImportDraft, ex: Exercise, sessionId: string, newId: () => string): { session: Session; intervals: IntervalLog[] } {
  const pool = d.poolLength ?? DEFAULT_POOL_M;
  const laps = !splitList(ex.fields).includes('laps') ? null : d.laps ?? (d.distanceM != null ? Math.trunc(d.distanceM / pool) : null);
  const session: Session = {
    ...Defaults.session,
    id: sessionId,
    date: draftDate(d),
    startedAt: d.start,
    finishedAt: d.end,
    itemType: ItemType.CARDIO,
    exerciseId: ex.id,
    exerciseName: ex.name,
    timeSec: draftSeconds(d),
    distanceM: d.distanceM,
    avgHr: d.avgHr,
    laps,
    poolLength: laps != null ? pool : null,
    sourceId: d.mergedIds.join(','),
    note: d.origins.length > 1 ? `From ${originLabel(d)}` : '',
  };
  const intervals = intervalHeartRates(d, ex).map((avgHr, index) => ({ ...Defaults.intervalLog, id: newId(), sessionId, index, avgHr }));
  return { session, intervals };
}

/** Row title: "Mon, Oct 5 · Run · Mi Fitness". */
const draftParts = (d: ImportDraft, hrPrefix: string) =>
  [`${Math.trunc(draftSeconds(d) / 60)} min`, d.distanceM != null ? `${fmt(d.distanceM / 1000)} km` : null, d.avgHr != null ? `${hrPrefix}${d.avgHr} bpm` : null];

/** A row's second line under the workout's type: "Mon, Oct 5 · 32 min · 5.2 km · 141 bpm". */
export const draftStats = (d: ImportDraft) => [shortDate(draftDate(d)), ...draftParts(d, '')].filter((p): p is string => p != null).join(' · ');

/** The picker's line: "32 min · 5.2 km · avg 141 bpm · 40 laps · from Mi Fitness". */
export const draftDetail = (d: ImportDraft) =>
  [...draftParts(d, 'avg '), d.laps != null ? `${d.laps} laps` : null, `from ${originLabel(d)}`].filter((p): p is string => p != null).join(' · ');

/** The import button: "Import 1 session", "Import all 3 sessions", or "Import 2 matched" when some need a pick. */
export function importAllLabel(ready: number, total: number): string {
  if (ready < total) return `Import ${ready} matched`;
  return total === 1 ? 'Import 1 session' : `Import all ${total} sessions`;
}

/** Today's band row as a title over a line: "2 new sessions", "From the band, ready to import". */
export const pendingRow = (n: number) => ({ title: n === 1 ? '1 new session' : `${n} new sessions`, line: 'From the band, ready to import' });

/** Today's row: "1 new session from the band", "3 new sessions from the band". */
export const pendingLine = (n: number) => (n === 1 ? '1 new session from the band' : `${n} new sessions from the band`);
