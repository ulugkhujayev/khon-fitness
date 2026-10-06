import { Pressable, StyleSheet, Text, View } from 'react-native';
import { longDate, parseIso, weekdayName, weekOf } from '@/domain/dates';
import { itemFor, PlanData } from '@/domain/plan';
import { hexAlpha, Modality } from '@/domain/types';
import { Dot } from '@/components/ui';
import { Font, K, tabular, Type } from '@/theme';
import { itemColor } from './colors';

/**
 * Monday to Sunday of this week (DESIGN.md, Today): each day a rounded tile on the group surface with the
 * weekday letter, the number and a dot for the planned item, past days muted, today filled in accent with
 * a white dot. Tap a day to see or change it. Port of WeekStrip in TodayScreen.kt, the cells as tiles.
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
            style={({ pressed }) => [styles.tile, isToday && styles.today, pressed && styles.pressed]}>
            <Text style={[styles.letter, isToday && styles.todayText]}>{weekdayName(i + 1)[0]}</Text>
            <Text style={[styles.number, past && styles.past, isToday && styles.todayText, isToday && styles.todayNumber]}>{parseIso(date).getDate()}</Text>
            <View style={styles.dot}>
              {c ? <Dot color={isToday ? K.AccentInk : past ? hexAlpha(c.color, 0.5) : c.color} filled={c.filled} size={7} /> : null}
            </View>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', gap: 6 },
  // Not in Theme.kt: a tile on the group surface with continuous corners (a squircle on iOS).
  tile: { flex: 1, alignItems: 'center', paddingVertical: 8, borderRadius: 12, borderCurve: 'continuous', backgroundColor: K.Surface },
  today: { backgroundColor: K.Accent },
  pressed: { opacity: 0.6 },
  letter: { ...Type.caption1, fontFamily: Font.medium, color: K.Muted },
  number: { ...Type.body, fontFamily: Font.medium, ...tabular, marginTop: 2 },
  past: { color: K.Muted },
  todayText: { color: K.AccentInk },
  todayNumber: { fontFamily: Font.bold },
  dot: { marginTop: 4, height: 8, justifyContent: 'center' },
});
