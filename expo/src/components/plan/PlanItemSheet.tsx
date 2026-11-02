import { ReactNode } from 'react';
import { ChoiceList, ChoiceRow, Sheet, SheetGroupTitle } from '@/components/ui';
import { PlanData } from '@/domain/plan';
import { Intensity, ItemType, Kind, Modality } from '@/domain/types';
import { K } from '@/theme';
import { exerciseColor } from './colors';

/** What a day holds, as the picker sees it. */
export interface PlanChoice {
  itemType: string;
  itemId: string | null;
}

/**
 * Pick what a day holds: a program, a cardio exercise, or rest (PlanSheet in TodayScreen.kt and the
 * weekday sheet in WeekPlanScreen). Used for one date on Today and for a weekday of the template.
 */
export function PlanItemSheet({
  visible,
  title,
  plan,
  modalities,
  current,
  onPick,
  onDismiss,
  footer,
}: {
  visible: boolean;
  title?: string;
  plan: PlanData;
  modalities: Modality[];
  current: PlanChoice | null;
  onPick: (choice: PlanChoice) => void;
  onDismiss: () => void;
  /** Below the lists, e.g. "Back to weekly template". */
  footer?: ReactNode;
}) {
  const cardio = plan.exercises.filter((e) => e.kind === Kind.CARDIO && !e.archived);
  const isCurrent = (itemType: string, itemId: string | null) => current?.itemType === itemType && current.itemId === itemId;
  const isRest = !current || current.itemType === ItemType.REST;
  return (
    <Sheet visible={visible} title={title} onDismiss={onDismiss}>
      <SheetGroupTitle text="Programs" />
      <ChoiceList>
        {plan.programs.map((p, i) => (
          <ChoiceRow
            key={p.id}
            title={p.name}
            selected={isCurrent(ItemType.PROGRAM, p.id)}
            dot={K.Accent}
            last={i === plan.programs.length - 1}
            onPress={() => onPick({ itemType: ItemType.PROGRAM, itemId: p.id })}
          />
        ))}
      </ChoiceList>
      <SheetGroupTitle text="Cardio exercises" />
      <ChoiceList>
        {cardio.map((e, i) => (
          <ChoiceRow
            key={e.id}
            title={e.name}
            selected={isCurrent(ItemType.CARDIO, e.id)}
            dot={exerciseColor(e, modalities)}
            dotFilled={e.intensity === Intensity.HIGH}
            last={i === cardio.length - 1}
            onPress={() => onPick({ itemType: ItemType.CARDIO, itemId: e.id })}
          />
        ))}
      </ChoiceList>
      <SheetGroupTitle text="Rest" />
      <ChoiceList>
        <ChoiceRow title="Rest" selected={isRest} dot={K.Dim} last onPress={() => onPick({ itemType: ItemType.REST, itemId: null })} />
      </ChoiceList>
      {footer}
    </Sheet>
  );
}
