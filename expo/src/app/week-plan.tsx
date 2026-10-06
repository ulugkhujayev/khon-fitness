import { useState } from 'react';
import { EditorScreen } from '@/components/layout';
import { itemColor } from '@/components/plan/colors';
import { itemIcon } from '@/components/plan/icons';
import { PlanItemSheet } from '@/components/plan/PlanItemSheet';
import { GroupedList, GroupFooter, IconTile, ListRow, SectionTitle } from '@/components/ui';
import { today, weekday, weekdayName } from '@/domain/dates';
import { itemId, templateItem, weekSummary } from '@/domain/plan';
import { usePlan } from '@/db/hooks';
import * as repo from '@/db/repository';
import { K } from '@/theme';

const DAYS = [1, 2, 3, 4, 5, 6, 7];

/**
 * The weekly template (WeekPlanScreen in ProgramsScreen.kt): the week at a glance, then each weekday with
 * what it holds as a symbol in its color (today's name in accent), and a note on when a change applies.
 * A change fills future dates only: saveWeekPlan freezes the past days of that weekday first.
 */
export default function WeekPlan() {
  const { data } = usePlan();
  // The weekday stays set while its sheet slides out.
  const [day, setDay] = useState(1);
  const [open, setOpen] = useState(false);
  if (!data) return <EditorScreen title="Week plan" />;
  const { plan, modalities } = data;
  const current = templateItem(plan, day);
  const todayDay = weekday(today());

  return (
    <EditorScreen title="Week plan">
      <SectionTitle text={weekSummary(plan)} />
      <GroupedList>
        {DAYS.map((d) => {
          const item = templateItem(plan, d);
          return (
            <ListRow
              key={d}
              leading={<IconTile icon={itemIcon(item)} color={itemColor(item, modalities)?.color ?? K.Dim} />}
              title={weekdayName(d)}
              color={d === todayDay ? K.Accent : K.Text}
              value={item.name}
              last={d === 7}
              onPress={() => {
                setDay(d);
                setOpen(true);
              }}
            />
          );
        })}
      </GroupedList>
      <GroupFooter text="A change applies from today on; past days keep the plan they had. To change one date only, tap it on Today." />
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
