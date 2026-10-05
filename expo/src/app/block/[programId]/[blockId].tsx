import { useState } from 'react';
import { View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { SearchField } from '@/components/controls';
import { EditorScreen } from '@/components/EditorScreen';
import { NumberRow } from '@/components/NumberField';
import { ChoiceList, ChoiceRow, Sheet } from '@/components/Sheet';
import { BottomActions, Capsule, FieldRow, FieldValue, GroupedList, SectionTitle, TextButton, Toggle } from '@/components/ui';
import { freeSlot, moveBlock, withMaxReps, withMinReps, withSets } from '@/domain/editors';
import { Defaults, Kind } from '@/domain/types';
import * as repo from '@/db/repository';
import { newId } from '@/db/id';
import { TABLES as T } from '@/db/schema';
import { useLive } from '@/db/useLive';
import { K } from '@/theme';

/** Block editor (BlockEditorScreen in Editors.kt): superset, one or two exercises with sets and rep range, order. */
export default function BlockEditor() {
  const { programId, blockId } = useLocalSearchParams<{ programId: string; blockId: string }>();
  const { data } = useLive(
    async () => {
      const [siblings, mine, exercises] = await Promise.all([repo.blocksFor(programId), repo.blockExercisesFor(blockId), repo.exercises()]);
      return { siblings, mine, exercises };
    },
    [T.blocks, T.blockExercises, T.exercises],
    [programId, blockId],
  );
  const [picking, setPicking] = useState<number | null>(null);
  const [pickOpen, setPickOpen] = useState(false);
  const [query, setQuery] = useState('');

  const index = data ? data.siblings.findIndex((b) => b.id === blockId) : -1;
  const block = data?.siblings[index];
  if (!data || !block) return <EditorScreen title="">{null}</EditorScreen>;

  const existing = picking == null ? undefined : data.mine.find((be) => be.slot === picking);
  const q = query.trim().toLowerCase();
  const choices = data.exercises.filter((e) => e.kind === Kind.STRENGTH && !e.archived && (!q || e.name.toLowerCase().includes(q)));
  const slot = freeSlot(data.mine);

  const pick = (exerciseId: string) => {
    if (picking == null) return;
    if (existing) repo.patchBlockExercise(existing.id, { exerciseId });
    else repo.saveBlockExercise({ ...Defaults.blockExercise, id: newId(), blockId, slot: picking, exerciseId });
    setPickOpen(false);
  };

  return (
    <EditorScreen title={`Block ${index + 1}`} trailing={<Capsule text="Done" onPress={() => router.back()} />}>
      <GroupedList>
        <FieldRow label="Superset" divider={false}>
          <Toggle value={block.superset} onChange={(superset) => repo.patchBlock(blockId, { superset })} label="Superset" />
        </FieldRow>
      </GroupedList>

      {data.mine.map((be) => {
        const ex = data.exercises.find((e) => e.id === be.exerciseId);
        const letter = String.fromCharCode(65 + be.slot);
        return (
          <View key={be.id}>
            <SectionTitle
              text={letter}
              style={{ marginTop: 20 }}
              trailing={<TextButton text="Remove" color={K.Red} label={`Remove ${letter}`} onPress={() => repo.deleteBlockExercise(be.id)} />}
            />
            <GroupedList>
              <FieldRow
                label="Exercise"
                divider={false}
                onPress={() => {
                  setQuery('');
                  setPicking(be.slot);
                  setPickOpen(true);
                }}>
                <FieldValue text={ex?.name ?? 'Choose'} />
              </FieldRow>
              <NumberRow label="Sets" value={be.sets} unit="" step={1} decimals={0} onChange={(v) => repo.patchBlockExercise(be.id, { sets: withSets(be, v).sets })} />
              <NumberRow
                label="Min reps"
                value={be.minReps}
                unit=""
                step={1}
                decimals={0}
                onChange={(v) => {
                  const { minReps, maxReps } = withMinReps(be, v);
                  repo.patchBlockExercise(be.id, { minReps, maxReps });
                }}
              />
              <NumberRow
                label="Max reps"
                value={be.maxReps}
                unit=""
                step={1}
                decimals={0}
                onChange={(v) => {
                  const { minReps, maxReps } = withMaxReps(be, v);
                  repo.patchBlockExercise(be.id, { minReps, maxReps });
                }}
              />
            </GroupedList>
          </View>
        );
      })}

      {slot != null ? (
        <View style={{ flexDirection: 'row', marginTop: 16 }}>
          <TextButton
            text={data.mine.length === 0 ? 'Add exercise' : 'Add second exercise'}
            onPress={() => {
              setQuery('');
              setPicking(slot);
              setPickOpen(true);
            }}
          />
        </View>
      ) : null}

      <BottomActions>
        {index > 0 ? <TextButton text="Move up" onPress={() => repo.saveBlocks(moveBlock(data.siblings, index, -1))} /> : null}
        {index < data.siblings.length - 1 ? <TextButton text="Move down" onPress={() => repo.saveBlocks(moveBlock(data.siblings, index, 1))} /> : null}
        <TextButton
          text="Delete block"
          color={K.Red}
          onPress={async () => {
            await repo.deleteBlock(blockId);
            router.back();
          }}
        />
      </BottomActions>

      <Sheet visible={pickOpen} title="Exercise" onDismiss={() => setPickOpen(false)}>
        <SearchField value={query} onChange={setQuery} elevated />
        <View style={{ height: 12 }} />
        <ChoiceList>
          {choices.map((e, i) => (
            <ChoiceRow key={e.id} title={e.name} selected={existing?.exerciseId === e.id} last={i === choices.length - 1} onPress={() => pick(e.id)} />
          ))}
        </ChoiceList>
      </Sheet>
    </EditorScreen>
  );
}
