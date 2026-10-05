/** Port of EatingWindowRulesTest.kt, plus the cases around the fixed hours. */
import { describe, expect, test } from 'bun:test';
import { EatingWindowRules, windowAlerts, windowStrip, windowSummary, withEnd, withRemind, withStart } from '../src/domain/window';
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

describe('window summary, strip, hours and alerts', () => {
  test('summary for the Today row', () => {
    expect(windowSummary(hours, null, at(18, 50))).toEqual({ open: true, text: 'closes in 1 h 10 m' });
    expect(windowSummary(hours, null, at(9))).toEqual({ open: false, text: 'opens 12:00' });
    expect(windowSummary({ ...hours, enabled: false }, null, at(13))).toEqual({ open: false, text: 'Off' });
    expect(windowSummary(null, null, at(13)).text).toBe('Off');
  });

  test('the strip marks changed days and leaves today open', () => {
    const strip = windowStrip([day({ date: '2026-09-21', openedAt: 1 }), day({ date: '2026-09-22', note: 'just a note' })], date);
    expect(strip).toHaveLength(28);
    expect(strip.slice(-3)).toEqual(['changed', 'kept', 'today']);
  });

  test('hours keep an hour apart and stay inside the day; the reminder stays 0..120', () => {
    expect(withStart(hours, 19 * 60 + 30).startMinute).toBe(19 * 60);
    expect(withEnd(hours, 12 * 60 + 15).endMinute).toBe(13 * 60);
    expect(withEnd(hours, 24 * 60 + 30).endMinute).toBe(24 * 60 - 1);
    expect(withRemind(hours, 500).remindBeforeMin).toBe(120);
  });

  test('alerts ahead: reminder and boundary for each change, today counts its manual close', () => {
    const alerts = windowAlerts(hours, [], at(9), 1);
    expect(alerts.map((a) => [new Date(a.at).getHours() * 60 + new Date(a.at).getMinutes(), a.title, a.body])).toEqual([
      [11 * 60 + 45, 'Window opens in 15 min', 'Opens at 12:00'],
      [12 * 60, 'Eating window open', 'Until 20:00'],
      [19 * 60 + 45, 'Window closes in 15 min', 'Closes at 20:00'],
      [20 * 60, 'Eating window closed', 'Opens at 12:00'],
    ]);
    const closedEarly = windowAlerts(hours, [day({ closedAt: at(13).getTime() })], at(14), 1);
    expect(closedEarly).toEqual([]);
    expect(windowAlerts({ ...hours, enabled: false }, [], at(9))).toEqual([]);
    expect(windowAlerts({ ...hours, remindBeforeMin: 0 }, [], at(9), 1).map((a) => a.title)).toEqual(['Eating window open', 'Eating window closed']);
    // A week ahead from the morning: two boundaries and two reminders a day, plus tomorrow's in the last.
    expect(windowAlerts(hours, [], at(9)).length).toBe(28);
  });
});
