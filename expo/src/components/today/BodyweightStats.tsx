import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import { GroupedList, LineChart, Segmented, StatRow, TitleRow } from '@/components/ui';
import { DEFAULT_PERIOD, periodStats, PERIODS, signedKg } from '@/domain/bodyweight';
import { fmt } from '@/domain/format';
import { Bodyweight } from '@/domain/types';
import { K, Space, Type } from '@/theme';

const LABELS = PERIODS.map((p) => p.label);
const kg = (v: number | null) => (v == null ? '—' : `${fmt(v)} kg`);

/**
 * Bodyweight statistics on Today's first card: the latest value, a period switch (1W, 1M, 3M, 1Y), the
 * change, lowest and highest in that period, and its chart.
 */
export function BodyweightStats({ today, bodyweights }: { today: string; bodyweights: Bodyweight[] }) {
  const [period, setPeriod] = useState(DEFAULT_PERIOD);
  const latest = bodyweights.at(-1);
  const stats = periodStats(bodyweights, today, PERIODS[period].days);

  return (
    <GroupedList style={styles.card}>
      <TitleRow title="Bodyweight" value={latest ? `${fmt(latest.kg)} kg` : '—'} valueLabel={latest ? undefined : 'No entry yet'} large />
      {latest ? (
        <>
          <Segmented options={LABELS} selected={period} onSelect={setPeriod} elevated style={styles.periods} />
          <StatRow
            items={[
              { value: stats.change == null ? '—' : signedKg(stats.change), label: 'Change' },
              { value: kg(stats.lowest), label: 'Lowest' },
              { value: kg(stats.highest), label: 'Highest' },
            ]}
            style={styles.stats}
          />
          <LineChart
            points={stats.points.map((b) => ({ date: b.date, value: b.kg }))}
            unit="kg"
            height={150}
            surface={K.Surface}
            emptyText="No entries in this period"
            style={styles.chart}
          />
        </>
      ) : (
        <Text style={styles.empty}>No entries yet. Pick your weight below.</Text>
      )}
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  card: { marginTop: 28, padding: Space.margin },
  periods: { marginTop: 14 },
  stats: { marginTop: 18 },
  chart: { marginTop: 12 },
  empty: { ...Type.subheadline, color: K.Dim, marginTop: 6 },
});
