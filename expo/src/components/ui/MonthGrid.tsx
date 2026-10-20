import { Pressable, StyleSheet, Text, View } from 'react-native';
import { longDate, monthGrid, parseIso, today as todayIso, weekdayName } from '@/domain/dates';
import { Font, K, Space, tabular, Type } from '@/theme';

const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7];

/** Month calendar, Monday first: the selected day in an accent circle, today in accent text. */
export function MonthGrid({ month, selected, onPress }: { month: string; selected: string | null; onPress: (date: string) => void }) {
  const today = todayIso();
  const cells = monthGrid(month);
  const weeks = Array.from({ length: cells.length / 7 }, (_, w) => cells.slice(w * 7, w * 7 + 7));
  return (
    <View>
      <View style={styles.week}>
        {WEEKDAYS.map((d) => (
          <Text key={d} style={styles.letter}>
            {weekdayName(d)[0]}
          </Text>
        ))}
      </View>
      {weeks.map((week, w) => (
        <View key={w} style={styles.week}>
          {week.map((date, i) =>
            date ? (
              <Pressable
                key={date}
                onPress={() => onPress(date)}
                accessibilityRole="button"
                accessibilityLabel={longDate(date)}
                accessibilityState={{ selected: date === selected }}
                style={styles.cell}>
                <View style={[styles.day, date === selected && styles.selectedDay]}>
                  <Text style={[styles.number, date === today && styles.today, date === selected && styles.selectedNumber]}>
                    {parseIso(date).getDate()}
                  </Text>
                </View>
              </Pressable>
            ) : (
              <View key={`pad${i}`} style={styles.cell} />
            ),
          )}
        </View>
      ))}
    </View>
  );
}

const styles = StyleSheet.create({
  week: { flexDirection: 'row' },
  letter: { ...Type.caption1, fontFamily: Font.medium, flex: 1, textAlign: 'center', color: K.Muted, paddingVertical: 6 },
  cell: { flex: 1, height: Space.tapMin, alignItems: 'center', justifyContent: 'center' },
  day: { width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center' },
  selectedDay: { backgroundColor: K.Accent },
  number: { ...Type.body, fontFamily: Font.medium, ...tabular },
  today: { color: K.Accent },
  selectedNumber: { fontFamily: Font.bold, color: K.AccentInk },
});
