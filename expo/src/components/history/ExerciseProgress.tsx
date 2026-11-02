import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { ChoiceSheet, FieldRow, FieldValue, GroupedList, LineChart, ListRow, TitleRow } from '@/components/ui';
import { shortDate } from '@/domain/dates';
import { fmt } from '@/domain/format';
import { defaultExerciseId, progressFor } from '@/domain/history';
import { Exercise, Kind, Session, SetLog } from '@/domain/types';
import { K, Type } from '@/theme';

/**
 * Progress of one strength exercise (SPEC.md, Exercise progress): pick the exercise, then the estimated 1RM
 * per session (Epley) and the top set of each session, newest first.
 */
export function ExerciseProgress({ exercises, logs, sessions }: { exercises: Exercise[]; logs: SetLog[]; sessions: Session[] }) {
  const [picked, setPicked] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const strength = exercises.filter((e) => e.kind === Kind.STRENGTH);
  const chosen = picked ?? defaultExerciseId(exercises, logs);
  const progress = progressFor(chosen, logs, sessions);
  const name = strength.find((e) => e.id === chosen)?.name;

  return (
    <View>
      <GroupedList>
        <FieldRow label="Exercise" divider={false} onPress={() => setOpen(true)}>
          <FieldValue text={name ?? 'Choose'} />
        </FieldRow>
      </GroupedList>
      <TitleRow title="Estimated 1RM" muted style={styles.chartTitle} />
      {progress.length === 0 ? (
        <Text style={styles.empty}>No finished sets for this exercise yet.</Text>
      ) : (
        <>
          <LineChart points={progress.map((p) => ({ date: p.date, value: p.e1rm }))} unit="kg" />
          <GroupedList style={styles.table}>
            {[...progress].reverse().map((p, i) => (
              <ListRow key={`${p.date}${i}`} title={shortDate(p.date)} color={K.Muted} value={`${fmt(p.topW)} kg × ${p.topR} reps`} chevron={false} last={i === progress.length - 1} />
            ))}
          </GroupedList>
        </>
      )}
      <ChoiceSheet
        visible={open}
        title="Exercise"
        choices={strength.map((e) => ({ id: e.id, title: e.name }))}
        isSelected={(id) => id === chosen}
        onPick={(id) => {
          setPicked(id);
          setOpen(false);
        }}
        onDismiss={() => setOpen(false)}
        searchable
      />
    </View>
  );
}

const styles = StyleSheet.create({
  chartTitle: { marginTop: 20, marginBottom: 4 },
  empty: { ...Type.subheadline, color: K.Dim },
  table: { marginTop: 12 },
});
