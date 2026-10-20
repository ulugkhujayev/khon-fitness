import { describe, expect, test } from 'bun:test';
import { cardioPatch, fieldValue, formFields, runPace, swimFromLaps, withFieldValue } from '../src/domain/cardio';
import { Defaults, Exercise, ItemType, Kind, Session } from '../src/domain/types';

const session = (p: Partial<Session> = {}): Session => ({ ...Defaults.session, id: 's', date: '2026-10-05', startedAt: 0, itemType: ItemType.CARDIO, ...p });
const exercise = (fields: string): Exercise => ({ ...Defaults.exercise, id: 'e', name: 'Swim', kind: Kind.CARDIO, fields });

describe('cardio form', () => {
  test('fields: the exercise first, then stored values it no longer lists', () => {
    expect(formFields(exercise('laps,poolLength,time'), session())).toEqual(['laps', 'poolLength', 'time']);
    expect(formFields(exercise('time'), session({ avgHr: 140, stroke: 'Mixed' }))).toEqual(['time', 'avgHr', 'stroke']);
    expect(formFields(null, session())).toEqual(['time', 'distance', 'avgHr']);
    expect(formFields(exercise('time,bogus'), session())).toEqual(['time']);
  });

  test('values in minutes and kilometres go back to whole seconds and metres', () => {
    const s = session({ timeSec: 1830, distanceM: 5250 });
    expect(fieldValue(s, 'time')).toBe(30.5);
    expect(fieldValue(s, 'distance')).toBe(5.25);
    expect(fieldValue(s, 'poolLength')).toBe(25);
    expect(withFieldValue(s, 'distance', 1.15).distanceM).toBe(1150);
    expect(withFieldValue(s, 'time', 25.5).timeSec).toBe(1530);
    expect(withFieldValue(s, 'avgHr', 151.6).avgHr).toBe(152);
    expect(withFieldValue(s, 'time', null).timeSec).toBeNull();
  });

  test('pace per km and swims from laps', () => {
    expect(runPace(1535, 5000)).toBe('5:07 /km');
    expect(runPace(null, 5000)).toBeNull();
    expect(swimFromLaps(40, null, 1500)).toEqual({ distance: '1000 m', pace: '2:30 /100 m' });
    expect(swimFromLaps(20, 50, null)).toEqual({ distance: '1000 m', pace: null });
    expect(swimFromLaps(0, 25, 100)).toBeNull();
  });
});

describe('cardio save', () => {
  test('writes the form fields and keeps an earlier finish time', () => {
    const draft = session({ timeSec: 1800, note: 'easy', programName: 'ignored' });
    const patch = cardioPatch(draft, 42);
    expect(patch).toMatchObject({ timeSec: 1800, note: 'easy', finishedAt: 42 });
    expect('programName' in patch).toBe(false);
    expect(cardioPatch({ ...draft, finishedAt: 7 }, 42).finishedAt).toBe(7);
  });
});
