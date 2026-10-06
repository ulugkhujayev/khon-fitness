import { StyleSheet, Text, View } from 'react-native';
import { K, tabular, Type } from '@/theme';

// Not in DESIGN.md before the Expo app: 3 pt segments 3 pt apart, like story progress.
const SEGMENT = 3;

/**
 * Where a routine run is, under the player's bar: one segment per stretch and side (done ones white, the
 * current one white, the rest soft), and the line "3 of 12 · 6:30 remaining" under them.
 */
export function RoutineProgress({ count, index, text }: { count: number; index: number; text: string }) {
  return (
    <View accessible accessibilityRole="progressbar" accessibilityLabel={text} accessibilityValue={{ min: 1, max: count, now: index + 1 }}>
      <View style={styles.segments}>
        {Array.from({ length: count }, (_, i) => (
          <View key={i} style={[styles.segment, i < index && styles.done, i === index && styles.current]} />
        ))}
      </View>
      <Text style={styles.text}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  segments: { flexDirection: 'row', gap: 3 },
  segment: { flex: 1, height: SEGMENT, borderRadius: SEGMENT / 2, backgroundColor: K.OnPhaseSoft },
  done: { backgroundColor: K.OnPhaseDim },
  current: { backgroundColor: K.White },
  text: { ...Type.footnote, ...tabular, color: K.OnPhaseDim, textAlign: 'center', marginTop: 6 },
});
