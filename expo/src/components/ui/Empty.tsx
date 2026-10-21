import { StyleSheet, Text } from 'react-native';
import { K, Type } from '@/theme';

/** Empty state (DESIGN.md, Lists): one muted line, no illustration. */
export function Empty({ text }: { text: string }) {
  return <Text style={styles.text}>{text}</Text>;
}

const styles = StyleSheet.create({
  text: { ...Type.subheadline, color: K.Muted, paddingHorizontal: 16, paddingVertical: 14 },
});
