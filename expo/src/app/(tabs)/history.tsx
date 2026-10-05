import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { BodyweightHistory } from '@/components/history/BodyweightHistory';
import { CalendarGrid } from '@/components/history/CalendarGrid';
import { CardioTrendsSection } from '@/components/history/CardioTrendsSection';
import { DayDetail } from '@/components/history/DayDetail';
import { ExerciseProgress } from '@/components/history/ExerciseProgress';
import { MuscleVolumeList } from '@/components/history/MuscleVolumeList';
import { TabScreen } from '@/components/layout';
import { DayPlanSheet } from '@/components/plan/DayPlanSheet';
import { MonthSwitcher, ScreenTitle, TextButton, TitleRow } from '@/components/ui';
import { monthStart, monthWeeks, today as todayIso } from '@/domain/dates';
import { muscleVolume, stretchMonth } from '@/domain/history';
import { itemFor, sessionScreen } from '@/domain/plan';
import { useHistory } from '@/db/hooks';
import * as repo from '@/db/repository';
import { openSession } from '@/navigation';
import { getView } from '@/timer/engine';

/**
 * History (HistoryScreen.kt): the month calendar, the selected day, stretching in the month, bodyweight,
 * exercise progress, sets per muscle and cardio trends. Import brings in band workouts.
 */
export default function HistoryScreen() {
  const today = todayIso();
  const { data } = useHistory();
  const [month, setMonth] = useState(monthStart(today));
  const [selected, setSelected] = useState(today);
  // The date of the plan sheet stays set while the sheet slides out.
  const [planDate, setPlanDate] = useState(today);
  const [planOpen, setPlanOpen] = useState(false);
  // A lifted day owns the touch, so the screen stops scrolling under it.
  const [dragging, setDragging] = useState(false);

  const openPlan = (date: string) => {
    setSelected(date);
    setPlanDate(date);
    setPlanOpen(true);
  };

  const stretching = data ? stretchMonth(data.stretches, month) : null;

  return (
    <TabScreen scrollEnabled={!dragging}>
      <ScreenTitle text="History" trailing={<TextButton text="Import" label="Import workouts" onPress={() => router.push('/import')} />} />
      {data ? (
        <>
          <MonthSwitcher month={month} onChange={setMonth} />
          <CalendarGrid
            weeks={monthWeeks(month)}
            month={month}
            today={today}
            selected={selected}
            plan={data.plan}
            sessions={data.sessions}
            stretches={data.stretches}
            exercises={data.exercises}
            modalities={data.modalities}
            onSelect={setSelected}
            onPlan={openPlan}
            onSwap={(a, b) => repo.swapDays(a, b)}
            onDragActive={setDragging}
          />
          <View style={styles.day}>
            <DayDetail
              date={selected}
              item={itemFor(data.plan, selected)}
              sessions={data.sessions.filter((s) => s.date === selected)}
              stretches={data.stretches.filter((r) => r.date === selected)}
              logs={data.logs}
              exercises={data.exercises}
              modalities={data.modalities}
              onPlan={() => openPlan(selected)}
              onOpen={(s) => openSession(s.id, sessionScreen(s.itemType, getView().state?.sessionId === s.id))}
            />
          </View>
          {stretching ? <TitleRow title="Stretching" value={stretching} style={styles.section} /> : null}
          <View style={styles.section}>
            <BodyweightHistory bodyweights={data.bodyweights} />
          </View>
          <View style={styles.progress}>
            <ExerciseProgress exercises={data.exercises} logs={data.logs} sessions={data.sessions} />
          </View>
          <View style={styles.section}>
            <MuscleVolumeList volume={muscleVolume(data.logs, data.sessions, data.exercises, today)} />
          </View>
          <View style={styles.section}>
            <CardioTrendsSection exercises={data.exercises} modalities={data.modalities} sessions={data.sessions} intervals={data.intervals} />
          </View>
          <DayPlanSheet visible={planOpen} date={planDate} plan={data.plan} modalities={data.modalities} onDismiss={() => setPlanOpen(false)} />
        </>
      ) : null}
    </TabScreen>
  );
}

const styles = StyleSheet.create({
  day: { marginTop: 20 },
  section: { marginTop: 28 },
  progress: { marginTop: 24 },
});
