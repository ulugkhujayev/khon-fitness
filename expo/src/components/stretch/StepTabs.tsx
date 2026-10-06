import { Pressable, StyleSheet, Text, View } from 'react-native';
import { DemoStep } from '@/domain/stretch';
import { Font, K, tabular, Type } from '@/theme';

/**
 * A guide's steps as a segmented control (the look of Segmented, two lines tall so four steps and long titles
 * fit): the step number over its title, the shown step on a lighter segment with its number in accent.
 * A tap holds that step's pose.
 */
export function StepTabs({ steps, selected, onSelect }: { steps: DemoStep[]; selected: number; onSelect: (index: number) => void }) {
  return (
    <View style={styles.track} accessibilityRole="tablist">
      {steps.map((step, i) => {
        const active = i === selected;
        return (
          <Pressable
            key={step.title}
            onPress={() => onSelect(i)}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            accessibilityLabel={`Step ${i + 1}: ${step.title}`}
            style={({ pressed }) => [styles.segment, active && styles.active, pressed && !active && styles.pressed]}>
            <Text style={[styles.number, active && styles.activeNumber]}>{i + 1}</Text>
            <Text style={[styles.title, active && styles.activeTitle]} numberOfLines={2}>
              {step.title}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  track: { flexDirection: 'row', borderRadius: 12, backgroundColor: K.Surface, padding: 3, gap: 3 },
  segment: { flex: 1, alignItems: 'center', justifyContent: 'center', borderRadius: 9, paddingVertical: 6, paddingHorizontal: 4, minHeight: 52 },
  active: { backgroundColor: K.Surface3 },
  pressed: { backgroundColor: K.Surface2 },
  number: { ...Type.caption1, ...tabular, fontFamily: Font.bold, color: K.Dim },
  activeNumber: { color: K.Accent },
  title: { ...Type.footnote, fontFamily: Font.medium, color: K.Muted, textAlign: 'center', marginTop: 1 },
  activeTitle: { color: K.Text },
});
