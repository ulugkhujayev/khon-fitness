import { useState } from 'react';
import { EditorScreen } from '@/components/layout';
import { itemColor } from '@/components/plan/colors';
import { PlanItemSheet } from '@/components/plan/PlanItemSheet';
import { GroupedList, ListRow } from '@/components/ui';
import { weekdayName } from '@/domain/dates';
import { itemId, templateItem } from '@/domain/plan';
import { usePlan } from '@/db/hooks';
import * as repo from '@/db/repository';
import { K } from '@/theme';

const DAYS = [1, 2, 3, 4, 5, 6, 7];

/**
 * The weekly template (WeekPlanScreen in ProgramsScreen.kt). A change fills future dates only:
 * saveWeekPlan freezes the past days of that weekday first.
 */
export default function WeekPlan() {
  const { data } = usePlan();
  // The weekday stays set while its sheet slides out.
  const [day, setDay] = useState(1);
  const [open, setOpen] = useState(false);
  if (!data) return <EditorScreen title="Week plan" />;
  const { plan, modalities } = data;
  const current = templateItem(plan, day);

  return (
    <EditorScreen title="Week plan">
      <GroupedList>
        {DAYS.map((d) => {
          const item = templateItem(plan, d);
          const c = itemColor(item, modalities);
          return (
            <ListRow
              key={d}
              title={weekdayName(d)}
              value={item.name}
              dot={c?.color ?? K.Dim}
              dotFilled={c?.filled ?? false}
              last={d === 7}
              onPress={() => {
                setDay(d);
                setOpen(true);
              }}
            />
          );
        })}
      </GroupedList>
      <PlanItemSheet
        visible={open}
        title={weekdayName(day)}
        plan={plan}
        modalities={modalities}
        current={{ itemType: current.itemType, itemId: itemId(current) }}
        onPick={({ itemType, itemId: id }) => {
          repo.saveWeekPlan(day, itemType, id);
          setOpen(false);
        }}
        onDismiss={() => setOpen(false)}
      />
    </EditorScreen>
  );
}
