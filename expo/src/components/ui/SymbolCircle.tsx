import { StyleSheet, View } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { hexAlpha } from '@/domain/types';

/** Size of the circle and its symbol (DESIGN.md, Today: the card's leading icon). */
const SIZE = 40;
const SYMBOL = 20;

/** A symbol in its color on a soft circle of the same color (20 percent), e.g. a workout's kind. Decorative: the text next to it names the thing. */
export function SymbolCircle({ icon, color }: { icon: SymbolViewProps['name']; color: string }) {
  return (
    <View style={[styles.circle, { backgroundColor: hexAlpha(color, 0.2) }]} importantForAccessibility="no-hide-descendants" accessibilityElementsHidden>
      <SymbolView name={icon} tintColor={color} size={SYMBOL} />
    </View>
  );
}

const styles = StyleSheet.create({
  circle: { width: SIZE, height: SIZE, borderRadius: SIZE / 2, alignItems: 'center', justifyContent: 'center' },
});
