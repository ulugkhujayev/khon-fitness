/** Opening a session in the screen it belongs to (`sessionScreen` decides which), and the stretch player. */
import { router } from 'expo-router';
import { PlanItem, SessionScreen, sessionScreen } from '@/domain/plan';
import { Session } from '@/domain/types';
import * as repo from '@/db/repository';
import { getView } from '@/timer/engine';

const path = (id: string, screen: SessionScreen) => {
  switch (screen) {
    case 'session':
      return { pathname: '/session/[id]', params: { id } } as const;
    case 'timer':
      return { pathname: '/timer/[id]', params: { id } } as const;
    case 'cardio':
      return { pathname: '/cardio/[id]', params: { id } } as const;
  }
};

export const openSession = (id: string, screen: SessionScreen) => router.push(path(id, screen));

/** Swaps the current screen for the session's form or timer, so Back skips the one that ended. */
export const replaceSession = (id: string, screen: SessionScreen) => router.replace(path(id, screen));

/** A running session continues where it is: its timer while that runs, else its screen. `replace` swaps the current screen. */
export function resumeSession(s: Session, replace = false) {
  const screen = sessionScreen(s.itemType, getView().state?.sessionId === s.id);
  if (replace) replaceSession(s.id, screen);
  else openSession(s.id, screen);
}

/** Starts a session for a plan item and opens it: an interval exercise in its timer, a steady one in the form. False for rest. */
export async function startItem(item: PlanItem, replace = false): Promise<boolean> {
  const s = await repo.startItem(item);
  if (!s) return false;
  const screen = sessionScreen(s.itemType, item.cardio?.intervals === true);
  if (replace) replaceSession(s.id, screen);
  else openSession(s.id, screen);
  return true;
}

/** The stretch player of a routine; it continues a run of that routine already going. */
export const openStretchPlayer = (routineId: string) => router.push({ pathname: '/stretch/[routineId]', params: { routineId } });
