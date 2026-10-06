/** How long a − or + has to be held before it repeats (ms). */
export const REPEAT_AFTER = 350;

/** Delay before the nth repeat of a held − or +: 160 ms, 15 ms faster each time, down to 50. */
export const repeatDelay = (n: number) => Math.max(50, 160 - n * 15);

/** A number widget's value kept inside its bounds (zero and up unless the field says otherwise). */
export const bound = (v: number, min = 0, max = Infinity) => Math.min(max, Math.max(min, v));

/** One step of a number widget from `base`, inside its bounds, rounded so steps of 0.1 or 2.5 do not drift. */
export const stepValue = (base: number, sign: 1 | -1, step: number, min = 0, max = Infinity) =>
  bound(Math.round((base + sign * step) * 10000) / 10000, min, max);
