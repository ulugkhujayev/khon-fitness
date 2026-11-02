/** Today's bodyweight entry: the ruler math and the rules around Add. */
import { describe, expect, test } from 'bun:test';
import { BODYWEIGHT_RULER, changeSince, DEFAULT_KG, entryAction, startKg } from '../src/domain/bodyweight';
import { offsetOf, rulerInset, rulerSegments, tickCount, tickIndex, tickKind, tickValue, valueAt } from '../src/domain/ruler';

const r = BODYWEIGHT_RULER;
const today = '2026-10-06';

describe('ruler', () => {
  test('ticks cover both ends in steps of 0.1 kg', () => {
    expect(tickCount(r)).toBe(2301);
    expect(tickValue(r, 0)).toBe(20);
    expect(tickValue(r, 2300)).toBe(250);
  });

  test('values do not drift with floating point steps', () => {
    expect(tickValue(r, 606)).toBe(80.6);
    for (let i = 0; i < tickCount(r); i++) expect(tickValue(r, tickIndex(r, tickValue(r, i)))).toBe(tickValue(r, i));
  });

  test('a value off the ruler lands on its nearer end', () => {
    expect(tickValue(r, tickIndex(r, 5))).toBe(20);
    expect(tickValue(r, tickIndex(r, 400))).toBe(250);
    expect(tickValue(r, -3)).toBe(20);
  });

  test('offsets are whole gaps, and between two ticks the nearer one wins', () => {
    expect(offsetOf(r, 80.6)).toBe(6060);
    expect(valueAt(r, 6060)).toBe(80.6);
    expect(valueAt(r, 6064)).toBe(80.6);
    expect(valueAt(r, 6066)).toBe(80.7);
    expect(valueAt(r, -40)).toBe(20);
    expect(offsetOf(r, 80.66)).toBe(6070);
  });

  test('the inset puts the first tick under the center line at offset 0', () => {
    const width = 343;
    // Tick 0 is centered in its first gap; the center line sits at offset + width / 2.
    expect(rulerInset(r, width) + r.gap / 2).toBe(width / 2);
    expect(rulerInset(r, 0)).toBe(0);
  });

  test('a label every kilogram, a half tick at .5', () => {
    expect(tickKind(r, 0)).toBe('major');
    expect(tickKind(r, 5)).toBe('half');
    expect(tickKind(r, 7)).toBe('minor');
    expect(tickKind(r, 610)).toBe('major');
  });

  test('segments hold ten ticks each and the last one the end tick alone', () => {
    const segments = rulerSegments(r);
    expect(segments).toHaveLength(231);
    expect(segments[0]).toEqual({ first: 0, count: 10, label: '20' });
    expect(segments[60]).toEqual({ first: 600, count: 10, label: '80' });
    expect(segments.at(-1)).toEqual({ first: 2300, count: 1, label: '250' });
    expect(segments.reduce((n, s) => n + s.count, 0)).toBe(tickCount(r));
  });
});

describe('bodyweight entry', () => {
  const weights = [
    { date: '2026-10-01', kg: 81 },
    { date: '2026-10-05', kg: 80.4 },
  ];

  test('starts at the latest entry, or the default before the first', () => {
    expect(startKg(weights)).toBe(80.4);
    expect(startKg([])).toBe(DEFAULT_KG);
  });

  test('change since the last entry before today', () => {
    expect(changeSince(weights, today, 80.6)).toBe('+0.2 kg since Oct 5');
    expect(changeSince(weights, today, 79.9)).toBe('−0.5 kg since Oct 5');
    expect(changeSince(weights, today, 80.4)).toBe('Same as Oct 5');
    expect(changeSince([], today, 80)).toBe('');
  });

  test("today's own entry is not the baseline", () => {
    const withToday = [...weights, { date: today, kg: 80.1 }];
    expect(changeSince(withToday, today, 80.1)).toBe('−0.3 kg since Oct 5');
  });

  test("Add for a new day, Update for another value today, Saved when it is today's entry", () => {
    expect(entryAction(weights, today, 80.4)).toEqual({ text: 'Add', enabled: true });
    const withToday = [...weights, { date: today, kg: 80.1 }];
    expect(entryAction(withToday, today, 80.1)).toEqual({ text: 'Saved', enabled: false });
    expect(entryAction(withToday, today, 80.2)).toEqual({ text: 'Update', enabled: true });
    expect(entryAction(weights, today, 0)).toEqual({ text: 'Add', enabled: false });
  });
});
