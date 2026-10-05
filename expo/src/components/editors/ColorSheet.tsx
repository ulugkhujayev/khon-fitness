import { Pressable, StyleSheet, View } from 'react-native';
import { Sheet } from '@/components/ui';
import { PRESET_COLORS } from '@/domain/editors';
import { argbToHex } from '@/domain/types';
import { K, Space } from '@/theme';

const ROWS = [PRESET_COLORS.slice(0, 6), PRESET_COLORS.slice(6)];

/** Twelve preset colors in two rows (DESIGN.md, Color): tap one. No native color input. */
export function ColorSheet({ visible, selected, onPick, onDismiss }: { visible: boolean; selected: number; onPick: (argb: number) => void; onDismiss: () => void }) {
  return (
    <Sheet visible={visible} title="Color" onDismiss={onDismiss}>
      {ROWS.map((row, r) => (
        <View key={r} style={styles.row}>
          {row.map((argb, i) => (
            <Pressable
              key={argb}
              onPress={() => onPick(argb)}
              accessibilityRole="radio"
              accessibilityState={{ selected: argb === selected }}
              accessibilityLabel={`Color ${r * row.length + i + 1}`}
              style={[styles.swatch, argb === selected && styles.chosen]}>
              <View style={[styles.fill, { backgroundColor: argbToHex(argb) }]} />
            </Pressable>
          ))}
        </View>
      ))}
    </Sheet>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 8 },
  swatch: { width: Space.tapMin, height: Space.tapMin, borderRadius: Space.tapMin / 2, padding: 3 },
  chosen: { backgroundColor: K.White },
  fill: { flex: 1, borderRadius: Space.tapMin / 2 },
});
