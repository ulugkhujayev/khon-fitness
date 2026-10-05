import { useEffect } from 'react';
import * as Notifications from 'expo-notifications';
import { openSession } from '@/navigation';
import { alertSession } from './alerts';
import { restoreTimer } from './engine';

/**
 * Brings back a timer the system closed, and opens the timer when one of its alerts is tapped.
 * Lives in the root layout, under the data provider. Renders nothing.
 */
export function TimerHost() {
  const lastResponse = Notifications.useLastNotificationResponse();

  useEffect(() => {
    Notifications.setNotificationHandler({
      handleNotification: async () => ({ shouldShowBanner: true, shouldShowList: true, shouldPlaySound: true, shouldSetBadge: false }),
    });
    void restoreTimer();
  }, []);

  useEffect(() => {
    if (!lastResponse || lastResponse.actionIdentifier !== Notifications.DEFAULT_ACTION_IDENTIFIER) return;
    const sessionId = alertSession(lastResponse);
    if (sessionId) openSession(sessionId, 'timer');
  }, [lastResponse]);

  return null;
}
