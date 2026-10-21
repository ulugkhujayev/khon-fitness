import { Pressable, StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { SymbolView } from 'expo-symbols';
import { K } from '@/theme';

/** Round 36 pt Back button with a chevron and no text (DESIGN.md, navigation bar). */
export function BackButton({ label = 'Back', onPress = () => router.back() }: { label?: string; onPress?: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={label}
      hitSlop={4}
      style={({ pressed }) => [styles.button, pressed && styles.pressed]}>
      <SymbolView name={{ ios: 'chevron.left', android: 'arrow_back_ios_new', web: 'arrow_back_ios_new' }} tintColor={K.Text} size={17} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: { width: 36, height: 36, borderRadius: 18, backgroundColor: K.Surface2, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.6 },
});
