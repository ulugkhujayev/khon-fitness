/**
 * Apple Health: workouts with their total distance and the heart rate recorded during them. Reads only.
 * HealthKit never says whether reading was allowed, only whether the question was asked; a refusal reads as no workouts.
 */
import Constants from 'expo-constants';
import {
  AuthorizationRequestStatus, getRequestStatusForAuthorization, isHealthDataAvailable, queryQuantitySamples, queryWorkoutSamples, requestAuthorization,
  WorkoutActivityType, WorkoutEventType,
} from '@kingstinct/react-native-healthkit';
import { HealthWorkout, lengthInMeters, WorkoutKind } from '@/domain/health';
import { HealthSource } from './types';

const TO_READ = { toRead: ['HKWorkoutTypeIdentifier', 'HKQuantityTypeIdentifierHeartRate'] } as const;
const OWN_BUNDLE = Constants.expoConfig?.ios?.bundleIdentifier ?? '';
const DAY_MS = 86_400_000;

const KINDS: Partial<Record<WorkoutActivityType, WorkoutKind>> = {
  [WorkoutActivityType.running]: 'run',
  [WorkoutActivityType.swimming]: 'swim',
  [WorkoutActivityType.cycling]: 'bike',
  [WorkoutActivityType.handCycling]: 'bike',
  [WorkoutActivityType.rowing]: 'row',
  [WorkoutActivityType.highIntensityIntervalTraining]: 'hiit',
  [WorkoutActivityType.walking]: 'walk',
  [WorkoutActivityType.hiking]: 'hike',
  [WorkoutActivityType.elliptical]: 'elliptical',
  [WorkoutActivityType.traditionalStrengthTraining]: 'strength',
  [WorkoutActivityType.functionalStrengthTraining]: 'strength',
  [WorkoutActivityType.coreTraining]: 'strength',
};

const asked = async () => (await getRequestStatusForAuthorization(TO_READ)) === AuthorizationRequestStatus.unnecessary;

async function heartRateIn(startDate: Date, endDate: Date): Promise<[number, number][]> {
  try {
    const samples = await queryQuantitySamples('HKQuantityTypeIdentifierHeartRate', { limit: 0, ascending: true, unit: 'count/min', filter: { date: { startDate, endDate } } });
    return samples.map((s): [number, number] => [s.startDate.getTime(), Math.trunc(s.quantity)]);
  } catch {
    return [];
  }
}

export const healthSource: HealthSource = {
  name: 'Apple Health',
  status: async () => (isHealthDataAvailable() ? 'available' : 'unsupported'),
  hasPermission: asked,
  async requestPermission() {
    await requestAuthorization(TO_READ);
    return asked();
  },
  async workouts(days, detailed = true) {
    const endDate = new Date();
    const startDate = new Date(endDate.getTime() - days * DAY_MS);
    const workouts = await queryWorkoutSamples({ limit: 0, ascending: false, filter: { date: { startDate, endDate } } });
    const mine = (bundle: string) => !__DEV__ && bundle === OWN_BUNDLE;
    return Promise.all(
      workouts
        .filter((w) => !mine(w.sourceRevision.source.bundleIdentifier))
        .map(async (w): Promise<HealthWorkout> => {
          const laps = (w.events ?? []).filter((e) => e.type === WorkoutEventType.lap).length;
          return {
            sourceId: w.uuid,
            start: w.startDate.getTime(),
            end: w.endDate.getTime(),
            kind: KINDS[w.workoutActivityType] ?? 'other',
            distanceM: lengthInMeters(w.totalDistance),
            hrSamples: detailed ? await heartRateIn(w.startDate, w.endDate) : [],
            origin: w.sourceRevision.source.name,
            laps: laps > 0 ? laps : null,
            poolLength: null,
          };
        }),
    );
  },
  storeUrl: () => null,
  writeTestRun: null,
};
