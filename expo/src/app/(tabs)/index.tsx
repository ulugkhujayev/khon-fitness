import { useRef, useState } from 'react';
import { StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { TabScreen } from '@/components/layout';
import { PlanItemSheet } from '@/components/plan/PlanItemSheet';
import { WeekStrip } from '@/components/plan/WeekStrip';
import { BodyweightSection } from '@/components/today/BodyweightSection';
import { TodayCard } from '@/components/today/TodayCard';
import { BarCircleButton, ScreenTitle, TextButton } from '@/components/ui';
import { longDate, today as todayIso } from '@/domain/dates';
import { hasSessionScreen, itemFor, itemId } from '@/domain/plan';
import { Session } from '@/domain/types';
import { useToday } from '@/db/hooks';
import * as repo from '@/db/repository';

function openSession(s: Session) {
  if (hasSessionScreen(s.itemType)) router.push(`/session/${s.id}`);
}

/** Today (TodayScreen.kt): the week strip, today's item with Start or Resume, bodyweight. */
export default function TodayScreen() {
  const today = todayIso();
  const { data } = useToday(today);
  const [planDate, setPlanDate] = useState(today);
  const [planOpen, setPlanOpen] = useState(false);
  const starting = useRef(false);

  const start = async () => {
    if (!data || starting.current) return;
    starting.current = true;
    try {
      const s = await repo.startItem(data.item);
      if (s) openSession(s);
    } finally {
      starting.current = false;
    }
  };

  const planned = data ? itemFor(data.plan, planDate) : null;

  return (
    <TabScreen>
      <ScreenTitle
        text="Today"
        trailing={<BarCircleButton icon={{ ios: 'gearshape', android: 'settings', web: 'settings' }} label="Settings" onPress={() => router.push('/settings')} />}
      />
      {data ? (
        <>
          <WeekStrip
            today={today}
            plan={data.plan}
            modalities={data.modalities}
            onPress={(date) => {
              setPlanDate(date);
              setPlanOpen(true);
            }}
          />
          <TodayCard item={data.item} line={data.line} unfinished={data.unfinished} doneText={data.doneText} onStart={start} onResume={openSession} />
          <BodyweightSection today={today} bodyweights={data.bodyweights} onAdd={(kg) => repo.saveBodyweight(today, kg)} />
          <PlanItemSheet
            visible={planOpen}
            title={longDate(planDate)}
            plan={data.plan}
            modalities={data.modalities}
            current={planned ? { itemType: planned.itemType, itemId: itemId(planned) } : null}
            onPick={({ itemType, itemId: id }) => {
              repo.saveOverride(planDate, itemType, id);
              setPlanOpen(false);
            }}
            onDismiss={() => setPlanOpen(false)}
            footer={
              planned?.overridden ? (
                <TextButton
                  text="Back to weekly template"
                  style={styles.backToTemplate}
                  onPress={() => {
                    repo.clearOverride(planDate);
                    setPlanOpen(false);
                  }}
                />
              ) : null
            }
          />
        </>
      ) : null}
    </TabScreen>
  );
}

const styles = StyleSheet.create({
  backToTemplate: { alignSelf: 'flex-start', marginTop: 12 },
});
