import { TabScreen } from '@/components/Screen';
import { Empty, GroupedList, ScreenTitle } from '@/components/ui';

/** Placeholder until stage 3: month calendar, bodyweight chart, exercise progress. */
export default function HistoryScreen() {
  return (
    <TabScreen>
      <ScreenTitle text="History" />
      <GroupedList>
        <Empty text="No sessions yet" />
      </GroupedList>
    </TabScreen>
  );
}
