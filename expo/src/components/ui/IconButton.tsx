import { Pressable, StyleSheet } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { K, Space } from '@/theme';

/** A bare symbol with a 44 pt target, for the few controls that live inside a row (a routine's order). Dim when off. */
export function IconButton({
  icon,
  label,
  color = K.Accent,
  enabled = true,
  onPress,
}: {
  icon: SymbolViewProps['name'];
  /** Spoken name, e.g. "Move Hamstring stretch up". */
  label: string;
  color?: string;
  enabled?: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: !enabled }}
      style={({ pressed }) => [styles.target, pressed && styles.pressed]}>
      <SymbolView name={icon} tintColor={enabled ? color : K.Surface3} size={18} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  target: { width: Space.tapMin, height: Space.tapMin, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.5 },
});
