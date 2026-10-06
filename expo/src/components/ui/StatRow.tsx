import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { K, tabular, Type } from '@/theme';

/** A few figures side by side, each over its name: "−1.2 kg / Change". Every column gets an equal share. */
export function StatRow({ items, style }: { items: readonly { value: string; label: string }[]; style?: ViewStyle }) {
  return (
    <View style={[styles.row, style]}>
      {items.map((item) => (
        <View key={item.label} style={styles.item} accessible accessibilityLabel={`${item.label}: ${item.value}`}>
          <Text style={styles.value} numberOfLines={1} adjustsFontSizeToFit>
            {item.value}
          </Text>
          <Text style={styles.label}>{item.label}</Text>
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row' },
  item: { flex: 1 },
  value: { ...Type.headline, ...tabular },
  label: { ...Type.footnote, color: K.Muted, marginTop: 2 },
});
