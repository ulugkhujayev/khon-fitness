import { StyleSheet, Text, View } from 'react-native';
import { Dot, FieldRow, GroupedList, GroupFooter, NumberRow, Sheet, SheetGroupTitle, TimeStepper, TintedButton, ToggleRow } from '@/components/ui';
import { EatingWindow, WindowDay } from '@/domain/types';
import { EatingWindowRules, stripSummary, windowStatus, windowStrip, withEnd, withRemind, withStart } from '@/domain/window';
import { K, Type } from '@/theme';
import { WindowDayBar } from './WindowDayBar';

/** The 28-day strip as four weeks of seven, oldest first. */
const WEEKS = [0, 1, 2, 3];

/**
 * The eating-window sheet (WindowSheet.kt): where the window stands now in words and on a day bar, Open now
 * or Close now, then the switch, the hours and the reminder, and the last 28 days as four weeks: filled when
 * the fixed hours were kept, a ring when opened or closed by hand, today in white.
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
  const day = days.find((d) => d.date === today);
  const open = w.enabled && EatingWindowRules.state(w, day, now).open;
  const strip = windowStrip(days, today);
  return (
    <Sheet visible={visible} title="Eating window" onDismiss={onDismiss}>
      <GroupedList elevated style={styles.status}>
        <View style={styles.headline}>
          <Dot color={open ? K.Green : K.Muted} filled={open} size={10} />
          <Text style={styles.headlineText}>{windowStatus(w, day, now)}</Text>
        </View>
        {w.enabled ? <WindowDayBar window={w} now={now} /> : null}
      </GroupedList>
      {w.enabled ? (
        <TintedButton
          text={open ? 'Close now' : 'Open now'}
          onPress={() => {
            if (open) onCloseNow();
            else onOpenNow();
            onDismiss();
          }}
          style={styles.action}
        />
      ) : null}

      <GroupedList elevated style={styles.group}>
        <ToggleRow label="On" divider={false} value={w.enabled} onChange={(enabled) => onSave({ ...w, enabled })} />
        <FieldRow label="Opens">
          <TimeStepper minute={w.startMinute} name="Opens" elevated onChange={(m) => onSave(withStart(w, m))} />
        </FieldRow>
        <FieldRow label="Closes">
          <TimeStepper minute={w.endMinute} name="Closes" elevated onChange={(m) => onSave(withEnd(w, m))} />
        </FieldRow>
        <NumberRow label="Remind before" value={w.remindBeforeMin} unit="min" step={5} decimals={0} elevated onChange={(v) => onSave(withRemind(w, v))} />
      </GroupedList>

      {w.enabled ? (
        <>
          <SheetGroupTitle text="Last 28 days" />
          <GroupedList elevated style={styles.weeks}>
            <View accessible accessibilityLabel={`Last 28 days. ${stripSummary(strip)}`}>
              {WEEKS.map((week) => (
                <View key={week} style={styles.week}>
                  {strip.slice(week * 7, week * 7 + 7).map((cell, i) => (
                    <View key={i} style={styles.cell}>
                      <Dot color={cell === 'today' ? K.Text : K.Green} filled={cell === 'kept'} size={12} />
                    </View>
                  ))}
                </View>
              ))}
            </View>
          </GroupedList>
          <GroupFooter text={`${stripSummary(strip)} A ring is a day opened or closed by hand; today is white.`} />
        </>
      ) : null}
    </Sheet>
  );
}

const styles = StyleSheet.create({
  status: { padding: 16, gap: 14 },
  headline: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  headlineText: { ...Type.headline, flex: 1 },
  action: { marginTop: 12 },
  group: { marginTop: 20 },
  weeks: { paddingVertical: 10, paddingHorizontal: 12 },
  week: { flexDirection: 'row', justifyContent: 'space-between' },
  cell: { width: 28, height: 24, alignItems: 'center', justifyContent: 'center' },
});
