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

/** Local midnight plus minutes, as a Date. */
export function atMinute(date: string, minute: number): Date {
  const d = parseIso(date);
  d.setMinutes(minute);
  return d;
}
