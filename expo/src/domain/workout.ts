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
    if (!map.has(l.blockIndex)) map.set(l.blockIndex, []);
    map.get(l.blockIndex)!.push(l);
  }
  return [...map.keys()].sort((a, b) => a - b).map((k) => map.get(k)!);
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
