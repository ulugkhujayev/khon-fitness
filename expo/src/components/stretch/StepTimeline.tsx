import { useState } from 'react';
import { Pressable, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { DemoStep } from '@/domain/stretch';
import { Font, K, Space, tabular, Type } from '@/theme';

// Not in DESIGN.md before the Expo app: 26 pt nodes, a 34 pt halo around the shown one, a 3 pt line.
const NODE = 26;
const HALO = 34;
const LINE = 3;

/**
 * A guide's steps as a timeline: numbered nodes on a line, each title under its node. The steps up to the
 * shown one are accent, the shown one in a soft halo, the rest gray; the line fills in accent up to `fill`
 * (0..1 from the first node to the last, timelineFill), so while the demo plays it runs on as the pose moves
 * into each step. A tap holds that step's pose.
 */
export function StepTimeline({
  steps,
  selected,
  fill,
  onSelect,
  style,
}: {
  steps: DemoStep[];
  selected: number;
  fill: number;
  onSelect: (index: number) => void;
  style?: ViewStyle;
}) {
  const [width, setWidth] = useState(0);
  // The line runs between the first and the last node's centers.
  const column = steps.length > 0 ? width / steps.length : 0;
  const track = Math.max(0, width - column);

  return (
    <View style={[styles.row, style]} onLayout={(e) => setWidth(e.nativeEvent.layout.width)} accessibilityRole="tablist">
      {width > 0 && steps.length > 1 ? (
        <>
          <View style={[styles.line, styles.track, { left: column / 2, width: track }]} />
          <View style={[styles.line, styles.fill, { left: column / 2, width: track * Math.min(1, Math.max(0, fill)) }]} />
        </>
      ) : null}
      {steps.map((step, i) => {
        const active = i === selected;
        const reached = i <= selected;
        return (
          <Pressable
            key={step.title}
            onPress={() => onSelect(i)}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            accessibilityLabel={`Step ${i + 1}: ${step.title}`}
            style={({ pressed }) => [styles.step, pressed && styles.pressed]}>
            <View style={[styles.halo, active && styles.activeHalo]}>
              <View style={[styles.node, reached && styles.reachedNode]}>
                <Text style={[styles.number, reached && styles.reachedNumber]}>{i + 1}</Text>
              </View>
            </View>
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
  row: { flexDirection: 'row' },
  line: { position: 'absolute', top: HALO / 2 - LINE / 2, height: LINE, borderRadius: LINE / 2 },
  track: { backgroundColor: K.Surface2 },
  fill: { backgroundColor: K.Accent },
  step: { flex: 1, alignItems: 'center', minHeight: Space.tapMin },
  pressed: { opacity: 0.7 },
  halo: { width: HALO, height: HALO, borderRadius: HALO / 2, alignItems: 'center', justifyContent: 'center' },
  activeHalo: { backgroundColor: K.AccentSoft },
  node: { width: NODE, height: NODE, borderRadius: NODE / 2, alignItems: 'center', justifyContent: 'center', backgroundColor: K.Surface3 },
  reachedNode: { backgroundColor: K.Accent },
  number: { ...Type.footnote, ...tabular, fontFamily: Font.bold, color: K.Muted },
  reachedNumber: { color: K.AccentInk },
  title: { ...Type.footnote, color: K.Muted, textAlign: 'center', paddingHorizontal: 2, marginTop: 4 },
  activeTitle: { fontFamily: Font.semibold, color: K.Text },
});
