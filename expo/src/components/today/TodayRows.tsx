import { StyleSheet } from 'react-native';
import { SymbolViewProps } from 'expo-symbols';
import { Check, GroupedList } from '@/components/ui';
import { K } from '@/theme';
import { HomeRow } from './HomeRow';

/** Each row's symbol. */
const ICONS = {
  update: { ios: 'arrow.down.circle.fill', android: 'system_update', web: 'system_update' },
  stretch: { ios: 'figure.flexibility', android: 'self_improvement', web: 'self_improvement' },
  window: { ios: 'fork.knife', android: 'restaurant', web: 'restaurant' },
  band: { ios: 'heart.fill', android: 'favorite', web: 'favorite' },
} satisfies Record<string, SymbolViewProps['name']>;

/**
 * Today's rows under the workout, in one group of HomeRows: a downloaded update, the active stretch routine
 * (its line green and checked once stretched today), the eating window (green while open), and band
 * sessions waiting for import. Rows with nothing to say are left out.
 */
export function TodayRows({
  update,
  routine,
  window: w,
  pending,
}: {
  update: { onRestart: () => void } | null;
  routine: { name: string; line: string; doneToday: boolean; onOpen: () => void } | null;
  /** The state in words, "Open · closes in 2 h 10 m". */
  window: { open: boolean; text: string; onOpen: () => void };
  /** Null when no band session is waiting. */
  pending: { title: string; line: string; onOpen: () => void } | null;
}) {
  const rows = [
    update ? { key: 'update', icon: ICONS.update, color: K.Accent, title: 'Update ready', line: 'Restart to use the new version', onPress: update.onRestart } : null,
    routine
      ? {
          key: 'routine',
          icon: ICONS.stretch,
          color: K.Green,
          title: routine.name,
          line: routine.doneToday ? `Done today · ${routine.line}` : routine.line,
          lineColor: routine.doneToday ? K.Green : undefined,
          trailing: routine.doneToday ? <Check color={K.Green} /> : undefined,
          onPress: routine.onOpen,
        }
      : null,
    { key: 'window', icon: ICONS.window, color: w.open ? K.Green : K.Dim, title: 'Eating window', line: w.text, lineColor: w.open ? K.Green : undefined, onPress: w.onOpen },
    pending ? { key: 'band', icon: ICONS.band, color: K.Red, title: pending.title, line: pending.line, onPress: pending.onOpen } : null,
  ].filter((r) => r != null);

  return (
    <GroupedList style={styles.group}>
      {rows.map(({ key, ...row }, i) => (
        <HomeRow key={key} {...row} last={i === rows.length - 1} />
      ))}
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  group: { marginTop: 16 },
});
