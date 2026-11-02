/**
 * The eating window's notifications (WindowScheduler.kt). Without a receiver to re-arm one alarm at a time,
 * a week of boundaries and reminders is scheduled ahead, and replaced on every change and whenever the app opens.
 */
import { windowAlerts } from '@/domain/window';
import * as repo from '@/db/repository';
import { scheduleAlerts } from './notifications';

let queue: Promise<void> = Promise.resolve();

async function apply() {
  try {
    const [w, days] = await Promise.all([repo.eatingWindow(), repo.windowDays()]);
    await scheduleAlerts('window', windowAlerts(w, days, new Date()));
  } catch {
    // Notifications are a convenience: a failed schedule leaves the window itself working, and the next open retries.
  }
}

/** Runs one at a time, so quick edits never interleave a cancel with another's schedule. */
export function rescheduleWindow(): Promise<void> {
  queue = queue.then(apply);
  return queue;
}
