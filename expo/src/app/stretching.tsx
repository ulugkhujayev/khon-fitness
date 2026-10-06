import { StyleSheet, View } from 'react-native';
import { router } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import { BarCircleButton, Empty, GroupedList, ListRow } from '@/components/ui';
import { nextSortOrder } from '@/domain/editors';
import { routineSummary } from '@/domain/stretch';
import { useStretching } from '@/db/hooks';
import { newId } from '@/db/id';
import * as repo from '@/db/repository';
import { K } from '@/theme';

/** Stretching (RoutinesScreen in StretchEditors.kt): the routines, the active one filled, and the stretch library. */
export default function Stretching() {
  const { data } = useStretching();

  const addRoutine = async () => {
    if (!data) return;
    const id = newId();
    await repo.saveRoutine({ id, name: 'New routine', active: data.routines.length === 0, sortOrder: nextSortOrder(data.routines) });
    router.push({ pathname: '/routine/[id]', params: { id } });
  };

  return (
    <EditorScreen title="Stretching" large trailing={<BarCircleButton icon={{ ios: 'plus', android: 'add', web: 'add' }} label="New routine" onPress={addRoutine} />}>
      {data ? (
        <>
          <GroupedList>
            {data.routines.map((r, i) => (
              <ListRow
                key={r.id}
                title={r.name}
                value={routineSummary(r.id, data.routineStretches, data.stretches)}
                dot={K.Green}
                dotFilled={r.active}
                last={i === data.routines.length - 1}
                onPress={() => router.push({ pathname: '/routine/[id]', params: { id: r.id } })}
              />
            ))}
            {data.routines.length === 0 ? <Empty text="No routines yet" /> : null}
          </GroupedList>
          <View style={styles.gap} />
          <GroupedList>
            <ListRow title="Stretch library" value={String(data.stretches.filter((s) => !s.archived).length)} last onPress={() => router.push('/stretches')} />
          </GroupedList>
        </>
      ) : null}
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  gap: { height: 24 },
});
