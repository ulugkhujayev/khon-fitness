import { View } from 'react-native';
import { router } from 'expo-router';
import { TabScreen } from '@/components/Screen';
import { BarCircleButton, Empty, GroupedList, ListRow, ScreenTitle } from '@/components/ui';
import * as repo from '@/db/repository';
import { newId } from '@/db/id';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K } from '@/theme';

/** Program list (one active, filled dot), then the library, modalities, week plan and settings. */
export default function ProgramsScreen() {
  const { data } = useLive(
    async () => {
      const [programs, blocks, exercises, modalities] = await Promise.all([repo.programs(), repo.blocks(), repo.exercises(), repo.modalities()]);
      return { programs, blocks, exercises, modalities };
    },
    [T.programs, T.blocks, T.exercises, T.modalities],
  );
  const programs = data?.programs ?? [];

  const addProgram = async () => {
    const id = newId();
    await repo.saveProgram({ id, name: 'New program', active: programs.length === 0, sortOrder: programs.length });
    router.push(`/program/${id}`);
  };

  return (
    <TabScreen>
      <ScreenTitle text="Programs" trailing={<BarCircleButton icon={{ ios: 'plus', android: 'add', web: 'add' }} label="New program" onPress={addProgram} />} />
      <GroupedList>
        {programs.map((p, i) => (
          <ListRow
            key={p.id}
            title={p.name}
            value={`${data!.blocks.filter((b) => b.programId === p.id).length} blocks`}
            dot={K.Accent}
            dotFilled={p.active}
            last={i === programs.length - 1}
            onPress={() => router.push(`/program/${p.id}`)}
          />
        ))}
        {data && programs.length === 0 ? <Empty text="No programs yet" /> : null}
      </GroupedList>
      <View style={{ height: 24 }} />
      <GroupedList>
        <ListRow
          title="Exercises"
          value={data ? String(data.exercises.filter((e) => !e.archived).length) : undefined}
          onPress={() => router.push('/library')}
        />
        <ListRow
          title="Modalities"
          value={data ? String(data.modalities.filter((m) => !m.archived).length) : undefined}
          onPress={() => router.push('/modalities')}
        />
        <ListRow title="Week plan" onPress={() => router.push('/week-plan')} />
        <ListRow title="Settings" onPress={() => router.push('/settings')} last />
      </GroupedList>
    </TabScreen>
  );
}
