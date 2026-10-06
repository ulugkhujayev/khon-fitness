import { monthDay } from './dates';
import { fmt } from './format';
import { Ruler } from './ruler';
import { Bodyweight } from './types';

/** Today's bodyweight ruler: 20 to 250 kg in 0.1 kg ticks 10 pt apart, a label on every kilogram. */
export const BODYWEIGHT_RULER: Ruler = { min: 20, max: 250, step: 0.1, gap: 10, majorEvery: 10 };

/** Where the ruler starts before the first entry: a typical adult weight, so the first pick is a short drag. */
export const DEFAULT_KG = 70;

/** The ruler starts at the latest entry. Bodyweights come sorted by date. */
export const startKg = (bodyweights: Bodyweight[]) => bodyweights.at(-1)?.kg ?? DEFAULT_KG;

/**
 * "+0.2 kg since Oct 5": the picked value against the last entry before today, so an update of today's
 * entry still compares with the day before. Empty when there is no earlier entry.
 */
export function changeSince(bodyweights: Bodyweight[], today: string, kg: number): string {
  const before = bodyweights.filter((b) => b.date < today).at(-1);
  if (!before) return '';
  const diff = Math.round((kg - before.kg) * 10) / 10;
  if (diff === 0) return `Same as ${monthDay(before.date)}`;
  return `${diff > 0 ? '+' : '−'}${fmt(Math.abs(diff))} kg since ${monthDay(before.date)}`;
}

/** Add for a new day, Update when today has an entry (a save replaces it). Off when nothing would change. */
export function entryAction(bodyweights: Bodyweight[], today: string, kg: number): { text: 'Add' | 'Update'; enabled: boolean } {
  const todays = bodyweights.find((b) => b.date === today);
  return { text: todays ? 'Update' : 'Add', enabled: kg > 0 && todays?.kg !== kg };
}
