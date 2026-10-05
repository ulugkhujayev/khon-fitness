/**
 * Scheduled local notifications, by kind: the timer's phase changes while the app sleeps, and the eating
 * window's boundaries and reminders. Each kind is replaced or cancelled without touching the other.
 */
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { Alert } from '@/domain/timer';

export type AlertKind = 'timer' | 'window';

/** Android channels: the timer is loud and on the lock screen; the window is an ordinary reminder (WindowScheduler.kt). */
const CHANNELS: Record<AlertKind, Notifications.NotificationChannelInput> = {
  timer: {
    name: 'Interval timer',
    importance: Notifications.AndroidImportance.HIGH,
    sound: 'default',
    vibrationPattern: [0, 400],
    lockscreenVisibility: Notifications.AndroidNotificationVisibility.PUBLIC,
  },
  window: { name: 'Eating window', importance: Notifications.AndroidImportance.DEFAULT, sound: 'default' },
};

let permission: Promise<boolean> | null = null;

/** Creates the Android channels and asks for permission once. False when the user said no. */
export function prepareNotifications(): Promise<boolean> {
  permission ??= (async () => {
    if (Platform.OS === 'android') {
      await Promise.all(Object.entries(CHANNELS).map(([id, channel]) => Notifications.setNotificationChannelAsync(id, channel)));
    }
    const current = await Notifications.getPermissionsAsync();
    if (current.granted) return true;
    return (await Notifications.requestPermissionsAsync()).granted;
  })();
  return permission;
}

/** Replaces this kind's scheduled notifications. `data` rides along to the tap. */
export async function scheduleAlerts(kind: AlertKind, alerts: Alert[], data: Record<string, string | boolean> = {}) {
  await cancelAlerts(kind);
  if (alerts.length === 0 || !(await prepareNotifications())) return;
  for (const a of alerts) {
    await Notifications.scheduleNotificationAsync({
      content: { title: a.title, body: a.body, sound: true, data: { ...data, kind } },
      trigger: { type: Notifications.SchedulableTriggerInputTypes.DATE, date: new Date(a.at), channelId: kind },
    });
  }
}

export async function cancelAlerts(kind: AlertKind) {
  const scheduled = await Notifications.getAllScheduledNotificationsAsync();
  await Promise.all(scheduled.filter((n) => n.content.data?.kind === kind).map((n) => Notifications.cancelScheduledNotificationAsync(n.identifier)));
}

/** What a tapped notification belongs to: a timer run (cardio session or stretch run), the eating window, or nothing of ours. */
export type TappedAlert = { kind: 'timer'; sessionId: string; stretch: boolean } | { kind: 'window' } | null;

export function tappedAlert(response: Notifications.NotificationResponse): TappedAlert {
  const data = response.notification.request.content.data;
  if (data?.kind === 'window') return { kind: 'window' };
  if (data?.kind === 'timer' && typeof data.sessionId === 'string') return { kind: 'timer', sessionId: data.sessionId, stretch: data.stretch === true };
  return null;
}
