import { StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { TabScreen } from '@/components/layout';
import { BarCircleButton, Empty, GroupedList, ListRow } from '@/components/ui';
import { usePrograms } from '@/db/hooks';
import { K } from '@/theme';

/** Programs tab (ProgramsScreen.kt): the programs (the active one filled), then the library, stretching, modalities, week plan and settings. */
export default function ProgramsScreen() {
  const { data } = usePrograms();

  // A new program is only a draft in its editor until Done or its first block saves it.
  const addProgram = () => router.push('/program/new');

  return (
    <TabScreen title="Programs" trailing={<BarCircleButton icon={{ ios: 'plus', android: 'add', web: 'add' }} label="New program" onPress={addProgram} />}>
      {data ? (
        <>
          <GroupedList>
            {data.programs.map((p, i) => (
              <ListRow
                key={p.id}
                title={p.name}
                value={`${data.blocks.filter((b) => b.programId === p.id).length} blocks`}
                dot={K.Accent}
                dotFilled={p.active}
                last={i === data.programs.length - 1}
                onPress={() => router.push(`/program/${p.id}`)}
              />
            ))}
            {data.programs.length === 0 ? <Empty text="No programs yet" /> : null}
          </GroupedList>
          <View style={styles.gap} />
          <GroupedList>
            <ListRow title="Exercises" value={String(data.exercises.filter((e) => !e.archived).length)} onPress={() => router.push('/library')} />
            <ListRow title="Stretching" onPress={() => router.push('/stretching')} />
            <ListRow title="Modalities" value={String(data.modalities.filter((m) => !m.archived).length)} onPress={() => router.push('/modalities')} />
            <ListRow title="Week plan" onPress={() => router.push('/week-plan')} />
            <ListRow title="Settings" onPress={() => router.push('/settings')} last />
          </GroupedList>
        </>
      ) : null}
    </TabScreen>
  );
}

const styles = StyleSheet.create({
  gap: { height: 24 },
});
