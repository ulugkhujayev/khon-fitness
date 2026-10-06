import { useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { BlockExerciseGroup } from '@/components/editors/BlockExerciseGroup';
import { EditorScreen } from '@/components/layout';
import { Capsule, ChoiceSheet, ConfirmSheet, GroupedList, GroupFooter, TintedButton, ToggleRow } from '@/components/ui';
import { freeSlot, moveInOrder } from '@/domain/editors';
import { Defaults, Kind } from '@/domain/types';
import { useBlock } from '@/db/hooks';
import { newId } from '@/db/id';
import * as repo from '@/db/repository';
import { K } from '@/theme';

/** Block editor (BlockEditorScreen in Editors.kt): superset and what it does, one or two exercises with sets and rep range, order, delete. */
export default function BlockEditor() {
  const { programId, blockId } = useLocalSearchParams<{ programId: string; blockId: string }>();
  const { data } = useBlock(programId, blockId);
  // The slot being filled stays set while the picker slides out.
  const [slot, setSlot] = useState(0);
  const [picking, setPicking] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const index = data ? data.siblings.findIndex((b) => b.id === blockId) : -1;
  const block = data?.siblings[index];
  if (!data || !block) return <EditorScreen />;

  const existing = data.mine.find((be) => be.slot === slot);
  const strength = data.exercises.filter((e) => e.kind === Kind.STRENGTH && !e.archived);
  const free = freeSlot(data.mine);

  const choose = (s: number) => {
    setSlot(s);
    setPicking(true);
  };

  const pick = (exerciseId: string) => {
    if (existing) repo.patchBlockExercise(existing.id, { exerciseId });
    else repo.saveBlockExercise({ ...Defaults.blockExercise, id: newId(), blockId, slot, exerciseId });
    setPicking(false);
  };

  return (
    <EditorScreen title={`Block ${index + 1}`} trailing={<Capsule text="Done" onPress={() => router.back()} />}>
      <GroupedList>
        <ToggleRow label="Superset" divider={false} value={block.superset} onChange={(superset) => repo.patchBlock(blockId, { superset })} />
      </GroupedList>
      <GroupFooter text={block.superset ? 'A and B alternate, set by set: A1, B1, A2, B2…' : 'All sets of A, then all sets of B.'} />
      {data.mine.map((be) => (
        <BlockExerciseGroup
          key={be.id}
          be={be}
          exerciseName={data.exercises.find((e) => e.id === be.exerciseId)?.name ?? null}
          onChoose={() => choose(be.slot)}
          onRemove={() => repo.deleteBlockExercise(be.id)}
          onChange={(patch) => repo.patchBlockExercise(be.id, patch)}
        />
      ))}
      {free != null ? (
        <TintedButton text={data.mine.length === 0 ? 'Add exercise' : 'Add second exercise'} onPress={() => choose(free)} style={styles.add} />
      ) : null}
      {data.siblings.length > 1 ? (
        <View style={styles.move}>
          {index > 0 ? <TintedButton text="Move up" onPress={() => repo.saveBlocks(moveInOrder(data.siblings, index, -1))} style={styles.moveButton} /> : null}
          {index < data.siblings.length - 1 ? (
            <TintedButton text="Move down" onPress={() => repo.saveBlocks(moveInOrder(data.siblings, index, 1))} style={styles.moveButton} />
          ) : null}
        </View>
      ) : null}
      <TintedButton text="Delete block" color={K.Red} onPress={() => setConfirmDelete(true)} style={styles.delete} />
      <ChoiceSheet
        visible={picking}
        title="Exercise"
        choices={strength.map((e) => ({ id: e.id, title: e.name }))}
        isSelected={(id) => existing?.exerciseId === id}
        onPick={pick}
        onDismiss={() => setPicking(false)}
        searchable
      />
      <ConfirmSheet
        visible={confirmDelete}
        title={`Delete block ${index + 1}?`}
        body="Its exercises go with it. Past sessions keep their sets."
        action="Delete"
        onDismiss={() => setConfirmDelete(false)}
        onConfirm={async () => {
          setConfirmDelete(false);
          await repo.deleteBlock(blockId);
          router.back();
        }}
      />
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  add: { marginTop: 20 },
  move: { flexDirection: 'row', gap: 12, marginTop: 32 },
  moveButton: { flex: 1 },
  delete: { marginTop: 12 },
});
