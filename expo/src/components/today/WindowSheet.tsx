import { StyleSheet, View } from 'react-native';
import { Dot, FieldRow, GroupedList, ListRow, NumberRow, Sheet, SheetGroupTitle, TimeStepper, ToggleRow } from '@/components/ui';
import { EatingWindow, WindowDay } from '@/domain/types';
import { EatingWindowRules, windowStrip, withEnd, withRemind, withStart } from '@/domain/window';
import { K } from '@/theme';

/**
 * The eating-window sheet (WindowSheet.kt): Open now or Close now, the hours, the reminder, the switch,
 * and the last 28 days, filled when the fixed hours were kept and a ring when they changed.
 */
export function WindowSheet({
  visible,
  window: w,
  days,
  today,
  now,
  onSave,
  onOpenNow,
  onCloseNow,
  onDismiss,
}: {
  visible: boolean;
  window: EatingWindow;
  days: WindowDay[];
  today: string;
  now: Date;
  onSave: (w: EatingWindow) => void;
  onOpenNow: () => void;
  onCloseNow: () => void;
  onDismiss: () => void;
}) {
  const open = EatingWindowRules.state(w, days.find((d) => d.date === today), now).open;
  return (
    <Sheet visible={visible} title="Eating window" onDismiss={onDismiss}>
      <GroupedList>
        {w.enabled ? (
          <ListRow
            title={open ? 'Close now' : 'Open now'}
            color={K.Accent}
            chevron={false}
            last
            onPress={() => {
              if (open) onCloseNow();
              else onOpenNow();
              onDismiss();
            }}
          />
        ) : null}
        <FieldRow label="Opens" divider={w.enabled}>
          <TimeStepper minute={w.startMinute} name="Opens" onChange={(m) => onSave(withStart(w, m))} />
        </FieldRow>
        <FieldRow label="Closes">
          <TimeStepper minute={w.endMinute} name="Closes" onChange={(m) => onSave(withEnd(w, m))} />
        </FieldRow>
        <NumberRow label="Remind before" value={w.remindBeforeMin} unit="min" step={5} decimals={0} onChange={(v) => onSave(withRemind(w, v))} />
        <ToggleRow label="On" value={w.enabled} onChange={(enabled) => onSave({ ...w, enabled })} />
      </GroupedList>
      {w.enabled ? (
        <>
          <SheetGroupTitle text="Last 28 days" />
          <View style={styles.strip} accessibilityLabel="Last 28 days">
            {windowStrip(days, today).map((cell, i) => (
              <Dot key={i} color={K.Green} filled={cell === 'kept'} size={8} />
            ))}
          </View>
        </>
      ) : null}
    </Sheet>
  );
}

const styles = StyleSheet.create({
  strip: { flexDirection: 'row', flexWrap: 'wrap', gap: 4, paddingHorizontal: 16, paddingBottom: 8 },
});
