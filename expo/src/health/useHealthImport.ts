import { useEffect, useState } from 'react';
import { HealthWorkout } from '@/domain/health';
import { healthSource } from './source';
import { HealthStatus, IMPORT_DAYS } from './types';

interface HealthRead {
  status: HealthStatus | null;
  granted: boolean;
  /** Null while reading. */
  workouts: HealthWorkout[] | null;
  error: string | null;
}

const message = (e: unknown) => (e instanceof Error ? e.message : 'Read failed');

/**
 * The import screen's read of the health store (ImportScreen.kt): status, permission, then the last 60 days of
 * workouts. `refresh` reads again (Check again, after a permission answer or a debug write).
 */
export function useHealthImport() {
  const [read, setRead] = useState<HealthRead>({ status: null, granted: false, workouts: null, error: null });
  const [round, setRound] = useState(0);

  useEffect(() => {
    let alive = true;
    const set = (r: HealthRead) => alive && setRead(r);
    (async () => {
      const status = await healthSource.status();
      if (status !== 'available') return set({ status, granted: false, workouts: null, error: null });
      const granted = await healthSource.hasPermission();
      if (!granted) return set({ status, granted, workouts: null, error: null });
      try {
        set({ status, granted, workouts: await healthSource.workouts(IMPORT_DAYS), error: null });
      } catch (e) {
        set({ status, granted, workouts: [], error: message(e) });
      }
    })().catch((e: unknown) => set({ status: 'unsupported', granted: false, workouts: null, error: message(e) }));
    return () => {
      alive = false;
    };
  }, [round]);

  const refresh = () => setRound((r) => r + 1);
  return {
    ...read,
    refresh,
    allow: async () => {
      await healthSource.requestPermission().catch(() => false);
      refresh();
    },
    // A failed write keeps its message on screen; reading again would clear it.
    writeTestRun: healthSource.writeTestRun
      ? async () => {
          try {
            await healthSource.writeTestRun?.();
            refresh();
          } catch (e) {
            setRead((r) => ({ ...r, error: message(e) }));
          }
        }
      : null,
  };
}
