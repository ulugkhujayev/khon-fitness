/**
 * What the home-screen widgets show (TodayWidget.kt, WindowWidget.kt). Both platforms render these cards;
 * the iOS window widget also gets the cards ahead, one per change, because it redraws only on a schedule.
 */
import { iso } from './dates';
import { PlanItem } from './plan';
import { EatingWindow, ItemType, Session, StretchRoutine, WindowDay } from './types';
import { EatingWindowRules } from './window';

/** The Today widget: today's item or the running session, the stretch line, and its button. */
export interface TodayCard {
  /** "In progress", "Rest day", "Gym" or "Cardio". */
  sub: string;
  title: string;
  /** "Resume", "Start", or "Open" on a rest day. */
  action: string;
  /** False on a rest day with nothing running: a tap only opens the app. */
  starts: boolean;
  /** "Stretched today" or "Daily mobility not yet"; null without an active routine. */
  stretch: string | null;
  stretched: boolean;
  description: string;
}

export function todayCard(item: PlanItem, unfinished: Session | null, routine: StretchRoutine | null, stretched: boolean): TodayCard {
  const title = unfinished?.programName ?? unfinished?.exerciseName ?? item.name;
  const sub = unfinished ? 'In progress' : item.isRest ? 'Rest day' : item.itemType === ItemType.PROGRAM ? 'Gym' : 'Cardio';
  const action = unfinished ? 'Resume' : item.isRest ? 'Open' : 'Start';
  return {
    sub,
    title,
    action,
    starts: unfinished != null || !item.isRest,
    stretch: routine ? (stretched ? 'Stretched today' : `${routine.name} not yet`) : null,
    stretched,
    description: `Today: ${title}${unfinished ? ', in progress' : ''}, ${action}`,
  };
}

/** The window widget: open or closed and the next change as a clock time (it does not redraw every minute). */
export interface WindowCard {
  open: boolean;
  /** "Open", "Closed" or "Window off". */
  status: string;
  /** "until 20:00", "opens 12:00", "until midnight", or "Tap to set hours" when off. */
  text: string;
  description: string;
}

export function windowCard(w: EatingWindow, day: WindowDay | undefined, now: Date): WindowCard {
  if (!w.enabled) return { open: false, status: 'Window off', text: 'Tap to set hours', description: 'Eating window: off, Tap to set hours' };
  const st = EatingWindowRules.state(w, day, now);
  const at = EatingWindowRules.hhmm(st.changesAt.getHours() * 60 + st.changesAt.getMinutes());
  const text = st.open && iso(st.changesAt) > iso(now) ? 'until midnight' : st.open ? `until ${at}` : `opens ${at}`;
  const status = st.open ? 'Open' : 'Closed';
  return { open: st.open, status, text, description: `Eating window: ${status}, ${text}` };
}

/** The window card now and at each change in the next `hours`, for a widget that redraws on a timeline. */
export function windowCards(w: EatingWindow, days: WindowDay[], now: Date, hours = 48): { at: Date; card: WindowCard }[] {
  const dayOf = (t: Date) => days.find((d) => d.date === iso(t));
  const out = [{ at: now, card: windowCard(w, dayOf(now), now) }];
  if (!w.enabled) return out;
  const until = now.getTime() + hours * 3_600_000;
  let t = now;
  for (;;) {
    const next = EatingWindowRules.state(w, dayOf(t), t).changesAt;
    if (next.getTime() <= t.getTime() || next.getTime() > until) return out;
    out.push({ at: next, card: windowCard(w, dayOf(next), next) });
    t = next;
  }
}
