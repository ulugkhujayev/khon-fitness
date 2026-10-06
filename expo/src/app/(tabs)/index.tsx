import { useRef, useState } from 'react';
import { StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { rescheduleWindow } from '@/alerts/windowReminders';
import { clearWindowRequest, useWindowRequest } from '@/alerts/windowRequest';
import { TabScreen } from '@/components/layout';
import { DayPlanSheet } from '@/components/plan/DayPlanSheet';
import { WeekStrip } from '@/components/plan/WeekStrip';
import { BodyweightSection } from '@/components/today/BodyweightSection';
import { TodayCard } from '@/components/today/TodayCard';
import { useNow } from '@/components/today/useNow';
import { WindowSheet } from '@/components/today/WindowSheet';
import { BarCircleButton, GroupedList, ListRow, ScreenTitle } from '@/components/ui';
import { today as todayIso } from '@/domain/dates';
import { pendingLine } from '@/domain/health';
import { windowSummary } from '@/domain/window';
import { useToday } from '@/db/hooks';
import * as repo from '@/db/repository';
import { usePendingImports } from '@/health/usePendingImports';
import { useAppUpdate } from '@/updates/useAppUpdate';
import { openStretchPlayer, resumeSession, startItem } from '@/navigation';
import { K } from '@/theme';

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
    <TabScreen>
      <ScreenTitle
        text="Today"
        trailing={<BarCircleButton icon={{ ios: 'gearshape', android: 'settings', web: 'settings' }} label="Settings" onPress={() => router.push('/settings')} />}
      />
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
          {update.ready ? (
            <GroupedList style={styles.row}>
              <ListRow title="Update ready" value="Restart" dot={K.Accent} last onPress={update.restart} />
            </GroupedList>
          ) : null}
          {data.routine ? (
            <GroupedList style={styles.row}>
              <ListRow
                title={data.routine.name}
                value={data.routine.line}
                dot={K.Green}
                dotFilled={data.routine.doneToday}
                last
                onPress={() => data.routine && openStretchPlayer(data.routine.id)}
              />
            </GroupedList>
          ) : null}
          <GroupedList style={styles.row}>
            <ListRow
              title="Eating window"
              value={summary.text}
              dot={summary.open ? K.Green : K.Muted}
              dotFilled={summary.open}
              last
              onPress={() => setWindowOpen(true)}
            />
          </GroupedList>
          {pending > 0 ? (
            <GroupedList style={styles.row}>
              <ListRow title={pendingLine(pending)} dot={K.Green} last onPress={() => router.push('/import')} />
            </GroupedList>
          ) : null}
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

const styles = StyleSheet.create({
  row: { marginTop: 16 },
});
