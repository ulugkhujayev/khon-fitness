import { fmt } from './format';
import { epley } from './session';
import { ItemType, Session, SetLog, SetStatus } from './types';

/** What the finish sheet shows. Port of Summary / summarize() in SessionScreen.kt. */
export interface Summary {
  done: number;
  total: number;
  volume: number;
  /** Exercises whose best e1RM today beats every earlier done set. */
  bests: number;
}

const e1rm = (l: SetLog) => epley(l.weightKg ?? 0, l.reps ?? 0);

export function summarize(logs: SetLog[], all: SetLog[], sessionId: string): Summary {
  const done = logs.filter((l) => l.status === SetStatus.DONE);
  const volume = done.reduce((v, l) => v + (l.weightKg ?? 0) * (l.reps ?? 0), 0);
  const before = new Map<string, number>();
  for (const l of all) {
    if (l.sessionId === sessionId || l.status !== SetStatus.DONE) continue;
    before.set(l.exerciseId, Math.max(before.get(l.exerciseId) ?? 0, e1rm(l)));
  }
  const today = new Map<string, number>();
  for (const l of done) today.set(l.exerciseId, Math.max(today.get(l.exerciseId) ?? 0, e1rm(l)));
  let bests = 0;
  today.forEach((best, ex) => {
    if (best > (before.get(ex) ?? 0)) bests++;
  });
  return { done: done.length, total: logs.length, volume, bests };
}

/** Set logs grouped by block, in block order. */
export function groupBlocks(logs: SetLog[]): SetLog[][] {
  const map = new Map<number, SetLog[]>();
  for (const l of [...logs].sort((a, b) => a.order - b.order)) {
    const block = map.get(l.blockIndex);
    if (block) block.push(l);
    else map.set(l.blockIndex, [l]);
  }
  return [...map.entries()].sort((a, b) => a[0] - b[0]).map(([, block]) => block);
}

/** The block to open first: the first one with a pending set, else the first. */
export function firstOpenBlock(blocks: SetLog[][]): number {
  const i = blocks.findIndex((b) => b.some((l) => l.status === SetStatus.PENDING));
  return i >= 0 ? i : 0;
}

/** Where focus goes after a check: the next pending set in execution order. */
export function nextPending(logs: SetLog[], after: SetLog): SetLog | undefined {
  return logs
    .filter((l) => l.order > after.order && l.status === SetStatus.PENDING)
    .sort((a, b) => a.order - b.order)[0];
}

/** Exercise names of a block, A first, joined: "Bench Press · Leg Press". */
export function blockTitle(logs: SetLog[]): string {
  const bySlot = new Map<number, string>();
  for (const l of logs) if (!bySlot.has(l.slot)) bySlot.set(l.slot, l.exerciseName);
  return [...bySlot.entries()].sort((a, b) => a[0] - b[0]).map(([, name]) => name).join(' · ');
}

/** Every set of the block is logged or skipped. */
export const isBlockDone = (logs: SetLog[]) => logs.every((l) => l.status !== SetStatus.PENDING);

/** Sets logged as done. */
export const doneCount = (logs: SetLog[]) => logs.filter((l) => l.status === SetStatus.DONE).length;

/** Effort is a whole number 1..10, or empty. */
export const clampEffort = (v: number | null): number | null => (v == null ? null : Math.min(10, Math.max(1, Math.trunc(v))));

/** "A1", "B3". */
export const setTag = (l: Pick<SetLog, 'slot' | 'setIndex'>) => String.fromCharCode(65 + l.slot) + (l.setIndex + 1);

/** A 2.5 step shows one decimal, a whole step none; a weight with a fraction always shows it, so 62.5 never reads 62. */
export const kgDecimals = (stepKg: number, ...weights: (number | null | undefined)[]) =>
  stepKg % 1 !== 0 || weights.some((w) => w != null && w % 1 !== 0) ? 1 : 0;

/** "+2.5 kg" when now beats last time, else null. */
export function gain(prev: number | null | undefined, now: number | null | undefined, decimals: number, unit: string): string | null {
  if (prev == null || now == null || now <= prev) return null;
  return `+${fmt(now - prev, decimals)} ${unit}`;
}

/** Green line on Today while a session runs: "In progress · 34 min", "In progress · 1 h 5 min", no time in its first minute. */
export function progressLine(startedAt: number, now: number): string {
  const m = Math.max(0, Math.floor((now - startedAt) / 60000));
  if (m === 0) return 'In progress';
  return `In progress · ${m >= 60 ? `${Math.floor(m / 60)} h ${m % 60} min` : `${m} min`}`;
}

/** Green line on Today once today's item is done: "Done · 12 sets · 3.4 t" or "Done · 30 min · 5 km". */
export function doneLine(s: Session, logs: SetLog[]): string {
  if (s.itemType === ItemType.PROGRAM) {
    const done = logs.filter((l) => l.sessionId === s.id && l.status === SetStatus.DONE);
    const tonnes = done.reduce((v, l) => v + (l.weightKg ?? 0) * (l.reps ?? 0), 0) / 1000;
    return `Done · ${done.length} ${done.length === 1 ? 'set' : 'sets'} · ${fmt(tonnes, 1)} t`;
  }
  const parts: string[] = [];
  if (s.timeSec != null) parts.push(`${Math.floor(s.timeSec / 60)} min`);
  if (s.distanceM != null) parts.push(`${fmt(s.distanceM / 1000)} km`);
  return ['Done', ...parts].join(' · ');
}
