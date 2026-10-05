import { router, useLocalSearchParams } from 'expo-router';
import { ExerciseForm } from '@/components/editors/ExerciseForm';
import { EditorScreen } from '@/components/layout';
import { useExerciseUsage, useLibrary } from '@/db/hooks';
import * as repo from '@/db/repository';

/** Exercise editor route: `new` or an exercise id. Saving, archiving, merging and deleting go back to the library. */
export default function ExerciseEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const isNew = id === 'new';
  const { data } = useLibrary();
  const usage = useExerciseUsage(isNew ? null : id);
  const original = data?.exercises.find((e) => e.id === id) ?? null;
  if (!data || (!isNew && !original)) return <EditorScreen />;

  return (
    <ExerciseForm
      key={id}
      original={original}
      exercises={data.exercises}
      modalities={data.modalities}
      usage={usage}
      onSave={(draft) => {
        const name = draft.name.trim();
        if (name) repo.saveExercise({ ...draft, name });
        router.back();
      }}
      onArchive={async (draft) => {
        await repo.saveExercise({ ...draft, archived: !draft.archived });
        router.back();
      }}
      onMerge={async (from, into) => {
        await repo.mergeExercise(from, into);
        router.back();
      }}
      onDelete={async (exerciseId) => {
        await repo.deleteExercise(exerciseId);
        router.back();
      }}
    />
  );
}
