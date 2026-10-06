import { Pressable, StyleSheet, Text, View } from 'react-native';
import Animated from 'react-native-reanimated';
import { Font, K, Type } from '@/theme';
import { useSlidingCapsule } from './useSlidingCapsule';

/**
 * Fixed-width segmented control (Segmented in ProgramsScreen.kt): every option gets an equal share, nothing
 * scrolls. The selected segment's capsule slides to the option tapped, like the tab bar's.
 */
export function Segmented({ options, selected, onSelect }: { options: string[]; selected: number; onSelect: (i: number) => void }) {
  const capsule = useSlidingCapsule(selected, options.length);
  return (
    <View style={styles.bar} accessibilityRole="tablist">
      <View style={styles.row} onLayout={capsule.onLayout}>
        {capsule.slot > 0 ? <Animated.View style={[styles.capsule, { width: capsule.slot }, capsule.style]} /> : null}
        {options.map((label, i) => {
          const active = i === selected;
          return (
            <Pressable key={label} onPress={() => onSelect(i)} accessibilityRole="tab" accessibilityState={{ selected: active }} style={styles.segment}>
              <Text style={[styles.text, active && styles.activeText]}>{label}</Text>
            </Pressable>
          );
        })}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { height: 36, borderRadius: 10, backgroundColor: K.Surface, padding: 3 },
  // The options' own row, with no padding, so a slot is exactly a share of its width.
  row: { flex: 1, flexDirection: 'row' },
  capsule: { position: 'absolute', top: 0, bottom: 0, left: 0, borderRadius: 8, backgroundColor: K.Surface3 },
  segment: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  text: { ...Type.subheadline, fontFamily: Font.medium, color: K.Muted },
  activeText: { color: K.Text },
});
