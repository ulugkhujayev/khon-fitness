import { ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import Animated, { Extrapolation, interpolate, SharedValue, useAnimatedStyle } from 'react-native-reanimated';
import { K, Space, Type } from '@/theme';

// Not in Theme.kt (the Kotlin title scrolls away): the title row is a tap target tall with 4 pt above and
// 12 above the content; folded, the bar keeps the row and 2 pt around it.
const ROW = Space.tapMin;
const ROW_TOP = 4;
/** Room the content starts below, so it begins under the unfolded bar. */
export const LARGE_TITLE_HEIGHT = ROW_TOP + ROW + 12;
const FOLDED = ROW + 4;
/** Scroll distance over which the bar's bottom edge rises with the content. */
const FOLD = LARGE_TITLE_HEIGHT - FOLDED;
/** Scroll distance over which the title shrinks, and how far: 34 pt to about 20. */
const TITLE_FOLD = 40;
const TITLE_SCALE = 0.6;

const fold = (y: number, distance: number, from: number, to: number) => {
  'worklet';
  return interpolate(y, [0, distance], [from, to], Extrapolation.CLAMP);
};

/**
 * The large title of a tab screen with its bar items, over the top of the content. As the content scrolls
 * the bar's bottom edge rises with it, the title shrinks toward its leading edge, and a hairline shows once
 * content passes under the bar (iOS large title). Everything runs on the UI thread from `scrollY`.
 */
export function LargeTitleHeader({ title, trailing, scrollY }: { title: string; trailing?: ReactNode; scrollY: SharedValue<number> }) {
  const bar = useAnimatedStyle(() => ({ transform: [{ translateY: -fold(scrollY.get(), FOLD, 0, FOLD) }] }));
  // The row moves down inside the rising bar, so it ends centered in the folded bar.
  const row = useAnimatedStyle(() => ({ transform: [{ translateY: fold(scrollY.get(), FOLD, 0, FOLD + (FOLDED - ROW) / 2 - ROW_TOP) }] }));
  const text = useAnimatedStyle(() => ({ transform: [{ scale: fold(scrollY.get(), TITLE_FOLD, 1, TITLE_SCALE) }] }));
  const hairline = useAnimatedStyle(() => ({ opacity: interpolate(scrollY.get(), [FOLD + 4, FOLD + 12], [0, 1], Extrapolation.CLAMP) }));

  return (
    <Animated.View style={[styles.bar, bar]}>
      <Animated.View style={[styles.row, row]}>
        <Animated.Text style={[Type.largeTitle, styles.title, text]} numberOfLines={1} accessibilityRole="header">
          {title}
        </Animated.Text>
        {trailing}
      </Animated.View>
      <Animated.View style={[styles.hairline, hairline]} />
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  bar: { position: 'absolute', top: 0, left: 0, right: 0, height: LARGE_TITLE_HEIGHT, paddingHorizontal: Space.margin, backgroundColor: K.Bg },
  row: { flexDirection: 'row', alignItems: 'center', height: ROW, marginTop: ROW_TOP },
  title: { flex: 1, transformOrigin: 'left center' },
  hairline: { position: 'absolute', left: 0, right: 0, bottom: 0, height: StyleSheet.hairlineWidth, backgroundColor: K.Divider },
});
