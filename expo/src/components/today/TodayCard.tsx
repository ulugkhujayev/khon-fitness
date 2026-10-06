import { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { SymbolViewProps } from 'expo-symbols';
import { itemColor, sessionColor } from '@/components/plan/colors';
import { itemIcon, PLAN_ICONS } from '@/components/plan/icons';
import { PrimaryButton, ProgressBar, SymbolCircle, TextButton } from '@/components/ui';
import { PlanItem } from '@/domain/plan';
import { Exercise, ItemType, Modality, Session } from '@/domain/types';
import { progressLine } from '@/domain/workout';
import { K, Space, tabular, Type } from '@/theme';

/**
 * Today's item (TodayScreen.kt) with its kind as a symbol in its color: a running session with the time
 * since its start, its sets done (a program) and Resume; else the item with Start, or its done line with
 * Start again once today's session is saved. Rest shows its line only.
 */
export function TodayCard({
  item,
  line,
  unfinished,
  progress,
  doneText,
  modalities,
  exercises,
  now,
  onStart,
  onResume,
}: {
  item: PlanItem;
  /** Preview: "7 blocks · 42 sets", "Steady on swim", "Rest day". */
  line: string;
  unfinished: Session | null;
  /** Sets done of total while a program session runs. */
  progress: { done: number; total: number } | null;
  /** "Done · 12 sets · 3.4 t" when today's item is already done. */
  doneText: string | null;
  modalities: Modality[];
  exercises: Exercise[];
  now: Date;
  onStart: () => void;
  onResume: (session: Session) => void;
}) {
  if (unfinished) {
    return (
      <View style={styles.card}>
        <Head
          icon={unfinished.itemType === ItemType.PROGRAM ? PLAN_ICONS.program : PLAN_ICONS.cardio}
          color={sessionColor(unfinished, exercises, modalities).color}
          title={unfinished.programName ?? unfinished.exerciseName ?? 'Session'}
          line={progressLine(unfinished.startedAt, now.getTime())}
          green
        />
        {progress ? (
          <View style={styles.progress}>
            <View style={styles.bar}>
              <ProgressBar done={progress.done} total={progress.total} label={`${progress.done} of ${progress.total} sets done`} />
            </View>
            <Text style={styles.count} importantForAccessibility="no" accessibilityElementsHidden>{`${progress.done} / ${progress.total}`}</Text>
          </View>
        ) : null}
        <PrimaryButton text="Resume" onPress={() => onResume(unfinished)} compact style={styles.button} />
      </View>
    );
  }
  if (doneText) {
    return (
      <View style={styles.card}>
        <Head icon={PLAN_ICONS.done} color={K.Green} title={item.name} line={doneText} green>
          <TextButton text="Start again" onPress={onStart} style={styles.again} />
        </Head>
      </View>
    );
  }
  const kind = itemColor(item, modalities);
  return (
    <View style={styles.card}>
      <Head icon={itemIcon(item)} color={kind?.color ?? K.Muted} title={item.name} line={line} />
      {item.isRest ? null : <PrimaryButton text="Start" onPress={onStart} compact style={styles.button} />}
    </View>
  );
}

/** The symbol, then the name over its line and anything that belongs under them. */
function Head({
  icon,
  color,
  title,
  line,
  green = false,
  children,
}: {
  icon: SymbolViewProps['name'];
  color: string;
  title: string;
  line: string;
  green?: boolean;
  children?: ReactNode;
}) {
  return (
    <View style={styles.head}>
      <SymbolCircle icon={icon} color={color} />
      <View style={styles.text}>
        <Text style={Type.title2} numberOfLines={2}>
          {title}
        </Text>
        <Text style={[styles.line, green && styles.green]}>{line}</Text>
        {children}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: { marginTop: 16, backgroundColor: K.Surface, borderRadius: Space.groupRadius, paddingHorizontal: Space.margin, paddingVertical: 14 },
  head: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  text: { flex: 1 },
  line: { ...Type.subheadline, color: K.Muted, marginTop: 1 },
  green: { color: K.Green },
  progress: { flexDirection: 'row', alignItems: 'center', gap: 12, marginTop: 12 },
  bar: { flex: 1 },
  count: { ...Type.footnote, ...tabular, color: K.Muted },
  button: { marginTop: 14 },
  again: { alignSelf: 'flex-start', marginTop: 2 },
});
