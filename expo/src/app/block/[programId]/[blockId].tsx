import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { BlockExerciseGroup } from '@/components/editors/BlockExerciseGroup';
import { EditorScreen } from '@/components/layout';
import { ACTION_ICONS, ActionRow, BottomActions, Capsule, ChoiceSheet, ConfirmSheet, GroupedList, GroupFooter, TintedButton, ToggleRow } from '@/components/ui';
import { freeSlot, moveInOrder, nextSortOrder } from '@/domain/editors';
import { Defaults, Kind } from '@/domain/types';
import { useBlock } from '@/db/hooks';
import { newId } from '@/db/id';
import * as repo from '@/db/repository';
import { K } from '@/theme';

/**
 * Block editor (BlockEditorScreen in Editors.kt): superset and what it does, one or two exercises with sets
 * and rep range, order, delete. A new block (`new`) is a draft until its first exercise is picked, which
 * saves the two together, so backing out of it leaves no empty block behind.
 */
export default function BlockEditor() {
  const { programId, blockId } = useLocalSearchParams<{ programId: string; blockId: string }>();
  const isNew = blockId === 'new';
  const { data } = useBlock(programId, blockId);
  // The slot being filled stays set while the picker slides out.
  const [slot, setSlot] = useState(0);
  const [picking, setPicking] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [draftSuperset, setDraftSuperset] = useState<boolean>(Defaults.block.superset);

  const index = data ? (isNew ? data.siblings.length : data.siblings.findIndex((b) => b.id === blockId)) : -1;
  const block = data ? (isNew ? { id: blockId, programId, sortOrder: nextSortOrder(data.siblings), superset: draftSuperset } : data.siblings[index]) : undefined;
  if (!data || !block) return <EditorScreen />;

  const existing = data.mine.find((be) => be.slot === slot);
  const strength = data.exercises.filter((e) => e.kind === Kind.STRENGTH && !e.archived);
  const free = freeSlot(data.mine);

  const choose = (s: number) => {
    setSlot(s);
    setPicking(true);
  };

  const pick = (exerciseId: string) => {
    setPicking(false);
    if (isNew) {
      const savedId = newId();
      repo.createBlock({ ...block, id: savedId }, { ...Defaults.blockExercise, id: newId(), blockId: savedId, slot, exerciseId });
      router.replace(`/block/${programId}/${savedId}`);
      return;
    }
    if (existing) repo.patchBlockExercise(existing.id, { exerciseId });
    else repo.saveBlockExercise({ ...Defaults.blockExercise, id: newId(), blockId, slot, exerciseId });
  };

  return (
    <EditorScreen title={`Block ${index + 1}`} trailing={<Capsule text="Done" onPress={() => router.back()} />}>
      <GroupedList>
        <ToggleRow
          label="Superset"
          divider={false}
          value={block.superset}
          onChange={(superset) => (isNew ? setDraftSuperset(superset) : repo.patchBlock(blockId, { superset }))}
        />
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
        <TintedButton text={data.mine.length === 0 ? 'Add exercise' : 'Add second exercise'} onPress={() => choose(free)} style={styles.action} />
      ) : null}
      {/* Order and delete come once the block is saved. */}
      {!isNew ? (
        <BottomActions>
          {data.siblings.length > 1 ? (
            <GroupedList>
              {index > 0 ? (
                <ActionRow
                  icon={ACTION_ICONS.up}
                  title="Move up"
                  last={index === data.siblings.length - 1}
                  onPress={() => repo.saveBlocks(moveInOrder(data.siblings, index, -1))}
                />
              ) : null}
              {index < data.siblings.length - 1 ? (
                <ActionRow icon={ACTION_ICONS.down} title="Move down" last onPress={() => repo.saveBlocks(moveInOrder(data.siblings, index, 1))} />
              ) : null}
            </GroupedList>
          ) : null}
          <GroupedList>
            <ActionRow icon={ACTION_ICONS.delete} title="Delete block" color={K.Red} last onPress={() => setConfirmDelete(true)} />
          </GroupedList>
        </BottomActions>
      ) : null}
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
  action: { marginTop: 12 },
});
