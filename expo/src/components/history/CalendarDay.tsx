import { Animated, StyleSheet, Text, View } from 'react-native';
import { Dot } from '@/components/ui';
import { Font, K, Type } from '@/theme';

/** A dot under a day: a done session or stretch run, or the planned item when nothing was done. */
export interface DayMark {
  color: string;
  filled: boolean;
}

/**
 * One calendar day (HistoryScreen.kt): the number (today in an accent circle, other months dim), a small mark
 * when the plan was changed, up to three dots for what was done or a faint dot for the plan.
 * Lifted while dragged; outlined in accent as a drop target.
 */
export function CalendarDay({
  day,
  inMonth,
  isToday,
  selected,
  overridden,
  dots,
  planned,
  lifted,
  target,
  numberSize,
  offset,
}: {
  day: number;
  inMonth: boolean;
  isToday: boolean;
  selected: boolean;
  overridden: boolean;
  dots: DayMark[];
  planned: DayMark | null;
  lifted: boolean;
  target: boolean;
  /** Circle around the number: 26, or 34 with large text. */
  numberSize: number;
  /** Follows the finger while lifted. */
  offset: Animated.ValueXY;
}) {
  const lift = lifted ? { transform: [...offset.getTranslateTransform(), { scale: 1.12 }] } : null;
  return (
    <Animated.View style={[styles.cell, selected && styles.selected, target && styles.target, lifted && styles.lifted, lift]}>
      <View>
        <View style={[styles.circle, { width: numberSize, height: numberSize, borderRadius: numberSize / 2 }, isToday && styles.today]}>
          <Text style={[styles.number, !inMonth && styles.otherMonth, isToday && styles.todayNumber]}>{day}</Text>
        </View>
        {overridden ? <View style={styles.changed} /> : null}
      </View>
      <View style={styles.dots}>
        {dots.length > 0
          ? dots.map((d, i) => <Dot key={i} color={d.color} filled={d.filled} size={7} />)
          : planned ? (
              <View style={styles.planned}>
                <Dot color={planned.color} filled={planned.filled} size={5} />
              </View>
            ) : null}
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  cell: { flex: 1, alignItems: 'center', paddingTop: 6, borderRadius: 10 },
  selected: { backgroundColor: K.Surface2 },
  target: { backgroundColor: K.AccentSoft, borderWidth: 2, borderColor: K.Accent },
  lifted: { backgroundColor: K.Surface3, zIndex: 1, elevation: 8, shadowColor: K.Shadow, shadowOpacity: 0.5, shadowRadius: 8, shadowOffset: { width: 0, height: 4 } },
  circle: { alignItems: 'center', justifyContent: 'center' },
  today: { backgroundColor: K.Accent },
  number: { ...Type.subheadline, fontVariant: ['tabular-nums'] },
  otherMonth: { color: K.Dim },
  todayNumber: { fontFamily: Font.bold, color: K.AccentInk },
  // A 5 pt mark at the top right of the number: the plan of this date was changed by hand.
  changed: { position: 'absolute', top: -1, right: -3, width: 5, height: 5, borderRadius: 2.5, backgroundColor: K.Muted },
  dots: { flexDirection: 'row', alignItems: 'center', gap: 3, height: 8, marginTop: 4 },
  planned: { opacity: 0.4 },
});
