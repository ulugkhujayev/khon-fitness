import { useRef, useState } from 'react';
import { router } from 'expo-router';
import { rescheduleWindow } from '@/alerts/windowReminders';
import { clearWindowRequest, useWindowRequest } from '@/alerts/windowRequest';
import { TabScreen } from '@/components/layout';
import { DayPlanSheet } from '@/components/plan/DayPlanSheet';
import { WeekStrip } from '@/components/plan/WeekStrip';
import { BodyweightSection } from '@/components/today/BodyweightSection';
import { TodayCard } from '@/components/today/TodayCard';
import { TodayRows } from '@/components/today/TodayRows';
import { useNow } from '@/components/today/useNow';
import { WindowSheet } from '@/components/today/WindowSheet';
import { BarCircleButton } from '@/components/ui';
import { today as todayIso } from '@/domain/dates';
import { pendingLine } from '@/domain/health';
import { windowSummary } from '@/domain/window';
import { useToday } from '@/db/hooks';
import * as repo from '@/db/repository';
import { usePendingImports } from '@/health/usePendingImports';
import { useAppUpdate } from '@/updates/useAppUpdate';
import { openStretchPlayer, resumeSession, startItem } from '@/navigation';

/**
 * Today (TodayScreen.kt): the week strip, today's item with Start or Resume, the stretch routine, the eating window,
 * band sessions waiting for import, a downloaded update, bodyweight.
 */
export default function TodayScreen() {
  const today = todayIso();
  const { data } = useToday(today);
  const [now, refreshNow] = useNow();
  const pending = usePendingImports(data?.imported ?? null);
  const update = useAppUpdate();
  const [planDate, setPlanDate] = useState(today);
  const [planOpen, setPlanOpen] = useState(false);
  const [windowOpen, setWindowOpen] = useState(false);
  // A tapped window notification opens the sheet too.
  const windowRequested = useWindowRequest();
  const starting = useRef(false);

  const start = async () => {
    if (!data || starting.current) return;
    starting.current = true;
    try {
      await startItem(data.item);
    } finally {
      starting.current = false;
    }
  };

  // Every window change re-plans its notifications; a manual open or close also changes what "now" shows.
  const changeWindow = async (write: Promise<void>) => {
    await write;
    refreshNow();
    await rescheduleWindow();
  };

  const summary = data ? windowSummary(data.eatingWindow, data.windowDays.find((d) => d.date === today), now) : null;

  return (
    <TabScreen
      title="Today"
      trailing={<BarCircleButton icon={{ ios: 'gearshape', android: 'settings', web: 'settings' }} label="Settings" onPress={() => router.push('/settings')} />}>
      {data && summary ? (
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
          <TodayCard
            item={data.item}
            line={data.line}
            unfinished={data.unfinished}
            progress={data.progress}
            doneText={data.doneText}
            modalities={data.modalities}
            exercises={data.exercises}
            now={now}
            onStart={start}
            onResume={(s) => resumeSession(s)}
          />
          <TodayRows
            update={update.ready ? { onRestart: update.restart } : null}
            routine={data.routine ? { ...data.routine, onOpen: () => data.routine && openStretchPlayer(data.routine.id) } : null}
            window={{ ...summary, onOpen: () => setWindowOpen(true) }}
            pending={pending > 0 ? { text: pendingLine(pending), onOpen: () => router.push('/import') } : null}
          />
          <BodyweightSection today={today} bodyweights={data.bodyweights} onAdd={(kg) => repo.saveBodyweight(today, kg)} />
          <DayPlanSheet visible={planOpen} date={planDate} plan={data.plan} modalities={data.modalities} onDismiss={() => setPlanOpen(false)} />
          <WindowSheet
            visible={windowOpen || windowRequested}
            window={data.eatingWindow}
            days={data.windowDays}
            today={today}
            now={now}
            onSave={(w) => changeWindow(repo.saveEatingWindow(w))}
            onOpenNow={() => changeWindow(repo.openWindowNow(today))}
            onCloseNow={() => changeWindow(repo.closeWindowNow(today))}
            onDismiss={() => {
              setWindowOpen(false);
              clearWindowRequest();
            }}
          />
        </>
      ) : null}
    </TabScreen>
  );
}
