import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import { exerciseColor } from '@/components/plan/colors';
import { BarCircleButton, ChoiceSheet, FieldRow, FieldValue, GroupedList, ListRow, SearchEmpty, SearchField, Segmented, ToggleRow } from '@/components/ui';
import { cap, filterLibrary, modalityName } from '@/domain/editors';
import { Intensity, Kind, MUSCLE_GROUPS } from '@/domain/types';
import { useLibrary } from '@/db/hooks';
import { K } from '@/theme';

const KINDS = ['', Kind.STRENGTH, Kind.CARDIO];
const MUSCLES = [{ id: '', title: 'Any' }, ...MUSCLE_GROUPS.map((m) => ({ id: m, title: cap(m) }))];

/** Exercise library (LibraryScreen in ProgramsScreen.kt): search, kind, muscle, archived. */
export default function Library() {
  const { data } = useLibrary();
  const [query, setQuery] = useState('');
  const [kind, setKind] = useState('');
  const [muscle, setMuscle] = useState('');
  const [showArchived, setShowArchived] = useState(false);
  const [muscleOpen, setMuscleOpen] = useState(false);
  const list = data ? filterLibrary(data.exercises, { query, kind, muscle, showArchived }) : [];

  return (
    <EditorScreen
      title="Exercises"
      trailing={<BarCircleButton icon={{ ios: 'plus', android: 'add', web: 'add' }} label="New exercise" onPress={() => router.push('/exercise/new')} />}>
      <SearchField value={query} onChange={setQuery} />
      <View style={styles.gap} />
      <Segmented options={['All', 'Strength', 'Cardio']} selected={KINDS.indexOf(kind)} onSelect={(i) => setKind(KINDS[i])} />
      {kind !== Kind.CARDIO ? (
        <GroupedList style={styles.muscle}>
          <FieldRow label="Muscle" divider={false} onPress={() => setMuscleOpen(true)}>
            <FieldValue text={muscle ? cap(muscle) : 'Any'} />
          </FieldRow>
        </GroupedList>
      ) : null}
      {data ? (
        <GroupedList style={styles.list}>
          {list.map((e, i) => (
            <ListRow
              key={e.id}
              title={e.name}
              value={e.kind === Kind.STRENGTH ? cap(e.primaryMuscle ?? '') : modalityName(e.modalityId, data.modalities)}
              dot={exerciseColor(e, data.modalities)}
              dotFilled={e.kind === Kind.STRENGTH || e.intensity === Intensity.HIGH}
              color={e.archived ? K.Dim : K.Text}
              last={i === list.length - 1}
              onPress={() => router.push(`/exercise/${e.id}`)}
            />
          ))}
          {list.length === 0 ? <SearchEmpty query={query} /> : null}
        </GroupedList>
      ) : null}
      <GroupedList style={styles.list}>
        <ToggleRow label="Show archived" divider={false} value={showArchived} onChange={setShowArchived} />
      </GroupedList>
      <ChoiceSheet
        visible={muscleOpen}
        title="Muscle"
        choices={MUSCLES}
        isSelected={(id) => id === muscle}
        onPick={(id) => {
          setMuscle(id);
          setMuscleOpen(false);
        }}
        onDismiss={() => setMuscleOpen(false)}
      />
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  gap: { height: 12 },
  muscle: { marginTop: 12 },
  list: { marginTop: 16 },
});
