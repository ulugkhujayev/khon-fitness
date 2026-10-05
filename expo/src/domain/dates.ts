/** Local calendar dates as "yyyy-MM-dd", the format Kotlin's ISO_LOCAL_DATE writes. Never UTC. */

const pad = (n: number) => String(n).padStart(2, '0');

export function iso(d: Date): string {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function parseIso(s: string): Date {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function today(now: Date = new Date()): string {
  return iso(now);
}

export function addDays(date: string, days: number): string {
  const d = parseIso(date);
  d.setDate(d.getDate() + days);
  return iso(d);
}

export function addYears(date: string, years: number): string {
  const d = parseIso(date);
  d.setFullYear(d.getFullYear() + years);
  return iso(d);
}

/** ISO weekday, 1 = Monday .. 7 = Sunday, like java.time.DayOfWeek.value. */
export function weekday(date: string): number {
  const js = parseIso(date).getDay();
  return js === 0 ? 7 : js;
}

/** Monday of the week that holds the date. */
export function mondayOf(date: string): string {
  return addDays(date, 1 - weekday(date));
}

const WEEKDAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'];
const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

/** 1 = Monday .. 7 = Sunday -> "Monday". */
export const weekdayName = (day: number): string => WEEKDAYS[day - 1];

/** "Monday, October 5, 2026", like Common.kt longDate. English only, never the device locale. */
export function longDate(date: string): string {
  const d = parseIso(date);
  return `${weekdayName(weekday(date))}, ${MONTHS[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
}

/** "Mon, Oct 5", like Common.kt shortDate. */
export function shortDate(date: string): string {
  const d = parseIso(date);
  return `${weekdayName(weekday(date)).slice(0, 3)}, ${MONTHS[d.getMonth()].slice(0, 3)} ${d.getDate()}`;
}

/** "Oct 5", the chart axis label. */
export function monthDay(date: string): string {
  const d = parseIso(date);
  return `${MONTHS[d.getMonth()].slice(0, 3)} ${d.getDate()}`;
}

/** First day of the month that holds the date, moved by `months`. */
export function monthStart(date: string, months = 0): string {
  const d = parseIso(date);
  return iso(new Date(d.getFullYear(), d.getMonth() + months, 1));
}

/** "October 2026". */
export function monthTitle(date: string): string {
  const d = parseIso(date);
  return `${MONTHS[d.getMonth()]} ${d.getFullYear()}`;
}

/** Calendar cells of a month in rows of seven, Monday first; null pads the first and last week. */
export function monthGrid(date: string): (string | null)[] {
  const first = monthStart(date);
  const next = monthStart(date, 1);
  const cells: (string | null)[] = Array(weekday(first) - 1).fill(null);
  for (let d = first; d < next; d = addDays(d, 1)) cells.push(d);
  while (cells.length % 7 !== 0) cells.push(null);
  return cells;
}

/** The seven dates of the week that holds the date, Monday first. */
export const weekOf = (date: string): string[] => Array.from({ length: 7 }, (_, i) => addDays(mondayOf(date), i));

/** Local midnight plus minutes, as a Date. */
export function atMinute(date: string, minute: number): Date {
  const d = parseIso(date);
  d.setMinutes(minute);
  return d;
}
