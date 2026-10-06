import { StyleSheet, View } from 'react-native';
import Svg, { Defs, LinearGradient, Rect, Stop } from 'react-native-svg';

/** Width of each fade (not in DESIGN.md; enough for two ruler ticks to thin out). */
const FADE = 40;

/** Fades the left and right edges of a strip into the surface under it, so its content runs out instead of being cut. */
export function EdgeFade({ width, height, color }: { width: number; height: number; color: string }) {
  if (width === 0) return null;
  return (
    <View style={[StyleSheet.absoluteFill, styles.passThrough]}>
      <Svg width={width} height={height}>
        <Defs>
          <LinearGradient id="fadeLeft" x1="0" y1="0" x2="1" y2="0">
            <Stop offset="0" stopColor={color} stopOpacity={1} />
            <Stop offset="1" stopColor={color} stopOpacity={0} />
          </LinearGradient>
          <LinearGradient id="fadeRight" x1="0" y1="0" x2="1" y2="0">
            <Stop offset="0" stopColor={color} stopOpacity={0} />
            <Stop offset="1" stopColor={color} stopOpacity={1} />
          </LinearGradient>
        </Defs>
        <Rect x={0} y={0} width={FADE} height={height} fill="url(#fadeLeft)" />
        <Rect x={width - FADE} y={0} width={FADE} height={height} fill="url(#fadeRight)" />
      </Svg>
    </View>
  );
}

const styles = StyleSheet.create({
  passThrough: { pointerEvents: 'none' },
});
