import { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import Svg, { Circle, Line, Path, Text as SvgText } from 'react-native-svg';
import { ChartPoint, dateLabelIndices, scaleFor, valueLabelIndices } from '@/domain/chart';
import { monthDay } from '@/domain/dates';
import { fmt } from '@/domain/format';
import { Font, K, Type } from '@/theme';

/** Plot insets: room for the value axis on the left, labels above points and dates below (LineChart.kt). */
const PAD = { l: 40, r: 14, t: 22, b: 24 };
// Axis and date labels in secondary label at 12: tertiary at 11 was too faint to read on black.
const TICK = { fill: K.Muted, fontSize: Type.caption1.fontSize, fontFamily: Font.medium };

type Anchor = 'start' | 'middle' | 'end';

/**
 * Line chart (DESIGN.md, Charts): dashed grid at round values labelled on the left, a marker on every
 * point, value labels (all up to seven points, else the highest and the last), the last point filled in
 * accent with its unit, dates along the bottom. Port of LineChart.kt.
 */
export function LineChart({
  points,
  unit,
  height = 180,
  decimals = 1,
  format,
  style,
}: {
  points: ChartPoint[];
  unit: string;
  height?: number;
  decimals?: number;
  /** Text for a value on the axis and the labels, e.g. a pace as m:ss. Defaults to the number with `decimals`. */
  format?: (v: number) => string;
  style?: ViewStyle;
}) {
  const [width, setWidth] = useState(0);
  return (
    <View style={[{ height }, style]} onLayout={(e) => setWidth(e.nativeEvent.layout.width)}>
      {points.length === 0 ? (
        <View style={styles.empty}>
          <Text style={styles.emptyText}>No data yet</Text>
        </View>
      ) : width > 0 ? (
        <Plot points={points} unit={unit} width={width} height={height} label={(v, d) => format?.(v) ?? fmt(v, d ?? decimals)} />
      ) : null}
    </View>
  );
}

function Plot({
  points,
  unit,
  width,
  height,
  label,
}: {
  points: ChartPoint[];
  unit: string;
  width: number;
  height: number;
  /** Value text; the axis passes its own decimals. */
  label: (v: number, decimals?: number) => string;
}) {
  const n = points.length;
  const plotW = width - PAD.l - PAD.r;
  const plotH = height - PAD.t - PAD.b;
  const values = points.map((p) => p.value);
  const { yMin, yMax, step, ticks } = scaleFor(values);
  const yFor = (v: number) => PAD.t + ((yMax - v) * plotH) / (yMax - yMin);
  const xs = points.map((_, i) => (n === 1 ? PAD.l + plotW / 2 : PAD.l + (i * plotW) / (n - 1)));
  const ys = values.map(yFor);
  const line = xs.map((x, i) => `${i === 0 ? 'M' : 'L'}${x},${ys[i]}`).join(' ');
  const area = `${line} L${xs[n - 1]},${PAD.t + plotH} L${xs[0]},${PAD.t + plotH} Z`;
  const labelled = valueLabelIndices(values);
  // The first label hangs right of its point and the last one left, so neither leaves the plot.
  const anchor = (i: number): Anchor => (n > 1 && i === 0 ? 'start' : n > 1 && i === n - 1 ? 'end' : 'middle');
  const radius = (i: number) => (i === n - 1 ? 4.5 : 3.5);

  return (
    <Svg width={width} height={height}>
      {ticks.map((v) => (
        <Line key={`g${v}`} x1={PAD.l} x2={width - PAD.r} y1={yFor(v)} y2={yFor(v)} stroke={K.Divider} strokeWidth={1} strokeDasharray="6 8" />
      ))}
      {ticks.map((v) => (
        <SvgText key={`t${v}`} x={PAD.l - 8} y={yFor(v) + 4} textAnchor="end" {...TICK}>
          {label(v, step < 1 ? 1 : 0)}
        </SvgText>
      ))}
      <Path d={area} fill={K.Accent} fillOpacity={0.13} />
      <Path d={line} stroke={K.Accent} strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" fill="none" />
      {points.map((p, i) => {
        const last = i === n - 1;
        return (
          <Circle key={`p${p.date}${i}`} cx={xs[i]} cy={ys[i]} r={radius(i)} fill={last ? K.Accent : K.Bg} stroke={K.Accent} strokeWidth={last ? 0 : 2} />
        );
      })}
      {points.map((p, i) => {
        if (!labelled.has(i)) return null;
        const last = i === n - 1;
        return (
          <SvgText
            key={`v${p.date}${i}`}
            x={xs[i]}
            y={ys[i] - radius(i) - 5}
            textAnchor={anchor(i)}
            fill={last ? K.Text : K.Muted}
            fontSize={last ? Type.footnote.fontSize : Type.caption1.fontSize}
            fontFamily={last ? Font.bold : Font.medium}>
            {label(p.value) + (last && unit ? ` ${unit}` : '')}
          </SvgText>
        );
      })}
      {dateLabelIndices(n, plotW).map((i) => (
        <SvgText key={`d${points[i].date}${i}`} x={xs[i]} y={height - 4} textAnchor={anchor(i)} {...TICK}>
          {monthDay(points[i].date)}
        </SvgText>
      ))}
    </Svg>
  );
}

const styles = StyleSheet.create({
  empty: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  emptyText: { ...Type.caption2, fontFamily: Font.medium, color: K.Dim },
});
