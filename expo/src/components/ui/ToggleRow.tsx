import { FieldRow } from './FieldRow';
import { Toggle } from './Toggle';

/** Editor row with a toggle on the right: Active, Superset, Intervals, Show archived. */
export function ToggleRow({ label, value, onChange, divider = true }: { label: string; value: boolean; onChange: (v: boolean) => void; divider?: boolean }) {
  return (
    <FieldRow label={label} divider={divider}>
      <Toggle value={value} onChange={onChange} label={label} />
    </FieldRow>
  );
}
