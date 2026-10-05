/** Scale math of the line chart (ui/components/LineChart.kt), kept apart from drawing so it can be tested. */

/** One point of a chart: a day and its value. */
export interface ChartPoint {
  /** yyyy-MM-dd */
  date: string;
  value: number;
}

const STEPS = [0.5, 1, 2, 5, 10, 20, 25, 50, 100];

/** Smallest round step that splits the span into about `target` parts. */
export function niceStep(span: number, target: number): number {
  const raw = span / Math.max(1, target);
  return STEPS.find((s) => s >= raw) ?? STEPS[STEPS.length - 1];
}

export interface Scale {
  yMin: number;
  yMax: number;
  step: number;
  /** Grid values from yMin to yMax. */
  ticks: number[];
}

/** Y range rounded out to the step; a flat series gets one unit of room on each side. */
export function scaleFor(values: number[]): Scale {
  let vmin = Math.min(...values);
  let vmax = Math.max(...values);
  if (vmin === vmax) {
    vmin -= 1;
    vmax += 1;
  }
  const step = niceStep(vmax - vmin, 3);
  const yMin = Math.floor(vmin / step) * step;
  const yMax = Math.ceil(vmax / step) * step;
  const ticks: number[] = [];
  for (let v = yMin; v <= yMax + step / 1000; v += step) ticks.push(Number(v.toFixed(6)));
  return { yMin, yMax, step, ticks };
}

/** Points that get a value label: every point when there are seven or fewer, else the highest and the last. */
export function valueLabelIndices(values: number[]): Set<number> {
  const n = values.length;
  if (n <= 7) return new Set(values.map((_, i) => i));
  let max = 0;
  values.forEach((v, i) => {
    if (v > values[max]) max = i;
  });
  return new Set([max, n - 1]);
}

/** Indices that get a date under the axis: two to four, spread evenly, first and last included. */
export function dateLabelIndices(n: number, plotWidth: number): number[] {
  if (n <= 0) return [];
  if (n === 1) return [0];
  const max = Math.min(4, Math.max(2, Math.floor(plotWidth / 70)));
  const out: number[] = [];
  for (let k = 0; k < max; k++) {
    const i = Math.round((k * (n - 1)) / (max - 1));
    if (!out.includes(i)) out.push(i);
  }
  return out;
}
