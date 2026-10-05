import { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { Font, K, Space, Type } from '@/theme';

/** Formats a number like the Kotlin fmt(): 60.0 -> "60", 62.5 -> "62.5". */
export function fmt(value: number | null | undefined, decimals = 1): string {
  if (value == null) return '';
  const v = decimals === 0 ? Math.trunc(value) : value;
  const s = v.toFixed(decimals);
  return decimals > 0 && s.endsWith('.0') ? s.slice(0, -2) : s;
}

/** Large title at the top of a scrolling tab screen, bar items trailing. */
export function ScreenTitle({ text, trailing }: { text: string; trailing?: ReactNode }) {
  return (
    <View style={styles.titleRow}>
      <Text style={[Type.largeTitle, { flex: 1 }]} accessibilityRole="header">
        {text}
      </Text>
      {trailing}
    </View>
  );
}

/** Grouped section header: Footnote in secondary label, inset 16. */
export function SectionTitle({ text, trailing }: { text: string; trailing?: ReactNode }) {
  return (
    <View style={styles.sectionRow}>
      <Text style={[Type.footnote, { color: K.Muted, flex: 1 }]}>{text}</Text>
      {trailing}
    </View>
  );
}

export function TextButton({
  text,
  color = K.Accent,
  enabled = true,
  onPress,
  label,
}: {
  text: string;
  color?: string;
  enabled?: boolean;
  onPress: () => void;
  label?: string;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      accessibilityLabel={label ?? text}
      hitSlop={4}
      style={({ pressed }) => [styles.textButton, pressed && { opacity: 0.5 }]}>
      <Text style={[Type.body, { fontFamily: Font.medium, color: enabled ? color : K.Dim }]}>{text}</Text>
    </Pressable>
  );
}

/** Round "+" bar or section action with a 44 pt target. */
export function PlusButton({ label = 'Add', onPress }: { label?: string; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      style={({ pressed }) => [styles.plus, pressed && { opacity: 0.5 }]}>
      <SymbolView name={{ ios: 'plus', android: 'add', web: 'add' }} tintColor={K.Accent} size={22} />
    </Pressable>
  );
}

/** One prominent button per screen: 50 pt full-width accent capsule. */
export function PrimaryButton({
  text,
  enabled = true,
  onPress,
  style,
}: {
  text: string;
  enabled?: boolean;
  onPress: () => void;
  style?: ViewStyle;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      style={({ pressed }) => [
        styles.primary,
        { backgroundColor: enabled ? K.Accent : K.Surface2, opacity: pressed ? 0.8 : 1 },
        style,
      ]}>
      <Text style={[Type.headline, { color: enabled ? K.AccentInk : K.Muted }]}>{text}</Text>
    </Pressable>
  );
}

/** iOS-style inset grouped list. Rows draw their own inset separators. */
export function GroupedList({ children, style }: { children: ReactNode; style?: ViewStyle }) {
  return <View style={[styles.group, style]}>{children}</View>;
}

/**
 * List row: optional color dot, primary text, spacer, optional value, optional chevron.
 * Tapping the row opens its editor; no buttons inside rows.
 */
export function ListRow({
  title,
  value,
  dot,
  dotFilled = true,
  chevron = true,
  last = false,
  color = K.Text,
  onPress,
}: {
  title: string;
  /** Title color; K.Red for destructive rows, K.Accent for plain actions. */
  color?: string;
  value?: string;
  dot?: string;
  /** Filled for an active or high-intensity item, a ring otherwise. */
  dotFilled?: boolean;
  chevron?: boolean;
  last?: boolean;
  onPress?: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      accessibilityRole={onPress ? 'button' : undefined}
      style={({ pressed }) => [styles.row, pressed && { backgroundColor: K.Surface2 }]}>
      {dot ? (
        <View style={styles.dotCol}>
          <View style={[styles.dot, dotFilled ? { backgroundColor: dot } : { borderWidth: 2, borderColor: dot }]} />
        </View>
      ) : null}
      <View style={[styles.rowInner, !last && styles.separator]}>
        <Text style={[Type.body, { flex: 1, color }]}>{title}</Text>
        {value ? <Text style={[Type.body, { color: K.Muted }]}>{value}</Text> : null}
        {chevron && onPress ? (
          <SymbolView
            name={{ ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }}
            tintColor={K.Dim}
            size={14}
            style={{ marginLeft: 8 }}
          />
        ) : null}
      </View>
    </Pressable>
  );
}

/** Empty state: one muted line, no illustration. */
export function Empty({ text }: { text: string }) {
  return <Text style={[Type.subheadline, { color: K.Muted, paddingHorizontal: 16, paddingVertical: 12 }]}>{text}</Text>;
}

const styles = StyleSheet.create({
  titleRow: { flexDirection: 'row', alignItems: 'center', paddingTop: 4, paddingBottom: 12 },
  sectionRow: { flexDirection: 'row', alignItems: 'center', minHeight: 32, paddingLeft: 16, paddingBottom: 6 },
  textButton: { minHeight: Space.tapMin, justifyContent: 'center', paddingHorizontal: 4 },
  plus: { width: 44, height: 44, borderRadius: 22, alignItems: 'center', justifyContent: 'center' },
  primary: { height: Space.buttonHeight, borderRadius: 25, alignItems: 'center', justifyContent: 'center', alignSelf: 'stretch' },
  group: { backgroundColor: K.Surface, borderRadius: Space.groupRadius, overflow: 'hidden' },
  row: { flexDirection: 'row', alignItems: 'center', paddingLeft: 16 },
  rowInner: { flex: 1, flexDirection: 'row', alignItems: 'center', minHeight: Space.rowMin, paddingRight: 16 },
  separator: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: K.Divider },
  dotCol: { width: 24 },
  dot: { width: 12, height: 12, borderRadius: 6 },
});
