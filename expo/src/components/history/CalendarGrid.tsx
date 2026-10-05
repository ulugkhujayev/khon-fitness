import { Pressable, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { itemColor, sessionColor } from '@/components/plan/colors';
import { parseIso, sameMonth, weekdayName } from '@/domain/dates';
import { dayCount, dayDots, dayLabel, dropAction, swapNeighbours } from '@/domain/history';
import { itemFor, PlanData } from '@/domain/plan';
import { Exercise, Modality, Session, StretchSession } from '@/domain/types';
import { Font, K, Type } from '@/theme';
import { CalendarDay } from './CalendarDay';
import { useDayDrag } from './useDayDrag';

const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7];

/**
 * Month calendar of History (HistoryScreen.kt): tap a day to select it; long press to change its plan, or
 * drag it onto another day ahead to swap the two. Screen readers get the same through custom actions.
 */
export function CalendarGrid({
  weeks,
  month,
  today,
  selected,
  plan,
  sessions,
  stretches,
  exercises,
  modalities,
  onSelect,
  onPlan,
  onSwap,
  onDragActive,
}: {
  weeks: string[][];
  /** Any date of the month shown; days outside it are dim. */
  month: string;
  today: string;
  selected: string;
  plan: PlanData;
  sessions: Session[];
  stretches: StretchSession[];
  exercises: Exercise[];
  modalities: Modality[];
  onSelect: (date: string) => void;
  onPlan: (date: string) => void;
  onSwap: (a: string, b: string) => void;
  onDragActive: (active: boolean) => void;
}) {
  const { fontScale } = useWindowDimensions();
  const large = fontScale >= 1.5;
  const rowHeight = large ? 56 : 46;
  const { drag, offset, begin, gridProps } = useDayDrag({ dates: weeks.flat(), rows: weeks.length, rowHeight, today, onPlan, onSwap, onActive: onDragActive });

  return (
    <View>
      <View style={styles.week} importantForAccessibility="no-hide-descendants" accessibilityElementsHidden>
        {WEEKDAYS.map((d) => (
          <Text key={d} style={styles.letter}>
            {weekdayName(d)[0]}
          </Text>
        ))}
      </View>
      <View {...gridProps}>
        {weeks.map((week) => (
          <View key={week[0]} style={[styles.week, { height: rowHeight }, drag != null && week.includes(drag.from) && styles.liftedRow]}>
            {week.map((date) => {
              const item = itemFor(plan, date);
              const planned = itemColor(item, modalities);
              const swaps = swapNeighbours(date, today);
              const actions = [
                { name: 'plan', label: 'Change plan' },
                ...(swaps.previous ? [{ name: 'previous', label: 'Swap with previous day' }] : []),
                ...(swaps.next ? [{ name: 'next', label: 'Swap with next day' }] : []),
              ];
              return (
                <Pressable
                  key={date}
                  onPress={() => onSelect(date)}
                  onLongPress={(e) => begin(date, e)}
                  delayLongPress={350}
                  accessibilityRole="button"
                  accessibilityLabel={dayLabel(date, today, dayCount(date, sessions, stretches), item)}
                  accessibilityState={{ selected: date === selected }}
                  accessibilityActions={actions}
                  onAccessibilityAction={(e) => {
                    const name = e.nativeEvent.actionName;
                    if (name === 'plan') onPlan(date);
                    else if (name === 'previous' && swaps.previous) onSwap(date, swaps.previous);
                    else if (name === 'next' && swaps.next) onSwap(date, swaps.next);
                  }}
                  style={[styles.cell, drag?.from === date && styles.liftedCell]}>
                  <CalendarDay
                    day={parseIso(date).getDate()}
                    inMonth={sameMonth(date, month)}
                    isToday={date === today}
                    selected={date === selected}
                    overridden={item.overridden}
                    dots={dayDots(date, sessions, stretches).map((s) => (s ? sessionColor(s, exercises, modalities) : { color: K.Green, filled: true }))}
                    planned={planned}
                    lifted={drag?.from === date}
                    target={drag != null && drag.over === date && dropAction(drag.from, date, today) === 'swap'}
                    numberSize={large ? 34 : 26}
                    offset={offset}
                  />
                </Pressable>
              );
            })}
          </View>
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  week: { flexDirection: 'row' },
  letter: { ...Type.caption1, fontFamily: Font.medium, flex: 1, textAlign: 'center', color: K.Dim, paddingBottom: 4 },
  cell: { flex: 1 },
  // The lifted day draws over its neighbours and the rows below.
  liftedRow: { zIndex: 1 },
  liftedCell: { zIndex: 1 },
});
