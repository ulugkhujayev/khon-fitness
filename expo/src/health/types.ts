import { HealthWorkout } from '@/domain/health';

/** Whether the platform's health store can be read: Health Connect may be missing or outdated on Android. */
export type HealthStatus = 'available' | 'notInstalled' | 'updateRequired' | 'unsupported';

/** The platform's health store as the import screen and Today use it. */
export interface HealthSource {
  /** "Health Connect" or "Apple Health". */
  name: string;
  status(): Promise<HealthStatus>;
  hasPermission(): Promise<boolean>;
  /** Asks to read workouts and heart rate; true when that is granted afterwards (or, on iOS, was asked). */
  requestPermission(): Promise<boolean>;
  /**
   * Workouts of the last `days` days; the app's own entries only in development.
   * Without `detailed` they come without distance and heart rate, which is enough to count them.
   */
  workouts(days: number, detailed?: boolean): Promise<HealthWorkout[]>;
  /** Where to install or update the store, or null. */
  storeUrl(status: HealthStatus): string | null;
  /** Development only: writes a 30 min run so the import can be tried on an emulator. */
  writeTestRun: (() => Promise<void>) | null;
}

/** How far back the import looks (HealthImport.drafts(context, 60, …)). */
export const IMPORT_DAYS = 60;
