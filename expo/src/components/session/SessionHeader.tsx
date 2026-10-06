import { StyleSheet, Text, View } from 'react-native';
import { NavBar } from '@/components/layout';
import { Capsule, ProgressBar } from '@/components/ui';
import { K, Space, tabular, Type } from '@/theme';

/**
 * Top of a running session, kept small so the sets get the room: the navigation bar with Back, the program
 * name and Finish, and under it the progress bar with sets done of total. It sits above the scrolling sets,
 * so it stays in view. (SessionScreen.kt has a large title and the date here.)
 */
export function SessionHeader({ title, done, total, onFinish }: { title: string; done: number; total: number; onFinish: () => void }) {
  return (
    <View>
      <NavBar title={title} backLabel="Today" trailing={<Capsule text="Finish" onPress={onFinish} />} />
      <View style={styles.progress}>
        <View style={styles.bar}>
          <ProgressBar done={done} total={total} label={`${done} of ${total} sets done`} />
        </View>
        <Text style={styles.count} importantForAccessibility="no" accessibilityElementsHidden>{`${done} / ${total}`}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  progress: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: Space.margin, paddingBottom: 10 },
  bar: { flex: 1 },
  count: { ...Type.footnote, ...tabular, color: K.Muted },
});
