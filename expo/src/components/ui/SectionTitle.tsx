import { ReactNode } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { K, Type } from '@/theme';

/** Grouped section header: Footnote in secondary label, inset 16, an optional action on the right. */
export function SectionTitle({ text, trailing, style }: { text: string; trailing?: ReactNode; style?: ViewStyle }) {
  return (
    <View style={[styles.row, style]}>
      <Text style={[Type.footnote, styles.text]}>{text}</Text>
      {trailing}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', minHeight: 32, paddingLeft: 16, paddingBottom: 6 },
  text: { flex: 1, color: K.Muted },
});
