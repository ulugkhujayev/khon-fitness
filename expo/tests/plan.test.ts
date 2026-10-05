/** Port of PlanRulesTest.kt. Today is Thursday 2026-09-24. */
import { describe, expect, test } from 'bun:test';
import { parseBackup } from '../src/domain/backup';
import { addDays } from '../src/domain/dates';
import { doneSessionFor, itemFor, PlanData, PlanRules, templateItem } from '../src/domain/plan';
import { DayOverride, Defaults, Exercise, ItemType, Kind, Program, Session } from '../src/domain/types';

const today = '2026-09-24';
const ex = (id: string, name: string): Exercise => ({ ...Defaults.exercise, id, name, kind: Kind.CARDIO });
const swim = ex('z2swim', 'Zone 2 swim');
const run = ex('z2run', 'Zone 2 run');
const tp: Program = { id: 'tpv3', name: 'TP v3', active: false, sortOrder: 0 };
const data = (...ov: DayOverride[]): PlanData => ({
  plan: [{ weekday: 4, itemType: ItemType.CARDIO, itemId: 'z2run' }],
  overrides: ov,
  programs: [tp],
  exercises: [swim, run],
});
const o = (date: string, itemType: string, itemId: string | null, auto = false): DayOverride => ({ date, itemType, itemId, auto });

describe('PlanRules', () => {
  test('auto override sets the item but shows no mark', () => {
    const item = itemFor(data(o('2026-09-17', ItemType.CARDIO, 'z2swim', true)), '2026-09-17');
    expect(item.name).toBe('Zone 2 swim');
    expect(item.overridden).toBe(false);
  });

  test('user override shows the mark and template days do not', () => {
    const plan = data(o('2026-09-17', ItemType.PROGRAM, 'tpv3'));
    expect(itemFor(plan, '2026-09-17').name).toBe('TP v3');
    expect(itemFor(plan, '2026-09-17').overridden).toBe(true);
    expect(itemFor(plan, today).name).toBe('Zone 2 run');
    expect(itemFor(plan, today).overridden).toBe(false);
  });

  test('auto rest override stays unmarked', () => {
    const item = itemFor(data(o('2026-09-17', ItemType.REST, null, true)), '2026-09-17');
    expect(item.isRest).toBe(true);
    expect(item.overridden).toBe(false);
  });

  test('missing program or exercise falls back to rest', () => {
    expect(itemFor(data(o(today, ItemType.PROGRAM, 'gone')), today).isRest).toBe(true);
    expect(itemFor(data(), '2026-09-21').isRest).toBe(true); // Monday has no template row
  });

  test('first use is the earliest date or null', () => {
    expect(PlanRules.firstUse(['2026-09-01', '2026-08-03', '', '2026-08-10'])).toBe('2026-08-03');
    expect(PlanRules.firstUse([])).toBeNull();
  });

  test('freeze starts a year back even when first use is recent or unknown', () => {
    expect(PlanRules.freezeFrom(['2026-09-23'], today)).toBe('2025-09-24');
    expect(PlanRules.freezeFrom([], today)).toBe('2025-09-24');
    expect(PlanRules.freezeFrom(['2024-01-05', '2026-09-23'], today)).toBe('2024-01-05');
  });

  test('freeze covers past weekdays from first use to yesterday and skips overrides', () => {
    expect(PlanRules.datesToFreeze(4, '2026-09-01', today, new Set(['2026-09-10']))).toEqual(['2026-09-03', '2026-09-17']);
  });

  test('freeze writes nothing without a first use date or when first use is today', () => {
    expect(PlanRules.datesToFreeze(4, null, today, new Set())).toEqual([]);
    expect(PlanRules.datesToFreeze(4, today, today, new Set())).toEqual([]);
  });

  test('swap needs two different dates from today on', () => {
    expect(PlanRules.canSwap(today, addDays(today, 1), today)).toBe(true);
    expect(PlanRules.canSwap(addDays(today, 9), addDays(today, 2), today)).toBe(true);
    expect(PlanRules.canSwap(today, addDays(today, -1), today)).toBe(false);
    expect(PlanRules.canSwap(addDays(today, -3), addDays(today, 1), today)).toBe(false);
    expect(PlanRules.canSwap(today, today, today)).toBe(false);
  });

  test('backup reads old overrides without auto as user edits', () => {
    const b = parseBackup(JSON.stringify({
      exercises: [], modalities: [], programs: [], blocks: [], blockExercises: [], sessions: [], setLogs: [], intervalLogs: [],
      bodyweights: [], weekPlan: [], overrides: [{ date: '2026-09-17', itemType: 'rest' }, { date: '2026-09-18', itemType: 'cardio', itemId: 'z2swim', auto: true }],
    }));
    expect(b.overrides[0].auto).toBe(false);
    expect(b.overrides[0].itemId).toBeNull();
    expect(b.overrides[1].auto).toBe(true);
  });
});

describe('template and done session', () => {
  test('templateItem ignores overrides and falls back to rest', () => {
    const plan = data(o('2026-09-24', ItemType.PROGRAM, 'tpv3'));
    expect(templateItem(plan, 4).name).toBe('Zone 2 run');
    expect(templateItem(plan, 4).overridden).toBe(false);
    expect(templateItem(plan, 1)).toMatchObject({ name: 'Rest', isRest: true });
    expect(templateItem({ ...plan, plan: [{ weekday: 2, itemType: ItemType.PROGRAM, itemId: 'gone' }] }, 2).isRest).toBe(true);
  });

  test('doneSessionFor needs a finished session of the same item on the date', () => {
    const item = itemFor(data(), today);
    const s = (id: string, p: Partial<Session>): Session => ({
      ...Defaults.session, id, date: today, startedAt: 0, itemType: ItemType.CARDIO, exerciseId: 'z2run', finishedAt: 1, ...p,
    });
    expect(doneSessionFor([s('open', { finishedAt: null }), s('other', { exerciseId: 'z2swim' }), s('ok', {})], item, today)?.id).toBe('ok');
    expect(doneSessionFor([s('yesterday', { date: addDays(today, -1) })], item, today)).toBeUndefined();
  });
});
