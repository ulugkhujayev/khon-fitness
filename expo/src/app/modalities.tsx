import { useState } from 'react';
import { router } from 'expo-router';
import { EditorScreen } from '@/components/EditorScreen';
import { BarCircleButton, FieldRow, GroupedList, ListRow, Toggle } from '@/components/ui';
import { argbToHex } from '@/domain/types';
import * as repo from '@/db/repository';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K } from '@/theme';

/** Modalities with their colors (ModalitiesScreen in ProgramsScreen.kt). */
export default function Modalities() {
  const { data } = useLive(() => repo.modalities(), [T.modalities]);
  const [showArchived, setShowArchived] = useState(false);
  const list = (data ?? []).filter((m) => showArchived || !m.archived);
  return (
    <EditorScreen
      title="Modalities"
      trailing={<BarCircleButton icon={{ ios: 'plus', android: 'add', web: 'add' }} label="New modality" onPress={() => router.push('/modality/new')} />}>
      <GroupedList>
        {list.map((m, i) => (
          <ListRow
            key={m.id}
            title={m.name}
            dot={argbToHex(m.color)}
            color={m.archived ? K.Dim : K.Text}
            last={i === list.length - 1}
            onPress={() => router.push(`/modality/${m.id}`)}
          />
        ))}
      </GroupedList>
      <GroupedList style={{ marginTop: 16 }}>
        <FieldRow label="Show archived" divider={false}>
          <Toggle value={showArchived} onChange={setShowArchived} label="Show archived" />
        </FieldRow>
      </GroupedList>
    </EditorScreen>
  );
}
