import { useState } from 'react';
import { EditorScreen } from '@/components/layout';
import { BottomActions, Capsule, ChoiceSheet, TintedButton } from '@/components/ui';
import { newExercise } from '@/domain/editors';
import { Exercise, Modality } from '@/domain/types';
import { newId } from '@/db/id';
import { K } from '@/theme';
import { ExerciseFields } from './ExerciseFields';
import { ExerciseSelect, selectConfig } from './exerciseChoices';

/**
 * Exercise editor (ExerciseEditorScreen in Editors.kt). Edits a draft; Done hands it back.
 * Archive, merge and delete live at the bottom; delete only while nothing uses the exercise.
 */
export function ExerciseForm({
  original,
  exercises,
  modalities,
  usage,
  onSave,
  onArchive,
  onMerge,
  onDelete,
}: {
  /** Null for a new exercise. */
  original: Exercise | null;
  exercises: Exercise[];
  modalities: Modality[];
  /** Sessions, program blocks and plan days that use it. */
  usage: number;
  onSave: (draft: Exercise) => void;
  onArchive: (draft: Exercise) => void;
  onMerge: (fromId: string, intoId: string) => void;
  onDelete: (id: string) => void;
}) {
  const [draft, setDraft] = useState<Exercise>(() => original ?? newExercise(newId()));
  // The select stays set while its sheet slides out, so the sheet keeps its content.
  const [select, setSelect] = useState<ExerciseSelect>('kind');
  const [open, setOpen] = useState(false);
  const config = selectConfig(
    select,
    draft,
    modalities,
    exercises.filter((e) => e.id !== draft.id && e.kind === draft.kind),
  );

  const pick = (id: string) => {
    if (select === 'merge') onMerge(draft.id, id);
    else if (config.apply) setDraft(config.apply(draft, id));
    if (!config.multi) setOpen(false);
  };

  return (
    <EditorScreen title={original ? original.name : 'New exercise'} trailing={<Capsule text="Done" onPress={() => onSave(draft)} />}>
      <ExerciseFields
        draft={draft}
        modalities={modalities}
        kindLocked={usage > 0}
        onChange={setDraft}
        onOpen={(s) => {
          setSelect(s);
          setOpen(true);
        }}
      />
      {original ? (
        <BottomActions note={usage > 0 ? 'Used in a session, program, or plan: archive or merge it instead.' : undefined}>
          <TintedButton text={draft.archived ? 'Unarchive' : 'Archive'} onPress={() => onArchive(draft)} />
          <TintedButton
            text="Merge into…"
            onPress={() => {
              setSelect('merge');
              setOpen(true);
            }}
          />
          <TintedButton text="Delete" color={K.Red} enabled={usage === 0} onPress={() => onDelete(draft.id)} />
        </BottomActions>
      ) : null}
      <ChoiceSheet visible={open} title={config.title} choices={config.choices} isSelected={config.isSelected} onPick={pick} onDismiss={() => setOpen(false)} />
    </EditorScreen>
  );
}
