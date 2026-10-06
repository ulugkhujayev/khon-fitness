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
  note,
  min,
  max,
  onChange,
}: {
  label: string;
  value: number | null;
  unit: string;
  step: number;
  decimals: number;
  divider?: boolean;
  placeholder?: string;
  /** A muted line under the label, e.g. "per side". */
  note?: string;
  min?: number;
  max?: number;
  onChange: (v: number | null) => void;
}) {
  return (
    <FieldRow label={label} note={note} divider={divider}>
      <NumberField
        value={value}
        onChange={onChange}
        step={step}
        unit={unit}
        decimals={decimals}
        height={38}
        label={label}
        placeholder={placeholder}
        min={min}
        max={max}
        style={styles.field}
      />
    </FieldRow>
  );
}

const styles = StyleSheet.create({
  field: { width: COMPACT_WIDTH },
});
