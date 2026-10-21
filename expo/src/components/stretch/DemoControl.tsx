import { Pressable, StyleSheet } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { K, Space } from '@/theme';

/** Size of the round control (not in DESIGN.md; a video player's corner control), with a 44 pt target around it. */
const SIZE = 40;

/** A round control floating over a corner of the demonstration: a white symbol on dark glass, like a video player's. */
export function DemoControl({ icon, label, onPress }: { icon: SymbolViewProps['name']; label: string; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" accessibilityLabel={label} hitSlop={(Space.tapMin - SIZE) / 2} style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
      <SymbolView name={icon} tintColor={K.White} size={18} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: { width: SIZE, height: SIZE, borderRadius: SIZE / 2, backgroundColor: K.Glass, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.7 },
});
