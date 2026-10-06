import { StyleSheet, Text, View } from 'react-native';
import Animated, { Extrapolation, interpolate, SharedValue, useAnimatedStyle } from 'react-native-reanimated';
import { NavBar } from '@/components/layout';
import { Capsule, ProgressBar } from '@/components/ui';
import { K, Space, tabular, Type } from '@/theme';

// The navigation bar (NavBar, 56 pt) and under it the progress row: the 18 pt count line and 10 below it.
const NAV = 56;
const ROW = 28;
const LINE = 2;
/** The unfolded header's height: where the list's content starts. */
export const SESSION_HEADER_HEIGHT = NAV + ROW;
/** What the folded header still covers: the bar and the progress line on its bottom edge. */
export const SESSION_HEADER_FOLDED = NAV + LINE;

/**
 * Top of a running session, over the top of its list: Back, the program name and Finish in the navigation
 * bar, under it the progress bar with sets done of total. As the sets scroll, the progress row slides up
 * under the bar with them and fades, and the progress becomes a 2 pt line on the bar's bottom edge, so
 * Finish and the progress stay in view while the sets get the room. Runs on the UI thread from `scrollY`.
 * (SessionScreen.kt has a large title and the date here.)
 */
export function SessionHeader({
  title,
  done,
  total,
  onFinish,
  scrollY,
}: {
  title: string;
  done: number;
  total: number;
  onFinish: () => void;
  scrollY: SharedValue<number>;
}) {
  const row = useAnimatedStyle(() => {
    const y = scrollY.get();
    return {
      transform: [{ translateY: -interpolate(y, [0, ROW], [0, ROW], Extrapolation.CLAMP) }],
      opacity: interpolate(y, [0, ROW * 0.8], [1, 0], Extrapolation.CLAMP),
    };
  });
  const line = useAnimatedStyle(() => ({ opacity: interpolate(scrollY.get(), [ROW - 8, ROW], [0, 1], Extrapolation.CLAMP) }));
  const label = `${done} of ${total} sets done`;

  return (
    <View style={styles.header} pointerEvents="box-none">
      <Animated.View style={[styles.row, row]}>
        <View style={styles.bar}>
          <ProgressBar done={done} total={total} label={label} />
        </View>
        <Text style={styles.count} importantForAccessibility="no" accessibilityElementsHidden>{`${done} / ${total}`}</Text>
      </Animated.View>
      {/* Drawn after the row, so the row slides under it. */}
      <View style={styles.nav}>
        <NavBar title={title} backLabel="Today" trailing={<Capsule text="Finish" onPress={onFinish} />} />
      </View>
      <Animated.View style={[styles.line, line]} importantForAccessibility="no-hide-descendants" accessibilityElementsHidden>
        <ProgressBar done={done} total={total} label={label} thin />
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: { position: 'absolute', top: 0, left: 0, right: 0 },
  nav: { position: 'absolute', top: 0, left: 0, right: 0, backgroundColor: K.Bg },
  row: {
    position: 'absolute',
    top: NAV,
    left: 0,
    right: 0,
    height: ROW,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingHorizontal: Space.margin,
    paddingBottom: ROW - 18,
    backgroundColor: K.Bg,
  },
  bar: { flex: 1 },
  count: { ...Type.footnote, ...tabular, color: K.Muted },
  line: { position: 'absolute', top: NAV, left: 0, right: 0 },
});
