import { StyleSheet, Text } from 'react-native';
import { Font, K, Type } from '@/theme';

/** Under a set widget: the gain since last time in green, else the unit when the widget is too narrow to show it. */
export function SetDelta({ gain, unit }: { gain: string | null; unit: string | null }) {
  if (gain) return <Text style={[styles.text, styles.gain]}>{gain}</Text>;
  if (unit) return <Text style={[styles.text, styles.unit]}>{unit}</Text>;
  return null;
}

const styles = StyleSheet.create({
  // Lines up with the value: past the 36-40 pt minus button (Delta in SessionScreen.kt).
  text: { ...Type.caption1, fontFamily: Font.semibold, paddingLeft: 40, marginTop: 3 },
  gain: { color: K.Green },
  unit: { color: K.Dim },
});
