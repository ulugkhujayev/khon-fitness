import { useCallback, useState } from 'react';
import { useFocusEffect } from 'expo-router';
import { HealthWorkout, pendingCount } from '@/domain/health';
import { healthSource } from './source';
import { IMPORT_DAYS } from './types';

/**
 * How many band sessions wait for import (HealthImport.pendingCount): read each time the screen comes into view,
 * only when the store is there and reading was allowed, so Today never asks for anything itself.
 */
export function usePendingImports(imported: Set<string> | null): number {
  const [workouts, setWorkouts] = useState<HealthWorkout[]>([]);

  useFocusEffect(
    useCallback(() => {
      let alive = true;
      (async () => {
        if ((await healthSource.status()) !== 'available' || !(await healthSource.hasPermission())) return [];
        return healthSource.workouts(IMPORT_DAYS, false);
      })()
        .catch(() => [])
        .then((w) => alive && setWorkouts(w));
      return () => {
        alive = false;
      };
    }, []),
  );

  return imported ? pendingCount(workouts, imported) : 0;
}
