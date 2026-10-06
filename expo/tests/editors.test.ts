import { describe, expect, test } from 'bun:test';
import { axisWidth, dateLabelIndices, niceStep, scaleFor, valueLabelIndices } from '../src/domain/chart';
import { longDate, monthDay, monthGrid, monthStart, monthTitle, shortDate, weekdayName, weekOf } from '../src/domain/dates';
import {
  asCardio, asStrength, blockSummary, filterLibrary, freeSlot, moveInOrder, newExercise, nextSortOrder, noResults, searchByName, toggleIn, withIntervals,
  withMaxReps, withMinReps, withSets,
} from '../src/domain/editors';
import { SEED_EXERCISES } from '../src/domain/seed';
import { Block, BlockExercise, Kind } from '../src/domain/types';

const be: BlockExercise = { id: 'x', blockId: 'b', slot: 0, exerciseId: 'bench', sets: 3, minReps: 8, maxReps: 12 };

describe('block editor', () => {
  test('sets stay 1..10', () => {
    expect(withSets(be, 0).sets).toBe(1);
    expect(withSets(be, 14).sets).toBe(10);
    expect(withSets(be, null).sets).toBe(1);
  });

  test('min and max reps keep a valid range', () => {
    expect(withMinReps(be, 15)).toMatchObject({ minReps: 15, maxReps: 15 });
    expect(withMaxReps(be, 6)).toMatchObject({ minReps: 6, maxReps: 6 });
    expect(withMinReps(be, 99).minReps).toBe(50);
  });

  test('a new exercise takes the free slot', () => {
    expect(freeSlot([])).toBe(0);
    expect(freeSlot([{ ...be, slot: 1 }])).toBe(0);
    expect(freeSlot([be])).toBe(1);
    expect(freeSlot([be, { ...be, slot: 1 }])).toBeNull();
  });

  test('summary lists names A first and sums sets; a new block goes after the highest order', () => {
    const exs = [{ ...newExercise('bench'), name: 'Bench' }, { ...newExercise('row'), name: 'Row' }];
    const bes = [{ ...be, id: 'y', slot: 1, exerciseId: 'row', sets: 4 }, be, { ...be, id: 'z', blockId: 'other' }];
    expect(blockSummary('b', bes, exs)).toEqual({ title: 'Bench · Row', sets: 7 });
    expect(blockSummary('none', bes, exs)).toEqual({ title: '', sets: 0 });
    const blocks: Block[] = [0, 5, 2].map((o, i) => ({ id: `b${i}`, programId: 'p', sortOrder: o, superset: true }));
    expect(nextSortOrder(blocks)).toBe(6);
    expect(nextSortOrder([])).toBe(0);
  });

  test('name search ignores case and surrounding spaces', () => {
    const list = [{ name: 'Incline DB Press' }, { name: 'Leg Press' }, { name: 'Row' }];
    expect(searchByName(list, '  PRESS ').map((x) => x.name)).toEqual(['Incline DB Press', 'Leg Press']);
    expect(searchByName(list, '')).toHaveLength(3);
  });

  test('a search that finds nothing names the query and what to try', () => {
    expect(noResults('  squat ')).toEqual({ title: 'No Results for “squat”', hint: 'Check the spelling or try a new search.' });
    expect(noResults(' ')).toEqual({ title: 'No Results', hint: 'Try another filter.' });
  });

  test('moving a block swaps sort orders with its neighbour', () => {
    const bs: Block[] = [0, 1, 2].map((i) => ({ id: `b${i}`, programId: 'p', sortOrder: i, superset: true }));
    expect(moveInOrder(bs, 1, -1).map((b) => [b.id, b.sortOrder])).toEqual([['b1', 0], ['b0', 1]]);
    expect(moveInOrder(bs, 2, 1)).toEqual([]);
  });
});

describe('exercise editor', () => {
  test('kind switch fills cardio defaults and keeps what is set', () => {
    const c = asCardio(newExercise('n'), 'bike');
    expect(c).toMatchObject({ kind: Kind.CARDIO, modalityId: 'bike', intensity: 'low', fields: 'time,distance,avgHr' });
    expect(asStrength(c)).toMatchObject({ kind: Kind.STRENGTH, primaryMuscle: 'chest' });
    expect(asCardio({ ...c, fields: 'laps' }, 'run').fields).toBe('laps');
  });

  test('intervals on fills an empty structure only', () => {
    expect(withIntervals(newExercise('n'), true)).toMatchObject({ intervals: true, rounds: 4, workSec: 240, restSec: 180 });
    expect(withIntervals({ ...newExercise('n'), rounds: 8, workSec: 20, restSec: 10 }, true)).toMatchObject({ rounds: 8, workSec: 20, restSec: 10 });
  });

  test('toggleIn adds and removes from a comma list', () => {
    expect(toggleIn('glutes,hamstrings', 'glutes')).toBe('hamstrings');
    expect(toggleIn('', 'core')).toBe('core');
  });

  test('library filter: kind, muscle incl. secondary, search, archived', () => {
    const list = [...SEED_EXERCISES, { ...newExercise('gone'), name: 'Old curl', primaryMuscle: 'biceps', archived: true }];
    const base = { query: '', kind: '', muscle: '', showArchived: false };
    expect(filterLibrary(list, base).some((e) => e.id === 'gone')).toBe(false);
    expect(filterLibrary(list, { ...base, showArchived: true }).some((e) => e.id === 'gone')).toBe(true);
    expect(filterLibrary(list, { ...base, kind: Kind.CARDIO }).every((e) => e.kind === Kind.CARDIO)).toBe(true);
    const glutes = filterLibrary(list, { ...base, muscle: 'glutes' });
    expect(glutes.some((e) => e.primaryMuscle !== 'glutes' && e.secondaryMuscles.includes('glutes'))).toBe(true);
    expect(filterLibrary(list, { ...base, query: '  BENCH ' }).every((e) => e.name.toLowerCase().includes('bench'))).toBe(true);
  });
});

describe('chart scale', () => {
  test('round steps and a padded flat line', () => {
    expect(niceStep(3, 3)).toBe(1);
    expect(niceStep(0.9, 3)).toBe(0.5);
    expect(niceStep(1000, 3)).toBe(100);
    expect(scaleFor([80])).toEqual({ yMin: 79, yMax: 81, step: 1, ticks: [79, 80, 81] });
    expect(scaleFor([78.4, 79.1, 80.6])).toMatchObject({ yMin: 78, yMax: 81, step: 1 });
  });

  test('labels: all points up to seven, else the highest and the last', () => {
    expect([...valueLabelIndices([1, 2, 3])]).toEqual([0, 1, 2]);
    expect([...valueLabelIndices([1, 9, 3, 4, 5, 6, 7, 2])].sort()).toEqual([1, 7]);
  });

  test('dates: first and last, at most four', () => {
    expect(dateLabelIndices(1, 300)).toEqual([0]);
    expect(dateLabelIndices(30, 300)).toEqual([0, 10, 19, 29]);
    expect(dateLabelIndices(30, 100)).toEqual([0, 29]);
  });
});

describe('date labels', () => {
  test('English names, never the device locale', () => {
    expect(longDate('2026-10-05')).toBe('Monday, October 5, 2026');
    expect(shortDate('2026-10-11')).toBe('Sun, Oct 11');
    expect(monthDay('2026-01-31')).toBe('Jan 31');
    expect(weekdayName(7)).toBe('Sunday');
    expect(weekOf('2026-10-08')).toEqual(['2026-10-05', '2026-10-06', '2026-10-07', '2026-10-08', '2026-10-09', '2026-10-10', '2026-10-11']);
  });

  test('month grid starts on Monday and pads to whole weeks', () => {
    expect(monthStart('2026-10-31')).toBe('2026-10-01');
    expect(monthStart('2026-01-15', -1)).toBe('2025-12-01');
    expect(monthStart('2026-12-15', 1)).toBe('2027-01-01');
    expect(monthTitle('2026-10-05')).toBe('October 2026');
    const oct = monthGrid('2026-10-05');
    // October 1, 2026 is a Thursday.
    expect(oct.slice(0, 4)).toEqual([null, null, null, '2026-10-01']);
    expect(oct.filter((d) => d !== null).length).toBe(31);
    expect(oct.length % 7).toBe(0);
    // October 31 is a Saturday, so Sunday pads the last week.
    expect(oct.slice(-2)).toEqual(['2026-10-31', null]);
    // March 2026 holds the DST switch in most zones; every day still appears once.
    expect(new Set(monthGrid('2026-03-10').filter(Boolean)).size).toBe(31);
  });
});

describe('value axis width', () => {
  test('hugs the widest label', () => {
    expect(axisWidth(['77', '78', '79', '80', '81'], 12)).toBe(15);
    expect(axisWidth(['80', '80.5', '81'], 12)).toBe(26);
    expect(axisWidth(['5:00', '5:30'], 12)).toBe(26);
    expect(axisWidth(['100', '120'], 12)).toBeGreaterThan(axisWidth(['80'], 12));
  });

  test('no labels, no width', () => {
    expect(axisWidth([], 12)).toBe(0);
  });
});
