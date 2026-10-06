import { Pressable, StyleSheet, Text, View } from 'react-native';
import { longDate, parseIso, weekdayName, weekOf } from '@/domain/dates';
import { itemFor, PlanData } from '@/domain/plan';
import { hexAlpha, Modality } from '@/domain/types';
import { Font, K, Space, tabular, Type } from '@/theme';
import { GroupedList } from '@/components/ui';
import { itemColor } from './colors';

/**
 * Monday to Sunday of this week on a group-surface card (DESIGN.md, Today): weekday letter over the day
 * number in a rounded square whose border carries the planned item's color (tinted inside for high
 * intensity, none for rest), past days muted, today filled in accent. Tap a day to see or change it.
 * Port of WeekStrip in TodayScreen.kt, with the dot moved onto the border.
 */
export function WeekStrip({
  today,
  plan,
  modalities,
  onPress,
}: {
  today: string;
  plan: PlanData;
  modalities: Modality[];
  onPress: (date: string) => void;
}) {
  return (
    <GroupedList style={styles.row}>
      {weekOf(today).map((date, i) => {
        const item = itemFor(plan, date);
        const c = itemColor(item, modalities);
        const isToday = date === today;
        const past = date < today;
        return (
          <Pressable
            key={date}
            onPress={() => onPress(date)}
            accessibilityRole="button"
            accessibilityLabel={`${longDate(date)}, ${item.name}${isToday ? ', today' : ''}`}
            accessibilityHint="Change plan"
            style={({ pressed }) => [styles.cell, pressed && styles.pressed]}>
            <Text style={[styles.letter, isToday && styles.todayLetter]}>{weekdayName(i + 1)[0]}</Text>
            <View
              style={[
                styles.day,
                c && { borderColor: past ? hexAlpha(c.color, 0.5) : c.color, backgroundColor: c.filled ? hexAlpha(c.color, 0.2) : 'transparent' },
                isToday && styles.todayDay,
              ]}>
              <Text style={[styles.number, past && styles.past, isToday && styles.todayNumber]}>{parseIso(date).getDate()}</Text>
            </View>
          </Pressable>
        );
      })}
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 2, paddingVertical: 8, paddingHorizontal: 6, borderRadius: Space.groupRadius },
  cell: { flex: 1, alignItems: 'center', borderRadius: 12, paddingVertical: 4 },
  pressed: { backgroundColor: K.Surface2 },
  letter: { ...Type.caption1, fontFamily: Font.medium, color: K.Muted },
  todayLetter: { color: K.Accent },
  // Not in Theme.kt: a 38 pt square with continuous corners (a squircle on iOS) and a 2 pt border.
  day: {
    marginTop: 4,
    width: 38,
    height: 38,
    borderRadius: 11,
    borderCurve: 'continuous',
    borderWidth: 2,
    borderColor: 'transparent',
    alignItems: 'center',
    justifyContent: 'center',
  },
  todayDay: { backgroundColor: K.Accent },
  number: { ...Type.body, fontFamily: Font.medium, ...tabular },
  past: { color: K.Muted },
  todayNumber: { fontFamily: Font.bold, color: K.AccentInk },
});
