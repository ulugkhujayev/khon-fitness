import { router, useLocalSearchParams } from 'expo-router';
import { ModalityForm } from '@/components/editors/ModalityForm';
import { EditorScreen } from '@/components/layout';
import { useModalities } from '@/db/hooks';
import * as repo from '@/db/repository';

/** Modality editor route: `new` or a modality id. */
export default function ModalityEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data } = useModalities();
  const original = data?.modalities.find((m) => m.id === id) ?? null;
  if (!data || (id !== 'new' && !original)) return <EditorScreen />;

  return (
    <ModalityForm
      key={id}
      original={original}
      onSave={(draft) => {
        const name = draft.name.trim();
        if (name) repo.saveModality({ ...draft, name });
        router.back();
      }}
      onArchive={async (draft) => {
        await repo.saveModality({ ...draft, archived: !draft.archived });
        router.back();
      }}
    />
  );
}
