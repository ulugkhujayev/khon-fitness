/** Port of ImportDefaultTest.kt, plus the merge, interval heart rate and import rules of HealthImport.kt and importDraft. */
import { describe, expect, test } from 'bun:test';
import {
  appName, defaultExercise, lapsOf, lengthInMeters, draftDetail, draftStats, HealthWorkout, importDrafts, importedIds, importedSession, intervalHeartRates,
  mergeOverlaps, pendingCount, pendingLine, toDraft,
} from '../src/domain/health';
import { Defaults, Exercise, Kind } from '../src/domain/types';

const ex = (p: Partial<Exercise> & Pick<Exercise, 'id' | 'name'>): Exercise => ({ ...Defaults.exercise, kind: Kind.CARDIO, ...p });
const nor = ex({ id: 'nor4x4', name: 'Norwegian 4x4', modalityId: 'bike', intervals: true, warmupSec: 600, workSec: 240, restSec: 180, rounds: 4 });
const z2bike = ex({ id: 'z2bike', name: 'Zone 2 bike', modalityId: 'bike' });
const z2run = ex({ id: 'z2run', name: 'Zone 2 run', modalityId: 'run' });
const cardio = [nor, z2bike, z2run];

const at = (h: number, m = 0) => new Date(2026, 9, 5, h, m).getTime();
const workout = (p: Partial<HealthWorkout> = {}): HealthWorkout => ({
  sourceId: 'a', start: at(18), end: at(18, 30), kind: 'run', distanceM: 5200.7, hrSamples: [], origin: 'Mi Fitness', laps: null, poolLength: null, ...p,
});

describe('default exercise (ImportDefaultTest)', () => {
  test('a workout with no modality gets no exercise, even when cardio is planned', () => {
    expect(defaultExercise(null, nor, cardio)).toBeNull();
    expect(defaultExercise(null, null, cardio)).toBeNull();
  });

  test('a modality with no exercise gets none instead of the first cardio', () => {
    expect(defaultExercise('row', null, cardio)).toBeNull();
  });

  test('planned cardio wins when its modality matches, else the plainest of that modality', () => {
    expect(defaultExercise('bike', nor, cardio)).toBe(nor);
    expect(defaultExercise('bike', z2run, cardio)).toBe(z2bike);
    expect(defaultExercise('run', null, cardio)).toBe(z2run);
  });
});

describe('drafts', () => {
  test('a workout becomes a draft: kind name and modality, truncated heart rate, no zero distance', () => {
    const d = toDraft(workout({ hrSamples: [[at(18, 1), 140], [at(18, 2), 143]] }));
    expect(d).toMatchObject({ typeName: 'Run', modalityId: 'run', distanceM: 5200, avgHr: 141, origins: ['Mi Fitness'], mergedIds: ['a'], strength: false });
    expect(toDraft(workout({ distanceM: 0, kind: 'strength' }))).toMatchObject({ distanceM: null, avgHr: null, modalityId: null, strength: true });
  });

  test('band and watch copies of one workout merge; the one with more heart rate is the base', () => {
    const band = toDraft(workout({ sourceId: 'band', hrSamples: [[at(18, 1), 150]], distanceM: null }));
    const watch = toDraft(workout({ sourceId: 'watch', start: at(18, 2), end: at(18, 40), origin: 'Garmin', hrSamples: [[at(18, 3), 140], [at(18, 4), 142]] }));
    const bike = toDraft(workout({ sourceId: 'bike', kind: 'bike' }));
    const [merged, other] = mergeOverlaps([band, watch, bike]);
    expect(merged).toMatchObject({ start: at(18), end: at(18, 40), avgHr: 141, distanceM: 5200, origins: ['Mi Fitness', 'Garmin'], mergedIds: ['band', 'watch'] });
    expect(other.sourceId).toBe('bike');
  });

  test('imported records stay out; the newest comes first; Today counts without strength copies', () => {
    const list = [workout({ sourceId: 'old', start: at(7), end: at(7, 30) }), workout({ sourceId: 'new' }), workout({ sourceId: 'gym', kind: 'strength', start: at(10), end: at(11) })];
    expect(importDrafts(list, new Set(['old'])).map((d) => d.sourceId)).toEqual(['new', 'gym']);
    expect(pendingCount(list, new Set())).toBe(2);
    expect(importedIds([{ sourceId: 'a,b' }, { sourceId: null }, { sourceId: 'c' }])).toEqual(new Set(['a', 'b', 'c']));
  });
});

describe('import', () => {
  test('heart rate per work interval from the session start and the exercise structure', () => {
    const start = at(7);
    const sample = (sec: number, bpm: number): [number, number] => [start + sec * 1000, bpm];
    const d = toDraft(workout({ start, end: at(7, 40), hrSamples: [sample(100, 120), sample(610, 160), sample(700, 170), sample(1100, 175)] }));
    // Work 1 is 600..840 s, work 2 is 1020..1260 s, works 3 and 4 have no samples.
    expect(intervalHeartRates(d, nor)).toEqual([165, 175, null, null]);
    expect(intervalHeartRates(d, z2bike)).toEqual([]);
  });

  test('a draft becomes a finished cardio session; laps only when the exercise logs them', () => {
    const swim = ex({ id: 'swim', name: 'Pool swim', modalityId: 'swim', fields: 'time,laps,poolLength' });
    const d = { ...toDraft(workout({ kind: 'swim', distanceM: 1500 })), origins: ['Mi Fitness', 'Garmin'], mergedIds: ['a', 'b'] };
    let n = 0;
    const { session, intervals } = importedSession(d, swim, 's1', () => `i${n++}`);
    expect(session).toMatchObject({
      id: 's1', date: '2026-10-05', startedAt: at(18), finishedAt: at(18, 30), itemType: 'cardio', exerciseId: 'swim', exerciseName: 'Pool swim',
      timeSec: 1800, distanceM: 1500, laps: 60, poolLength: 25, sourceId: 'a,b', note: 'From Mi Fitness + Garmin',
    });
    expect(intervals).toEqual([]);
    expect(importedSession(toDraft(workout()), z2run, 's2', () => 'x').session).toMatchObject({ laps: null, poolLength: null, note: '' });
    expect(importedSession(toDraft(workout({ start: at(7), end: at(7, 40) })), nor, 's3', () => `i${n++}`).intervals.map((i) => [i.sessionId, i.index])).toEqual([
      ['s3', 0], ['s3', 1], ['s3', 2], ['s3', 3],
    ]);
  });

  test('words on the import screen and Today', () => {
    const d = toDraft(workout({ hrSamples: [[at(18, 1), 141]] }));
    expect(draftStats(d)).toBe('Mon, Oct 5 · 30 min · 5.2 km · 141 bpm');
    expect(draftStats({ ...d, distanceM: null, avgHr: null })).toBe('Mon, Oct 5 · 30 min');
    expect(draftDetail({ ...d, laps: 40 })).toBe('30 min · 5.2 km · avg 141 bpm · 40 laps · from Mi Fitness');
    expect([pendingLine(1), pendingLine(3)]).toEqual(['1 new session from the band', '3 new sessions from the band']);
    expect([appName('com.xiaomi.wearable'), appName('com.example.fitbuddy')]).toEqual(['Mi Fitness', 'Fitbuddy']);
  });
});

describe('platform values', () => {
  test('lengths in metres from either shape; unknown units and junk give null', () => {
    expect(lengthInMeters({ inMeters: 25, inKilometers: 0.025 })).toBe(25);
    expect(lengthInMeters({ value: 5.2, unit: 'kilometers' })).toBe(5200);
    expect(lengthInMeters({ quantity: 1, unit: 'mi' })).toBe(1609.344);
    expect([lengthInMeters({ value: 1, unit: 'furlong' }), lengthInMeters(null), lengthInMeters(42)]).toEqual([null, null, null]);
  });

  test('laps and the pool length as the median lap', () => {
    expect(lapsOf([25, 25, 50, 0])).toEqual({ laps: 4, poolLength: 25 });
    expect(lapsOf([null, 0])).toEqual({ laps: 2, poolLength: null });
    expect(lapsOf([])).toEqual({ laps: null, poolLength: null });
  });
});
