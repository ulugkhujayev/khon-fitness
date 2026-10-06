import { useEffect, useRef } from 'react';
import { Pressable, ScrollView, StyleSheet, Text } from 'react-native';
import { DemoStep } from '@/domain/stretch';
import { Font, K, tabular, Type } from '@/theme';

// Not in DESIGN.md before the Expo app: 32 pt capsules, 8 pt apart, like iOS filter chips.
const CHIP = 32;

/**
 * A guide's numbered steps as capsules in one row: the shown step filled in accent, the others on Surface2.
 * The row scrolls sideways when the steps do not fit, and keeps the shown step in view as the demo plays.
 * A tap holds that step's pose.
 */
export function StepTabs({ steps, selected, onSelect }: { steps: DemoStep[]; selected: number; onSelect: (index: number) => void }) {
  const scroll = useRef<ScrollView>(null);
  const lefts = useRef<number[]>([]);

  useEffect(() => {
    const x = lefts.current[selected];
    if (x != null) scroll.current?.scrollTo({ x: Math.max(0, x - 16), animated: true });
  }, [selected]);

  return (
    <ScrollView ref={scroll} horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.row} accessibilityRole="tablist">
      {steps.map((step, i) => {
        const active = i === selected;
        return (
          <Pressable
            key={step.title}
            onPress={() => onSelect(i)}
            onLayout={(e) => {
              lefts.current[i] = e.nativeEvent.layout.x;
            }}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            accessibilityLabel={`Step ${i + 1}: ${step.title}`}
            hitSlop={6}
            style={({ pressed }) => [styles.chip, active && styles.active, pressed && styles.pressed]}>
            <Text style={[styles.text, active && styles.activeText]} numberOfLines={1}>
              <Text style={styles.number}>{`${i + 1}  `}</Text>
              {step.title}
            </Text>
          </Pressable>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  row: { gap: 8, paddingVertical: 4 },
  chip: { height: CHIP, borderRadius: CHIP / 2, paddingHorizontal: 14, justifyContent: 'center', backgroundColor: K.Surface2 },
  active: { backgroundColor: K.Accent },
  pressed: { opacity: 0.7 },
  text: { ...Type.footnote, fontFamily: Font.medium, color: K.White },
  activeText: { color: K.AccentInk, fontFamily: Font.semibold },
  number: { ...tabular, fontFamily: Font.bold },
});
