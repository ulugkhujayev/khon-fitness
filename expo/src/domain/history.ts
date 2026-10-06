/** The History tab's numbers (HistoryScreen.kt): calendar marks, day lines, exercise progress, weekly volume, cardio trends. */
import { ChartPoint } from './chart';
import { addDays, longDate, mondayOf, sameMonth } from './dates';
import { fmt, sets } from './format';
import { PlanItem, PlanRules } from './plan';
import { epley } from './session';
import { Exercise, IntervalLog, ItemType, Kind, Session, SetLog, SetStatus, splitList, StretchSession } from './types';

const finished = (s: Session) => s.finishedAt != null;
const plural = (n: number, one: string, many: string) => `${n} ${n === 1 ? one : many}`;

// ---------------------------------------------------------------- calendar

/** One entry per finished session and stretch run on the date, in start order, at most `max`: the session, or null for a stretch run. */
export function dayDots(date: string, sessions: Session[], stretches: StretchSession[], max = 3): (Session | null)[] {
  const entries: [number, Session | null][] = [
    ...sessions.filter((s) => s.date === date && finished(s)).map((s): [number, Session | null] => [s.startedAt, s]),
    ...stretches.filter((r) => r.date === date).map((r): [number, Session | null] => [r.startedAt, null]),
  ];
  return entries
    .sort((a, b) => a[0] - b[0])
    .slice(0, max)
    .map(([, s]) => s);
}

/** Finished sessions and stretch runs on a date. */
export const dayCount = (date: string, sessions: Session[], stretches: StretchSession[]) =>
  sessions.filter((s) => s.date === date && finished(s)).length + stretches.filter((r) => r.date === date).length;

/** What a screen reader hears for a calendar day: the date, today, sessions or the plan, and a changed plan. */
export function dayLabel(date: string, today: string, count: number, item: PlanItem): string {
  let label = longDate(date);
  if (date === today) label += ', today';
  if (count > 0) label += `, ${plural(count, 'session', 'sessions')}`;
  else label += item.isRest ? ', rest' : `, planned ${item.name}`;
  if (item.overridden) label += ', plan changed';
  return label;
}

/** Index of the calendar cell under a point of a seven-column grid, or -1 outside it. */
export function cellAt(x: number, y: number, width: number, rowHeight: number, rows: number): number {
  if (x < 0 || y < 0 || x >= width || y >= rows * rowHeight) return -1;
  return Math.floor(y / rowHeight) * 7 + Math.floor(x / (width / 7));
}

/** A long press released on its own day opens the plan; released on another day it swaps the two when both lie ahead. */
export function dropAction(from: string, drop: string | null, today: string): 'plan' | 'swap' | null {
  if (drop === from) return 'plan';
  if (drop != null && PlanRules.canSwap(from, drop, today)) return 'swap';
  return null;
}

/** The days around a date that it can swap with, for the screen reader actions. */
export function swapNeighbours(date: string, today: string): { previous: string | null; next: string | null } {
  const previous = addDays(date, -1);
  const next = addDays(date, 1);
  return { previous: PlanRules.canSwap(date, previous, today) ? previous : null, next: PlanRules.canSwap(date, next, today) ? next : null };
}

// ---------------------------------------------------------------- day list

/** Second line of a session row: "12 sets · 3400 kg", "30 min · 5 km", or "in progress". */
export function sessionLine(s: Session, logs: SetLog[]): string {
  if (!finished(s)) return 'in progress';
  if (s.itemType === ItemType.PROGRAM) {
    const done = logs.filter((l) => l.sessionId === s.id && l.status === SetStatus.DONE);
    const volume = done.reduce((v, l) => v + (l.weightKg ?? 0) * (l.reps ?? 0), 0);
    return `${sets(done.length)} · ${fmt(volume, 0)} kg`;
  }
  const parts: string[] = [];
  if (s.timeSec != null) parts.push(s.timeSec % 60 === 0 ? `${s.timeSec / 60} min` : `${fmt(s.timeSec / 60, 1)} min`);
  if (s.distanceM != null) parts.push(`${fmt(s.distanceM / 1000)} km`);
  return parts.join(' · ');
}

/** Second line of a stretch run: "12 min · 9 done · 1 skipped". */
export function stretchLine(r: StretchSession): string {
  const base = `${Math.floor((r.totalSec + 30) / 60)} min · ${r.completed} done`;
  return r.skipped > 0 ? `${base} · ${r.skipped} skipped` : base;
}

/** Stretching in a month: "3 days · avg 12 min", or null when there was none. */
export function stretchMonth(stretches: StretchSession[], month: string): string | null {
  const inMonth = stretches.filter((r) => sameMonth(r.date, month));
  if (inMonth.length === 0) return null;
  const days = new Set(inMonth.map((r) => r.date)).size;
  const avg = Math.floor(inMonth.reduce((t, r) => t + r.totalSec, 0) / inMonth.length / 60);
  return `${plural(days, 'day', 'days')} · avg ${avg} min`;
}

// ---------------------------------------------------------------- exercise progress

export interface ProgressPoint {
  date: string;
  /** Epley estimate of the best set, to the nearest 0.5 kg. */
  e1rm: number;
  /** Top set: the heaviest, then the most reps at that weight. */
  topW: number;
  topR: number;
}

/** Per finished session that did the exercise, oldest first: the estimated 1RM of its best set and its top set. */
export function progressFor(exerciseId: string | null, logs: SetLog[], sessions: Session[]): ProgressPoint[] {
  if (exerciseId == null) return [];
  const byId = new Map(sessions.filter(finished).map((s) => [s.id, s]));
  const bySession = new Map<string, SetLog[]>();
  for (const l of logs) {
    if (l.exerciseId !== exerciseId || l.status !== SetStatus.DONE || !byId.has(l.sessionId)) continue;
    const list = bySession.get(l.sessionId);
    if (list) list.push(l);
    else bySession.set(l.sessionId, [l]);
  }
  const e1rm = (l: SetLog) => epley(l.weightKg ?? 0, l.reps ?? 0);
  const points: (ProgressPoint & { startedAt: number })[] = [];
  bySession.forEach((ls, sessionId) => {
    const s = byId.get(sessionId);
    if (!s) return;
    const best = ls.reduce((a, b) => (e1rm(b) > e1rm(a) ? b : a));
    const top = ls.reduce((a, b) => {
      const [aw, bw] = [a.weightKg ?? 0, b.weightKg ?? 0];
      return bw > aw || (bw === aw && (b.reps ?? 0) > (a.reps ?? 0)) ? b : a;
    });
    points.push({ date: s.date, startedAt: s.startedAt, e1rm: Math.round(e1rm(best) * 2) / 2, topW: top.weightKg ?? 0, topR: top.reps ?? 0 });
  });
  return points.sort((a, b) => a.date.localeCompare(b.date) || a.startedAt - b.startedAt).map(({ date, e1rm: e, topW, topR }) => ({ date, e1rm: e, topW, topR }));
}

/** The exercise the progress card starts on: the first strength exercise with a logged set, else the first one. */
export function defaultExerciseId(exercises: Exercise[], logs: SetLog[]): string | null {
  const logged = new Set(logs.filter((l) => l.status === SetStatus.DONE).map((l) => l.exerciseId));
  const strength = exercises.filter((e) => e.kind === Kind.STRENGTH);
  return (strength.find((e) => logged.has(e.id)) ?? strength[0])?.id ?? null;
}

// ---------------------------------------------------------------- weekly volume

export interface MuscleVolume {
  muscle: string;
  thisWeek: number;
  lastWeek: number;
}

/** Done sets per muscle group this week and last week; the primary muscle counts 1, each secondary 0.5. Biggest first. */
export function muscleVolume(logs: SetLog[], sessions: Session[], exercises: Exercise[], today: string): MuscleVolume[] {
  const monday = mondayOf(today);
  const lastMonday = addDays(monday, -7);
  const dates = new Map(sessions.filter(finished).map((s) => [s.id, s.date]));
  const exById = new Map(exercises.map((e) => [e.id, e]));
  const acc = new Map<string, [number, number]>();
  const add = (muscle: string, week: 0 | 1, amount: number) => {
    const v = acc.get(muscle) ?? [0, 0];
    v[week] += amount;
    acc.set(muscle, v);
  };
  for (const l of logs) {
    const date = dates.get(l.sessionId);
    const ex = exById.get(l.exerciseId);
    if (l.status !== SetStatus.DONE || !date || !ex || date < lastMonday) continue;
    const week = date >= monday ? 0 : 1;
    if (ex.primaryMuscle) add(ex.primaryMuscle, week, 1);
    for (const m of splitList(ex.secondaryMuscles)) add(m, week, 0.5);
  }
  return [...acc.entries()]
    .map(([muscle, [thisWeek, lastWeek]]) => ({ muscle, thisWeek, lastWeek }))
    .sort((a, b) => Math.max(b.thisWeek, b.lastWeek) - Math.max(a.thisWeek, a.lastWeek) || a.muscle.localeCompare(b.muscle));
}

/** A bar above zero never shrinks below this share of the track, so a small count still shows. */
const MIN_SHARE = 0.02;

/**
 * Where the sets-per-muscle bars end, as shares of the biggest count of either week: this week's fill and
 * the mark for last week. Zero stays zero, so nothing shows for a week without sets.
 */
export function volumeShares(volume: MuscleVolume[]): { now: number; last: number }[] {
  const max = Math.max(1, ...volume.map((m) => Math.max(m.thisWeek, m.lastWeek)));
  const share = (v: number) => (v <= 0 ? 0 : Math.max(MIN_SHARE, v / max));
  return volume.map((m) => ({ now: share(m.thisWeek), last: share(m.lastWeek) }));
}

// ---------------------------------------------------------------- cardio trends

export interface CardioTrends {
  /** Finished sessions of the exercise. */
  count: number;
  /** Minutes per km, or per 100 m for swims. */
  pace: ChartPoint[];
  paceUnit: string;
  hr: ChartPoint[];
  /** Average heart rate over the work intervals, for interval exercises. */
  workHr: ChartPoint[];
  /** Minutes; the chart shows it only when there is no pace and no heart rate. */
  time: ChartPoint[];
}

const SWIM = 'swim';
const DEFAULT_POOL = 25;

/** Trend lines of one cardio exercise over its finished sessions, oldest first. */
export function cardioTrends(exercise: Exercise, sessions: Session[], intervals: IntervalLog[]): CardioTrends {
  const swim = exercise.modalityId === SWIM;
  const mine = sessions.filter((s) => s.exerciseId === exercise.id && finished(s)).sort((a, b) => a.date.localeCompare(b.date) || a.startedAt - b.startedAt);
  const pace: ChartPoint[] = [];
  const hr: ChartPoint[] = [];
  const workHr: ChartPoint[] = [];
  const time: ChartPoint[] = [];
  for (const s of mine) {
    const dist = s.distanceM ?? (s.laps != null ? s.laps * (s.poolLength ?? DEFAULT_POOL) : null);
    if (dist != null && dist > 0 && s.timeSec != null && s.timeSec > 0) {
      pace.push({ date: s.date, value: s.timeSec / (dist / (swim ? 100 : 1000)) / 60 });
    }
    if (s.avgHr != null) hr.push({ date: s.date, value: s.avgHr });
    if (exercise.intervals) {
      const beats = intervals.filter((i) => i.sessionId === s.id).flatMap((i) => (i.avgHr != null ? [i.avgHr] : []));
      if (beats.length > 0) workHr.push({ date: s.date, value: beats.reduce((a, b) => a + b, 0) / beats.length });
    }
    if (s.timeSec != null) time.push({ date: s.date, value: s.timeSec / 60 });
  }
  return { count: mine.length, pace, paceUnit: swim ? '/100 m' : '/km', hr, workHr, time };
}

/** 5.12 minutes -> "5:07". */
export function formatPace(minutes: number): string {
  const sec = Math.round(minutes * 60);
  return `${Math.floor(sec / 60)}:${String(sec % 60).padStart(2, '0')}`;
}

/** The cardio exercise the trends start on: the one of the latest finished cardio session, else the first one. */
export function defaultCardioId(exercises: Exercise[], sessions: Session[]): string | null {
  const latest = sessions.filter((s) => s.itemType === ItemType.CARDIO && finished(s)).sort((a, b) => b.date.localeCompare(a.date) || b.startedAt - a.startedAt)[0];
  return latest?.exerciseId ?? exercises.find((e) => e.kind === Kind.CARDIO)?.id ?? null;
}
