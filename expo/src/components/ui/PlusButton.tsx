import { Pressable, StyleSheet } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { K, Space } from '@/theme';

/** Round "+" section action with a 44 pt target. */
export function PlusButton({ label = 'Add', onPress }: { label?: string; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
      <SymbolView name={{ ios: 'plus', android: 'add', web: 'add' }} tintColor={K.Accent} size={22} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: { width: Space.tapMin, height: Space.tapMin, borderRadius: Space.tapMin / 2, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.5 },
});
