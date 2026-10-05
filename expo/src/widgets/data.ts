/** Everything the widgets show, read straight from the database: the app may not be running when a widget asks. */
import { addDays, atMinute, today as todayIso } from '@/domain/dates';
import { itemFor } from '@/domain/plan';
import { todayCard, windowCards } from '@/domain/widgets';
import * as repo from '@/db/repository';
import { TABLES as T } from '@/db/schema';

export async function widgetData(now = new Date()) {
  const date = todayIso();
  const tomorrow = addDays(date, 1);
  const [plan, unfinished, routines, stretchSessions, window, windowDays] = await Promise.all([
    repo.planData(), repo.unfinishedSession(), repo.stretchRoutines(), repo.stretchSessions(), repo.eatingWindow(), repo.windowDays(),
  ]);
  // The widget names the active routine only, as TodayWidget.kt does (Today falls back to the first).
  const routine = routines.find((r) => r.active) ?? null;
  return {
    /** Today now, and tomorrow from midnight, for a widget that redraws on a timeline. */
    today: [
      { at: now, card: todayCard(itemFor(plan, date), unfinished, routine, stretchSessions.some((s) => s.date === date)) },
      { at: atMinute(tomorrow, 0), card: todayCard(itemFor(plan, tomorrow), unfinished, routine, false) },
    ],
    window: windowCards(window, windowDays, now),
  };
}

export type WidgetData = Awaited<ReturnType<typeof widgetData>>;

/** The tables a widget's content depends on; a write to any of them refreshes the widgets. */
export const WIDGET_TABLES = [...repo.PLAN_TABLES, T.sessions, T.stretchRoutines, T.stretchSessions, T.eatingWindow, T.windowDays];
