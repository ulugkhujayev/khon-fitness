import { StyleSheet, Text, View } from 'react-native';
import { LineChart, TitleRow } from '@/components/ui';
import { fmt } from '@/domain/format';
import { Bodyweight } from '@/domain/types';
import { K, Type } from '@/theme';

/** Every bodyweight entry as a chart, the latest value next to the title (HistoryScreen.kt). */
export function BodyweightHistory({ bodyweights }: { bodyweights: Bodyweight[] }) {
  const latest = bodyweights.at(-1);
  return (
    <View>
      <TitleRow title="Bodyweight" value={latest ? `${fmt(latest.kg)} kg` : undefined} large />
      {latest ? (
        <LineChart points={bodyweights.map((b) => ({ date: b.date, value: b.kg }))} unit="kg" height={170} style={styles.chart} />
      ) : (
        <Text style={styles.empty}>No entries yet. Add one on Today.</Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  chart: { marginTop: 8 },
  empty: { ...Type.subheadline, color: K.Dim, marginTop: 8 },
});
