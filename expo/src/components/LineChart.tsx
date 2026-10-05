import { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import Svg, { Circle, Line, Path, Text as SvgText } from 'react-native-svg';
import { dateLabelIndices, scaleFor, valueLabelIndices } from '@/domain/chart';
import { monthDay } from '@/domain/dates';
import { fmt } from '@/domain/format';
import { Font, K } from '@/theme';

export interface ChartPoint {
  /** yyyy-MM-dd */
  date: string;
  value: number;
}

const PAD = { l: 40, r: 14, t: 22, b: 24 };

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
  format?: (v: number) => string;
  style?: ViewStyle;
}) {
  const [width, setWidth] = useState(0);
  const n = points.length;
  const label = (v: number, d: number) => format?.(v) ?? fmt(v, d);

  let chart = null;
  if (width > 0 && n > 0) {
    const plotW = width - PAD.l - PAD.r;
    const plotH = height - PAD.t - PAD.b;
    const values = points.map((p) => p.value);
    const { yMin, yMax, step, ticks } = scaleFor(values);
    const yFor = (v: number) => PAD.t + ((yMax - v) * plotH) / (yMax - yMin);
    const xFor = (i: number) => (n === 1 ? PAD.l + plotW / 2 : PAD.l + (i * plotW) / (n - 1));
    const xs = points.map((_, i) => xFor(i));
    const ys = values.map(yFor);
    const line = xs.map((x, i) => `${i === 0 ? 'M' : 'L'}${x},${ys[i]}`).join(' ');
    const area = `${line} L${xs[n - 1]},${PAD.t + plotH} L${xs[0]},${PAD.t + plotH} Z`;
    const labelled = valueLabelIndices(values);
    const anchor = (i: number) => (n > 1 && i === 0 ? 'start' : n > 1 && i === n - 1 ? 'end' : 'middle');

    chart = (
      <Svg width={width} height={height}>
        {ticks.map((v) => (
          <Line key={`g${v}`} x1={PAD.l} x2={width - PAD.r} y1={yFor(v)} y2={yFor(v)} stroke={K.Divider} strokeWidth={1} strokeDasharray="6 8" />
        ))}
        {ticks.map((v) => (
          <SvgText key={`t${v}`} x={PAD.l - 8} y={yFor(v) + 4} textAnchor="end" fill={K.Dim} fontSize={11} fontFamily={Font.medium}>
            {label(v, step < 1 ? 1 : 0)}
          </SvgText>
        ))}
        <Path d={area} fill={K.Accent} fillOpacity={0.13} />
        <Path d={line} stroke={K.Accent} strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" fill="none" />
        {points.map((p, i) => {
          const last = i === n - 1;
          const r = last ? 4.5 : 3.5;
          return (
            <Circle
              key={`p${i}`}
              cx={xs[i]}
              cy={ys[i]}
              r={r}
              fill={last ? K.Accent : K.Bg}
              stroke={K.Accent}
              strokeWidth={last ? 0 : 2}
            />
          );
        })}
        {points.map((p, i) => {
          if (!labelled.has(i)) return null;
          const last = i === n - 1;
          const r = last ? 4.5 : 3.5;
          return (
            <SvgText
              key={`v${i}`}
              x={xs[i]}
              y={ys[i] - r - 5}
              textAnchor={anchor(i)}
              fill={last ? K.Text : K.Muted}
              fontSize={last ? 12 : 11}
              fontFamily={last ? Font.bold : Font.medium}>
              {label(p.value, decimals) + (last && unit ? ` ${unit}` : '')}
            </SvgText>
          );
        })}
        {dateLabelIndices(n, plotW).map((i) => (
          <SvgText key={`d${i}`} x={xs[i]} y={height - 4} textAnchor={anchor(i)} fill={K.Dim} fontSize={11} fontFamily={Font.medium}>
            {monthDay(points[i].date)}
          </SvgText>
        ))}
      </Svg>
    );
  }

  return (
    <View style={[{ height }, style]} onLayout={(e) => setWidth(e.nativeEvent.layout.width)}>
      {n === 0 ? (
        <View style={styles.emptyBox}>
          <Text style={styles.empty}>No data yet</Text>
        </View>
      ) : (
        chart
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  emptyBox: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  empty: { fontFamily: Font.medium, fontSize: 11, color: K.Dim },
});
