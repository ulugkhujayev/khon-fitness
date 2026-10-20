import { fmt } from './format';

/**
 * A horizontal ruler picker: ticks `step` apart in value and `gap` points apart on screen, a labelled
 * major tick every `majorEvery` ticks and a half tick between two of them.
 */
export type Ruler = { min: number; max: number; step: number; gap: number; majorEvery: number };

export type TickKind = 'major' | 'half' | 'minor';

/** One list item of the ruler: a labelled major tick and the ticks after it, up to the next label. */
export type RulerSegment = { first: number; count: number; label: string };

/** Number of ticks, both ends included. */
export const tickCount = (r: Ruler) => Math.round((r.max - r.min) / r.step) + 1;

const clampIndex = (r: Ruler, i: number) => Math.min(tickCount(r) - 1, Math.max(0, i));

/** The tick nearest to a value. A value off the ruler lands on its nearer end. */
export const tickIndex = (r: Ruler, value: number) => clampIndex(r, Math.round((value - r.min) / r.step));

/** The value of a tick, rounded so steps of 0.1 do not drift (80.6, not 80.60000000000001). */
export const tickValue = (r: Ruler, index: number) => Math.round((r.min + clampIndex(r, index) * r.step) * 1e6) / 1e6;

/**
 * Space before the first tick and after the last, so the scroll offset that puts tick i under the
 * center line is exactly i × gap: the multiples a scroll view's snap interval stops at.
 */
export const rulerInset = (r: Ruler, width: number) => Math.max(0, width / 2 - r.gap / 2);

/** Scroll offset that puts the value under the center line. */
export const offsetOf = (r: Ruler, value: number) => tickIndex(r, value) * r.gap;

/** The value under the center line at a scroll offset. */
export const valueAt = (r: Ruler, offset: number) => tickValue(r, Math.round(offset / r.gap));

export function tickKind(r: Ruler, index: number): TickKind {
  if (index % r.majorEvery === 0) return 'major';
  return r.majorEvery % 2 === 0 && index % (r.majorEvery / 2) === 0 ? 'half' : 'minor';
}

/** The ruler cut into list items, one per label. The last one may hold fewer ticks. */
export function rulerSegments(r: Ruler): RulerSegment[] {
  const n = tickCount(r);
  return Array.from({ length: Math.ceil(n / r.majorEvery) }, (_, j) => {
    const first = j * r.majorEvery;
    return { first, count: Math.min(r.majorEvery, n - first), label: fmt(tickValue(r, first)) };
  });
}
