import { StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { SymbolViewProps } from 'expo-symbols';
import { TabScreen } from '@/components/layout';
import { BarCircleButton, Empty, GroupedList, IconTile, ListRow, SectionTitle } from '@/components/ui';
import { usePrograms } from '@/db/hooks';
import { K } from '@/theme';

/** Each row's icon tile, as in iOS Settings. */
const ICONS = {
  exercises: { ios: 'dumbbell.fill', android: 'fitness_center', web: 'fitness_center' },
  stretching: { ios: 'figure.flexibility', android: 'self_improvement', web: 'self_improvement' },
  modalities: { ios: 'figure.run', android: 'directions_run', web: 'directions_run' },
  weekPlan: { ios: 'calendar', android: 'calendar_month', web: 'calendar_month' },
  settings: { ios: 'gearshape.fill', android: 'settings', web: 'settings' },
} satisfies Record<string, SymbolViewProps['name']>;

/**
 * Programs tab (ProgramsScreen.kt): the programs (the active one filled), then grouped rows with icon tiles:
 * Library (exercises, stretching, modalities), Plan (week plan), and Settings on its own.
 */
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
          <SectionTitle text="Library" style={styles.section} />
          <GroupedList>
            <ListRow
              leading={<IconTile icon={ICONS.exercises} color={K.Accent} />}
              title="Exercises"
              value={String(data.exercises.filter((e) => !e.archived).length)}
              onPress={() => router.push('/library')}
            />
            <ListRow leading={<IconTile icon={ICONS.stretching} color={K.Green} />} title="Stretching" onPress={() => router.push('/stretching')} />
            <ListRow
              leading={<IconTile icon={ICONS.modalities} color={K.Blue} />}
              title="Modalities"
              value={String(data.modalities.filter((m) => !m.archived).length)}
              onPress={() => router.push('/modalities')}
              last
            />
          </GroupedList>
          <SectionTitle text="Plan" style={styles.section} />
          <GroupedList>
            <ListRow leading={<IconTile icon={ICONS.weekPlan} color={K.Red} />} title="Week plan" onPress={() => router.push('/week-plan')} last />
          </GroupedList>
          <GroupedList style={styles.settings}>
            {/* Gray like the system's own Settings icon. */}
            <ListRow leading={<IconTile icon={ICONS.settings} color={K.Dim} />} title="Settings" onPress={() => router.push('/settings')} last />
          </GroupedList>
        </>
      ) : null}
    </TabScreen>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 24 },
  settings: { marginTop: 32 },
});
