import { StyleSheet, View } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { K } from '@/theme';

/** Size of the tile and its symbol (not in DESIGN.md; the icon tiles of iOS Settings). */
const SIZE = 30;
const SYMBOL = 17;

/** A white symbol on a rounded square of its color, leading a list row like iOS Settings. Decorative: the row names it. */
export function IconTile({ icon, color }: { icon: SymbolViewProps['name']; color: string }) {
  return (
    <View style={[styles.tile, { backgroundColor: color }]} importantForAccessibility="no-hide-descendants" accessibilityElementsHidden>
      <SymbolView name={icon} tintColor={K.White} size={SYMBOL} />
    </View>
  );
}

const styles = StyleSheet.create({
  tile: { width: SIZE, height: SIZE, borderRadius: 7, borderCurve: 'continuous', alignItems: 'center', justifyContent: 'center' },
});
