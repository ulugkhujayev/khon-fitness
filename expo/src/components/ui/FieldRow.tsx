import { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { K, Space, Type } from '@/theme';

/** Phones narrower than this get a slimmer label column (FieldRow in Components.kt: 104 dp under 400, else 128). */
const NARROW = 400;

/** Editor field row (DESIGN.md, Editors): the label on the left, the value or control on the right. */
export function FieldRow({
  label,
  divider = true,
  onPress,
  children,
}: {
  label: string;
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
        <Text style={[Type.body, { width: width < NARROW ? 104 : 128 }]}>{label}</Text>
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
});
