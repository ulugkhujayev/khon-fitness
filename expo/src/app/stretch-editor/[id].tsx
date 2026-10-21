import { router, useLocalSearchParams } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import { StretchForm } from '@/components/stretch/StretchForm';
import { useStretching, useStretchUseCount } from '@/db/hooks';
import * as repo from '@/db/repository';

/** Stretch editor route: `new` or a stretch id. Saving and deleting go back to the library. */
export default function StretchEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const isNew = id === 'new';
  const { data } = useStretching();
  const useCount = useStretchUseCount(isNew ? null : id);
  const original = data?.stretches.find((s) => s.id === id) ?? null;
  if (!data || (!isNew && !original)) return <EditorScreen />;

  return (
    <StretchForm
      key={id}
      original={original}
      useCount={useCount}
      onSave={(draft) => {
        const name = draft.name.trim();
        if (name) repo.saveStretch({ ...draft, name });
        router.back();
      }}
      onDelete={async (stretchId) => {
        await repo.deleteStretch(stretchId);
        router.back();
      }}
    />
  );
}
