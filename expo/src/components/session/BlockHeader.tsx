import { LayoutChangeEvent, Pressable, StyleSheet, Text, View } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { Check } from '@/components/ui';
import { sets } from '@/domain/format';
import { Font, K, Space, tabular, Type } from '@/theme';

/**
 * The header of a session block (DESIGN.md, Set row, "Blocks"): a 56 pt row with the block number, the
 * exercise names, its sets done of total and its state. The top of the block's card; the whole card while
 * closed. The list keeps it stuck to the top while its sets scroll under it, until the next header pushes
 * it off, so it sits on a strip of the screen background that hides what scrolls behind.
 */
export function BlockHeader({
  index,
  title,
  doneSets,
  setCount,
  done,
  open,
  onToggle,
  onLayout,
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
}) {
  return (
    <View style={[styles.strip, index > 0 && styles.next]} onLayout={onLayout}>
      <Pressable
        onPress={onToggle}
        accessibilityRole="button"
        accessibilityState={{ expanded: open }}
        accessibilityLabel={`Block ${index + 1}, ${title}, ${doneSets} of ${sets(setCount)} done${done ? ', block done' : ''}`}
        style={({ pressed }) => [styles.header, open && styles.openHeader, pressed && styles.pressed]}>
        <View style={styles.number}>
          <Text style={styles.numberText}>{index + 1}</Text>
        </View>
        <Text style={styles.title}>{title}</Text>
        <Text style={styles.count}>{`${doneSets} / ${setCount}`}</Text>
        <View style={styles.state}>
          {done ? (
            <Check color={K.Green} />
          ) : (
            <SymbolView
              name={open ? { ios: 'chevron.up', android: 'expand_less', web: 'expand_less' } : { ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }}
              tintColor={K.Dim}
              size={14}
            />
          )}
        </View>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  strip: { backgroundColor: K.Bg, paddingTop: 6 },
  next: { paddingTop: 10 },
  header: { flexDirection: 'row', alignItems: 'center', minHeight: 56, paddingLeft: 16, paddingRight: 12, backgroundColor: K.Surface, borderRadius: Space.groupRadius },
  // Open, the sets continue the card below.
  openHeader: { borderBottomLeftRadius: 0, borderBottomRightRadius: 0 },
  pressed: { backgroundColor: K.Surface2 },
  number: { width: 24, height: 24, borderRadius: 12, backgroundColor: K.AccentSoft, alignItems: 'center', justifyContent: 'center' },
  numberText: { ...Type.footnote, fontFamily: Font.bold, color: K.Accent },
  title: { ...Type.body, fontFamily: Font.semibold, flex: 1, paddingHorizontal: 10, paddingVertical: 12 },
  count: { ...Type.subheadline, ...tabular, color: K.Muted },
  state: { marginLeft: 10, minWidth: 18, alignItems: 'center' },
});
