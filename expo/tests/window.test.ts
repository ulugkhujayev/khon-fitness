/** Port of EatingWindowRulesTest.kt, plus the cases around the fixed hours. */
import { describe, expect, test } from 'bun:test';
import { EatingWindowRules } from '../src/domain/window';
import { EatingWindow, WindowDay } from '../src/domain/types';

const hours: EatingWindow = { id: 1, enabled: true, startMinute: 12 * 60, endMinute: 20 * 60, remindBeforeMin: 15 };
const date = '2026-09-23';
const at = (h: number, m = 0) => new Date(2026, 8, 23, h, m);
const day = (d: Partial<WindowDay>): WindowDay => ({ date, openedAt: null, closedAt: null, note: '', ...d });

describe('EatingWindowRules', () => {
  test('open now after scheduled end stays open until manually closed or midnight', () => {
    const opened = day({ openedAt: at(20, 30).getTime() });
    const late = EatingWindowRules.state(hours, opened, at(20, 31));
    expect(late.open).toBe(true);
    expect(late.changesAt.getTime()).toBe(new Date(2026, 8, 24, 0, 0).getTime());

    const closed = EatingWindowRules.state(hours, { ...opened, closedAt: at(20, 45).getTime() }, at(20, 46));
    expect(closed.open).toBe(false);
  });

  test('opening early does not reopen after the scheduled end', () => {
    const opened = day({ openedAt: at(11, 30).getTime() });
    expect(EatingWindowRules.state(hours, opened, at(11, 31)).open).toBe(true);
    expect(EatingWindowRules.state(hours, opened, at(20, 31)).open).toBe(false);
  });

  test('fixed hours open at start and close at end, changes point at the next edge', () => {
    const before = EatingWindowRules.state(hours, null, at(9));
    expect(before.open).toBe(false);
    expect(before.changesAt.getTime()).toBe(at(12).getTime());
    expect(before.kept).toBe(true);

    const inside = EatingWindowRules.state(hours, null, at(12));
    expect(inside.open).toBe(true);
    expect(inside.changesAt.getTime()).toBe(at(20).getTime());

    const after = EatingWindowRules.state(hours, null, at(20));
    expect(after.open).toBe(false);
    expect(after.changesAt.getTime()).toBe(new Date(2026, 8, 24, 12, 0).getTime());
  });

  test('formats hours and remaining time', () => {
    expect(EatingWindowRules.hhmm(12 * 60 + 5)).toBe('12:05');
    expect(EatingWindowRules.untilText(at(9, 50), at(12))).toBe('2 h 10 m');
    expect(EatingWindowRules.untilText(at(11, 20), at(12))).toBe('40 m');
    expect(EatingWindowRules.untilText(at(13), at(12))).toBe('0 m');
  });
});
