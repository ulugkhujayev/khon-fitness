import { ReactNode } from 'react';
import { LayoutChangeEvent, Pressable, StyleSheet, Text, View } from 'react-native';
import { Check, Disclosure } from '@/components/ui';
import { sets } from '@/domain/format';
import { Font, K, Space, tabular, Type } from '@/theme';

/**
 * One block of a session (DESIGN.md, Set row, "Blocks"): a 56 pt header with the block number, the
 * exercise names, its sets done of total and its state; the set rows under it while open.
 */
export function BlockCard({
  index,
  title,
  doneSets,
  setCount,
  done,
  open,
  onToggle,
  onLayout,
  children,
}: {
  /** Zero-based; shown as 1, 2, 3. */
  index: number;
  title: string;
  doneSets: number;
  setCount: number;
  /** Every set logged or skipped. */
  done: boolean;
  open: boolean;
  onToggle: () => void;
  onLayout?: (e: LayoutChangeEvent) => void;
  /** The set rows, rendered only while open. */
  children: ReactNode;
}) {
  return (
    <View style={[styles.card, index > 0 && styles.next]} onLayout={onLayout}>
      <Pressable
        onPress={onToggle}
        accessibilityRole="button"
        accessibilityState={{ expanded: open }}
        accessibilityLabel={`Block ${index + 1}, ${title}, ${doneSets} of ${sets(setCount)} done${done ? ', block done' : ''}`}
        style={({ pressed }) => [styles.header, pressed && styles.pressed]}>
        <View style={styles.number}>
          <Text style={styles.numberText}>{index + 1}</Text>
        </View>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.count}>{`${doneSets} / ${setCount}`}</Text>
        <View style={styles.state}>
          {done ? <Check color={K.Green} /> : <Disclosure open={open} />}
        </View>
      </Pressable>
      {open ? children : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: { backgroundColor: K.Surface, borderRadius: Space.groupRadius, overflow: 'hidden', marginTop: 6 },
  next: { marginTop: 10 },
  header: { flexDirection: 'row', alignItems: 'center', minHeight: 56, paddingLeft: 16, paddingRight: 12 },
  pressed: { backgroundColor: K.Surface2 },
  number: { width: 24, height: 24, borderRadius: 12, backgroundColor: K.AccentSoft, alignItems: 'center', justifyContent: 'center' },
  numberText: { ...Type.footnote, fontFamily: Font.bold, color: K.Accent },
  title: { ...Type.body, fontFamily: Font.semibold, flex: 1, paddingHorizontal: 10, paddingVertical: 12 },
  count: { ...Type.subheadline, ...tabular, color: K.Muted },
  state: { marginLeft: 10, minWidth: 18, alignItems: 'center' },
});
