import { Pressable, StyleSheet, Text, View } from 'react-native';
import { DemoStep } from '@/domain/stretch';
import { K, Space, Type } from '@/theme';

/**
 * A guide's numbered steps as tabs with an underline (StretchDemonstration.kt): the shown step in accent.
 * Compact: the line sits right under the title, and every tab keeps its title on the line's edge, so a
 * title that wraps only makes its row taller, never leaves a gap under a short one. A tap holds that step.
 */
export function StepTabs({ steps, selected, onSelect }: { steps: DemoStep[]; selected: number; onSelect: (index: number) => void }) {
  return (
    <View style={styles.row} accessibilityRole="tablist">
      {steps.map((step, i) => {
        const active = i === selected;
        return (
          <Pressable
            key={step.title}
            onPress={() => onSelect(i)}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            accessibilityLabel={`Step ${i + 1}: ${step.title}`}
            style={({ pressed }) => [styles.tab, pressed && styles.pressed]}>
            <Text style={[styles.title, active && styles.activeTitle]} numberOfLines={2}>
              {`${i + 1}. ${step.title}`}
            </Text>
            <View style={[styles.line, active && styles.activeLine]} />
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  tab: { flex: 1, minHeight: Space.tapMin, justifyContent: 'flex-end' },
  pressed: { opacity: 0.6 },
  title: { ...Type.footnote, color: K.White, textAlign: 'center', paddingHorizontal: 4, marginBottom: 6 },
  activeTitle: { color: K.Accent },
  line: { height: 2, backgroundColor: K.Surface3 },
  activeLine: { backgroundColor: K.Accent },
});
