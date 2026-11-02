/** A tap on a window notification asks Today to open the eating-window sheet (MainActivity.windowRequested in Kotlin). */
import { useSyncExternalStore } from 'react';
import { router } from 'expo-router';

let requested = false;
const listeners = new Set<() => void>();
const emit = () => listeners.forEach((l) => l());

const subscribe = (listener: () => void) => {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
};

/** Goes back to Today, closing any screens above the tabs, and asks it for the sheet. */
export function requestWindowSheet() {
  requested = true;
  emit();
  if (router.canDismiss()) router.dismissAll();
  router.navigate('/');
}

export function clearWindowRequest() {
  requested = false;
  emit();
}

export const useWindowRequest = () => useSyncExternalStore(subscribe, () => requested);
