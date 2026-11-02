import { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SymbolViewProps } from 'expo-symbols';
import { Chevron, SymbolCircle } from '@/components/ui';
import { K, Space, Type } from '@/theme';

/**
 * A row of Today's group, sized for the home screen and matching the workout card above it: the symbol in a
 * 40 pt circle of its color, the name in Headline over a line of state (in `lineColor` when it is good
 * news), a mark and the chevron.
 */
export function HomeRow({
  icon,
  color,
  title,
  line,
  lineColor = K.Muted,
  trailing,
  last,
  onPress,
}: {
  icon: SymbolViewProps['name'];
  color: string;
  title: string;
  line: string;
  lineColor?: string;
  trailing?: ReactNode;
  last: boolean;
  onPress: () => void;
}) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" accessibilityLabel={`${title}, ${line}`} style={({ pressed }) => [styles.row, pressed && styles.pressed]}>
      <SymbolCircle icon={icon} color={color} />
      <View style={[styles.inner, !last && styles.separator]}>
        <View style={styles.words}>
          <Text style={styles.title} numberOfLines={1}>
            {title}
          </Text>
          <Text style={[styles.line, { color: lineColor }]} numberOfLines={1}>
            {line}
          </Text>
        </View>
        {trailing}
        <Chevron />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', paddingLeft: Space.margin, gap: 12 },
  pressed: { backgroundColor: K.Surface2 },
  // 64 pt rows: room to tap on a screen people open many times a day.
  inner: { flex: 1, flexDirection: 'row', alignItems: 'center', minHeight: 64, paddingVertical: 10, paddingRight: Space.margin },
  separator: { borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: K.Divider },
  words: { flex: 1 },
  title: { ...Type.headline },
  line: { ...Type.subheadline, marginTop: 2 },
});
