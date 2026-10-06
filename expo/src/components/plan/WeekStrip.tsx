import { Pressable, StyleSheet, Text, View } from 'react-native';
import { longDate, parseIso, weekdayName, weekOf } from '@/domain/dates';
import { itemFor, PlanData } from '@/domain/plan';
import { Modality } from '@/domain/types';
import { Font, K, Space, tabular, Type } from '@/theme';
import { Dot, GroupedList } from '@/components/ui';
import { itemColor } from './colors';

/**
 * Monday to Sunday of this week on a group-surface card (DESIGN.md, Today): weekday letter over the day
 * number, past days muted, today in an accent circle, one dot for the planned item. Tap a day to see or
 * change it. Port of WeekStrip in TodayScreen.kt.
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
            <View style={[styles.day, isToday && styles.todayDay]}>
              <Text style={[styles.number, past && styles.past, isToday && styles.todayNumber]}>{parseIso(date).getDate()}</Text>
            </View>
            <View style={styles.dotBox}>{c ? <Dot color={c.color} filled={c.filled} size={7} /> : null}</View>
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
  day: { marginTop: 4, width: 32, height: 32, borderRadius: 16, alignItems: 'center', justifyContent: 'center' },
  todayDay: { backgroundColor: K.Accent },
  number: { ...Type.body, fontFamily: Font.medium, ...tabular },
  past: { color: K.Muted },
  todayNumber: { fontFamily: Font.bold, color: K.AccentInk },
  dotBox: { marginTop: 4, height: 8, justifyContent: 'center' },
});
