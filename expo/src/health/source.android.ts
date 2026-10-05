/**
 * Health Connect (HealthImport.kt): exercise sessions with their distance and heart rate. Reads only; the
 * debug build may also write a test run, with write permissions declared in the debug manifest alone.
 */
import Constants from 'expo-constants';
import {
  aggregateRecord, ExerciseType, getGrantedPermissions, getSdkStatus, initialize, insertRecords, Permission, readRecords, RecordType,
  requestPermission, SdkAvailabilityStatus,
} from 'react-native-health-connect';
import { appName, HealthWorkout, lapsOf, lengthInMeters, WorkoutKind } from '@/domain/health';
import { HealthSource, HealthStatus } from './types';

const PROVIDER = 'com.google.android.apps.healthdata';
const TYPES: RecordType[] = ['ExerciseSession', 'HeartRate', 'Distance'];
const READ: Permission[] = TYPES.map((recordType) => ({ accessType: 'read', recordType }));
const WRITE: Permission[] = TYPES.map((recordType) => ({ accessType: 'write', recordType }));
const OWN_PACKAGE = Constants.expoConfig?.android?.package ?? '';
const DAY_MS = 86_400_000;

const KINDS: Partial<Record<number, WorkoutKind>> = {
  [ExerciseType.RUNNING]: 'run',
  [ExerciseType.RUNNING_TREADMILL]: 'run',
  [ExerciseType.SWIMMING_POOL]: 'swim',
  [ExerciseType.SWIMMING_OPEN_WATER]: 'swim',
  [ExerciseType.BIKING]: 'bike',
  [ExerciseType.BIKING_STATIONARY]: 'bike',
  [ExerciseType.ROWING]: 'row',
  [ExerciseType.ROWING_MACHINE]: 'row',
  [ExerciseType.HIGH_INTENSITY_INTERVAL_TRAINING]: 'hiit',
  [ExerciseType.WALKING]: 'walk',
  [ExerciseType.HIKING]: 'hike',
  [ExerciseType.ELLIPTICAL]: 'elliptical',
  [ExerciseType.STRENGTH_TRAINING]: 'strength',
  [ExerciseType.WEIGHTLIFTING]: 'strength',
  [ExerciseType.CALISTHENICS]: 'strength',
};

let client: Promise<boolean> | null = null;
const ready = () => (client ??= initialize());

const between = (start: number, end: number) => ({ operator: 'between', startTime: new Date(start).toISOString(), endTime: new Date(end).toISOString() }) as const;

const granted = async (wanted: Permission[]) => {
  await ready();
  const have = await getGrantedPermissions();
  return wanted.every((p) => have.some((g) => g.accessType === p.accessType && g.recordType === p.recordType));
};

/** Every exercise session in the range, across pages. */
async function sessions(start: number, end: number) {
  const out = [];
  let pageToken: string | undefined;
  do {
    const page = await readRecords('ExerciseSession', { timeRangeFilter: between(start, end), pageToken });
    out.push(...page.records);
    pageToken = page.pageToken || undefined;
  } while (pageToken);
  return out;
}

async function distanceIn(start: number, end: number): Promise<number | null> {
  try {
    return (await aggregateRecord({ recordType: 'Distance', timeRangeFilter: between(start, end) })).DISTANCE.inMeters;
  } catch {
    return null;
  }
}

async function heartRateIn(start: number, end: number): Promise<[number, number][]> {
  try {
    const { records } = await readRecords('HeartRate', { timeRangeFilter: between(start, end) });
    return records.flatMap((r) => r.samples.map((s): [number, number] => [Date.parse(s.time), Math.trunc(s.beatsPerMinute)])).sort((a, b) => a[0] - b[0]);
  } catch {
    return [];
  }
}

function status(sdk: number): HealthStatus {
  if (sdk === SdkAvailabilityStatus.SDK_AVAILABLE) return 'available';
  if (sdk === SdkAvailabilityStatus.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) return 'updateRequired';
  return 'notInstalled';
}

export const healthSource: HealthSource = {
  name: 'Health Connect',
  status: async () => status(await getSdkStatus()),
  hasPermission: () => granted(READ),
  async requestPermission() {
    await ready();
    await requestPermission(READ);
    return granted(READ);
  },
  async workouts(days, detailed = true) {
    await ready();
    const end = Date.now();
    const records = await sessions(end - days * DAY_MS, end);
    const mine = (origin: string | undefined) => !__DEV__ && origin === OWN_PACKAGE;
    return Promise.all(
      records
        .filter((r) => r.metadata?.id && !mine(r.metadata.dataOrigin))
        .map(async (r): Promise<HealthWorkout> => {
          const start = Date.parse(r.startTime);
          const stop = Date.parse(r.endTime);
          const origin = r.metadata?.dataOrigin ?? '';
          return {
            sourceId: r.metadata?.id ?? '',
            start,
            end: stop,
            kind: KINDS[r.exerciseType] ?? 'other',
            distanceM: detailed ? await distanceIn(start, stop) : null,
            hrSamples: detailed ? await heartRateIn(start, stop) : [],
            origin: origin === OWN_PACKAGE ? 'Khon Fitness' : appName(origin),
            ...lapsOf((r.laps ?? []).map((lap) => lengthInMeters(lap.length))),
          };
        }),
    );
  },
  storeUrl: (s) =>
    s === 'notInstalled'
      ? `market://details?id=${PROVIDER}&url=healthconnect%3A%2F%2Fonboarding`
      : s === 'updateRequired'
        ? `market://details?id=${PROVIDER}`
        : null,
  writeTestRun: __DEV__ ? writeTestRun : null,
};

/** Debug: a 30 min run an hour ago with heart rate and 5.2 km, as HealthImport.writeTestRun does. */
async function writeTestRun() {
  await ready();
  await requestPermission(WRITE);
  const end = Date.now() - 3_600_000;
  const start = end - 30 * 60_000;
  const range = { startTime: new Date(start).toISOString(), endTime: new Date(end).toISOString() };
  // The library inserts one record type per call.
  await insertRecords([{ recordType: 'ExerciseSession', ...range, exerciseType: ExerciseType.RUNNING, title: 'Evening run' }]);
  await insertRecords([
    {
      recordType: 'HeartRate',
      ...range,
      samples: Array.from({ length: 30 }, (_, m) => ({ time: new Date(start + m * 60_000).toISOString(), beatsPerMinute: 135 + (m % 7) * 3 })),
    },
  ]);
  await insertRecords([{ recordType: 'Distance', ...range, distance: { value: 5200, unit: 'meters' } }]);
}
