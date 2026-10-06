import { ReactElement } from 'react';
import { StyleSheet } from 'react-native';
import { SymbolViewProps } from 'expo-symbols';
import { Check, GroupedList, IconTile, ListRow } from '@/components/ui';
import { K } from '@/theme';

/** Each row's icon tile, as in iOS Settings. */
const ICONS = {
  update: { ios: 'arrow.down.circle.fill', android: 'system_update', web: 'system_update' },
  stretch: { ios: 'figure.flexibility', android: 'self_improvement', web: 'self_improvement' },
  window: { ios: 'fork.knife', android: 'restaurant', web: 'restaurant' },
  band: { ios: 'heart.fill', android: 'favorite', web: 'favorite' },
} satisfies Record<string, SymbolViewProps['name']>;

/** One row of the group: its tile, title and value, what it opens, and a mark before the chevron. */
interface Row {
  key: string;
  icon: SymbolViewProps['name'];
  color: string;
  title: string;
  value?: string;
  trailing?: ReactElement;
  onPress: () => void;
}

/**
 * Today's rows under the workout, in one group with icon tiles: a downloaded update, the active stretch
 * routine (checked once stretched today), the eating window (green while open), and band sessions waiting
 * for import. Rows with nothing to say are left out.
 */
export function TodayRows({
  update,
  routine,
  window: w,
  pending,
}: {
  update: { onRestart: () => void } | null;
  routine: { name: string; line: string; doneToday: boolean; onOpen: () => void } | null;
  window: { open: boolean; text: string; onOpen: () => void };
  /** Null when no band session is waiting. */
  pending: { text: string; onOpen: () => void } | null;
}) {
  const rows: Row[] = [
    ...(update ? [{ key: 'update', icon: ICONS.update, color: K.Accent, title: 'Update ready', value: 'Restart', onPress: update.onRestart }] : []),
    ...(routine
      ? [
          {
            key: 'routine',
            icon: ICONS.stretch,
            color: K.Green,
            title: routine.name,
            value: routine.line,
            trailing: routine.doneToday ? <Check color={K.Green} /> : undefined,
            onPress: routine.onOpen,
          },
        ]
      : []),
    { key: 'window', icon: ICONS.window, color: w.open ? K.Green : K.Dim, title: 'Eating window', value: w.text, onPress: w.onOpen },
    ...(pending ? [{ key: 'band', icon: ICONS.band, color: K.Red, title: pending.text, onPress: pending.onOpen }] : []),
  ];
  return (
    <GroupedList style={styles.group}>
      {rows.map((r, i) => (
        <ListRow
          key={r.key}
          leading={<IconTile icon={r.icon} color={r.color} />}
          title={r.title}
          value={r.value}
          trailing={r.trailing}
          last={i === rows.length - 1}
          onPress={r.onPress}
        />
      ))}
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  group: { marginTop: 16 },
});
