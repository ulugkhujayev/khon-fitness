import { StyleSheet, Text } from 'react-native';
import { K, Type } from '@/theme';

/** A muted note under a group that says what its setting does (a grouped list's footer in HIG); red for an error. */
export function GroupFooter({ text, error = false }: { text: string; error?: boolean }) {
  return <Text style={[styles.text, error && styles.error]}>{text}</Text>;
}

const styles = StyleSheet.create({
  text: { ...Type.footnote, color: K.Muted, paddingHorizontal: 16, marginTop: 6 },
  error: { color: K.Red },
});
