import { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { K, Space, Type } from '@/theme';
import { Chevron } from './Chevron';
import { Dot } from './Dot';

/**
 * List row (DESIGN.md, Lists): optional color dot, the title, a muted value, an optional trailing mark,
 * a chevron when it opens something. Tapping the row opens its editor; there are no buttons inside rows.
 */
export function ListRow({
  title,
  value,
  dot,
  dotFilled = true,
  chevron = true,
  last = false,
  color = K.Text,
  trailing,
  selected,
  elevated = false,
  leading,
  onPress,
}: {
  title: string;
  value?: string;
  dot?: string;
  /** Filled for an active or high-intensity item, a ring otherwise. */
  dotFilled?: boolean;
  chevron?: boolean;
  /** The last row draws no separator. */
  last?: boolean;
  /** Title color: K.Red for destructive rows, K.Accent for plain actions, K.Dim for archived. */
  color?: string;
  trailing?: ReactNode;
  /** Set on choice rows, so screen readers hear the state. */
  selected?: boolean;
  /** On the elevated fill inside a sheet. */
  elevated?: boolean;
  /** In place of the dot, e.g. a block's IndexBadge. */
  leading?: ReactNode;
  onPress?: () => void;
}) {
  const role = onPress ? (selected !== undefined ? 'radio' : 'button') : undefined;
  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      accessibilityRole={role}
      accessibilityState={selected !== undefined ? { selected } : undefined}
      style={({ pressed }) => [styles.row, pressed && { backgroundColor: elevated ? K.Surface3 : K.Surface2 }]}>
      {leading ? <View style={styles.leading}>{leading}</View> : null}
      {dot && !leading ? (
        <View style={styles.dotColumn}>
          <Dot color={dot} filled={dotFilled} />
        </View>
      ) : null}
      <View style={[styles.inner, !last && styles.separator]}>
        <Text style={[styles.title, { color }]} numberOfLines={2}>
          {title}
        </Text>
        {value ? (
          <Text style={styles.value} numberOfLines={1}>
            {value}
          </Text>
        ) : null}
        {trailing}
        {chevron && onPress ? <Chevron /> : null}
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', paddingLeft: 16 },
  dotColumn: { width: 24 },
  leading: { marginRight: 12 },
  inner: { flex: 1, flexDirection: 'row', alignItems: 'center', minHeight: Space.rowMin, paddingRight: 16 },
  separator: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: K.Divider },
  title: { ...Type.body, flex: 1, paddingVertical: 12 },
  value: { ...Type.subheadline, color: K.Muted, marginLeft: 8, flexShrink: 1 },
});
