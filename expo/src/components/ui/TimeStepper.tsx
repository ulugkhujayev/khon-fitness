import { Pressable, StyleSheet, Text, View } from 'react-native';
import { EatingWindowRules } from '@/domain/window';
import { K, Type } from '@/theme';

const STEP_MIN = 15;

/** A time of day as hh:mm in 15-minute steps, in the number widget's shape (TimeStepper in WindowSheet.kt). */
export function TimeStepper({ minute, name, onChange }: { minute: number; name: string; onChange: (minute: number) => void }) {
  const text = EatingWindowRules.hhmm(minute);
  return (
    <View style={styles.box} accessible={false}>
      <Step glyph="−" label={`${name} ${STEP_MIN} minutes earlier`} onPress={() => onChange(minute - STEP_MIN)} />
      <View style={styles.divider} />
      <Text style={styles.value} accessibilityLabel={`${name} ${text}`}>
        {text}
      </Text>
      <View style={styles.divider} />
      <Step glyph="+" label={`${name} ${STEP_MIN} minutes later`} onPress={() => onChange(minute + STEP_MIN)} />
    </View>
  );
}

function Step({ glyph, label, onPress }: { glyph: string; label: string; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" accessibilityLabel={label} style={({ pressed }) => [styles.step, pressed && styles.pressed]}>
      <Text style={Type.stepGlyph}>{glyph}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  box: { width: 150, height: 38, flexDirection: 'row', alignItems: 'stretch', borderRadius: 10, backgroundColor: K.Surface2, overflow: 'hidden' },
  step: { width: 40, alignItems: 'center', justifyContent: 'center' },
  pressed: { backgroundColor: K.Surface3 },
  divider: { width: 1, backgroundColor: K.Divider },
  value: { ...Type.value, flex: 1, textAlign: 'center', alignSelf: 'center' },
});
