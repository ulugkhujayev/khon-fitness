import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { EditorScreen } from '@/components/layout';
import {
  Capsule, ConfirmSheet, Empty, FieldRow, GroupedList, GroupFooter, IndexBadge, InlineTextField, ListRow, SectionTitle, TintedButton, ToggleRow,
} from '@/components/ui';
import { blockSummary, newProgram } from '@/domain/editors';
import { sets } from '@/domain/format';
import { useProgram } from '@/db/hooks';
import { newId } from '@/db/id';
import * as repo from '@/db/repository';
import { K } from '@/theme';

/**
 * Program editor (ProgramEditorScreen in Editors.kt): name, active, the numbered blocks in order with Add
 * block, Delete. A new program (`new`) is a draft: nothing is saved until Done, or Add block, which saves
 * it first so the block has a program to belong to.
 */
export default function ProgramEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const isNew = id === 'new';
  const { data } = useProgram(id);
  // Null until edited: Done saves only a changed name; a draft keeps its active choice here.
  const [name, setName] = useState<string | null>(null);
  const [draftActive, setDraftActive] = useState<boolean | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const program = data ? (isNew ? newProgram(id, data.programCount) : data.program) : null;
  if (!data || !program) return <EditorScreen />;
  const active = draftActive ?? program.active;

  const create = async () => {
    const programId = newId();
    await repo.createProgram({ ...program, id: programId, name: name?.trim() || program.name, active });
    return programId;
  };

  const addBlock = async () => {
    if (!isNew) return router.push(`/block/${id}/new`);
    const programId = await create();
    router.replace(`/program/${programId}`);
    router.push(`/block/${programId}/new`);
  };

  const done = async () => {
    const n = name?.trim();
    if (isNew) await create();
    else if (n && n !== program.name) repo.patchProgram(id, { name: n });
    router.back();
  };

  return (
    <EditorScreen title={isNew ? 'New program' : program.name} trailing={<Capsule text="Done" onPress={done} />}>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={name ?? program.name} onChange={setName} placeholder="Program name" label="Name" />
        </FieldRow>
        {/* A saved active program stays on: turning another one on is what moves the mark. */}
        <ToggleRow
          label="Active"
          value={active}
          disabled={!isNew && program.active}
          onChange={(on) => (isNew ? setDraftActive(on) : on && repo.setActiveProgram(id))}
        />
      </GroupedList>
      <GroupFooter text="One program is active: the one you train now. Turning another one on moves it there." />

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
      <TintedButton text="Add block" onPress={addBlock} style={styles.action} />

      {/* Nothing to delete before the first save; the last program cannot go, Today always plans from one. */}
      {!isNew && data.programCount > 1 ? (
        <TintedButton text="Delete program" color={K.Red} onPress={() => setConfirmDelete(true)} style={styles.action} />
      ) : null}

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
  action: { marginTop: 12 },
});
