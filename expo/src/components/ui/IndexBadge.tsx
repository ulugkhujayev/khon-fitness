import { StyleSheet, Text, View } from 'react-native';
import { Font, K, Type } from '@/theme';

/** A block's number (zero-based `index`, shown from 1) in a small accent-tinted circle: session blocks, a program's block list. */
export function IndexBadge({ index }: { index: number }) {
  return (
    <View style={styles.badge}>
      <Text style={styles.text}>{index + 1}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: { width: 24, height: 24, borderRadius: 12, backgroundColor: K.AccentSoft, alignItems: 'center', justifyContent: 'center' },
  text: { ...Type.footnote, fontFamily: Font.bold, color: K.Accent },
});
