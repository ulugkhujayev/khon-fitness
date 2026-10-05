import { addDays, addYears, weekday } from './dates';
import { DayOverride, Exercise, ItemType, Program, WeekPlan } from './types';

/** What a day holds once template and override are resolved. */
export interface PlanItem {
  itemType: string;
  program: Program | null;
  cardio: Exercise | null;
  overridden: boolean;
  name: string;
  isRest: boolean;
}

function planItem(itemType: string, program: Program | null = null, cardio: Exercise | null = null, overridden = false): PlanItem {
  return {
    itemType,
    program,
    cardio,
    overridden,
    name: program?.name ?? cardio?.name ?? 'Rest',
    isRest: itemType === ItemType.REST,
  };
}

export interface PlanData {
  plan: WeekPlan[];
  overrides: DayOverride[];
  programs: Program[];
  exercises: Exercise[];
}

/** Resolved plan for a date: an override wins over the weekday template. Missing items fall back to rest. */
export function itemFor(data: PlanData, date: string): PlanItem {
  const o = data.overrides.find((x) => x.date === date);
  let type: string;
  let id: string | null;
  let overridden: boolean;
  if (o) {
    type = o.itemType;
    id = o.itemId;
    overridden = !o.auto;
  } else {
    const w = data.plan.find((x) => x.weekday === weekday(date));
    type = w?.itemType ?? ItemType.REST;
    id = w?.itemId ?? null;
    overridden = false;
  }
  switch (type) {
    case ItemType.PROGRAM: {
      const p = data.programs.find((x) => x.id === id);
      return p ? planItem(type, p, null, overridden) : planItem(ItemType.REST);
    }
    case ItemType.CARDIO: {
      const e = data.exercises.find((x) => x.id === id);
      return e ? planItem(type, null, e, overridden) : planItem(ItemType.REST);
    }
    default:
      return planItem(ItemType.REST, null, null, overridden);
  }
}

/** Id of the item a plan item points at, or null for rest. */
export const itemId = (item: PlanItem): string | null => item.program?.id ?? item.cardio?.id ?? null;

export const PlanRules = {
  /** First day the app was in use: the earliest date any record carries, or null on a fresh install. */
  firstUse(dates: Iterable<string>): string | null {
    let min: string | null = null;
    for (const d of dates) if (d && (min === null || d < min)) min = d;
    return min;
  },

  /** Where freezing starts: the first-use date or one year back, whichever is earlier, so no visible past day moves. */
  freezeFrom(dates: Iterable<string>, today: string): string {
    const first = PlanRules.firstUse(dates);
    const yearBack = addYears(today, -1);
    return first !== null && first < yearBack ? first : yearBack;
  },

  /** Past dates of the weekday from first to yesterday that still follow the template. */
  datesToFreeze(day: number, first: string | null, today: string, overridden: Set<string>): string[] {
    if (first === null) return [];
    const out: string[] = [];
    for (let d = first; d < today; d = addDays(d, 1)) {
      if (weekday(d) === day && !overridden.has(d)) out.push(d);
    }
    return out;
  },

  /** Two days can swap when they differ and neither is in the past. */
  canSwap(a: string, b: string, today: string): boolean {
    return a !== b && a >= today && b >= today;
  },
};
