import { Pressable, StyleSheet, Text } from 'react-native';
import { Font, K, Type } from '@/theme';

/** The trailing bar action as a 34 pt accent capsule (DESIGN.md, navigation bar): Done, Edit, Finish. */
export function Capsule({ text, onPress }: { text: string; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" hitSlop={6} style={({ pressed }) => [styles.capsule, pressed && styles.pressed]}>
      <Text style={styles.text}>{text}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  capsule: { height: 34, borderRadius: 17, backgroundColor: K.Accent, paddingHorizontal: 14, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.8 },
  text: { ...Type.subheadline, fontFamily: Font.semibold, color: K.AccentInk },
});
