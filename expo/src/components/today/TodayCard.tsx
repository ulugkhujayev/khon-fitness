import { StyleSheet, Text, View } from 'react-native';
import { PrimaryButton, TextButton } from '@/components/ui';
import { PlanItem } from '@/domain/plan';
import { Session } from '@/domain/types';
import { K, Space, Type } from '@/theme';

/**
 * Today's item (TodayScreen.kt): a running session with Resume; else the item with Start, or its done line
 * with Start again once today's session is saved. Rest shows its line only.
 */
export function TodayCard({
  item,
  line,
  unfinished,
  doneText,
  onStart,
  onResume,
}: {
  item: PlanItem;
  /** Preview: "7 blocks · 42 sets", "Steady on swim", "Rest day". */
  line: string;
  unfinished: Session | null;
  /** "Done · 12 sets · 3.4 t" when today's item is already done. */
  doneText: string | null;
  onStart: () => void;
  onResume: (session: Session) => void;
}) {
  if (unfinished) {
    return (
      <View style={styles.card}>
        <Text style={Type.title2}>{unfinished.programName ?? unfinished.exerciseName ?? 'Session'}</Text>
        <Text style={[styles.line, styles.green]}>In progress</Text>
        <PrimaryButton text="Resume" onPress={() => onResume(unfinished)} />
      </View>
    );
  }
  if (doneText) {
    return (
      <View style={styles.card}>
        <Text style={Type.title2}>{item.name}</Text>
        <Text style={[styles.line, styles.green, styles.doneLine]}>{doneText}</Text>
        <TextButton text="Start again" onPress={onStart} style={styles.again} />
      </View>
    );
  }
  return (
    <View style={styles.card}>
      <Text style={Type.title2}>{item.name}</Text>
      <Text style={[styles.line, item.isRest && styles.lastLine]}>{line}</Text>
      {item.isRest ? null : <PrimaryButton text="Start" onPress={onStart} />}
    </View>
  );
}

const styles = StyleSheet.create({
  card: { marginTop: 24, backgroundColor: K.Surface, borderRadius: Space.groupRadius, paddingHorizontal: 20, paddingVertical: 18 },
  line: { ...Type.subheadline, color: K.Muted, marginTop: 6, marginBottom: 18 },
  lastLine: { marginBottom: 0 },
  green: { color: K.Green },
  doneLine: { marginBottom: 10 },
  again: { alignSelf: 'flex-start' },
});
