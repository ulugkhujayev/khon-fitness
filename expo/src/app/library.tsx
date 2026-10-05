import { useState } from 'react';
import { View } from 'react-native';
import { router } from 'expo-router';
import { exerciseColor, modalityName } from '@/components/colors';
import { SearchField, Segmented } from '@/components/controls';
import { EditorScreen } from '@/components/EditorScreen';
import { ChoiceList, ChoiceRow, Sheet } from '@/components/Sheet';
import { BarCircleButton, Empty, FieldRow, FieldValue, GroupedList, ListRow, Toggle } from '@/components/ui';
import { cap, filterLibrary } from '@/domain/editors';
import { Intensity, Kind, MUSCLE_GROUPS } from '@/domain/types';
import * as repo from '@/db/repository';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K } from '@/theme';

const KINDS = ['', Kind.STRENGTH, Kind.CARDIO];

/** Exercise library (LibraryScreen in ProgramsScreen.kt): search, kind, muscle, archived. */
export default function Library() {
  const { data } = useLive(
    async () => {
      const [exercises, modalities] = await Promise.all([repo.exercises(), repo.modalities()]);
      return { exercises, modalities };
    },
    [T.exercises, T.modalities],
  );
  const [query, setQuery] = useState('');
  const [kind, setKind] = useState('');
  const [muscle, setMuscle] = useState('');
  const [showArchived, setShowArchived] = useState(false);
  const [muscleSheet, setMuscleSheet] = useState(false);

  const list = data ? filterLibrary(data.exercises, { query, kind, muscle, showArchived }) : [];

  return (
    <EditorScreen
      title="Exercises"
      trailing={<BarCircleButton icon={{ ios: 'plus', android: 'add', web: 'add' }} label="New exercise" onPress={() => router.push('/exercise/new')} />}>
      <SearchField value={query} onChange={setQuery} />
      <View style={{ height: 12 }} />
      <Segmented options={['All', 'Strength', 'Cardio']} selected={KINDS.indexOf(kind)} onSelect={(i) => setKind(KINDS[i])} />
      {kind !== Kind.CARDIO ? (
        <GroupedList style={{ marginTop: 12 }}>
          <FieldRow label="Muscle" divider={false} onPress={() => setMuscleSheet(true)}>
            <FieldValue text={muscle ? cap(muscle) : 'Any'} />
          </FieldRow>
        </GroupedList>
      ) : null}
      <GroupedList style={{ marginTop: 16 }}>
        {data
          ? list.map((e, i) => (
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
            ))
          : null}
        {data && list.length === 0 ? <Empty text="Nothing matches" /> : null}
      </GroupedList>
      <GroupedList style={{ marginTop: 16 }}>
        <FieldRow label="Show archived" divider={false}>
          <Toggle value={showArchived} onChange={setShowArchived} label="Show archived" />
        </FieldRow>
      </GroupedList>

      <Sheet visible={muscleSheet} title="Muscle" onDismiss={() => setMuscleSheet(false)}>
        <ChoiceList>
          <ChoiceRow
            title="Any"
            selected={muscle === ''}
            onPress={() => {
              setMuscle('');
              setMuscleSheet(false);
            }}
          />
          {MUSCLE_GROUPS.map((m, i) => (
            <ChoiceRow
              key={m}
              title={cap(m)}
              selected={muscle === m}
              last={i === MUSCLE_GROUPS.length - 1}
              onPress={() => {
                setMuscle(m);
                setMuscleSheet(false);
              }}
            />
          ))}
        </ChoiceList>
      </Sheet>
    </EditorScreen>
  );
}
