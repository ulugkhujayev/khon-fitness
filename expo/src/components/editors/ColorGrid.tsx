import { Pressable, StyleSheet, View } from 'react-native';
import { GroupedList } from '@/components/ui';
import { PRESET_COLORS } from '@/domain/editors';
import { argbToHex } from '@/domain/types';
import { K, Space } from '@/theme';

// Not in DESIGN.md: 34 pt swatches; the chosen one in a white ring 2.5 pt wide with a 2 pt gap, on a 44 pt target.
const FILL = 34;
const RING = FILL + 2 * (2 + 2.5);
const ROWS = [PRESET_COLORS.slice(0, 6), PRESET_COLORS.slice(6)];

/**
 * The twelve preset colors in two rows on a card, right in the editor (DESIGN.md, Color): one tap picks,
 * the chosen one ringed in white. No native color input. The ring is a border that is always there (clear
 * when not chosen), so the swatch is never a bare view that loses its round corners on Android.
 */
export function ColorGrid({ selected, onPick }: { selected: number; onPick: (argb: number) => void }) {
  return (
    <GroupedList style={styles.card}>
      {ROWS.map((row, r) => (
        <View key={r} style={styles.row}>
          {row.map((argb, i) => {
            const chosen = argb === selected;
            return (
              <Pressable
                key={argb}
                onPress={() => onPick(argb)}
                accessibilityRole="radio"
                accessibilityState={{ selected: chosen }}
                accessibilityLabel={`Color ${r * row.length + i + 1}`}
                style={styles.target}>
                <View style={[styles.ring, chosen && styles.chosen]}>
                  <View style={[styles.fill, { backgroundColor: argbToHex(argb) }]} />
                </View>
              </Pressable>
            );
          })}
        </View>
      ))}
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  card: { paddingVertical: 10, paddingHorizontal: 12 },
  row: { flexDirection: 'row', justifyContent: 'space-between' },
  target: { width: Space.tapMin + 4, height: Space.tapMin + 4, alignItems: 'center', justifyContent: 'center' },
  ring: { width: RING, height: RING, borderRadius: RING / 2, borderWidth: 2.5, borderColor: 'transparent', alignItems: 'center', justifyContent: 'center' },
  chosen: { borderColor: K.White },
  fill: { width: FILL, height: FILL, borderRadius: FILL / 2 },
});
