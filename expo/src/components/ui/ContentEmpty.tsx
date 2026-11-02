import { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { SymbolView, SymbolViewProps } from 'expo-symbols';
import { K, Type } from '@/theme';

/** Size of the symbol (not in DESIGN.md; the system's content-unavailable view). */
const SYMBOL = 32;

/**
 * A screen or list with nothing to show yet, after iOS ContentUnavailableView: a symbol in its color, a title,
 * a muted line saying why or what to do, and any actions under them.
 */
export function ContentEmpty({
  icon,
  color = K.Muted,
  title,
  text,
  children,
}: {
  icon: SymbolViewProps['name'];
  color?: string;
  title: string;
  text: string;
  children?: ReactNode;
}) {
  return (
    <View style={styles.box}>
      <View accessible accessibilityLabel={`${title}. ${text}`} style={styles.words}>
        <SymbolView name={icon} tintColor={color} size={SYMBOL} />
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.text}>{text}</Text>
      </View>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  box: { paddingVertical: 28, paddingHorizontal: 24, gap: 16 },
  words: { alignItems: 'center' },
  title: { ...Type.headline, textAlign: 'center', marginTop: 10 },
  text: { ...Type.subheadline, color: K.Muted, textAlign: 'center', marginTop: 4 },
});
