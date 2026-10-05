import { useState } from 'react';
import { router, useLocalSearchParams } from 'expo-router';
import { EditorScreen } from '@/components/EditorScreen';
import { ConfirmSheet } from '@/components/Sheet';
import { BottomActions, Capsule, Empty, FieldRow, GroupedList, InlineTextField, ListRow, PlusButton, SectionTitle, TextButton, Toggle } from '@/components/ui';
import { sets } from '@/domain/format';
import * as repo from '@/db/repository';
import { newId } from '@/db/id';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K } from '@/theme';

/** Program editor (ProgramEditorScreen in Editors.kt): name, active, the blocks in order. */
export default function ProgramEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data } = useLive(
    async () => {
      const [programs, blocks, blockExercises, exercises] = await Promise.all([
        repo.programs(), repo.blocksFor(id), repo.blockExercises(), repo.exercises(),
      ]);
      return { programs, program: programs.find((p) => p.id === id) ?? null, blocks, blockExercises, exercises };
    },
    [T.programs, T.blocks, T.blockExercises, T.exercises],
    [id],
  );
  const [name, setName] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const program = data?.program;

  if (!data || !program) return <EditorScreen title="">{null}</EditorScreen>;

  const addBlock = async () => {
    const blockId = newId();
    // After a delete the orders have gaps, so a count could collide with an existing block.
    const sortOrder = data.blocks.reduce((m, b) => Math.max(m, b.sortOrder + 1), 0);
    await repo.saveBlock({ id: blockId, programId: id, sortOrder, superset: true });
    router.push(`/block/${id}/${blockId}`);
  };

  return (
    <EditorScreen
      title={program.name}
      trailing={
        <Capsule
          text="Done"
          onPress={() => {
            const n = (name ?? '').trim();
            if (n && n !== program.name) repo.patchProgram(id, { name: n });
            router.back();
          }}
        />
      }>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={name ?? program.name} onChange={setName} placeholder="Program name" label="Name" />
        </FieldRow>
        <FieldRow label="Active">
          <Toggle value={program.active} onChange={(on) => on && repo.setActiveProgram(id)} label="Active" />
        </FieldRow>
      </GroupedList>

      <SectionTitle text="Blocks" style={{ marginTop: 24 }} trailing={<PlusButton label="Add block" onPress={addBlock} />} />
      <GroupedList>
        {data.blocks.map((b, i) => {
          const mine = data.blockExercises.filter((be) => be.blockId === b.id).sort((x, y) => x.slot - y.slot);
          const names = mine.map((be) => data.exercises.find((e) => e.id === be.exerciseId)?.name).filter(Boolean);
          return (
            <ListRow
              key={b.id}
              title={names.join(' · ') || 'Empty block'}
              value={sets(mine.reduce((n, be) => n + be.sets, 0))}
              last={i === data.blocks.length - 1}
              onPress={() => router.push(`/block/${id}/${b.id}`)}
            />
          );
        })}
        {data.blocks.length === 0 ? <Empty text="No blocks yet" /> : null}
      </GroupedList>

      {data.programs.length > 1 ? (
        <BottomActions>
          <TextButton text="Delete program" color={K.Red} onPress={() => setConfirmDelete(true)} />
        </BottomActions>
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
