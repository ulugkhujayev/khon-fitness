import { Pressable, StyleSheet, Text, View } from 'react-native';
import { longDate, parseIso, weekdayName, weekOf } from '@/domain/dates';
import { itemFor, PlanData } from '@/domain/plan';
import { Modality } from '@/domain/types';
import { Font, K, tabular, Type } from '@/theme';
import { Dot } from '@/components/ui';
import { itemColor } from './colors';

/**
 * Monday to Sunday of this week (DESIGN.md, Today): weekday letter over the day number, today in an
 * accent circle, one dot for the planned item. Tap a day to see or change it. Port of WeekStrip in TodayScreen.kt.
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
    <View style={styles.row}>
      {weekOf(today).map((date, i) => {
        const item = itemFor(plan, date);
        const c = itemColor(item, modalities);
        const isToday = date === today;
        return (
          <Pressable
            key={date}
            onPress={() => onPress(date)}
            accessibilityRole="button"
            accessibilityLabel={`${longDate(date)}, ${item.name}${isToday ? ', today' : ''}`}
            accessibilityHint="Change plan"
            style={({ pressed }) => [styles.cell, pressed && styles.pressed]}>
            <Text style={[styles.letter, isToday && styles.todayLetter]}>{weekdayName(i + 1)[0]}</Text>
            <View style={[styles.day, isToday && styles.todayDay]}>
              <Text style={[styles.number, isToday && styles.todayNumber]}>{parseIso(date).getDate()}</Text>
            </View>
            <View style={styles.dotBox}>{c ? <Dot color={c.color} filled={c.filled} size={7} /> : null}</View>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 4 },
  cell: { flex: 1, alignItems: 'center', borderRadius: 10, paddingVertical: 4 },
  pressed: { backgroundColor: K.Surface },
  letter: { ...Type.caption1, fontFamily: Font.medium, color: K.Muted },
  todayLetter: { color: K.Accent },
  day: { marginTop: 5, width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center' },
  todayDay: { backgroundColor: K.Accent },
  number: { ...Type.body, fontFamily: Font.medium, ...tabular },
  todayNumber: { fontFamily: Font.bold, color: K.AccentInk },
  dotBox: { marginTop: 5, height: 8, justifyContent: 'center' },
});
