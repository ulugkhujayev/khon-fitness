import { useRef, useState } from 'react';
import { router } from 'expo-router';
import { TabScreen } from '@/components/layout';
import { DayPlanSheet } from '@/components/plan/DayPlanSheet';
import { WeekStrip } from '@/components/plan/WeekStrip';
import { BodyweightSection } from '@/components/today/BodyweightSection';
import { TodayCard } from '@/components/today/TodayCard';
import { BarCircleButton, ScreenTitle } from '@/components/ui';
import { today as todayIso } from '@/domain/dates';
import { sessionScreen } from '@/domain/plan';
import { Session } from '@/domain/types';
import { useToday } from '@/db/hooks';
import * as repo from '@/db/repository';
import { openSession } from '@/navigation';
import { getView } from '@/timer/engine';

/** A running session continues where it is: its timer while that runs, else its screen. */
const resume = (s: Session) => openSession(s.id, sessionScreen(s.itemType, getView().state?.sessionId === s.id));

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
      // An interval exercise starts in its timer, a steady one in the form.
      if (s) openSession(s.id, sessionScreen(s.itemType, data.item.cardio?.intervals === true));
    } finally {
      starting.current = false;
    }
  };

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
          <TodayCard item={data.item} line={data.line} unfinished={data.unfinished} doneText={data.doneText} onStart={start} onResume={resume} />
          <BodyweightSection today={today} bodyweights={data.bodyweights} onAdd={(kg) => repo.saveBodyweight(today, kg)} />
          <DayPlanSheet visible={planOpen} date={planDate} plan={data.plan} modalities={data.modalities} onDismiss={() => setPlanOpen(false)} />
        </>
      ) : null}
    </TabScreen>
  );
}

