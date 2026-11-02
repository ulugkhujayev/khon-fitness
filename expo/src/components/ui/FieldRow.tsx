import { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { K, Space, Type } from '@/theme';

/** Phones narrower than this get a slimmer label column (FieldRow in Components.kt: 104 dp under 400, else 128). */
const NARROW = 400;

/** Editor field row (DESIGN.md, Editors): the label on the left, the value or control on the right. */
export function FieldRow({
  label,
  note,
  divider = true,
  onPress,
  children,
}: {
  label: string;
  /** A muted line under the label, e.g. "per side"; it never takes width from the control. */
  note?: string;
  /** Hairline above the row; off for the first row of a group. */
  divider?: boolean;
  onPress?: () => void;
  children: ReactNode;
}) {
  const { width } = useWindowDimensions();
  return (
    <Pressable
      onPress={onPress}
      disabled={!onPress}
      accessibilityRole={onPress ? 'button' : undefined}
      accessibilityLabel={onPress ? label : undefined}
      style={({ pressed }) => [pressed && styles.pressed]}>
      {divider ? <View style={styles.divider} /> : null}
      <View style={styles.row}>
        <View style={[styles.label, { width: width < NARROW ? 104 : 128 }]}>
          <Text style={Type.body}>{label}</Text>
          {note ? <Text style={styles.note}>{note}</Text> : null}
        </View>
        <View style={styles.value}>{children}</View>
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  pressed: { backgroundColor: K.Surface2 },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: K.Divider, marginLeft: 16 },
  row: { flexDirection: 'row', alignItems: 'center', minHeight: Space.rowMin, paddingHorizontal: 16, gap: 12 },
  value: { flex: 1, flexDirection: 'row', justifyContent: 'flex-end', alignItems: 'center' },
  // A label that wraps keeps clear of the separators; one line stays inside the 48 pt row.
  label: { paddingVertical: 10 },
  note: { ...Type.footnote, color: K.Muted },
});
