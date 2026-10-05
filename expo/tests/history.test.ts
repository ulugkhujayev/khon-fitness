/** History numbers. The dayDots cases port DayDotsTest.kt. */
import { describe, expect, test } from 'bun:test';
import { monthWeeks, sameMonth } from '../src/domain/dates';
import {
  cardioTrends, cellAt, dayCount, dayDots, dayLabel, defaultCardioId, defaultExerciseId, dropAction, formatPace, muscleVolume, progressFor,
  sessionLine, stretchLine, stretchMonth, swapNeighbours,
} from '../src/domain/history';
import { PlanItem } from '../src/domain/plan';
import { Defaults, Exercise, IntervalLog, ItemType, Kind, Session, SetLog, SetStatus, StretchSession } from '../src/domain/types';

const date = '2026-09-23';
const session = (id: string, at: number, p: Partial<Session> = {}): Session => ({
  ...Defaults.session, id, date, startedAt: at, finishedAt: at + 1, itemType: ItemType.CARDIO, ...p,
});
const stretch = (id: string, at: number, p: Partial<StretchSession> = {}): StretchSession => ({
  id, routineId: 'daily', routineName: 'Daily mobility', date, startedAt: at, totalSec: 600, completed: 9, skipped: 0, ...p,
});
const log = (p: Partial<SetLog> & Pick<SetLog, 'id' | 'sessionId'>): SetLog => ({
  ...Defaults.setLog, blockIndex: 0, blockSuperset: true, slot: 0, setIndex: 0, order: 0, exerciseId: 'bench', exerciseName: 'Bench',
  minReps: 8, maxReps: 12, stepKg: 2.5, status: SetStatus.DONE, ...p,
});
const ex = (id: string, p: Partial<Exercise> = {}): Exercise => ({ ...Defaults.exercise, id, name: id, kind: Kind.STRENGTH, ...p });
const item = (p: Partial<PlanItem>): PlanItem => ({ itemType: ItemType.REST, program: null, cardio: null, overridden: false, name: 'Rest', isRest: true, ...p });

describe('dayDots (DayDotsTest)', () => {
  test('one dot per finished session in start order with stretch runs as null', () => {
    const dots = dayDots(date, [session('b', 30), session('a', 10), session('open', 5, { finishedAt: null }), session('other', 1, { date: '2026-09-22' })], [stretch('s', 20)]);
    expect(dots.map((d) => d?.id ?? null)).toEqual(['a', null, 'b']);
  });

  test('more than three sessions draw three dots', () => {
    const dots = dayDots(date, [1, 2, 3, 4].map((i) => session(`s${i}`, i)), [stretch('x', 0)]);
    expect(dots.map((d) => d?.id ?? null)).toEqual([null, 's1', 's2']);
  });

  test('no session gives no dots', () => {
    expect(dayDots(date, [], [])).toEqual([]);
  });

  test('count takes finished sessions and stretch runs', () => {
    expect(dayCount(date, [session('a', 1), session('b', 2, { finishedAt: null })], [stretch('s', 3)])).toBe(2);
  });
});

describe('calendar', () => {
  test('month weeks run Monday to Sunday and cover the month', () => {
    const weeks = monthWeeks('2026-10-15');
    expect(weeks[0][0]).toBe('2026-09-28');
    expect(weeks.at(-1)?.at(-1)).toBe('2026-11-01');
    expect(weeks.every((w) => w.length === 7)).toBe(true);
    expect(weeks.flat().filter((d) => sameMonth(d, '2026-10-01'))).toHaveLength(31);
    // February 2027 starts on a Monday and ends on a Sunday: exactly four weeks.
    expect(monthWeeks('2027-02-10')).toHaveLength(4);
  });

  test('day label: date, today, sessions or plan, changed plan', () => {
    expect(dayLabel('2026-10-05', '2026-10-05', 0, item({ isRest: false, name: 'TP v3', overridden: true }))).toBe('Monday, October 5, 2026, today, planned TP v3, plan changed');
    expect(dayLabel('2026-10-04', '2026-10-05', 1, item({}))).toBe('Sunday, October 4, 2026, 1 session');
    expect(dayLabel('2026-10-06', '2026-10-05', 0, item({}))).toBe('Tuesday, October 6, 2026, rest');
  });

  test('hit test maps a point to a cell index', () => {
    expect(cellAt(0, 0, 350, 46, 5)).toBe(0);
    expect(cellAt(349, 46 * 5 - 1, 350, 46, 5)).toBe(34);
    expect(cellAt(120, 50, 350, 46, 5)).toBe(9);
    expect(cellAt(-1, 10, 350, 46, 5)).toBe(-1);
    expect(cellAt(10, 46 * 5, 350, 46, 5)).toBe(-1);
  });

  test('a long press drops into plan, swap or nothing', () => {
    const today = '2026-10-05';
    expect(dropAction('2026-10-07', '2026-10-07', today)).toBe('plan');
    expect(dropAction('2026-10-07', '2026-10-09', today)).toBe('swap');
    expect(dropAction('2026-10-07', '2026-10-01', today)).toBeNull();
    expect(dropAction('2026-10-07', null, today)).toBeNull();
    expect(swapNeighbours('2026-10-05', today)).toEqual({ previous: null, next: '2026-10-06' });
  });
});

describe('day list', () => {
  test('session lines', () => {
    const p = session('p', 1, { itemType: ItemType.PROGRAM });
    const logs = [log({ id: 'a', sessionId: 'p', weightKg: 100, reps: 10 }), log({ id: 'b', sessionId: 'p', status: SetStatus.SKIPPED })];
    expect(sessionLine(p, logs)).toBe('1 set · 1000 kg');
    expect(sessionLine(session('c', 1, { timeSec: 1800, distanceM: 5000 }), [])).toBe('30 min · 5 km');
    expect(sessionLine(session('c', 1, { timeSec: 1830 }), [])).toBe('30.5 min');
    expect(sessionLine(session('o', 1, { finishedAt: null }), [])).toBe('in progress');
  });

  test('stretch lines and the month summary', () => {
    expect(stretchLine(stretch('s', 1, { totalSec: 615, completed: 8, skipped: 1 }))).toBe('10 min · 8 done · 1 skipped');
    expect(stretchMonth([stretch('a', 1), stretch('b', 2, { totalSec: 900 }), stretch('c', 3, { date: '2026-09-24' })], '2026-09-01')).toBe('2 days · avg 11 min');
    expect(stretchMonth([stretch('a', 1)], '2026-10-01')).toBeNull();
  });
});

describe('exercise progress', () => {
  const sessions = [session('s1', 1, { date: '2026-09-20', itemType: ItemType.PROGRAM }), session('s2', 2, { date: '2026-09-10', itemType: ItemType.PROGRAM }), session('open', 3, { finishedAt: null })];
  const logs = [
    log({ id: 'a', sessionId: 's1', weightKg: 100, reps: 5 }),
    log({ id: 'b', sessionId: 's1', weightKg: 90, reps: 12 }),
    log({ id: 'c', sessionId: 's1', weightKg: 100, reps: 3 }),
    log({ id: 'd', sessionId: 's2', weightKg: 80, reps: 8 }),
    log({ id: 'e', sessionId: 's2', weightKg: 120, reps: 1, status: SetStatus.SKIPPED }),
    log({ id: 'f', sessionId: 'open', weightKg: 200, reps: 1 }),
    log({ id: 'g', sessionId: 's1', exerciseId: 'row', weightKg: 60, reps: 10 }),
  ];

  test('one point per finished session, oldest first, best e1RM and top set', () => {
    // s1: 90 x 12 has the best e1RM (126); the top set is 100 x 5, the heaviest with the most reps.
    expect(progressFor('bench', logs, sessions)).toEqual([
      { date: '2026-09-10', e1rm: 101.5, topW: 80, topR: 8 },
      { date: '2026-09-20', e1rm: 126, topW: 100, topR: 5 },
    ]);
    expect(progressFor(null, logs, sessions)).toEqual([]);
  });

  test('the picker starts on an exercise that has sets', () => {
    expect(defaultExerciseId([ex('curl'), ex('bench'), ex('z2', { kind: Kind.CARDIO })], logs)).toBe('bench');
    expect(defaultExerciseId([ex('curl')], [])).toBe('curl');
    expect(defaultExerciseId([], [])).toBeNull();
  });
});

describe('weekly volume', () => {
  test('primary counts 1, secondary half, this week and last, biggest first', () => {
    const today = '2026-10-07';
    const sessions = [
      session('now', 1, { date: '2026-10-05', itemType: ItemType.PROGRAM }),
      session('last', 2, { date: '2026-09-30', itemType: ItemType.PROGRAM }),
      session('old', 3, { date: '2026-09-20', itemType: ItemType.PROGRAM }),
    ];
    const exercises = [ex('bench', { primaryMuscle: 'chest', secondaryMuscles: 'triceps,shoulders' }), ex('curl', { primaryMuscle: 'biceps' })];
    const logs = [
      log({ id: 'a', sessionId: 'now' }),
      log({ id: 'b', sessionId: 'now' }),
      log({ id: 'c', sessionId: 'last', exerciseId: 'curl' }),
      log({ id: 'd', sessionId: 'old' }),
      log({ id: 'e', sessionId: 'now', status: SetStatus.SKIPPED }),
    ];
    expect(muscleVolume(logs, sessions, exercises, today)).toEqual([
      { muscle: 'chest', thisWeek: 2, lastWeek: 0 },
      { muscle: 'biceps', thisWeek: 0, lastWeek: 1 },
      { muscle: 'shoulders', thisWeek: 1, lastWeek: 0 },
      { muscle: 'triceps', thisWeek: 1, lastWeek: 0 },
    ]);
  });
});

describe('cardio trends', () => {
  const swim = ex('z2swim', { kind: Kind.CARDIO, modalityId: 'swim' });
  const bike = ex('4x4', { kind: Kind.CARDIO, modalityId: 'bike', intervals: true });

  test('swim pace per 100 m from laps and pool length, heart rate, time', () => {
    const t = cardioTrends(swim, [session('a', 1, { exerciseId: 'z2swim', timeSec: 1500, laps: 40, avgHr: 130 }), session('x', 2, { exerciseId: 'other', timeSec: 60 })], []);
    expect(t.count).toBe(1);
    expect(t.paceUnit).toBe('/100 m');
    expect(t.pace).toEqual([{ date, value: 2.5 }]);
    expect(t.hr).toEqual([{ date, value: 130 }]);
    expect(t.time).toEqual([{ date, value: 25 }]);
  });

  test('work interval heart rate averages the filled rows', () => {
    const intervals: IntervalLog[] = [
      { id: 'i1', sessionId: 'b', index: 0, avgHr: 160, watts: null },
      { id: 'i2', sessionId: 'b', index: 1, avgHr: 170, watts: null },
      { id: 'i3', sessionId: 'b', index: 2, avgHr: null, watts: 250 },
    ];
    const t = cardioTrends(bike, [session('b', 1, { exerciseId: '4x4', timeSec: 1800, distanceM: 15000 })], intervals);
    expect(t.paceUnit).toBe('/km');
    expect(t.pace[0].value).toBe(2);
    expect(t.workHr).toEqual([{ date, value: 165 }]);
  });

  test('pace text and the default exercise', () => {
    expect(formatPace(5.12)).toBe('5:07');
    expect(formatPace(2.5)).toBe('2:30');
    const sessions = [session('old', 1, { date: '2026-09-01', exerciseId: '4x4' }), session('new', 2, { date: '2026-09-05', exerciseId: 'z2swim' })];
    expect(defaultCardioId([bike, swim], sessions)).toBe('z2swim');
    expect(defaultCardioId([bike, swim], [])).toBe('4x4');
  });
});
