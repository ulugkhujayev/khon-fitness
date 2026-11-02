import { SymbolViewProps } from 'expo-symbols';
import { PlanItem } from '@/domain/plan';
import { ItemType } from '@/domain/types';

/** Symbols for what a day holds, the same on Today's card and in the week plan. */
export const PLAN_ICONS = {
  program: { ios: 'dumbbell.fill', android: 'fitness_center', web: 'fitness_center' },
  cardio: { ios: 'figure.run', android: 'directions_run', web: 'directions_run' },
  rest: { ios: 'moon.zzz.fill', android: 'bedtime', web: 'bedtime' },
  done: { ios: 'checkmark', android: 'check', web: 'check' },
} satisfies Record<string, SymbolViewProps['name']>;

/** A plan item's symbol: a dumbbell for a program, a runner for cardio, a moon for rest. */
export const itemIcon = (item: PlanItem): SymbolViewProps['name'] =>
  item.isRest ? PLAN_ICONS.rest : item.itemType === ItemType.PROGRAM ? PLAN_ICONS.program : PLAN_ICONS.cardio;
