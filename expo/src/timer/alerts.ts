/**
 * Phase changes as scheduled local notifications, so a locked phone still hears them while the app sleeps.
 * They are scheduled when the app goes to the background and cancelled when it comes back.
 */
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { Alert } from '@/domain/timer';

const CHANNEL = 'timer';
/** Marks the timer's notifications, so cancelling them leaves other reminders alone. */
const KIND = 'timer';

let permission: Promise<boolean> | null = null;

/** Creates the Android channel and asks for permission once. False when the user said no. */
export function prepareAlerts(): Promise<boolean> {
  permission ??= (async () => {
    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync(CHANNEL, {
        name: 'Interval timer',
        importance: Notifications.AndroidImportance.HIGH,
        sound: 'default',
        vibrationPattern: [0, 400],
        lockscreenVisibility: Notifications.AndroidNotificationVisibility.PUBLIC,
      });
    }
    const current = await Notifications.getPermissionsAsync();
    if (current.granted) return true;
    return (await Notifications.requestPermissionsAsync()).granted;
  })();
  return permission;
}

export async function scheduleAlerts(alerts: Alert[], sessionId: string) {
  await cancelAlerts();
  if (alerts.length === 0 || !(await prepareAlerts())) return;
  for (const a of alerts) {
    await Notifications.scheduleNotificationAsync({
      content: { title: a.title, body: a.body, sound: true, data: { kind: KIND, sessionId } },
      trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date: new Date(a.at), channelId: CHANNEL },
    });
  }
}

export async function cancelAlerts() {
  const scheduled = await Notifications.getAllScheduledNotificationsAsync();
  await Promise.all(scheduled.filter((n) => n.content.data?.kind === KIND).map((n) => Notifications.cancelScheduledNotificationAsync(n.identifier)));
}

/** The session a tapped timer notification belongs to, or null for any other notification. */
export function alertSession(response: Notifications.NotificationResponse): string | null {
  const data = response.notification.request.content.data;
  return data?.kind === KIND && typeof data.sessionId === 'string' ? data.sessionId : null;
}
