import { addDays, atMinute, iso } from './dates';
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
