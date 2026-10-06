import { StyleSheet, Text } from 'react-native';
import { K, Type } from '@/theme';

/** A muted note under a group that says what its setting does (a grouped list's footer in HIG). */
export function GroupFooter({ text }: { text: string }) {
  return <Text style={styles.text}>{text}</Text>;
}

const styles = StyleSheet.create({
  text: { ...Type.footnote, color: K.Muted, paddingHorizontal: 16, marginTop: 6 },
});
