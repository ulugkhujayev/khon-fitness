import { Text, View } from 'react-native';
import { TabScreen } from '@/components/Screen';
import { PrimaryButton, ScreenTitle } from '@/components/ui';
import { K, Type } from '@/theme';

/** Placeholder until stage 2: week strip, today's item, Start, bodyweight. */
export default function TodayScreen() {
  return (
    <TabScreen>
      <ScreenTitle text="Today" />
      <View style={{ gap: 8, marginTop: 24 }}>
        <Text style={[Type.title1, { fontSize: 30 }]}>TP v3</Text>
        <Text style={[Type.subheadline, { color: K.Muted }]}>7 blocks, 42 sets</Text>
      </View>
      <PrimaryButton text="Start" onPress={() => {}} style={{ marginTop: 24 }} />
    </TabScreen>
  );
}
