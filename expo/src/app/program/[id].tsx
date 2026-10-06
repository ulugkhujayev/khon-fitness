import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import { Capsule, ConfirmSheet, Empty, FieldRow, GroupedList, IndexBadge, InlineTextField, ListRow, SectionTitle, TintedButton, ToggleRow } from '@/components/ui';
import { blockSummary, nextSortOrder } from '@/domain/editors';
import { sets } from '@/domain/format';
import { useProgram } from '@/db/hooks';
import { newId } from '@/db/id';
import * as repo from '@/db/repository';
import { K } from '@/theme';

/** Program editor (ProgramEditorScreen in Editors.kt): name, active, the numbered blocks in order with Add block, Delete. */
export default function ProgramEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data } = useProgram(id);
  // Null until the name is edited; Done saves only a changed name.
  const [name, setName] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const program = data?.program;
  if (!data || !program) return <EditorScreen />;

  const addBlock = async () => {
    const blockId = newId();
    await repo.saveBlock({ id: blockId, programId: id, sortOrder: nextSortOrder(data.blocks), superset: true });
    router.push(`/block/${id}/${blockId}`);
  };

  const done = () => {
    const n = name?.trim();
    if (n && n !== program.name) repo.patchProgram(id, { name: n });
    router.back();
  };

  return (
    <EditorScreen title={program.name} trailing={<Capsule text="Done" onPress={done} />}>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={name ?? program.name} onChange={setName} placeholder="Program name" label="Name" />
        </FieldRow>
        {/* One program is always active: turning it on moves the mark here, turning it off does nothing. */}
        <ToggleRow label="Active" value={program.active} onChange={(on) => on && repo.setActiveProgram(id)} />
      </GroupedList>

      <SectionTitle text="Blocks" style={styles.blocks} />
      <GroupedList>
        {data.blocks.map((b, i) => {
          const summary = blockSummary(b.id, data.blockExercises, data.exercises);
          return (
            <ListRow
              key={b.id}
              leading={<IndexBadge index={i} />}
              title={summary.title || 'Empty block'}
              value={sets(summary.sets)}
              last={i === data.blocks.length - 1}
              onPress={() => router.push(`/block/${id}/${b.id}`)}
            />
          );
        })}
        {data.blocks.length === 0 ? <Empty text="No blocks yet" /> : null}
      </GroupedList>
      <TintedButton text="Add block" onPress={addBlock} style={styles.add} />

      {/* The last program cannot go: Today always has one to plan from. */}
      {data.programCount > 1 ? <TintedButton text="Delete program" color={K.Red} onPress={() => setConfirmDelete(true)} style={styles.delete} /> : null}

      <ConfirmSheet
        visible={confirmDelete}
        title={`Delete ${program.name}?`}
        body="The program and its blocks go away. Past sessions keep their sets."
        action="Delete"
        onDismiss={() => setConfirmDelete(false)}
        onConfirm={async () => {
          setConfirmDelete(false);
          await repo.deleteProgram(id);
          router.back();
        }}
      />
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  blocks: { marginTop: 24 },
  add: { marginTop: 12 },
  delete: { marginTop: 32 },
});
