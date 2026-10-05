import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { exerciseColor } from '@/components/plan/colors';
import { ChoiceSheet, FieldRow, FieldValue, GroupedList, LineChart, TitleRow } from '@/components/ui';
import { ChartPoint } from '@/domain/chart';
import { cardioTrends, defaultCardioId, formatPace } from '@/domain/history';
import { Exercise, Intensity, IntervalLog, Kind, Modality, Session } from '@/domain/types';
import { K, Type } from '@/theme';

/**
 * Trends of one cardio exercise (HistoryScreen.kt): pace, average heart rate, heart rate in the work
 * intervals; time only when there is neither pace nor heart rate.
 */
export function CardioTrendsSection({
  exercises,
  modalities,
  sessions,
  intervals,
}: {
  exercises: Exercise[];
  modalities: Modality[];
  sessions: Session[];
  intervals: IntervalLog[];
}) {
  const [picked, setPicked] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const cardio = exercises.filter((e) => e.kind === Kind.CARDIO);
  const chosenId = picked ?? defaultCardioId(exercises, sessions);
  const chosen = cardio.find((e) => e.id === chosenId) ?? null;
  const t = chosen ? cardioTrends(chosen, sessions, intervals) : null;

  return (
    <View>
      <GroupedList>
        <FieldRow label="Cardio" divider={false} onPress={() => setOpen(true)}>
          <FieldValue text={chosen?.name ?? 'Choose'} />
        </FieldRow>
      </GroupedList>
      {!t || t.count === 0 ? (
        <Text style={styles.empty}>No finished sessions for this exercise yet.</Text>
      ) : (
        <>
          <Trend title={`Pace ${t.paceUnit}`} points={t.pace} unit="" format={formatPace} />
          <Trend title="Average heart rate" points={t.hr} unit="bpm" decimals={0} />
          <Trend title="Heart rate in work intervals" points={t.workHr} unit="bpm" decimals={0} />
          {t.pace.length === 0 && t.hr.length === 0 ? <Trend title="Time, min" points={t.time} unit="min" decimals={0} /> : null}
        </>
      )}
      <ChoiceSheet
        visible={open}
        title="Cardio exercise"
        choices={cardio.map((e) => ({ id: e.id, title: e.name, dot: exerciseColor(e, modalities), dotFilled: e.intensity === Intensity.HIGH }))}
        isSelected={(id) => id === chosenId}
        onPick={(id) => {
          setPicked(id);
          setOpen(false);
        }}
        onDismiss={() => setOpen(false)}
      />
    </View>
  );
}

/** One titled chart; nothing when the series is empty. */
function Trend({ title, points, unit, decimals, format }: { title: string; points: ChartPoint[]; unit: string; decimals?: number; format?: (v: number) => string }) {
  if (points.length === 0) return null;
  return (
    <View>
      <TitleRow title={title} muted style={styles.title} />
      <LineChart points={points} unit={unit} height={170} decimals={decimals} format={format} />
    </View>
  );
}

const styles = StyleSheet.create({
  empty: { ...Type.subheadline, color: K.Dim, marginTop: 12 },
  title: { marginTop: 20, marginBottom: 4 },
});
