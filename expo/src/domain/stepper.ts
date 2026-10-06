/** How long a − or + has to be held before it repeats (ms). */
export const REPEAT_AFTER = 350;

/** Delay before the nth repeat of a held − or +: 160 ms, 15 ms faster each time, down to 50. */
export const repeatDelay = (n: number) => Math.max(50, 160 - n * 15);

/** One step of a number widget from `base`: never below zero, rounded so steps of 0.1 or 2.5 do not drift. */
export const stepValue = (base: number, sign: 1 | -1, step: number) => Math.max(0, Math.round((base + sign * step) * 10000) / 10000);
