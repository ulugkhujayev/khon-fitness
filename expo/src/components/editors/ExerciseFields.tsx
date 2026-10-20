import { StyleSheet } from 'react-native';
import { FieldRow, FieldValue, GroupedList, GroupFooter, InlineTextField, NumberRow, SectionTitle, ToggleRow } from '@/components/ui';
import { cap, clampRounds, fieldLabel, modalityName, withIntervals } from '@/domain/editors';
import { Exercise, Intensity, Kind, Modality, splitList } from '@/domain/types';
import { ExerciseSelect } from './exerciseChoices';

/**
 * The fields of the exercise editor, in groups: name and kind; for strength the muscles, then weight step
 * and notes; for cardio the modality, intensity and fields, then the intervals toggle and its times, then notes.
 */
export function ExerciseFields({
  draft,
  modalities,
  kindLocked,
  onChange,
  onOpen,
}: {
  draft: Exercise;
  modalities: Modality[];
  /** Kind cannot change once a session, program or plan uses the exercise. */
  kindLocked: boolean;
  onChange: (draft: Exercise) => void;
  onOpen: (select: ExerciseSelect) => void;
}) {
  const set = (patch: Partial<Exercise>) => onChange({ ...draft, ...patch });
  const notes = (
    <FieldRow label="Notes" divider={draft.kind === Kind.STRENGTH}>
      <InlineTextField value={draft.notes} onChange={(n) => set({ notes: n })} placeholder="Seat height, grip…" label="Notes" />
    </FieldRow>
  );
  return (
    <>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={draft.name} onChange={(name) => set({ name })} placeholder="Exercise name" label="Name" />
        </FieldRow>
        <FieldRow label="Kind" onPress={kindLocked ? undefined : () => onOpen('kind')}>
          <FieldValue text={cap(draft.kind)} locked={kindLocked} />
        </FieldRow>
      </GroupedList>
      {kindLocked ? <GroupFooter text="The kind is fixed: sessions, programs or the plan already use this exercise as it is." /> : null}
      {draft.kind === Kind.STRENGTH ? (
        <>
          <SectionTitle text="Muscles" style={styles.section} />
          <GroupedList>
            <FieldRow label="Primary" divider={false} onPress={() => onOpen('primary')}>
              <FieldValue text={cap(draft.primaryMuscle ?? '')} />
            </FieldRow>
            <FieldRow label="Secondary" onPress={() => onOpen('secondary')}>
              <FieldValue text={splitList(draft.secondaryMuscles).map(cap).join(', ') || 'None'} />
            </FieldRow>
          </GroupedList>
          <SectionTitle text="Details" style={styles.section} />
          <GroupedList>
            <NumberRow label="Weight step" divider={false} value={draft.stepKg} unit="kg" step={0.5} decimals={1} onChange={(v) => set({ stepKg: v || 2.5 })} />
            {notes}
          </GroupedList>
        </>
      ) : (
        <>
          <SectionTitle text="Cardio" style={styles.section} />
          <GroupedList>
            <FieldRow label="Modality" divider={false} onPress={() => onOpen('modality')}>
              <FieldValue text={modalityName(draft.modalityId, modalities) || 'Choose'} />
            </FieldRow>
            <FieldRow label="Intensity" onPress={() => onOpen('intensity')}>
              <FieldValue text={cap(draft.intensity ?? Intensity.LOW)} />
            </FieldRow>
            <FieldRow label="Fields" onPress={() => onOpen('fields')}>
              <FieldValue text={splitList(draft.fields).map(fieldLabel).join(', ') || 'None'} />
            </FieldRow>
          </GroupedList>
          <GroupedList style={styles.group}>
            <ToggleRow label="Intervals" divider={false} value={draft.intervals} onChange={(on) => onChange(withIntervals(draft, on))} />
            {draft.intervals ? (
              <>
                <NumberRow label="Warm-up" value={draft.warmupSec / 60} unit="min" step={1} decimals={0} onChange={(v) => set({ warmupSec: Math.trunc((v ?? 0) * 60) })} />
                <NumberRow label="Work" value={draft.workSec} unit="s" step={10} decimals={0} onChange={(v) => set({ workSec: Math.trunc(v ?? 0) })} />
                <NumberRow label="Rest" value={draft.restSec} unit="s" step={10} decimals={0} onChange={(v) => set({ restSec: Math.trunc(v ?? 0) })} />
                <NumberRow label="Rounds" value={draft.rounds} unit="" step={1} decimals={0} onChange={(v) => set({ rounds: clampRounds(v) })} />
              </>
            ) : null}
          </GroupedList>
          <GroupedList style={styles.group}>{notes}</GroupedList>
        </>
      )}
    </>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 24 },
  group: { marginTop: 24 },
});
