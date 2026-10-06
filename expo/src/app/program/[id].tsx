import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { useDropOnLeave } from '@/components/editors/useDropOnLeave';
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
 * it first so the block has a program to belong to. Saved that way it stays provisional (`draft`): left
 * without Done and without a block, it is deleted again, and its Active choice (`activate`) waits for Done,
 * so the program active before keeps the mark until then.
 */
export default function ProgramEditor() {
  const { id, draft, activate } = useLocalSearchParams<{ id: string; draft?: string; activate?: string }>();
  const isNew = id === 'new';
  const provisional = draft === '1';
  // A choice not saved yet: a new program's, or a provisional one's until Done.
  const pending = isNew || provisional;
  const { data } = useProgram(id);
  const { keep } = useDropOnLeave(provisional, (data?.blocks.length ?? 0) > 0, () => repo.deleteProgram(id));
  // Null until edited: Done saves only a changed name; a draft keeps its active choice here.
  const [name, setName] = useState<string | null>(null);
  const [draftActive, setDraftActive] = useState<boolean | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const program = data ? (isNew ? newProgram(id, data.programCount) : data.program) : null;
  if (!data || !program) return <EditorScreen />;
  const active = draftActive ?? (program.active || activate === '1');

  const create = async (makeActive: boolean) => {
    const programId = newId();
    await repo.createProgram({ ...program, id: programId, name: name?.trim() || program.name, active: makeActive });
    return programId;
  };

  const addBlock = async () => {
    if (!isNew) return router.push(`/block/${id}/new`);
    // Active only if it is the first program; a chosen Active waits for Done.
    const programId = await create(program.active);
    router.replace({ pathname: '/program/[id]', params: { id: programId, draft: '1', activate: active ? '1' : '0' } });
    router.push(`/block/${programId}/new`);
  };

  const done = async () => {
    const n = name?.trim();
    keep();
    if (isNew) await create(active);
    else {
      if (n && n !== program.name) repo.patchProgram(id, { name: n });
      if (provisional && active && !program.active) repo.setActiveProgram(id);
    }
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
          disabled={!pending && program.active}
          onChange={(on) => (pending ? setDraftActive(on) : on && repo.setActiveProgram(id))}
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

      {/* Nothing to delete before Done; the last program cannot go, Today always plans from one. */}
      {!isNew && !provisional && data.programCount > 1 ? (
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
