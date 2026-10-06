import { ReactNode } from 'react';
import { LayoutChangeEvent, StyleSheet, View } from 'react-native';
import { K, Space } from '@/theme';

/** The sets of an open session block: the rest of its card under BlockHeader, rounded at the bottom. */
export function BlockBody({ onLayout, children }: { onLayout?: (e: LayoutChangeEvent) => void; children: ReactNode }) {
  return (
    <View style={styles.body} onLayout={onLayout}>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  body: { backgroundColor: K.Surface, borderBottomLeftRadius: Space.groupRadius, borderBottomRightRadius: Space.groupRadius, overflow: 'hidden' },
});
