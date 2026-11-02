import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import { BarCircleButton, Empty, GroupedList, GroupFooter, ListRow, SectionTitle, ToggleRow } from '@/components/ui';
import { modalityUse } from '@/domain/editors';
import { argbToHex, Modality } from '@/domain/types';
import { useModalities } from '@/db/hooks';
import { K } from '@/theme';

/**
 * Modalities (ModalitiesScreen in ProgramsScreen.kt): each with its color and how many exercises it groups,
 * a note on what they are for, and the archived ones in their own group when shown.
 */
export default function Modalities() {
  const { data } = useModalities();
  const [showArchived, setShowArchived] = useState(false);
  const all = data?.modalities ?? [];
  const exercises = data?.exercises ?? [];
  const active = all.filter((m) => !m.archived);
  const archived = all.filter((m) => m.archived);

  const row = (m: Modality, i: number, list: Modality[]) => (
    <ListRow
      key={m.id}
      title={m.name}
      value={modalityUse(m.id, exercises)}
      dot={argbToHex(m.color)}
      color={m.archived ? K.Dim : K.Text}
      last={i === list.length - 1}
      onPress={() => router.push(`/modality/${m.id}`)}
    />
  );

  return (
    <EditorScreen
      title="Modalities"
      trailing={<BarCircleButton icon={{ ios: 'plus', android: 'add', web: 'add' }} label="New modality" onPress={() => router.push('/modality/new')} />}>
      <GroupedList>
        {active.map((m, i) => row(m, i, active))}
        {data && active.length === 0 ? <Empty text="No modalities yet" /> : null}
      </GroupedList>
      <GroupFooter text="A modality groups cardio exercises, such as running or cycling, and colors them on Today and in History." />
      <GroupedList style={styles.toggle}>
        <ToggleRow label="Show archived" divider={false} value={showArchived} onChange={setShowArchived} />
      </GroupedList>
      {showArchived && archived.length > 0 ? (
        <>
          <SectionTitle text="Archived" style={styles.section} />
          <GroupedList>{archived.map((m, i) => row(m, i, archived))}</GroupedList>
        </>
      ) : null}
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  toggle: { marginTop: 24 },
  section: { marginTop: 24 },
});
