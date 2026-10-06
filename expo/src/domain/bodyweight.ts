import { addDays, monthDay } from './dates';
import { fmt } from './format';
import { Ruler } from './ruler';
import { Bodyweight } from './types';

/** Today's bodyweight ruler: 20 to 250 kg in 0.1 kg ticks 10 pt apart, a label on every kilogram. */
export const BODYWEIGHT_RULER: Ruler = { min: 20, max: 250, step: 0.1, gap: 10, majorEvery: 10 };

/** Where the ruler starts before the first entry: a typical adult weight, so the first pick is a short drag. */
export const DEFAULT_KG = 70;

/** The periods the bodyweight card can show, each ending today. */
export const PERIODS = [
  { label: '1W', days: 7 },
  { label: '1M', days: 30 },
  { label: '3M', days: 90 },
  { label: '1Y', days: 365 },
] as const;

/** The card opens on a month, the span Today's chart always had. */
export const DEFAULT_PERIOD = 1;

const round1 = (v: number) => Math.round(v * 10) / 10;

/** "+0.3 kg", "−1.2 kg" (a real minus), "0 kg". */
export function signedKg(v: number): string {
  const r = round1(v);
  return r === 0 ? '0 kg' : `${r > 0 ? '+' : '−'}${fmt(Math.abs(r))} kg`;
}

/**
 * Bodyweight over the last `days` days, today included: the entries to chart, the change from the first
 * entry to the last (null below two entries), and the lowest and highest (null without entries).
 */
export function periodStats(bodyweights: Bodyweight[], today: string, days: number) {
  const from = addDays(today, 1 - days);
  const points = bodyweights.filter((b) => b.date >= from && b.date <= today);
  const kgs = points.map((b) => b.kg);
  return {
    points,
    change: points.length >= 2 ? round1(kgs[kgs.length - 1] - kgs[0]) : null,
    lowest: kgs.length ? Math.min(...kgs) : null,
    highest: kgs.length ? Math.max(...kgs) : null,
  };
}

/** The ruler starts at the latest entry. Bodyweights come sorted by date. */
export const startKg = (bodyweights: Bodyweight[]) => bodyweights.at(-1)?.kg ?? DEFAULT_KG;

/**
 * "+0.2 kg since Oct 5": the picked value against the last entry before today, so an update of today's
 * entry still compares with the day before. Empty when there is no earlier entry.
 */
export function changeSince(bodyweights: Bodyweight[], today: string, kg: number): string {
  const before = bodyweights.filter((b) => b.date < today).at(-1);
  if (!before) return '';
  if (round1(kg - before.kg) === 0) return `Same as ${monthDay(before.date)}`;
  return `${signedKg(kg - before.kg)} since ${monthDay(before.date)}`;
}

/**
 * Add for a new day, Update when today has another value (a save replaces it), Saved (off) when the
 * picked value is today's entry already.
 */
export function entryAction(bodyweights: Bodyweight[], today: string, kg: number): { text: 'Add' | 'Update' | 'Saved'; enabled: boolean } {
  const todays = bodyweights.find((b) => b.date === today);
  if (todays?.kg === kg) return { text: 'Saved', enabled: false };
  return { text: todays ? 'Update' : 'Add', enabled: kg > 0 };
}
