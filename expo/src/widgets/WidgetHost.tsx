import { useEffect } from 'react';
import { AppState } from 'react-native';
import { subscribe } from '@/db/database';
import { WIDGET_TABLES } from './data';
import { refreshWidgets } from './refresh';

/** How long writes settle before the widgets redraw, so a burst of edits redraws once. */
const SETTLE_MS = 800;

/**
 * Keeps the home-screen widgets current (the refresh calls after each write in Kotlin): on start, after writes
 * to the tables they show, and when the app leaves the screen. Lives in the root layout; renders nothing.
 */
export function WidgetHost() {
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout> | null = null;
    const redraw = () => void refreshWidgets().catch(() => undefined);
    const soon = () => {
      if (timer) clearTimeout(timer);
      timer = setTimeout(redraw, SETTLE_MS);
    };
    redraw();
    const unsubscribe = subscribe(WIDGET_TABLES, soon);
    const sub = AppState.addEventListener('change', (status) => {
      if (status === 'background') redraw();
    });
    return () => {
      unsubscribe();
      sub.remove();
      if (timer) clearTimeout(timer);
    };
  }, []);
  return null;
}
