import { Pressable, StyleSheet, Text, View } from 'react-native';
import { PLAN_ICONS } from '@/components/plan/icons';
import { Chevron, IconTile } from '@/components/ui';
import { draftStats, ImportDraft } from '@/domain/health';
import { K, Space, Type } from '@/theme';

/**
 * One band or watch workout waiting for import: a tile in its exercise's color, the workout type over its date
 * and numbers, and on the right the exercise it goes under, or Choose in accent when none matched.
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
          <Text style={styles.title} numberOfLines={1}>
            {draft.typeName}
          </Text>
          <Text style={styles.stats} numberOfLines={1}>
            {draftStats(draft)}
          </Text>
        </View>
        <Text style={[styles.exercise, !exerciseName && styles.choose]} numberOfLines={1}>
          {exerciseName ?? 'Choose'}
        </Text>
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
  title: { ...Type.body },
  stats: { ...Type.footnote, color: K.Muted, marginTop: 2 },
  exercise: { ...Type.subheadline, color: K.Muted, marginLeft: 8, maxWidth: 130 },
  choose: { color: K.Accent },
});
