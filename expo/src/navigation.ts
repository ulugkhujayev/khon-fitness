/** Opening a session in the screen it belongs to (`sessionScreen` decides which), and the stretch player. */
import { router } from 'expo-router';
import { SessionScreen } from '@/domain/plan';

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

/** The stretch player of a routine; it continues a run of that routine already going. */
export const openStretchPlayer = (routineId: string) => router.push({ pathname: '/stretch/[routineId]', params: { routineId } });
