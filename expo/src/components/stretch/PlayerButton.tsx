import { Pressable, StyleSheet } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { K } from '@/theme';

/** Size of the round control (not in DESIGN.md before the Expo app), in a 48 pt target. */
const SIZE = 40;
const TARGET = 48;

/** A round control on the player's bar, Stop or Skip: a white symbol on soft white, readable on black and on green. */
export function PlayerButton({ icon, label, enabled = true, onPress }: { icon: SymbolViewProps['name']; label: string; enabled?: boolean; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      disabled={!enabled}
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled: !enabled }}
      hitSlop={(TARGET - SIZE) / 2}
      style={({ pressed }) => [styles.button, !enabled && styles.off, pressed && styles.pressed]}>
      <SymbolView name={icon} tintColor={K.White} size={17} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: { width: SIZE, height: SIZE, borderRadius: SIZE / 2, backgroundColor: K.OnPhaseSoft, alignItems: 'center', justifyContent: 'center' },
  off: { opacity: 0.35 },
  pressed: { opacity: 0.6 },
});
