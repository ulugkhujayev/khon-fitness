import { useEffect, useRef, useState } from 'react';
import { FlatList, Keyboard, StyleSheet, Text, View, ViewStyle } from 'react-native';
import { fmt } from '@/domain/format';
import { offsetOf, Ruler, rulerInset, RulerSegment, rulerSegments, tickIndex, tickKind, tickValue, valueAt } from '@/domain/ruler';
import { tick } from '@/haptics';
import { Font, K, tabular, Type } from '@/theme';
import { EdgeFade } from './EdgeFade';

// Not in DESIGN.md (the ruler is new to the Expo app): tick heights hang from the top, labels sit under them.
const TICK = { major: 22, half: 15, minor: 9, width: 2 } as const;
const LINE = { width: 3, height: TICK.major + 4 } as const;
const LABEL = { top: TICK.major + 4, width: 48 } as const;
const HEIGHT = LABEL.top + 16;

/**
 * Horizontal ruler picker: the ticks scroll under a fixed accent line, stop on a tick, click on each one
 * they pass (firmer on a labelled one) and report the value. A value set elsewhere scrolls the ruler to
 * it. Screen readers get one adjustable element that steps a tick at a time.
 */
export function RulerPicker({
  ruler,
  value,
  onChange,
  label,
  unit,
  surface = K.Bg,
  style,
}: {
  ruler: Ruler;
  value: number;
  onChange: (v: number) => void;
  /** Accessible name, e.g. "Bodyweight ruler". */
  label: string;
  unit: string;
  /** The fill under the ruler, which its edges fade into. */
  surface?: string;
  style?: ViewStyle;
}) {
  const list = useRef<FlatList<RulerSegment>>(null);
  const [width, setWidth] = useState(0);
  // Scroll events pick a value only while the user drags or the fling after it runs, never on a scroll
  // made here; `shown` is the value under the line, so a value that came from the ruler is not scrolled to again.
  const moving = useRef(false);
  const shown = useRef<number | null>(null);
  const segments = rulerSegments(ruler);
  const segmentLength = ruler.majorEvery * ruler.gap;
  const inset = rulerInset(ruler, width);

  useEffect(() => {
    if (width === 0 || value === shown.current) return;
    shown.current = value;
    moving.current = false;
    list.current?.scrollToOffset({ offset: offsetOf(ruler, value), animated: false });
  }, [ruler, value, width]);

  const pick = (offset: number) => {
    const v = valueAt(ruler, offset);
    if (v === shown.current) return;
    shown.current = v;
    tick(tickKind(ruler, tickIndex(ruler, v)) === 'major');
    onChange(v);
  };

  return (
    <View
      style={[styles.ruler, style]}
      onLayout={(e) => setWidth(e.nativeEvent.layout.width)}
      accessible
      accessibilityRole="adjustable"
      accessibilityLabel={label}
      accessibilityValue={{ text: `${fmt(value)} ${unit}` }}
      accessibilityActions={[{ name: 'increment' }, { name: 'decrement' }]}
      onAccessibilityAction={(e) => onChange(tickValue(ruler, tickIndex(ruler, value) + (e.nativeEvent.actionName === 'increment' ? 1 : -1)))}>
      <FlatList
        ref={list}
        horizontal
        data={segments}
        keyExtractor={(s) => String(s.first)}
        renderItem={({ item }) => <Segment segment={item} ruler={ruler} />}
        // The inset counts: the list compares these offsets with the scroll position.
        getItemLayout={(_, i) => ({ length: (segments[i]?.count ?? 0) * ruler.gap, offset: inset + i * segmentLength, index: i })}
        initialScrollIndex={Math.floor(tickIndex(ruler, value) / ruler.majorEvery)}
        windowSize={5}
        contentContainerStyle={{ paddingHorizontal: inset }}
        showsHorizontalScrollIndicator={false}
        snapToInterval={ruler.gap}
        decelerationRate="fast"
        scrollEventThrottle={16}
        onScrollBeginDrag={() => {
          moving.current = true;
          // Picking with the ruler ends typing in the figure above it.
          Keyboard.dismiss();
        }}
        onMomentumScrollBegin={() => {
          moving.current = true;
        }}
        onScroll={(e) => {
          if (moving.current) pick(e.nativeEvent.contentOffset.x);
        }}
        onMomentumScrollEnd={(e) => {
          pick(e.nativeEvent.contentOffset.x);
          moving.current = false;
        }}
      />
      <View style={[styles.line, { left: width / 2 - LINE.width / 2 }]} />
      <EdgeFade width={width} height={HEIGHT} color={surface} />
    </View>
  );
}

/** One labelled tick and the ticks after it, each centered in its own gap. */
function Segment({ segment, ruler }: { segment: RulerSegment; ruler: Ruler }) {
  const gap = ruler.gap;
  return (
    <View style={{ width: segment.count * gap, height: HEIGHT }}>
      {Array.from({ length: segment.count }, (_, i) => {
        const kind = tickKind(ruler, segment.first + i);
        return <View key={i} style={[styles.tick, kind === 'minor' && styles.minor, { left: i * gap + (gap - TICK.width) / 2, height: TICK[kind] }]} />;
      })}
      <Text style={[styles.label, { left: (gap - LABEL.width) / 2 }]}>{segment.label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  ruler: { height: HEIGHT },
  tick: { position: 'absolute', top: 0, width: TICK.width, borderRadius: TICK.width / 2, backgroundColor: K.Muted },
  minor: { backgroundColor: K.Dim },
  label: { ...Type.caption1, ...tabular, fontFamily: Font.medium, color: K.Muted, position: 'absolute', top: LABEL.top, width: LABEL.width, textAlign: 'center' },
  line: { position: 'absolute', top: 0, width: LINE.width, height: LINE.height, borderRadius: LINE.width / 2, backgroundColor: K.Accent, pointerEvents: 'none' },
});
