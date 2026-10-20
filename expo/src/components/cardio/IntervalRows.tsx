import { StyleSheet, View } from 'react-native';
import { COMPACT_WIDTH, FieldRow, GroupedList, NumberField } from '@/components/ui';
import { IntervalLog } from '@/domain/types';

/** One row per work interval with its average heart rate and watts (CardioScreen.kt). */
export function IntervalRows({ intervals, onChange }: { intervals: IntervalLog[]; onChange: (intervals: IntervalLog[]) => void }) {
  const set = (id: string, patch: Partial<IntervalLog>) => onChange(intervals.map((iv) => (iv.id === id ? { ...iv, ...patch } : iv)));
  const whole = (v: number | null) => (v == null ? null : Math.round(v));
  return (
    <GroupedList>
      {intervals.map((iv, i) => (
        <FieldRow key={iv.id} label={`Round ${i + 1}`} divider={i > 0}>
          <View style={styles.fields}>
            <NumberField value={iv.avgHr} onChange={(v) => set(iv.id, { avgHr: whole(v) })} step={1} unit="bpm" decimals={0} height={38} label={`Round ${i + 1} heart rate`} style={styles.field} />
            <NumberField value={iv.watts} onChange={(v) => set(iv.id, { watts: whole(v) })} step={5} unit="W" decimals={0} height={38} label={`Round ${i + 1} watts`} style={styles.field} />
          </View>
        </FieldRow>
      ))}
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  fields: { alignItems: 'flex-end', gap: 6, paddingVertical: 8 },
  field: { width: COMPACT_WIDTH },
});
