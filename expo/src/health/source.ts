/** Platforms without a health store: nothing to import. Android and iOS replace this file (source.android.ts, source.ios.ts). */
import { HealthSource } from './types';

export const healthSource: HealthSource = {
  name: 'Health',
  status: async () => 'unsupported',
  hasPermission: async () => false,
  requestPermission: async () => false,
  workouts: async () => [],
  storeUrl: () => null,
  writeTestRun: null,
};
