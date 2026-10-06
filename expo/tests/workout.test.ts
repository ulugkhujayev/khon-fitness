import { describe, expect, test } from 'bun:test';
import { fmt, parseNumberInput, sets } from '../src/domain/format';
import { Defaults, hexAlpha, ItemType, Session, SetLog, SetStatus } from '../src/domain/types';
import {
  blockTitle, clampEffort, doneCount, doneLine, progressLine, openBlocks, gain, groupBlocks, isBlockDone, kgDecimals, afterCheck, headerIndices, setTag, summarize,
} from '../src/domain/workout';

const log = (p: Partial<SetLog> & Pick<SetLog, 'id' | 'order'>): SetLog => ({
  ...Defaults.setLog,
  sessionId: 's',
  blockIndex: 0,
  blockSuperset: true,
  slot: 0,
  setIndex: 0,
  exerciseId: 'bench',
  exerciseName: 'Bench press',
  minReps: 8,
  maxReps: 12,
  stepKg: 2.5,
  ...p,
});

const session = (p: Partial<Session>): Session => ({
  ...Defaults.session, id: 's', date: '2026-10-05', startedAt: 0, itemType: ItemType.PROGRAM, ...p,
});

describe('summarize', () => {
  test('counts done sets and volume, skips pending ones', () => {
    const logs = [
      log({ id: 'a', order: 0, weightKg: 60, reps: 8, status: SetStatus.DONE }),
      log({ id: 'b', order: 1, weightKg: 60, reps: 6, status: SetStatus.DONE }),
      log({ id: 'c', order: 2, weightKg: 60, reps: 8, status: SetStatus.PENDING }),
    ];
    const s = summarize(logs, logs, 's');
    expect(s).toEqual({ done: 2, total: 3, volume: 60 * 8 + 60 * 6, bests: 1 });
  });

  test('a best needs a higher e1RM than every earlier done set of that exercise', () => {
    const before = [
      log({ id: 'old1', order: 0, sessionId: 'old', weightKg: 62.5, reps: 8, status: SetStatus.DONE }),
      log({ id: 'old2', order: 1, sessionId: 'old', exerciseId: 'row', weightKg: 40, reps: 10, status: SetStatus.DONE }),
      log({ id: 'old3', order: 2, sessionId: 'old', exerciseId: 'row', weightKg: 90, reps: 10, status: SetStatus.SKIPPED }),
    ];
    const now = [
      log({ id: 'a', order: 0, weightKg: 60, reps: 8, status: SetStatus.DONE }),
      log({ id: 'b', order: 1, exerciseId: 'row', weightKg: 42.5, reps: 10, status: SetStatus.DONE }),
    ];
    expect(summarize(now, [...before, ...now], 's').bests).toBe(1);
  });
});

describe('session flow', () => {
  const logs = [
    log({ id: 'a1', order: 0, blockIndex: 0, status: SetStatus.DONE }),
    log({ id: 'b1', order: 1, blockIndex: 0, slot: 1, status: SetStatus.DONE }),
    log({ id: 'c1', order: 3, blockIndex: 1, status: SetStatus.PENDING }),
    log({ id: 'a2', order: 2, blockIndex: 0, setIndex: 1, status: SetStatus.SKIPPED }),
    log({ id: 'c2', order: 4, blockIndex: 1, setIndex: 1, status: SetStatus.PENDING }),
  ];

  test('groups by block in execution order', () => {
    expect(groupBlocks(logs).map((b) => b.map((l) => l.id))).toEqual([['a1', 'b1', 'a2'], ['c1', 'c2']]);
  });

  test('only the first block with work left is open, unless the user chose', () => {
    const three = groupBlocks([...logs, log({ id: 'd1', order: 5, blockIndex: 2, status: SetStatus.PENDING })]);
    expect(openBlocks(three, {})).toEqual([false, true, false]);
    expect(openBlocks(three, { 0: true, 1: false })).toEqual([true, false, false]);
    expect(openBlocks(three, { 2: true })).toEqual([false, true, true]);
  });

  test('a check keeps its block open while it has a pending set, focus on the next one', () => {
    const blocks = groupBlocks(logs);
    expect(afterCheck(blocks, logs[2])).toEqual({ open: 1, focus: logs[4] });
    // c2 checked before c1: c1 was left behind, so focus goes back to it.
    expect(afterCheck(blocks, logs[4])).toEqual({ open: 1, focus: logs[2] });
  });

  test('a finished block opens the next one with a pending set, wrapping to an earlier one', () => {
    const three = [
      log({ id: 'a1', order: 0, blockIndex: 0, status: SetStatus.PENDING }),
      log({ id: 'b1', order: 1, blockIndex: 1, status: SetStatus.PENDING }),
      log({ id: 'b2', order: 2, blockIndex: 1, setIndex: 1, status: SetStatus.DONE }),
      log({ id: 'c1', order: 3, blockIndex: 2, status: SetStatus.PENDING }),
    ];
    const blocks = groupBlocks(three);
    expect(afterCheck(blocks, three[1])).toEqual({ open: 2, focus: three[3] });
    expect(afterCheck(blocks, three[3])).toEqual({ open: 0, focus: three[0] });
  });

  test('block headers among the list children, each open block adding its sets', () => {
    expect(headerIndices([false, false, false])).toEqual([0, 1, 2]);
    expect(headerIndices([true, false, false])).toEqual([0, 2, 3]);
    expect(headerIndices([true, true, false])).toEqual([0, 2, 4]);
    expect(headerIndices([])).toEqual([]);
  });

  test('the last pending set closes every block', () => {
    const blocks = groupBlocks(logs.map((l) => (l.id === 'c1' ? { ...l, status: SetStatus.DONE } : l)));
    expect(afterCheck(blocks, logs[4])).toEqual({ open: null, focus: undefined });
  });

  test('tags, decimals and gains', () => {
    expect(setTag({ slot: 1, setIndex: 2 })).toBe('B3');
    expect(kgDecimals(2.5)).toBe(1);
    expect(kgDecimals(5)).toBe(0);
    expect(kgDecimals(2, 62.5)).toBe(1);
    expect(kgDecimals(2, 62, null)).toBe(0);
    expect(gain(60, 62.5, 1, 'kg')).toBe('+2.5 kg');
    expect(gain(8, 9, 0, 'reps')).toBe('+1 reps');
    expect(gain(60, 60, 1, 'kg')).toBeNull();
    expect(gain(null, 60, 1, 'kg')).toBeNull();
  });
});

describe('doneLine', () => {
  test('program: sets and tonnes', () => {
    const logs = [
      log({ id: 'a', order: 0, weightKg: 100, reps: 10, status: SetStatus.DONE }),
      log({ id: 'b', order: 1, weightKg: 100, reps: 10, status: SetStatus.SKIPPED }),
      log({ id: 'x', order: 0, sessionId: 'other', weightKg: 100, reps: 10, status: SetStatus.DONE }),
    ];
    expect(doneLine(session({}), logs)).toBe('Done · 1 set · 1 t');
  });

  test('cardio: minutes and kilometres when present', () => {
    expect(doneLine(session({ itemType: ItemType.CARDIO, timeSec: 1830, distanceM: 5250 }), [])).toBe('Done · 30 min · 5.3 km');
    expect(doneLine(session({ itemType: ItemType.CARDIO }), [])).toBe('Done');
  });
});

describe('number input', () => {
  test('comma becomes a dot, junk goes, partial text has no value', () => {
    expect(parseNumberInput('62,5')).toEqual({ text: '62.5', value: 62.5 });
    expect(parseNumberInput('8 reps')).toEqual({ text: '8', value: 8 });
    expect(parseNumberInput('')).toEqual({ text: '', value: null });
    expect(parseNumberInput('.')).toEqual({ text: '.', value: null });
    expect(parseNumberInput('5.')).toEqual({ text: '5.', value: 5 });
    expect(parseNumberInput('1.2.3').value).toBeNull();
  });

  test('fmt drops a trailing .0', () => {
    expect(fmt(60)).toBe('60');
    expect(fmt(62.5)).toBe('62.5');
    expect(fmt(62.5, 0)).toBe('62');
    expect(fmt(null)).toBe('');
    expect(sets(1)).toBe('1 set');
    expect(sets(6)).toBe('6 sets');
  });
});

describe('block helpers', () => {
  const logs = [
    log({ id: 'b1', order: 1, slot: 1, exerciseName: 'Leg Press', status: SetStatus.DONE }),
    log({ id: 'a1', order: 0, status: SetStatus.SKIPPED }),
    log({ id: 'a2', order: 2, setIndex: 1, status: SetStatus.PENDING }),
  ];

  test('title lists each exercise once, A first', () => {
    expect(blockTitle(logs)).toBe('Bench press · Leg Press');
  });

  test('done means nothing pending; done count counts only logged sets', () => {
    expect(isBlockDone(logs)).toBe(false);
    expect(isBlockDone(logs.slice(0, 2))).toBe(true);
    expect(doneCount(logs)).toBe(1);
  });

  test('effort is a whole number 1..10 or empty', () => {
    expect(clampEffort(15)).toBe(10);
    expect(clampEffort(0)).toBe(1);
    expect(clampEffort(7.6)).toBe(7);
    expect(clampEffort(null)).toBeNull();
  });
});

describe('Today card', () => {
  const start = new Date(2026, 9, 6, 18, 0).getTime();
  const at = (min: number) => start + min * 60000;

  test('the running line counts whole minutes since the start', () => {
    expect(progressLine(start, at(0.5))).toBe('In progress');
    expect(progressLine(start, at(34))).toBe('In progress · 34 min');
    expect(progressLine(start, at(65))).toBe('In progress · 1 h 5 min');
    expect(progressLine(start, at(-2))).toBe('In progress');
  });

  test('a hex color with alpha', () => {
    expect(hexAlpha('#FF9F0A', 0.2)).toBe('rgba(255,159,10,0.2)');
    expect(hexAlpha('#30D158', 1)).toBe('rgba(48,209,88,1)');
  });
});
