import { StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import { BarCircleButton, Empty, GroupedList, IconTile, ListRow, SectionTitle } from '@/components/ui';
import { routineSummary } from '@/domain/stretch';
import { useStretching } from '@/db/hooks';
import { K } from '@/theme';

/**
 * Stretching (RoutinesScreen in StretchEditors.kt): the routines, the active one filled, then the stretch
 * library with its icon tile, as on the Programs tab. A new routine opens as a draft in its editor.
 */
export default function Stretching() {
  const { data } = useStretching();

  return (
    <EditorScreen
      title="Stretching"
      trailing={
        <BarCircleButton
          icon={{ ios: 'plus', android: 'add', web: 'add' }}
          label="New routine"
          onPress={() => router.push({ pathname: '/routine/[id]', params: { id: 'new' } })}
        />
      }>
      {data ? (
        <>
          <SectionTitle text="Routines" />
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
          <SectionTitle text="Library" style={styles.section} />
          <GroupedList>
            <ListRow
              leading={<IconTile icon={{ ios: 'figure.flexibility', android: 'self_improvement', web: 'self_improvement' }} color={K.Green} />}
              title="Stretches"
              value={String(data.stretches.filter((s) => !s.archived).length)}
              last
              onPress={() => router.push('/stretches')}
            />
          </GroupedList>
        </>
      ) : null}
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 24 },
});
