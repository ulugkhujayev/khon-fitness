import { useState } from 'react';
import { EditorScreen } from '@/components/EditorScreen';
import { ChoiceList, ChoiceRow, Sheet, SheetGroupTitle } from '@/components/Sheet';
import { GroupedList, ListRow } from '@/components/ui';
import { exerciseColor, itemColor } from '@/components/colors';
import { weekdayName } from '@/domain/dates';
import { Exercise, Intensity, ItemType, Kind, Program } from '@/domain/types';
import { PlanItem } from '@/domain/plan';
import * as repo from '@/db/repository';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K } from '@/theme';

const DAYS = [1, 2, 3, 4, 5, 6, 7];

const templateItem = (itemType: string | undefined, program: Program | null, cardio: Exercise | null): PlanItem => {
  if (itemType === ItemType.PROGRAM && program) return { itemType, program, cardio: null, overridden: false, name: program.name, isRest: false };
  if (itemType === ItemType.CARDIO && cardio) return { itemType, program: null, cardio, overridden: false, name: cardio.name, isRest: false };
  return { itemType: ItemType.REST, program: null, cardio: null, overridden: false, name: 'Rest', isRest: true };
};

/**
 * The weekly template (WeekPlanScreen in ProgramsScreen.kt). A change fills future dates only:
 * saveWeekPlan freezes the past days of that weekday first.
 */
export default function WeekPlan() {
  const { data } = useLive(
    async () => {
      const [plan, modalities] = await Promise.all([repo.planData(), repo.modalities()]);
      return { plan, modalities };
    },
    [...repo.PLAN_TABLES, T.modalities],
  );
  const [editing, setEditing] = useState<number | null>(null);
  const [open, setOpen] = useState(false);
  if (!data) return <EditorScreen title="Week plan">{null}</EditorScreen>;
  const { plan, modalities } = data;

  const itemOf = (day: number) => {
    const w = plan.plan.find((x) => x.weekday === day);
    return templateItem(
      w?.itemType,
      plan.programs.find((p) => p.id === w?.itemId) ?? null,
      plan.exercises.find((e) => e.id === w?.itemId) ?? null,
    );
  };
  const current = editing == null ? null : plan.plan.find((w) => w.weekday === editing);
  const cardio = plan.exercises.filter((e) => e.kind === Kind.CARDIO && !e.archived);
  const pick = (type: string, id: string | null) => {
    if (editing != null) repo.saveWeekPlan(editing, type, id);
    setOpen(false);
  };

  return (
    <EditorScreen title="Week plan">
      <GroupedList>
        {DAYS.map((day) => {
          const item = itemOf(day);
          const c = itemColor(item, modalities);
          return (
            <ListRow
              key={day}
              title={weekdayName(day)}
              value={item.name}
              dot={c?.color ?? K.Dim}
              dotFilled={c?.filled ?? false}
              last={day === 7}
              onPress={() => {
                setEditing(day);
                setOpen(true);
              }}
            />
          );
        })}
      </GroupedList>

      <Sheet visible={open} title={editing != null ? weekdayName(editing) : undefined} onDismiss={() => setOpen(false)}>
        <SheetGroupTitle text="Programs" />
        <ChoiceList>
          {plan.programs.map((p, i) => (
            <ChoiceRow
              key={p.id}
              title={p.name}
              selected={current?.itemType === ItemType.PROGRAM && current.itemId === p.id}
              dot={K.Accent}
              last={i === plan.programs.length - 1}
              onPress={() => pick(ItemType.PROGRAM, p.id)}
            />
          ))}
        </ChoiceList>
        <SheetGroupTitle text="Cardio exercises" />
        <ChoiceList>
          {cardio.map((e, i) => (
            <ChoiceRow
              key={e.id}
              title={e.name}
              selected={current?.itemType === ItemType.CARDIO && current.itemId === e.id}
              dot={exerciseColor(e, modalities)}
              dotFilled={e.intensity === Intensity.HIGH}
              last={i === cardio.length - 1}
              onPress={() => pick(ItemType.CARDIO, e.id)}
            />
          ))}
        </ChoiceList>
        <SheetGroupTitle text="Rest" />
        <ChoiceList>
          <ChoiceRow title="Rest" selected={!current || current.itemType === ItemType.REST} dot={K.Dim} last onPress={() => pick(ItemType.REST, null)} />
        </ChoiceList>
      </Sheet>
    </EditorScreen>
  );
}
