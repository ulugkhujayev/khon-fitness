import { StyleSheet } from 'react-native';
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
  onChange,
}: {
  label: string;
  value: number | null;
  unit: string;
  step: number;
  decimals: number;
  divider?: boolean;
  placeholder?: string;
  onChange: (v: number | null) => void;
}) {
  return (
    <FieldRow label={label} divider={divider}>
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
});
