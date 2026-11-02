import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import { DemoPoster } from '@/components/stretch/DemoPoster';
import { BarCircleButton, GroupedList, ListRow, SearchEmpty, SearchField } from '@/components/ui';
import { searchByName } from '@/domain/editors';
import { stretchLine } from '@/domain/stretch';
import { useStretching } from '@/db/hooks';

/** Stretch library (StretchLibraryScreen in StretchEditors.kt): search, each stretch with its pose thumbnail. */
export default function StretchLibrary() {
  const { data } = useStretching();
  const [query, setQuery] = useState('');
  const list = data ? searchByName(data.stretches.filter((s) => !s.archived), query) : [];

  return (
    <EditorScreen
      title="Stretches"
      trailing={
        <BarCircleButton
          icon={{ ios: 'plus', android: 'add', web: 'add' }}
          label="New stretch"
          onPress={() => router.push({ pathname: '/stretch-editor/[id]', params: { id: 'new' } })}
        />
      }>
      <SearchField value={query} onChange={setQuery} />
      {data ? (
        <GroupedList style={styles.list}>
          {list.map((st, i) => (
            <ListRow
              key={st.id}
              title={st.name}
              value={stretchLine(st)}
              trailing={<DemoPoster figure={st.figure} />}
              last={i === list.length - 1}
              onPress={() => router.push({ pathname: '/stretch-editor/[id]', params: { id: st.id } })}
            />
          ))}
          {list.length === 0 ? <SearchEmpty query={query} /> : null}
        </GroupedList>
      ) : null}
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  list: { marginTop: 16 },
});
