import { StyleSheet, Text, View } from 'react-native';
import { dayShares, EatingWindowRules } from '@/domain/window';
import { EatingWindow } from '@/domain/types';
import { K, tabular, Type } from '@/theme';

// Not in DESIGN.md before the Expo app: an 8 pt day track, a 2 × 16 pt mark for now, 40 pt per time label.
const TRACK = 8;
const NOW = { width: 2, height: 16 } as const;
const LABEL = 40;

/**
 * The day from 00:00 to 24:00 with the fixed hours in green and a white mark at now, the opening and closing
 * times under the ends of the green. Decorative: the headline above it says the same in words.
 */
export function WindowDayBar({ window: w, now }: { window: EatingWindow; now: Date }) {
  const s = dayShares(w, now);
  const pct = (share: number) => `${share * 100}%` as const;
  return (
    <View importantForAccessibility="no-hide-descendants" accessibilityElementsHidden>
      <View style={styles.bar}>
        <View style={styles.track}>
          <View style={[styles.hours, { left: pct(s.from), width: pct(s.to - s.from) }]} />
        </View>
        <View style={[styles.now, { left: pct(s.now) }]} />
      </View>
      <View style={styles.labels}>
        <Text style={[styles.label, { left: pct(s.from) }]}>{EatingWindowRules.hhmm(w.startMinute)}</Text>
        <Text style={[styles.label, { left: pct(s.to) }]}>{EatingWindowRules.hhmm(w.endMinute)}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { height: NOW.height, justifyContent: 'center' },
  track: { height: TRACK, borderRadius: TRACK / 2, backgroundColor: K.Surface3, overflow: 'hidden' },
  hours: { position: 'absolute', top: 0, bottom: 0, backgroundColor: K.Green },
  now: { position: 'absolute', top: 0, width: NOW.width, height: NOW.height, marginLeft: -NOW.width / 2, borderRadius: 1, backgroundColor: K.White },
  labels: { height: 18, marginTop: 4 },
  label: { ...Type.caption1, ...tabular, color: K.Muted, position: 'absolute', width: LABEL, marginLeft: -LABEL / 2, textAlign: 'center' },
});
