import { View } from 'react-native';
import { TabScreen } from '@/components/Screen';
import { GroupedList, ListRow, PlusButton, ScreenTitle, SectionTitle } from '@/components/ui';
import { K } from '@/theme';

/** Placeholder rows until stage 2 wires the data layer. */
export default function ProgramsScreen() {
  return (
    <TabScreen>
      <ScreenTitle text="Programs" trailing={<PlusButton label="Add program" onPress={() => {}} />} />
      <GroupedList>
        <ListRow title="TP v3" value="Active" dot={K.Accent} onPress={() => {}} last />
      </GroupedList>
      <View style={{ height: 24 }} />
      <SectionTitle text="Library" />
      <GroupedList>
        <ListRow title="Exercises" onPress={() => {}} />
        <ListRow title="Modalities" onPress={() => {}} />
        <ListRow title="Stretches" onPress={() => {}} />
        <ListRow title="Week plan" onPress={() => {}} />
        <ListRow title="Settings" onPress={() => {}} last />
      </GroupedList>
    </TabScreen>
  );
}
