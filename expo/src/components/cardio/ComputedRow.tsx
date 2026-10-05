import { StyleSheet, Text } from 'react-native';
import { FieldRow } from '@/components/ui';
import { K, Type } from '@/theme';

/** A value the form works out from other fields, in secondary label: pace, swim distance. */
export function ComputedRow({ label, value }: { label: string; value: string }) {
  return (
    <FieldRow label={label}>
      <Text style={styles.value}>{value}</Text>
    </FieldRow>
  );
}

const styles = StyleSheet.create({
  value: { ...Type.body, color: K.Muted },
});
