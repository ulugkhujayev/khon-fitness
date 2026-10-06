import { clampEffort } from '@/domain/workout';
import { NumberRow } from './NumberRow';

/** Effort as an editor row: a dash until set, "/10" after the value, steps and typing kept within 1 to 10. */
export function EffortRow({ value, divider, onChange }: { value: number | null; divider?: boolean; onChange: (effort: number | null) => void }) {
  return (
    <NumberRow
      label="Effort"
      value={value}
      unit="/10"
      step={1}
      decimals={0}
      min={1}
      max={10}
      placeholder="—"
      divider={divider}
      onChange={(v) => onChange(clampEffort(v))}
    />
  );
}
