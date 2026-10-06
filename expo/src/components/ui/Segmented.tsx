import { Pressable, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { Font, K, Type } from '@/theme';

/** Fixed-width segmented control (Segmented in ProgramsScreen.kt): every option gets an equal share, nothing scrolls. */
export function Segmented({
  options,
  selected,
  onSelect,
  elevated = false,
  style,
}: {
  options: readonly string[];
  selected: number;
  onSelect: (i: number) => void;
  /** On a card the track takes the elevated fill, else it would vanish into the card. */
  elevated?: boolean;
  style?: ViewStyle;
}) {
  return (
    <View style={[styles.bar, elevated && styles.elevated, style]} accessibilityRole="tablist">
      {options.map((label, i) => {
        const active = i === selected;
        return (
          <Pressable
            key={label}
            onPress={() => onSelect(i)}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            style={[styles.segment, active && styles.active]}>
            <Text style={[styles.text, active && styles.activeText]}>{label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { flexDirection: 'row', height: 36, borderRadius: 10, backgroundColor: K.Surface, padding: 3 },
  segment: { flex: 1, borderRadius: 8, alignItems: 'center', justifyContent: 'center' },
  elevated: { backgroundColor: K.Elevated },
  active: { backgroundColor: K.Surface3 },
  text: { ...Type.subheadline, fontFamily: Font.medium, color: K.Muted },
  activeText: { color: K.Text },
});
