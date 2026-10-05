import { useSyncExternalStore } from 'react';
import { getView, subscribe, TimerView } from './engine';

/** The running timer and how the last one ended; re-renders on every tick. */
export function useTimer(): TimerView {
  return useSyncExternalStore(subscribe, getView);
}
