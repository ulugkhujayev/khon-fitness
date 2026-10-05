import { View } from 'react-native';
import { router } from 'expo-router';
import { TabScreen } from '@/components/Screen';
import { Empty, GroupedList, ListRow, PlusButton, ScreenTitle } from '@/components/ui';
import * as repo from '@/db/repository';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K } from '@/theme';

/** Program list (one active, filled dot) and the library. Editors arrive in stage 2. */
export default function ProgramsScreen() {
  const { data } = useLive(
    async () => {
      const [programs, blocks, exercises, modalities] = await Promise.all([repo.programs(), repo.blocks(), repo.exercises(), repo.modalities()]);
      return { programs, blocks, exercises, modalities };
    },
    [T.programs, T.blocks, T.exercises, T.modalities],
  );
  const programs = data?.programs ?? [];

  return (
    <TabScreen>
      <ScreenTitle text="Programs" trailing={<PlusButton label="New program" onPress={() => {}} />} />
      <GroupedList>
        {programs.map((p, i) => (
          <ListRow
            key={p.id}
            title={p.name}
            value={`${data!.blocks.filter((b) => b.programId === p.id).length} blocks`}
            dot={K.Accent}
            dotFilled={p.active}
            last={i === programs.length - 1}
            onPress={() => {}}
          />
        ))}
        {data && programs.length === 0 ? <Empty text="No programs yet" /> : null}
      </GroupedList>
      <View style={{ height: 24 }} />
      <GroupedList>
        <ListRow title="Exercises" value={data ? String(data.exercises.filter((e) => !e.archived).length) : undefined} onPress={() => {}} />
        <ListRow title="Stretching" onPress={() => {}} />
        <ListRow title="Modalities" value={data ? String(data.modalities.filter((m) => !m.archived).length) : undefined} onPress={() => {}} />
        <ListRow title="Week plan" onPress={() => {}} />
        <ListRow title="Settings" onPress={() => router.push('/settings')} last />
      </GroupedList>
    </TabScreen>
  );
}
