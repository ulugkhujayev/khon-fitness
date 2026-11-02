/** The home-screen widget cards (TodayWidget.kt, WindowWidget.kt) and the iOS window timeline. */
import { describe, expect, test } from 'bun:test';
import { PlanItem } from '../src/domain/plan';
import { Defaults, ItemType, Session, StretchRoutine, WindowDay } from '../src/domain/types';
import { todayCard, windowCard, windowCards } from '../src/domain/widgets';

const item = (p: Partial<PlanItem>): PlanItem => ({ itemType: ItemType.PROGRAM, program: null, cardio: null, overridden: false, name: 'TP v3', isRest: false, ...p });
const routine: StretchRoutine = { id: 'daily', name: 'Daily mobility', active: true, sortOrder: 0 };
const running: Session = { ...Defaults.session, id: 's', date: '2026-10-05', startedAt: 0, itemType: ItemType.CARDIO, exerciseName: 'Zone 2 run' };
const hours = { ...Defaults.eatingWindow, enabled: true };
const at = (h: number, m = 0) => new Date(2026, 9, 5, h, m);
const day = (p: Partial<WindowDay>): WindowDay => ({ ...Defaults.windowDay, date: '2026-10-05', ...p });

describe('today widget', () => {
  test('the planned item with Start, the stretch line', () => {
    expect(todayCard(item({}), null, routine, false)).toEqual({
      sub: 'Gym', title: 'TP v3', action: 'Start', starts: true, stretch: 'Daily mobility not yet', stretched: false, description: 'Today: TP v3, Start',
    });
    expect(todayCard(item({ itemType: ItemType.CARDIO, name: 'Zone 2 run' }), null, routine, true)).toMatchObject({ sub: 'Cardio', stretch: 'Stretched today' });
  });

  test('a running session wins with Resume; a rest day only opens the app', () => {
    expect(todayCard(item({}), running, null, false)).toMatchObject({ sub: 'In progress', title: 'Zone 2 run', action: 'Resume', starts: true, stretch: null });
    expect(todayCard(item({ itemType: ItemType.REST, name: 'Rest', isRest: true }), null, null, false)).toMatchObject({ sub: 'Rest day', action: 'Open', starts: false });
  });
});

describe('window widget', () => {
  test('open until the closing time, closed until the opening time, off asks for hours', () => {
    expect(windowCard(hours, undefined, at(13))).toEqual({ open: true, status: 'Open', text: 'until 20:00', description: 'Eating window: Open, until 20:00' });
    expect(windowCard(hours, undefined, at(9)).text).toBe('opens 12:00');
    expect(windowCard(hours, undefined, at(21)).text).toBe('opens 12:00');
    expect(windowCard({ ...hours, enabled: false }, undefined, at(13))).toMatchObject({ status: 'Window off', text: 'Tap to set hours' });
  });

  test('opened late stays open until midnight', () => {
    expect(windowCard(hours, day({ openedAt: at(21).getTime() }), at(21, 30)).text).toBe('until midnight');
  });

  test('the timeline holds now and each change for two days', () => {
    const cards = windowCards(hours, [], at(9));
    expect(cards.map((c) => [c.at.getDate(), c.at.getHours(), c.card.text])).toEqual([
      [5, 9, 'opens 12:00'], [5, 12, 'until 20:00'], [5, 20, 'opens 12:00'], [6, 12, 'until 20:00'], [6, 20, 'opens 12:00'],
    ]);
    expect(windowCards({ ...hours, enabled: false }, [], at(9))).toHaveLength(1);
  });
});
