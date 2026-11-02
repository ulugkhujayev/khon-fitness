import { StyleSheet, Text } from 'react-native';
import { DateRow, EffortRow, FieldRow, GroupedList, InlineTextField } from '@/components/ui';
import { Session, SessionDetailsPatch } from '@/domain/types';
import { Type } from '@/theme';

/** Date, effort 1-10 and note of a session: editable while live, else only what was filled in. */
export function SessionDetails({ session, live, onChange }: { session: Session; live: boolean; onChange: (patch: SessionDetailsPatch) => void }) {
  if (live) {
    return (
      <GroupedList>
        <DateRow date={session.date} divider={false} onChange={(date) => onChange({ date })} />
        <EffortRow value={session.effort} onChange={(effort) => onChange({ effort })} />
        <FieldRow label="Note">
          <InlineTextField value={session.note} onChange={(note) => onChange({ note })} placeholder="Optional" label="Note" />
        </FieldRow>
      </GroupedList>
    );
  }
  const note = session.note.trim();
  if (!note && session.effort == null) return null;
  return (
    <GroupedList>
      {session.effort != null ? (
        <FieldRow label="Effort" divider={false}>
          <Text style={Type.body}>{`${session.effort} / 10`}</Text>
        </FieldRow>
      ) : null}
      {note ? (
        <FieldRow label="Note" divider={session.effort != null}>
          <Text style={styles.note}>{note}</Text>
        </FieldRow>
      ) : null}
    </GroupedList>
  );
}

const styles = StyleSheet.create({
  note: { ...Type.body, textAlign: 'right', flexShrink: 1 },
});
