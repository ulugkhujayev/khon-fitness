import { StyleSheet, View } from 'react-native';
import { K } from '@/theme';

/** Thin progress track (LinearProgressIndicator in SessionScreen.kt): green fill on Surface2, 5 pt tall. */
export function ProgressBar({
  done,
  total,
  label,
  thin = false,
}: {
  done: number;
  total: number;
  label: string;
  /** 2 pt and square, a line along a bar's bottom edge. */
  thin?: boolean;
}) {
  const share = total > 0 ? Math.min(1, done / total) : 0;
  return (
    <View
      style={[styles.track, thin && styles.thin]}
      accessibilityRole="progressbar"
      accessibilityLabel={label}
      accessibilityValue={{ min: 0, max: total, now: done }}>
      <View style={[styles.fill, thin && styles.thin, { width: `${share * 100}%` }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: { height: 5, borderRadius: 3, backgroundColor: K.Surface2, overflow: 'hidden' },
  fill: { height: 5, borderRadius: 3, backgroundColor: K.Green },
  thin: { height: 2, borderRadius: 0 },
});
