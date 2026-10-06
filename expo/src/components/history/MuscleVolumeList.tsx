import { StyleSheet, Text, View } from 'react-native';
import { GroupedList, TitleRow } from '@/components/ui';
import { cap } from '@/domain/editors';
import { fmt } from '@/domain/format';
import { MuscleVolume, volumeShares } from '@/domain/history';
import { Font, K, tabular, Type } from '@/theme';

// Not in Theme.kt: a 6 pt track with this week's fill, and a 2 × 14 pt mark where last week ended.
const TRACK = 6;
const MARK = { width: 2, height: 14 } as const;

/**
 * Sets per muscle this week against last week (HistoryScreen.kt): the muscle with this week's count and
 * "last 3", then one track filled to this week's sets with a mark where last week's ended, so passing the
 * mark reads as beating last week.
 */
export function MuscleVolumeList({ volume }: { volume: MuscleVolume[] }) {
  const shares = volumeShares(volume);
  return (
    <View>
      <TitleRow title="Sets per muscle" muted />
      <Text style={styles.note}>This week; the white mark is last week. Secondary muscles count half.</Text>
      {volume.length === 0 ? (
        <Text style={styles.empty}>No finished sets this week or last.</Text>
      ) : (
        <GroupedList>
          {volume.map((m, i) => (
            <View
              key={m.muscle}
              style={[styles.row, i > 0 && styles.divider]}
              accessible
              accessibilityLabel={`${cap(m.muscle)}, ${fmt(m.thisWeek)} sets this week, ${fmt(m.lastWeek)} last week`}>
              <View style={styles.line}>
                <Text style={styles.muscle}>{cap(m.muscle)}</Text>
                <Text style={styles.now}>{fmt(m.thisWeek)}</Text>
                <Text style={styles.before}>{`last ${fmt(m.lastWeek)}`}</Text>
              </View>
              <View style={styles.bar}>
                <View style={styles.track}>
                  <View style={[styles.fill, { width: `${shares[i].now * 100}%` }]} />
                </View>
                {shares[i].last > 0 ? <View style={[styles.mark, { left: `${shares[i].last * 100}%` }]} /> : null}
              </View>
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
  row: { paddingHorizontal: 16, paddingTop: 10, paddingBottom: 12 },
  divider: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: K.Divider },
  line: { flexDirection: 'row', alignItems: 'baseline', marginBottom: 6 },
  muscle: { ...Type.body, flex: 1 },
  now: { ...Type.body, fontFamily: Font.bold, ...tabular },
  before: { ...Type.footnote, color: K.Muted, marginLeft: 8, ...tabular },
  // As tall as the mark, the track centered in it, so the mark stands above and below the track.
  bar: { height: MARK.height, justifyContent: 'center' },
  track: { height: TRACK, borderRadius: TRACK / 2, backgroundColor: K.Surface2, overflow: 'hidden' },
  fill: { height: TRACK, borderRadius: TRACK / 2, backgroundColor: K.Accent },
  mark: { position: 'absolute', top: 0, width: MARK.width, height: MARK.height, marginLeft: -MARK.width / 2, borderRadius: 1, backgroundColor: K.Text },
});
