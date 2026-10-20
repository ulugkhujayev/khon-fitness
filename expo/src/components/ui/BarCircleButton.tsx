import { Pressable, StyleSheet, View } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { K, Space } from '@/theme';

/** Bar item (DESIGN.md, navigation bar): a 36 pt round button on a 44 pt target with an accent symbol. */
export function BarCircleButton({ icon, label, onPress }: { icon: SymbolViewProps['name']; label: string; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" accessibilityLabel={label} style={styles.target}>
      {({ pressed }) => (
        <View style={[styles.circle, pressed && styles.pressed]}>
          <SymbolView name={icon} tintColor={K.Accent} size={18} />
        </View>
      )}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  target: { width: Space.tapMin, height: Space.tapMin, alignItems: 'center', justifyContent: 'center' },
  circle: { width: 36, height: 36, borderRadius: 18, backgroundColor: K.Surface2, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.6 },
});
