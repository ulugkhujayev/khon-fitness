import { Animated, StyleSheet, Text, View } from 'react-native';
import { Dot } from '@/components/ui';
import { Font, K, tabular, Type } from '@/theme';

/** A dot under a day: a done session or stretch run, or the planned item when nothing was done. */
export interface DayMark {
  color: string;
  filled: boolean;
}

/**
 * One calendar day (HistoryScreen.kt) as a tile like Today's week strip: the number with up to three dots for
 * what was done or a faint dot for the plan, and a small mark when the plan was changed. Days of other months
 * have no tile and a dim number; the selected day's tile is lighter; today's is filled in accent, its marks
 * in white. Lifted while dragged; outlined in accent as a drop target.
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
  /** Follows the finger while lifted. */
  offset: Animated.ValueXY;
}) {
  const lift = lifted ? { transform: [...offset.getTranslateTransform(), { scale: 1.12 }] } : null;
  // On today's accent tile every mark turns white, as on the week strip.
  const tint = (color: string) => (isToday ? K.AccentInk : color);
  return (
    <Animated.View
      style={[styles.tile, !inMonth && styles.outside, selected && styles.selected, isToday && styles.today, target && styles.target, lifted && styles.lifted, lift]}>
      <Text style={[styles.number, !inMonth && styles.otherMonth, isToday && styles.todayNumber]}>{day}</Text>
      {overridden ? <View style={[styles.changed, isToday && styles.changedToday]} /> : null}
      <View style={styles.dots}>
        {dots.length > 0
          ? dots.map((d, i) => <Dot key={i} color={tint(d.color)} filled={d.filled} size={7} />)
          : planned ? (
              <View style={styles.planned}>
                <Dot color={tint(planned.color)} filled={planned.filled} size={5} />
              </View>
            ) : null}
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  // Not in Theme.kt: the week strip's tile (DESIGN.md, Today), 3 pt inside its cell so tiles sit 6 pt apart.
  tile: { flex: 1, margin: 3, alignItems: 'center', justifyContent: 'center', borderRadius: 12, borderCurve: 'continuous', backgroundColor: K.Surface },
  outside: { backgroundColor: 'transparent' },
  selected: { backgroundColor: K.Surface3 },
  today: { backgroundColor: K.Accent },
  target: { backgroundColor: K.AccentSoft, borderWidth: 2, borderColor: K.Accent },
  lifted: { backgroundColor: K.Surface3, zIndex: 1, elevation: 8, shadowColor: K.Shadow, shadowOpacity: 0.5, shadowRadius: 8, shadowOffset: { width: 0, height: 4 } },
  number: { ...Type.body, ...tabular, fontFamily: Font.medium },
  otherMonth: { color: K.Dim },
  todayNumber: { fontFamily: Font.bold, color: K.AccentInk },
  // A 5 pt mark in the tile's top right corner: the plan of this date was changed by hand.
  changed: { position: 'absolute', top: 5, right: 5, width: 5, height: 5, borderRadius: 2.5, backgroundColor: K.Muted },
  changedToday: { backgroundColor: K.AccentInk },
  dots: { flexDirection: 'row', alignItems: 'center', gap: 3, height: 8, marginTop: 3 },
  planned: { opacity: 0.4 },
});
