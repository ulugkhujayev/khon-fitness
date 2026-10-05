import { cap, clampRounds, fieldLabel, modalityName, withIntervals } from '@/domain/editors';
import { Exercise, Intensity, Kind, Modality, splitList } from '@/domain/types';
import { FieldRow, FieldValue, GroupedList, InlineTextField, NumberRow, ToggleRow } from '@/components/ui';
import { ExerciseSelect } from './exerciseChoices';

/** The field rows of the exercise editor: name, kind, then strength or cardio settings, then notes. */
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
  const isStrength = draft.kind === Kind.STRENGTH;
  return (
    <GroupedList>
      <FieldRow label="Name" divider={false}>
        <InlineTextField value={draft.name} onChange={(name) => set({ name })} placeholder="Exercise name" label="Name" />
      </FieldRow>
      <FieldRow label="Kind" onPress={kindLocked ? undefined : () => onOpen('kind')}>
        <FieldValue text={cap(draft.kind)} muted={kindLocked} />
      </FieldRow>
      {isStrength ? (
        <>
          <FieldRow label="Primary muscle" onPress={() => onOpen('primary')}>
            <FieldValue text={cap(draft.primaryMuscle ?? '')} />
          </FieldRow>
          <FieldRow label="Secondary muscles" onPress={() => onOpen('secondary')}>
            <FieldValue text={splitList(draft.secondaryMuscles).map(cap).join(', ') || 'None'} />
          </FieldRow>
          <NumberRow label="Weight step" value={draft.stepKg} unit="kg" step={0.5} decimals={1} onChange={(v) => set({ stepKg: v || 2.5 })} />
        </>
      ) : (
        <>
          <FieldRow label="Modality" onPress={() => onOpen('modality')}>
            <FieldValue text={modalityName(draft.modalityId, modalities) || 'Choose'} />
          </FieldRow>
          <FieldRow label="Intensity" onPress={() => onOpen('intensity')}>
            <FieldValue text={cap(draft.intensity ?? Intensity.LOW)} />
          </FieldRow>
          <ToggleRow label="Intervals" value={draft.intervals} onChange={(on) => onChange(withIntervals(draft, on))} />
          {draft.intervals ? (
            <>
              <NumberRow label="Warm-up" value={draft.warmupSec / 60} unit="min" step={1} decimals={0} onChange={(v) => set({ warmupSec: Math.trunc((v ?? 0) * 60) })} />
              <NumberRow label="Work" value={draft.workSec} unit="s" step={10} decimals={0} onChange={(v) => set({ workSec: Math.trunc(v ?? 0) })} />
              <NumberRow label="Rest" value={draft.restSec} unit="s" step={10} decimals={0} onChange={(v) => set({ restSec: Math.trunc(v ?? 0) })} />
              <NumberRow label="Rounds" value={draft.rounds} unit="" step={1} decimals={0} onChange={(v) => set({ rounds: clampRounds(v) })} />
            </>
          ) : null}
          <FieldRow label="Fields" onPress={() => onOpen('fields')}>
            <FieldValue text={splitList(draft.fields).map(fieldLabel).join(', ') || 'None'} />
          </FieldRow>
        </>
      )}
      <FieldRow label="Notes">
        <InlineTextField value={draft.notes} onChange={(notes) => set({ notes })} placeholder="Seat height, grip…" label="Notes" />
      </FieldRow>
    </GroupedList>
  );
}
