import { useEffect, useRef } from 'react';
import { AppState } from 'react-native';
import { usePathname } from 'expo-router';
import * as Notifications from 'expo-notifications';
import { routineOf } from '@/domain/stretch';
import { openSession, openStretchPlayer } from '@/navigation';
import { restoreTimer } from '@/timer/engine';
import { tappedAlert } from './notifications';
import { rescheduleWindow } from './windowReminders';
import { requestWindowSheet } from './windowRequest';

/**
 * Brings back a timer the system closed, keeps the eating window's week of reminders current, and opens
 * what a tapped notification belongs to. Lives in the root layout, under the data provider. Renders nothing.
 */
export function NotificationHost() {
  const lastResponse = Notifications.useLastNotificationResponse();
  const pathname = usePathname();
  // Each tap opens its screen once, however often the route changes afterwards.
  const handled = useRef<string | null>(null);

  useEffect(() => {
    Notifications.setNotificationHandler({
      handleNotification: async () => ({ shouldShowBanner: true, shouldShowList: true, shouldPlaySound: true, shouldSetBadge: false }),
    });
    void restoreTimer();
    void rescheduleWindow();
    // Coming back rolls the window's schedule forward, and follows a clock or time zone change.
    const sub = AppState.addEventListener('change', (status) => {
      if (status === 'active') void rescheduleWindow();
    });
    return () => sub.remove();
  }, []);

  useEffect(() => {
    if (!lastResponse || lastResponse.actionIdentifier !== Notifications.DEFAULT_ACTION_IDENTIFIER) return;
    const key = lastResponse.notification.request.identifier;
    if (handled.current === key) return;
    handled.current = key;
    const tapped = tappedAlert(lastResponse);
    if (tapped?.kind === 'window') return requestWindowSheet();
    if (tapped?.kind !== 'timer') return;
    // The timer's own screen may still be open under the notification; a second copy would stack on it.
    const routineId = routineOf(tapped.sessionId);
    if (pathname === (tapped.stretch ? `/stretch/${routineId}` : `/timer/${tapped.sessionId}`)) return;
    if (tapped.stretch) openStretchPlayer(routineId);
    else openSession(tapped.sessionId, 'timer');
  }, [lastResponse, pathname]);

  return null;
}
