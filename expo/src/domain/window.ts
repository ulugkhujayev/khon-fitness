import { addDays, atMinute, iso } from './dates';
import type { Alert } from './timer';
import { EatingWindow, WindowDay } from './types';

/** The eating-window state at one instant. */
export interface WindowState {
  open: boolean;
  changesAt: Date;
  /** True when the day followed the fixed hours with no manual open or close. */
  kept: boolean;
}

export const EatingWindowRules = {
  /** Open inside the hours or after an early or late open today, and not after a manual close today. */
  state(w: EatingWindow, day: WindowDay | null | undefined, now: Date): WindowState {
    const minute = now.getHours() * 60 + now.getMinutes();
    const date = iso(now);
    const start = atMinute(date, w.startMinute);
    const end = atMinute(date, w.endMinute);
    const inHours = minute >= w.startMinute && minute < w.endMinute;
    const opened = day?.openedAt ?? null;
    const openedEarly = opened !== null && opened < start.getTime() && minute < w.startMinute;
    const openedLate = opened !== null && opened >= end.getTime() && minute >= w.endMinute;
    const open = (inHours || openedEarly || openedLate) && (day?.closedAt ?? null) === null;
    let changesAt: Date;
    if (openedLate && open) changesAt = atMinute(addDays(date, 1), 0);
    else if (open) changesAt = end;
    else if (minute < w.startMinute) changesAt = start;
    else changesAt = atMinute(addDays(date, 1), w.startMinute);
    return { open, changesAt, kept: day?.openedAt == null && day?.closedAt == null };
  },

  hhmm(minute: number): string {
    return `${String(Math.floor(minute / 60)).padStart(2, '0')}:${String(minute % 60).padStart(2, '0')}`;
  },

  /** "2 h 10 m" or "40 m". */
  untilText(from: Date, to: Date): string {
    const m = Math.max(0, Math.floor((to.getTime() - from.getTime()) / 60000));
    return m >= 60 ? `${Math.floor(m / 60)} h ${m % 60} m` : `${m} m`;
  },
};

/** The Today row and the widget: open or not, and "closes in 2 h 10 m", "opens 12:00" or "Off" (windowSummary in Kotlin). */
export function windowSummary(w: EatingWindow | null, day: WindowDay | null | undefined, now: Date): { open: boolean; text: string } {
  if (!w || !w.enabled) return { open: false, text: 'Off' };
  const st = EatingWindowRules.state(w, day, now);
  return st.open
    ? { open: true, text: `closes in ${EatingWindowRules.untilText(now, st.changesAt)}` }
    : { open: false, text: `opens ${EatingWindowRules.hhmm(w.startMinute)}` };
}

/** One cell of the 28-day strip: kept the fixed hours, changed them, or today (not settled yet). */
export type StripCell = 'kept' | 'changed' | 'today';

/** The last 28 days, oldest first: a day with no early open and no early close kept the window. */
export function windowStrip(days: WindowDay[], today: string): StripCell[] {
  return Array.from({ length: 28 }, (_, i) => {
    const date = addDays(today, i - 27);
    if (date === today) return 'today';
    const d = days.find((x) => x.date === date);
    return !d || (d.openedAt == null && d.closedAt == null) ? 'kept' : 'changed';
  });
}

const MINUTES_PER_DAY = 24 * 60;

/** Opening time in 15-minute steps, at least an hour before closing. */
export const withStart = (w: EatingWindow, minute: number): EatingWindow => ({ ...w, startMinute: Math.min(Math.max(0, minute), w.endMinute - 60) });
/** Closing time in 15-minute steps, at least an hour after opening and before midnight. */
export const withEnd = (w: EatingWindow, minute: number): EatingWindow => ({
  ...w,
  endMinute: Math.min(Math.max(w.startMinute + 60, minute), MINUTES_PER_DAY - 1),
});
/** Reminder lead: 0..120 minutes. */
export const withRemind = (w: EatingWindow, minutes: number | null): EatingWindow => ({ ...w, remindBeforeMin: Math.min(120, Math.max(0, Math.trunc(minutes ?? 0))) });

const hhmmAt = (d: Date) => EatingWindowRules.hhmm(d.getHours() * 60 + d.getMinutes());

/**
 * The window's notifications for the next `days` days (WindowScheduler.kt arms one at a time; without a
 * receiver to re-arm, these are scheduled ahead and replaced whenever the app opens or the window changes):
 * each boundary, and a reminder `remindBeforeMin` before it. Today's open or close counts; later days follow the hours.
 */
export function windowAlerts(w: EatingWindow, dayRecords: WindowDay[], now: Date, days = 7): Alert[] {
  if (!w.enabled) return [];
  const until = atMinute(addDays(iso(now), days), 0).getTime();
  const alerts: Alert[] = [];
  let t = now;
  while (t.getTime() < until) {
    const day = dayRecords.find((d) => d.date === iso(t));
    const st = EatingWindowRules.state(w, day, t);
    const at = st.changesAt;
    if (at.getTime() >= until) break;
    // The state right after the boundary decides what it announces.
    const after = new Date(at.getTime() + 1000);
    const next = EatingWindowRules.state(w, dayRecords.find((d) => d.date === iso(after)), after);
    const nextChange = hhmmAt(next.changesAt);
    const remindAt = at.getTime() - w.remindBeforeMin * 60_000;
    if (w.remindBeforeMin > 0 && remindAt > now.getTime()) {
      alerts.push(
        st.open
          ? { at: remindAt, title: `Window closes in ${w.remindBeforeMin} min`, body: `Closes at ${hhmmAt(at)}` }
          : { at: remindAt, title: `Window opens in ${w.remindBeforeMin} min`, body: `Opens at ${hhmmAt(at)}` },
      );
    }
    alerts.push(
      next.open
        ? { at: at.getTime(), title: 'Eating window open', body: `Until ${nextChange}` }
        : { at: at.getTime(), title: 'Eating window closed', body: `Opens at ${nextChange}` },
    );
    t = after;
  }
  return alerts;
}
