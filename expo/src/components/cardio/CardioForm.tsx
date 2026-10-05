import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { EditorScreen } from '@/components/layout';
import { Capsule, ChoiceSheet, ConfirmSheet, DateRow, FieldRow, GroupedList, InlineTextField, NumberRow, SectionTitle, TextButton } from '@/components/ui';
import { formFields, STROKES } from '@/domain/cardio';
import { Exercise, IntervalLog, Session } from '@/domain/types';
import { clampEffort } from '@/domain/workout';
import { K } from '@/theme';
import { CardioFieldRows } from './CardioFieldRows';
import { IntervalRows } from './IntervalRows';

/**
 * Cardio session form (CardioScreen.kt): the date, the exercise's fields, one row per work interval, effort and
 * note. Edits a draft; Save writes it, Back drops it.
 */
export function CardioForm({
  session,
  exercise,
  intervals,
  onSave,
  onDelete,
}: {
  session: Session;
  exercise: Exercise | null;
  intervals: IntervalLog[];
  onSave: (draft: Session, intervals: IntervalLog[]) => void;
  onDelete: () => void;
}) {
  const [draft, setDraft] = useState(session);
  const [rows, setRows] = useState(intervals);
  const [strokeOpen, setStrokeOpen] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const done = session.finishedAt != null;
  const strokes = STROKES.map((s) => ({ id: s, title: s }));

  return (
    <EditorScreen title={session.exerciseName ?? 'Cardio'} trailing={<Capsule text="Save" onPress={() => onSave(draft, rows)} />}>
      <GroupedList>
        <DateRow date={draft.date} divider={false} onChange={(date) => setDraft({ ...draft, date })} />
        <CardioFieldRows draft={draft} fields={formFields(exercise, session)} onChange={setDraft} onStroke={() => setStrokeOpen(true)} />
      </GroupedList>
      {rows.length > 0 ? (
        <>
          <SectionTitle text="Intervals" style={styles.section} />
          <IntervalRows intervals={rows} onChange={setRows} />
        </>
      ) : null}
      <SectionTitle text="Notes" style={styles.section} />
      <GroupedList>
        <NumberRow label="Effort" divider={false} value={draft.effort} unit="/10" step={1} decimals={0} placeholder="1–10" onChange={(v) => setDraft({ ...draft, effort: clampEffort(v) })} />
        <FieldRow label="Note">
          <InlineTextField value={draft.note} onChange={(note) => setDraft({ ...draft, note })} placeholder="Optional" label="Note" />
        </FieldRow>
      </GroupedList>
      <TextButton text={done ? 'Delete session' : 'Discard'} color={K.Red} onPress={() => setConfirmDelete(true)} style={styles.delete} />

      <ChoiceSheet
        visible={strokeOpen}
        title="Stroke"
        choices={strokes}
        isSelected={(id) => (draft.stroke ?? 'Freestyle') === id}
        onPick={(stroke) => {
          setDraft({ ...draft, stroke });
          setStrokeOpen(false);
        }}
        onDismiss={() => setStrokeOpen(false)}
      />
      <ConfirmSheet
        visible={confirmDelete}
        title={done ? 'Delete this session?' : 'Discard this session?'}
        body={done ? 'It goes away from History.' : 'Nothing is kept.'}
        action={done ? 'Delete' : 'Discard'}
        onDismiss={() => setConfirmDelete(false)}
        onConfirm={() => {
          setConfirmDelete(false);
          onDelete();
        }}
      />
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 24 },
  delete: { alignSelf: 'flex-start', marginTop: 24 },
});
