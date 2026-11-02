import { StyleSheet, View } from 'react-native';
import { LineChart, TitleRow } from '@/components/ui';
import { addDays } from '@/domain/dates';
import { fmt } from '@/domain/format';
import { Bodyweight } from '@/domain/types';
import { BodyweightEntry } from './BodyweightEntry';

/** The chart covers the last 30 days, today included. */
const CHART_DAYS = 30;

/** Bodyweight on Today (DESIGN.md, Today): the latest value, a chart of the last 30 days, and the entry with its ruler. */
export function BodyweightSection({ today, bodyweights, onAdd }: { today: string; bodyweights: Bodyweight[]; onAdd: (kg: number) => void }) {
  const latest = bodyweights.at(-1);
  const from = addDays(today, 1 - CHART_DAYS);
  const recent = bodyweights.filter((b) => b.date >= from);

  return (
    <View>
      <TitleRow title="Bodyweight" value={latest ? `${fmt(latest.kg)} kg` : '—'} valueLabel={latest ? undefined : 'No entry yet'} large style={styles.header} />
      {recent.length > 0 ? <LineChart points={recent.map((b) => ({ date: b.date, value: b.kg }))} unit="kg" style={styles.chart} /> : null}
      <BodyweightEntry today={today} bodyweights={bodyweights} onSave={onAdd} />
    </View>
  );
}

const styles = StyleSheet.create({
  header: { marginTop: 28 },
  chart: { marginTop: 8 },
});
