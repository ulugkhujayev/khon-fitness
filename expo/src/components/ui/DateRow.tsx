import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { monthStart, monthTitle, shortDate } from '@/domain/dates';
import { K, Space, Type } from '@/theme';
import { FieldRow } from './FieldRow';
import { FieldValue } from './FieldValue';
import { MonthGrid } from './MonthGrid';
import { Sheet } from './Sheet';

/** Editor row that shows a date and opens a month calendar in a sheet (DateRow in Common.kt). */
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
          <Text style={styles.month}>{monthTitle(month)}</Text>
          <MonthButton direction={-1} onPress={() => setMonth(monthStart(month, -1))} />
          <MonthButton direction={1} onPress={() => setMonth(monthStart(month, 1))} />
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

function MonthButton({ direction, onPress }: { direction: -1 | 1; onPress: () => void }) {
  const back = direction < 0;
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityLabel={back ? 'Previous month' : 'Next month'}
      style={({ pressed }) => [styles.monthButton, pressed && styles.pressed]}>
      <SymbolView
        name={back ? { ios: 'chevron.left', android: 'chevron_left', web: 'chevron_left' } : { ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }}
        tintColor={K.Accent}
        size={18}
      />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  header: { flexDirection: 'row', alignItems: 'center', paddingLeft: 4, marginBottom: 8 },
  month: { ...Type.headline, flex: 1 },
  monthButton: { width: Space.tapMin, height: Space.tapMin, alignItems: 'center', justifyContent: 'center' },
  pressed: { opacity: 0.5 },
});
