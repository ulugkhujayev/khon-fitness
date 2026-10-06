import { FieldRow } from './FieldRow';
import { Toggle } from './Toggle';

/** Editor row with a toggle on the right: Active, Superset, Intervals, Show archived. */
export function ToggleRow({
  label,
  value,
  onChange,
  divider = true,
  disabled = false,
}: {
  label: string;
  value: boolean;
  onChange: (v: boolean) => void;
  divider?: boolean;
  /** Shown but fixed, e.g. the active program, which only another program can take over. */
  disabled?: boolean;
}) {
  return (
    <FieldRow label={label} divider={divider}>
      <Toggle value={value} onChange={onChange} label={label} disabled={disabled} />
    </FieldRow>
  );
}
