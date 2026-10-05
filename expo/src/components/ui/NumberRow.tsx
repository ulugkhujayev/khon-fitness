import { StyleSheet, Text } from 'react-native';
import { K, Type } from '@/theme';
import { FieldRow } from './FieldRow';
import { COMPACT_WIDTH, NumberField } from './NumberField';

/** Editor row with a number widget on the right, 176 wide and 38 tall (NumberRow in CardioScreen.kt). */
export function NumberRow({
  label,
  value,
  unit,
  step,
  decimals,
  divider = true,
  placeholder,
  note,
  onChange,
}: {
  label: string;
  value: number | null;
  unit: string;
  step: number;
  decimals: number;
  divider?: boolean;
  placeholder?: string;
  /** A muted word before the field, e.g. "per side". */
  note?: string;
  onChange: (v: number | null) => void;
}) {
  return (
    <FieldRow label={label} divider={divider}>
      {note ? <Text style={styles.note}>{note}</Text> : null}
      <NumberField
        value={value}
        onChange={onChange}
        step={step}
        unit={unit}
        decimals={decimals}
        height={38}
        label={label}
        placeholder={placeholder}
        style={styles.field}
      />
    </FieldRow>
  );
}

const styles = StyleSheet.create({
  field: { width: COMPACT_WIDTH },
  note: { ...Type.footnote, color: K.Muted, marginRight: 8 },
});
