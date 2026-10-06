import { Pressable, StyleSheet, Text, View } from 'react-native';
import { PLAN_ICONS } from '@/components/plan/icons';
import { Chevron, IconTile } from '@/components/ui';
import { draftStats, ImportDraft } from '@/domain/health';
import { K, Space, Type } from '@/theme';

/**
 * One band or watch workout waiting for import: a tile in its exercise's color, the workout type with the
 * exercise it goes under on the same line (Choose in accent when none matched), and its date and numbers on
 * a line of their own, so the numbers never get cut.
 */
export function DraftRow({
  draft,
  exerciseName,
  color,
  last,
  onPress,
}: {
  draft: ImportDraft;
  /** Null when no exercise matched. */
  exerciseName: string | null;
  color: string;
  last: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={`${draft.typeName}, ${draftStats(draft)}, ${exerciseName ? `imports as ${exerciseName}` : 'choose an exercise'}`}
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}>
      <IconTile icon={PLAN_ICONS.cardio} color={exerciseName ? color : K.Dim} />
      <View style={[styles.inner, !last && styles.separator]}>
        <View style={styles.words}>
          <View style={styles.top}>
            <Text style={styles.title} numberOfLines={1}>
              {draft.typeName}
            </Text>
            <Text style={[styles.exercise, !exerciseName && styles.choose]} numberOfLines={1}>
              {exerciseName ?? 'Choose'}
            </Text>
          </View>
          <Text style={styles.stats} numberOfLines={2}>
            {draftStats(draft)}
          </Text>
        </View>
        <Chevron />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', paddingLeft: 16, gap: 12 },
  pressed: { backgroundColor: K.Surface2 },
  inner: { flex: 1, flexDirection: 'row', alignItems: 'center', minHeight: Space.rowMin + 12, paddingVertical: 10, paddingRight: 16 },
  separator: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: K.Divider },
  words: { flex: 1 },
  top: { flexDirection: 'row', alignItems: 'baseline', justifyContent: 'space-between' },
  // Both give way on a narrow row; the exercise never takes more than a little over half of it.
  title: { ...Type.body, flexShrink: 1 },
  stats: { ...Type.footnote, color: K.Muted, marginTop: 2 },
  exercise: { ...Type.subheadline, color: K.Muted, flexShrink: 1, maxWidth: '55%', textAlign: 'right', marginLeft: 8 },
  choose: { color: K.Accent },
});
