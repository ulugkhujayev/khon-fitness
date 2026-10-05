/** The one running timer, saved on every transition so it survives the app being closed (TimerSnapshots in Kotlin). */
import Storage from 'expo-sqlite/kv-store';
import { decodeSnapshot, encodeSnapshot, TimerSnapshot, TimerState } from '@/domain/timer';

const KEY = 'timer.snapshot';

export function saveSnapshot(state: TimerState, now = Date.now()) {
  Storage.setItemSync(KEY, encodeSnapshot({ state, savedAt: now }));
}

export function loadSnapshot(): TimerSnapshot | null {
  const snap = decodeSnapshot(Storage.getItemSync(KEY));
  if (!snap) Storage.removeItemSync(KEY);
  return snap;
}

/** Clears only this session's snapshot, so a late cleanup never erases a timer started since. */
export function clearSnapshot(sessionId: string) {
  if (loadSnapshot()?.state.sessionId === sessionId) Storage.removeItemSync(KEY);
}
