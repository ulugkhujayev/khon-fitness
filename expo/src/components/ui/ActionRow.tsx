import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { K, Space, Type } from '@/theme';

/** Symbols for the editors' actions, so each action looks the same wherever it appears. */
export const ACTION_ICONS = {
  archive: { ios: 'archivebox', android: 'archive', web: 'archive' },
  unarchive: { ios: 'tray.and.arrow.up', android: 'unarchive', web: 'unarchive' },
  merge: { ios: 'arrow.triangle.merge', android: 'merge', web: 'merge' },
  delete: { ios: 'trash', android: 'delete', web: 'delete' },
  up: { ios: 'arrow.up', android: 'arrow_upward', web: 'arrow_upward' },
  down: { ios: 'arrow.down', android: 'arrow_downward', web: 'arrow_downward' },
} satisfies Record<string, SymbolViewProps['name']>;

/**
 * An action as a row in a GroupedList (DESIGN.md, Editors), like the actions at the end of an iOS contact:
 * a symbol and the action's name in its color, accent or red for a destructive one. Off, both turn dim and
 * the row does nothing.
 */
export function ActionRow({
  icon,
  title,
  color = K.Accent,
  enabled = true,
  last = false,
  onPress,
}: {
  icon: SymbolViewProps['name'];
  title: string;
  color?: string;
  enabled?: boolean;
  /** The last row draws no separator. */
  last?: boolean;
  onPress: () => void;
}) {
  const tint = enabled ? color : K.Dim;
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      accessibilityLabel={title}
      accessibilityState={{ disabled: !enabled }}
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}>
      <View style={styles.icon}>
        <SymbolView name={icon} tintColor={tint} size={20} />
      </View>
      <View style={[styles.inner, !last && styles.separator]}>
        <Text style={[styles.title, { color: tint }]}>{title}</Text>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', paddingLeft: 16 },
  pressed: { backgroundColor: K.Surface2 },
  icon: { width: 24, alignItems: 'center', marginRight: 12 },
  inner: { flex: 1, minHeight: Space.rowMin, justifyContent: 'center', paddingRight: 16 },
  separator: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: K.Divider },
  title: { ...Type.body },
});
