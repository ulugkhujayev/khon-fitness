import { StyleSheet, Text, View } from 'react-native';
import { BackButton } from '@/components/layout';
import { Capsule, ProgressBar } from '@/components/ui';
import { shortDate } from '@/domain/dates';
import { K, Space, Type } from '@/theme';

/**
 * Top of a running session (SessionScreen.kt): Back and Finish, the program name, the date, sets done and a
 * progress bar. It sits above the scrolling sets, so it stays in view.
 */
export function SessionHeader({ title, date, done, total, onFinish }: { title: string; date: string; done: number; total: number; onFinish: () => void }) {
  return (
    <View style={styles.header}>
      <View style={styles.bar}>
        <BackButton label="Today" />
        <Capsule text="Finish" onPress={onFinish} />
      </View>
      <Text style={styles.title} accessibilityRole="header">
        {title}
      </Text>
      <View style={styles.meta}>
        <Text style={[styles.metaText, styles.date]}>{shortDate(date)}</Text>
        <Text style={styles.metaText}>{`${done} / ${total} sets`}</Text>
      </View>
      <View style={styles.progress}>
        <ProgressBar done={done} total={total} label={`${done} of ${total} sets done`} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { paddingHorizontal: Space.margin, paddingTop: 6 },
  bar: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', height: Space.tapMin, marginBottom: 4 },
  title: { ...Type.title1, marginTop: 4 },
  meta: { flexDirection: 'row', alignItems: 'center', marginTop: 4 },
  metaText: { ...Type.subheadline, color: K.Muted },
  date: { flex: 1 },
  progress: { marginTop: 8, marginBottom: 6 },
});
