import { useEffect, useState } from 'react';
import { Text } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { modalityColor, modalityName } from '@/components/colors';
import { EditorScreen } from '@/components/EditorScreen';
import { NumberRow } from '@/components/NumberField';
import { ChoiceList, ChoiceRow, Sheet } from '@/components/Sheet';
import { BottomActions, Capsule, FieldRow, FieldValue, GroupedList, InlineTextField, TextButton, Toggle } from '@/components/ui';
import { asCardio, asStrength, cap, CARDIO_FIELDS, fieldLabel, newExercise, toggleIn, withIntervals } from '@/domain/editors';
import { Exercise, Intensity, Kind, Modality, MUSCLE_GROUPS, splitList } from '@/domain/types';
import * as repo from '@/db/repository';
import { newId } from '@/db/id';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K, Type } from '@/theme';

type SheetKind = 'kind' | 'primary' | 'secondary' | 'modality' | 'intensity' | 'fields' | 'merge';

/**
 * Exercise editor (ExerciseEditorScreen in Editors.kt). Edits a draft; Done saves it.
 * Archive, merge and delete live at the bottom; delete only while nothing uses the exercise.
 */
export default function ExerciseEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const isNew = id === 'new';
  const { data } = useLive(
    async () => {
      const [exercises, modalities] = await Promise.all([repo.exercises(), repo.modalities()]);
      return { exercises, modalities };
    },
    [T.exercises, T.modalities],
  );
  const original = data?.exercises.find((e) => e.id === id) ?? null;
  if (!data || (!isNew && !original)) return <EditorScreen title="">{null}</EditorScreen>;
  return <ExerciseForm key={id} original={original} exercises={data.exercises} modalities={data.modalities} />;
}

function ExerciseForm({ original, exercises, modalities }: { original: Exercise | null; exercises: Exercise[]; modalities: Modality[] }) {
  const isNew = original === null;
  const [draft, setDraft] = useState<Exercise>(() => original ?? newExercise(newId()));
  const [usage, setUsage] = useState(0);
  const [sheet, setSheet] = useState<SheetKind | null>(null);

  useEffect(() => {
    if (original) repo.exerciseUsage(original.id).then(setUsage);
  }, [original]);

  const set = (patch: Partial<Exercise>) => setDraft({ ...draft, ...patch });
  const close = () => setSheet(null);
  const isStrength = draft.kind === Kind.STRENGTH;
  const secondary = splitList(draft.secondaryMuscles);
  const fields = splitList(draft.fields);
  const mergeTargets = exercises.filter((e) => e.id !== draft.id && e.kind === draft.kind);

  return (
    <EditorScreen
      title={isNew ? 'New exercise' : (original?.name ?? '')}
      trailing={
        <Capsule
          text="Done"
          onPress={() => {
            if (draft.name.trim()) repo.saveExercise({ ...draft, name: draft.name.trim() });
            router.back();
          }}
        />
      }>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={draft.name} onChange={(name) => set({ name })} placeholder="Exercise name" label="Name" />
        </FieldRow>
        <FieldRow label="Kind" onPress={usage === 0 ? () => setSheet('kind') : undefined}>
          <FieldValue text={cap(draft.kind)} color={usage === 0 ? K.Text : K.Muted} />
        </FieldRow>
        {isStrength ? (
          <>
            <FieldRow label="Primary muscle" onPress={() => setSheet('primary')}>
              <FieldValue text={cap(draft.primaryMuscle ?? '')} />
            </FieldRow>
            <FieldRow label="Secondary muscles" onPress={() => setSheet('secondary')}>
              <FieldValue text={secondary.map(cap).join(', ') || 'None'} />
            </FieldRow>
            <NumberRow label="Weight step" value={draft.stepKg} unit="kg" step={0.5} decimals={1} onChange={(v) => set({ stepKg: v || 2.5 })} />
          </>
        ) : (
          <>
            <FieldRow label="Modality" onPress={() => setSheet('modality')}>
              <FieldValue text={modalityName(draft.modalityId, modalities) || 'Choose'} />
            </FieldRow>
            <FieldRow label="Intensity" onPress={() => setSheet('intensity')}>
              <FieldValue text={cap(draft.intensity ?? Intensity.LOW)} />
            </FieldRow>
            <FieldRow label="Intervals">
              <Toggle value={draft.intervals} onChange={(on) => setDraft(withIntervals(draft, on))} label="Intervals" />
            </FieldRow>
            {draft.intervals ? (
              <>
                <NumberRow label="Warm-up" value={draft.warmupSec / 60} unit="min" step={1} decimals={0} onChange={(v) => set({ warmupSec: Math.trunc((v ?? 0) * 60) })} />
                <NumberRow label="Work" value={draft.workSec} unit="s" step={10} decimals={0} onChange={(v) => set({ workSec: Math.trunc(v ?? 0) })} />
                <NumberRow label="Rest" value={draft.restSec} unit="s" step={10} decimals={0} onChange={(v) => set({ restSec: Math.trunc(v ?? 0) })} />
                <NumberRow
                  label="Rounds"
                  value={draft.rounds}
                  unit=""
                  step={1}
                  decimals={0}
                  onChange={(v) => set({ rounds: Math.min(30, Math.max(1, Math.trunc(v ?? 1))) })}
                />
              </>
            ) : null}
            <FieldRow label="Fields" onPress={() => setSheet('fields')}>
              <FieldValue text={fields.map(fieldLabel).join(', ') || 'None'} />
            </FieldRow>
          </>
        )}
        <FieldRow label="Notes">
          <InlineTextField value={draft.notes} onChange={(notes) => set({ notes })} placeholder="Seat height, grip…" label="Notes" />
        </FieldRow>
      </GroupedList>

      {!isNew ? (
        <>
          <BottomActions>
            <TextButton
              text={draft.archived ? 'Unarchive' : 'Archive'}
              onPress={async () => {
                await repo.saveExercise({ ...draft, archived: !draft.archived });
                router.back();
              }}
            />
            <TextButton text="Merge into…" onPress={() => setSheet('merge')} />
            <TextButton
              text="Delete"
              color={K.Red}
              enabled={usage === 0}
              onPress={async () => {
                await repo.deleteExercise(draft.id);
                router.back();
              }}
            />
          </BottomActions>
          {usage > 0 ? (
            <Text style={[Type.footnote, { color: K.Dim, marginTop: 6, paddingLeft: 2 }]}>Used in a session, program, or plan. Archive or merge instead.</Text>
          ) : null}
        </>
      ) : null}

      <Sheet visible={sheet === 'kind'} title="Kind" onDismiss={close}>
        <ChoiceList>
          <ChoiceRow title="Strength" selected={isStrength} onPress={() => (setDraft(asStrength(draft)), close())} />
          <ChoiceRow
            title="Cardio"
            selected={!isStrength}
            last
            onPress={() => (setDraft(asCardio(draft, modalities[0]?.id ?? null)), close())}
          />
        </ChoiceList>
      </Sheet>
      <Sheet visible={sheet === 'primary'} title="Primary muscle" onDismiss={close}>
        <ChoiceList>
          {MUSCLE_GROUPS.map((m, i) => (
            <ChoiceRow key={m} title={cap(m)} selected={draft.primaryMuscle === m} last={i === MUSCLE_GROUPS.length - 1} onPress={() => (set({ primaryMuscle: m }), close())} />
          ))}
        </ChoiceList>
      </Sheet>
      <Sheet visible={sheet === 'secondary'} title="Secondary muscles" onDismiss={close}>
        <ChoiceList>
          {MUSCLE_GROUPS.filter((m) => m !== draft.primaryMuscle).map((m, i, all) => (
            <ChoiceRow
              key={m}
              title={cap(m)}
              selected={secondary.includes(m)}
              last={i === all.length - 1}
              onPress={() => set({ secondaryMuscles: toggleIn(draft.secondaryMuscles, m) })}
            />
          ))}
        </ChoiceList>
      </Sheet>
      <Sheet visible={sheet === 'modality'} title="Modality" onDismiss={close}>
        <ChoiceList>
          {modalities.map((m, i) => (
            <ChoiceRow
              key={m.id}
              title={m.name}
              selected={draft.modalityId === m.id}
              dot={modalityColor(m.id, modalities)}
              last={i === modalities.length - 1}
              onPress={() => (set({ modalityId: m.id }), close())}
            />
          ))}
        </ChoiceList>
      </Sheet>
      <Sheet visible={sheet === 'intensity'} title="Intensity" onDismiss={close}>
        <ChoiceList>
          {[Intensity.LOW, Intensity.MODERATE, Intensity.HIGH].map((v, i) => (
            <ChoiceRow key={v} title={cap(v)} selected={draft.intensity === v} last={i === 2} onPress={() => (set({ intensity: v }), close())} />
          ))}
        </ChoiceList>
      </Sheet>
      <Sheet visible={sheet === 'fields'} title="Fields" onDismiss={close}>
        <ChoiceList>
          {CARDIO_FIELDS.map((f, i) => (
            <ChoiceRow
              key={f}
              title={fieldLabel(f)}
              selected={fields.includes(f)}
              last={i === CARDIO_FIELDS.length - 1}
              onPress={() => set({ fields: toggleIn(draft.fields, f) })}
            />
          ))}
        </ChoiceList>
      </Sheet>
      <Sheet visible={sheet === 'merge'} title={`Merge ${draft.name} into`} onDismiss={close}>
        <ChoiceList>
          {mergeTargets.map((e, i) => (
            <ChoiceRow
              key={e.id}
              title={e.name}
              selected={false}
              last={i === mergeTargets.length - 1}
              onPress={async () => {
                close();
                await repo.mergeExercise(draft.id, e.id);
                router.back();
              }}
            />
          ))}
        </ChoiceList>
      </Sheet>
    </EditorScreen>
  );
}
