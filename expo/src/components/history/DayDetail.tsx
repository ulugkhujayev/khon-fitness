import { StyleSheet, View } from 'react-native';
import { itemColor, sessionColor } from '@/components/plan/colors';
import { GroupedList, ListRow, TextButton, TitleRow } from '@/components/ui';
import { longDate } from '@/domain/dates';
import { sessionLine, stretchLine } from '@/domain/history';
import { PlanItem } from '@/domain/plan';
import { Exercise, Modality, Session, SetLog, StretchSession } from '@/domain/types';
import { K } from '@/theme';

interface Row {
  key: string;
  title: string;
  value?: string;
  dot: string;
  filled: boolean;
  onPress?: () => void;
}

/**
 * The selected day under the calendar (HistoryScreen.kt): its date with Plan, then its stretch runs and
 * sessions; a day with no session shows what is planned. Tap a session to open it.
 */
export function DayDetail({
  date,
  item,
  sessions,
  stretches,
  logs,
  exercises,
  modalities,
  onPlan,
  onOpen,
}: {
  date: string;
  /** What the plan holds for the date. */
  item: PlanItem;
  /** Sessions of this date. */
  sessions: Session[];
  /** Stretch runs of this date. */
  stretches: StretchSession[];
  logs: SetLog[];
  exercises: Exercise[];
  modalities: Modality[];
  onPlan: () => void;
  onOpen: (session: Session) => void;
}) {
  const planned = itemColor(item, modalities);
  const rows: Row[] = [
    ...(sessions.length === 0
      ? [{ key: 'plan', title: item.name, value: item.isRest ? undefined : 'planned', dot: planned?.color ?? K.Dim, filled: planned?.filled ?? false }]
      : []),
    ...stretches.map((r) => ({ key: r.id, title: r.routineName, value: stretchLine(r), dot: K.Green, filled: true })),
    ...sessions.map((s) => {
      const c = sessionColor(s, exercises, modalities);
      return {
        key: s.id,
        title: s.programName ?? s.exerciseName ?? 'Session',
        value: sessionLine(s, logs),
        dot: c.color,
        filled: c.filled,
        onPress: () => onOpen(s),
      };
    }),
  ];
  return (
    <View>
      <TitleRow title={longDate(date)} trailing={<TextButton text="Plan" label="Change plan" onPress={onPlan} />} />
      <GroupedList style={styles.list}>
        {rows.map((r, i) => (
          <ListRow key={r.key} title={r.title} value={r.value} dot={r.dot} dotFilled={r.filled} last={i === rows.length - 1} onPress={r.onPress} />
        ))}
      </GroupedList>
    </View>
  );
}

const styles = StyleSheet.create({
  list: { marginTop: 8 },
});
