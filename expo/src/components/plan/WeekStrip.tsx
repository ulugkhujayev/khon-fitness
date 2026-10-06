import { Pressable, StyleSheet, Text, View } from 'react-native';
import { longDate, parseIso, weekdayName, weekOf } from '@/domain/dates';
import { itemFor, PlanData } from '@/domain/plan';
import { blend, hexAlpha, Modality } from '@/domain/types';
import { Font, K, tabular, Type } from '@/theme';
import { itemColor } from './colors';

/**
 * Monday to Sunday of this week (DESIGN.md, Today): each day a rounded tile on the group surface with the
 * weekday letter over the number, its border in the planned item's color (tinted inside for high
 * intensity, none for rest), past days muted, today filled in accent. Tap a day to see or change it.
 * Port of WeekStrip in TodayScreen.kt, with the dot moved onto the tile's border.
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
        const past = date < today;
        return (
          <Pressable
            key={date}
            onPress={() => onPress(date)}
            accessibilityRole="button"
            accessibilityLabel={`${longDate(date)}, ${item.name}${isToday ? ', today' : ''}`}
            accessibilityHint="Change plan"
            style={({ pressed }) => [
              styles.tile,
              c && { borderColor: past ? hexAlpha(c.color, 0.5) : c.color, backgroundColor: c.filled ? blend(K.Surface, c.color, 0.2) : K.Surface },
              isToday && styles.today,
              pressed && styles.pressed,
            ]}>
            <Text style={[styles.letter, isToday && styles.todayText]}>{weekdayName(i + 1)[0]}</Text>
            <Text style={[styles.number, past && styles.past, isToday && styles.todayText, isToday && styles.todayNumber]}>{parseIso(date).getDate()}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 6 },
  // Not in Theme.kt: a tile with continuous corners (a squircle on iOS) and a 2 pt border in the plan's color.
  tile: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 8,
    borderRadius: 12,
    borderCurve: 'continuous',
    borderWidth: 2,
    borderColor: 'transparent',
    backgroundColor: K.Surface,
  },
  today: { backgroundColor: K.Accent },
  pressed: { opacity: 0.6 },
  letter: { ...Type.caption1, fontFamily: Font.medium, color: K.Muted },
  number: { ...Type.body, fontFamily: Font.medium, ...tabular, marginTop: 2 },
  past: { color: K.Muted },
  todayText: { color: K.AccentInk },
  todayNumber: { fontFamily: Font.bold },
});
