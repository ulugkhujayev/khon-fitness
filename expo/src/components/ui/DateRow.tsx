import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { monthStart, shortDate } from '@/domain/dates';
import { FieldRow } from './FieldRow';
import { FieldValue } from './FieldValue';
import { MonthGrid } from './MonthGrid';
import { MonthSwitcher } from './MonthSwitcher';
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
          <MonthSwitcher month={month} onChange={setMonth} />
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

const styles = StyleSheet.create({
  header: { marginBottom: 8 },
});
