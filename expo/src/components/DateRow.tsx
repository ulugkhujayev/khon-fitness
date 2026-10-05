import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { longDate, monthGrid, monthStart, monthTitle, parseIso, shortDate, today as todayIso, weekdayName } from '@/domain/dates';
import { Font, K, Type } from '@/theme';
import { Sheet } from './Sheet';
import { FieldRow, FieldValue } from './ui';

/** Month calendar, Monday first: the selected day in an accent circle, today in accent text. */
export function MonthGrid({ month, selected, onPress }: { month: string; selected: string | null; onPress: (date: string) => void }) {
  const today = todayIso();
  const cells = monthGrid(month);
  return (
    <View>
      <View style={styles.week}>
        {[1, 2, 3, 4, 5, 6, 7].map((d) => (
          <Text key={d} style={styles.letter}>
            {weekdayName(d)[0]}
          </Text>
        ))}
      </View>
      {Array.from({ length: cells.length / 7 }, (_, row) => (
        <View key={row} style={styles.week}>
          {cells.slice(row * 7, row * 7 + 7).map((date, i) => {
            if (!date) return <View key={i} style={styles.cell} />;
            const on = date === selected;
            return (
              <Pressable
                key={date}
                onPress={() => onPress(date)}
                accessibilityRole="button"
                accessibilityLabel={longDate(date)}
                accessibilityState={{ selected: on }}
                style={styles.cell}>
                <View style={[styles.day, on && { backgroundColor: K.Accent }]}>
                  <Text style={[styles.num, date === today && { color: K.Accent }, on && { color: K.AccentInk, fontFamily: Font.bold }]}>
                    {parseIso(date).getDate()}
                  </Text>
                </View>
              </Pressable>
            );
          })}
        </View>
      ))}
    </View>
  );
}

/** Editor row that shows a date and opens a month calendar in a sheet. */
export function DateRow({ date, divider = true, onChange }: { date: string; divider?: boolean; onChange: (date: string) => void }) {
  const [open, setOpen] = useState(false);
  const [month, setMonth] = useState(monthStart(date));
  return (
    <>
      <FieldRow
        label="Date"
        divider={divider}
        onPress={() => {
          setMonth(monthStart(date));
          setOpen(true);
        }}>
        <FieldValue text={shortDate(date)} />
      </FieldRow>
      <Sheet visible={open} onDismiss={() => setOpen(false)}>
        <View style={styles.header}>
          <Text style={[Type.headline, { flex: 1 }]}>{monthTitle(month)}</Text>
          <MonthButton dir={-1} onPress={() => setMonth(monthStart(month, -1))} />
          <MonthButton dir={1} onPress={() => setMonth(monthStart(month, 1))} />
        </View>
        <MonthGrid
          month={month}
          selected={date}
          onPress={(d) => {
            onChange(d);
            setOpen(false);
          }}
        />
      </Sheet>
    </>
  );
}

function MonthButton({ dir, onPress }: { dir: -1 | 1; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={dir < 0 ? 'Previous month' : 'Next month'}
      style={({ pressed }) => [styles.monthButton, pressed && { opacity: 0.5 }]}>
      <SymbolView
        name={dir < 0 ? { ios: 'chevron.left', android: 'chevron_left', web: 'chevron_left' } : { ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }}
        tintColor={K.Accent}
        size={18}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', paddingLeft: 4, marginBottom: 8 },
  monthButton: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center' },
  week: { flexDirection: 'row' },
  letter: { flex: 1, textAlign: 'center', fontFamily: Font.medium, fontSize: 12, color: K.Muted, paddingVertical: 6 },
  cell: { flex: 1, height: 44, alignItems: 'center', justifyContent: 'center' },
  day: { width: 36, height: 36, borderRadius: 18, alignItems: 'center', justifyContent: 'center' },
  num: { fontFamily: Font.medium, fontSize: 17, color: K.Text, fontVariant: ['tabular-nums'] },
});
