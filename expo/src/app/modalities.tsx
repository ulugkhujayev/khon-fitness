import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import { BarCircleButton, GroupedList, ListRow, ToggleRow } from '@/components/ui';
import { argbToHex } from '@/domain/types';
import { useModalities } from '@/db/hooks';
import { K } from '@/theme';

/** Modalities with their colors (ModalitiesScreen in ProgramsScreen.kt). */
export default function Modalities() {
  const { data } = useModalities();
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
      <GroupedList style={styles.archived}>
        <ToggleRow label="Show archived" divider={false} value={showArchived} onChange={setShowArchived} />
      </GroupedList>
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  archived: { marginTop: 16 },
});
