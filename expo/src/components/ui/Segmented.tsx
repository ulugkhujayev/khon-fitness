import { Pressable, StyleSheet, Text, View } from 'react-native';
import { Font, K, Type } from '@/theme';

/** Fixed-width segmented control (Segmented in ProgramsScreen.kt): every option gets an equal share, nothing scrolls. */
export function Segmented({ options, selected, onSelect }: { options: string[]; selected: number; onSelect: (i: number) => void }) {
  return (
    <View style={styles.bar} accessibilityRole="tablist">
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
  active: { backgroundColor: K.Surface3 },
  text: { ...Type.subheadline, fontFamily: Font.medium, color: K.Muted },
  activeText: { color: K.Text },
});
