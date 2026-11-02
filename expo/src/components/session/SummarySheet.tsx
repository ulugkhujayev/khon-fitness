import { StyleSheet } from 'react-native';
import { EffortRow, FieldRow, GroupedList, InlineTextField, ListRow, PrimaryButton, Sheet } from '@/components/ui';
import { fmt } from '@/domain/format';
import { Session, SessionDetailsPatch } from '@/domain/types';
import { Summary } from '@/domain/workout';

/** What the finish sheet shows: the figures as of opening it, and how long the workout took ("52 min"). */
export type FinishSummary = { stats: Summary; time: string };

/**
 * Finish sheet (SessionScreen.kt): the time, sets done, volume and new bests, then effort and a note, so
 * they are asked for as the workout ends (the page's own fields stay), then Save.
 */
export function SummarySheet({
  visible,
  summary,
  session,
  onChange,
  onSave,
  onDismiss,
}: {
  visible: boolean;
  summary: FinishSummary | null;
  session: Session;
  onChange: (patch: SessionDetailsPatch) => void;
  onSave: () => void;
  onDismiss: () => void;
}) {
  return (
    <Sheet visible={visible} title="Workout done" onDismiss={onDismiss}>
      {summary ? (
        <GroupedList elevated>
          <ListRow title="Time" value={summary.time} chevron={false} />
          <ListRow title="Sets done" value={`${summary.stats.done} of ${summary.stats.total}`} chevron={false} />
          <ListRow title="Volume" value={`${fmt(summary.stats.volume, 0)} kg`} chevron={false} />
          <ListRow title="New bests" value={String(summary.stats.bests)} chevron={false} last />
        </GroupedList>
      ) : null}
      <GroupedList elevated style={styles.details}>
        <EffortRow value={session.effort} divider={false} elevated onChange={(effort) => onChange({ effort })} />
        <FieldRow label="Note">
          <InlineTextField value={session.note} onChange={(note) => onChange({ note })} placeholder="Optional" label="Note" />
        </FieldRow>
      </GroupedList>
      <PrimaryButton text="Save" onPress={onSave} style={styles.save} />
    </Sheet>
  );
}

const styles = StyleSheet.create({
  details: { marginTop: 12 },
  save: { marginTop: 16 },
});
