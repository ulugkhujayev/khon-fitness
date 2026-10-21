import { StyleSheet, View } from 'react-native';
import { K } from '@/theme';

/** Thin progress track (LinearProgressIndicator in SessionScreen.kt): a green fill (or `color`) on Surface2, 5 pt tall. */
export function ProgressBar({
  done,
  total,
  label,
  thin = false,
  color = K.Green,
}: {
  done: number;
  total: number;
  label: string;
  /** 2 pt and square, a line along a bar's bottom edge. */
  thin?: boolean;
  /** The fill: green for sets done (the session), accent on Today's card, which is accent throughout. */
  color?: string;
}) {
  const share = total > 0 ? Math.min(1, done / total) : 0;
  return (
    <View
      style={[styles.track, thin && styles.thin]}
      accessibilityRole="progressbar"
      accessibilityLabel={label}
      accessibilityValue={{ min: 0, max: total, now: done }}>
      <View style={[styles.fill, thin && styles.thin, { width: `${share * 100}%`, backgroundColor: color }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  track: { height: 5, borderRadius: 3, backgroundColor: K.Surface2, overflow: 'hidden' },
  fill: { height: 5, borderRadius: 3 },
  thin: { height: 2, borderRadius: 0 },
});
