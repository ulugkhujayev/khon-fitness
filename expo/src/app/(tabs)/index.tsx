import { Text, View } from 'react-native';
import { TabScreen } from '@/components/Screen';
import { PrimaryButton, ScreenTitle } from '@/components/ui';
import { today } from '@/domain/dates';
import { preview } from '@/domain/format';
import { itemFor } from '@/domain/plan';
import * as repo from '@/db/repository';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K, Type } from '@/theme';

/** Today's resolved item from the database. Week strip, session start and bodyweight come in stage 2. */
export default function TodayScreen() {
  const { data } = useLive(
    async () => {
      const [plan, blocks, blockExercises, modalities] = await Promise.all([
        repo.planData(), repo.blocks(), repo.blockExercises(), repo.modalities(),
      ]);
      const item = itemFor(plan, today());
      return { item, line: preview(item, blocks, blockExercises, modalities) };
    },
    [...repo.PLAN_TABLES, T.blocks, T.blockExercises, T.modalities],
  );

  return (
    <TabScreen>
      <ScreenTitle text="Today" />
      {data ? (
        <>
          <View style={{ gap: 6, marginTop: 24 }}>
            <Text style={[Type.title1, { fontSize: 30 }]}>{data.item.name}</Text>
            <Text style={[Type.subheadline, { color: K.Muted }]}>{data.line}</Text>
          </View>
          {data.item.isRest ? null : <PrimaryButton text="Start" onPress={() => {}} style={{ marginTop: 18 }} />}
        </>
      ) : null}
    </TabScreen>
  );
}
