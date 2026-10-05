import { DimensionValue, StyleSheet, Text, View } from 'react-native';
import { GroupedList, TitleRow } from '@/components/ui';
import { cap } from '@/domain/editors';
import { fmt } from '@/domain/format';
import { MuscleVolume } from '@/domain/history';
import { Font, K, tabular, Type } from '@/theme';

/** A bar never shrinks below this share, so a small count still shows. */
const MIN_SHARE = 0.02;

/** Sets per muscle this week against last week (HistoryScreen.kt): the count and a bar for each week. */
export function MuscleVolumeList({ volume }: { volume: MuscleVolume[] }) {
  const max = Math.max(1, ...volume.map((m) => Math.max(m.thisWeek, m.lastWeek)));
  const share = (v: number): DimensionValue => `${Math.min(1, Math.max(MIN_SHARE, v / max)) * 100}%`;
  return (
    <View>
      <TitleRow title="Sets per muscle" muted />
      <Text style={styles.note}>This week, with last week in grey. Secondary muscles count half.</Text>
      {volume.length === 0 ? (
        <Text style={styles.empty}>No finished sets this week or last.</Text>
      ) : (
        <GroupedList>
          {volume.map((m, i) => (
            <View key={m.muscle} style={[styles.row, i > 0 && styles.divider]} accessible accessibilityLabel={`${cap(m.muscle)}, ${fmt(m.thisWeek)} sets this week, ${fmt(m.lastWeek)} last week`}>
              <View style={styles.line}>
                <Text style={styles.muscle}>{cap(m.muscle)}</Text>
                <Text style={styles.now}>{fmt(m.thisWeek)}</Text>
                <Text style={styles.before}>{fmt(m.lastWeek)}</Text>
              </View>
              <View style={[styles.bar, { width: share(m.thisWeek) }]} />
              <View style={[styles.lastBar, { width: share(m.lastWeek) }]} />
            </View>
          ))}
        </GroupedList>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  note: { ...Type.footnote, color: K.Dim, marginTop: 2, marginBottom: 10 },
  empty: { ...Type.subheadline, color: K.Dim },
  row: { paddingHorizontal: 16, paddingTop: 10, paddingBottom: 10 },
  divider: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: K.Divider },
  line: { flexDirection: 'row', alignItems: 'center', marginBottom: 4 },
  muscle: { ...Type.body, flex: 1 },
  now: { ...Type.body, fontFamily: Font.bold, ...tabular },
  before: { ...Type.subheadline, color: K.Muted, marginLeft: 8, ...tabular },
  bar: { height: 6, borderRadius: 3, backgroundColor: K.Accent },
  lastBar: { height: 4, borderRadius: 2, backgroundColor: K.Surface3, marginTop: 3 },
});
