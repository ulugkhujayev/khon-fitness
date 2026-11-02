import { StyleSheet, Text } from 'react-native';
import { K, Type } from '@/theme';

/** Group header inside a sheet. */
export function SheetGroupTitle({ text }: { text: string }) {
  return <Text style={styles.text}>{text}</Text>;
}

const styles = StyleSheet.create({
  text: { ...Type.footnote, color: K.Muted, paddingLeft: 16, paddingTop: 14, paddingBottom: 6 },
});
