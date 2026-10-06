import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { EditorScreen, useLeaveWhenGone } from '@/components/layout';
import { RoutineOrderList } from '@/components/stretch/RoutineOrderList';
import { useConfirm } from '@/components/ui/useConfirm';
import {
  ACTION_ICONS, ActionRow, BottomActions, Capsule, ChoiceSheet, ConfirmSheet, Empty, FieldRow, GroupedList, InlineTextField, NumberRow, PlusButton, SectionTitle,
  ToggleRow,
} from '@/components/ui';
import { moveInOrder, nextSortOrder } from '@/domain/editors';
import { clampStretchSeconds, entrySeconds } from '@/domain/stretch';
import { RoutineStretch } from '@/domain/types';
import { useStretching } from '@/db/hooks';
import { newId } from '@/db/id';
import * as repo from '@/db/repository';
import { K } from '@/theme';

/** Routine editor (RoutineEditorScreen in StretchEditors.kt): name, active, the stretches with their seconds, order. Edits save live. */
export default function RoutineEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data } = useStretching();
  const [picking, setPicking] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const removal = useConfirm<RoutineStretch>();
  const routine = data?.routines.find((r) => r.id === id) ?? null;
  const leave = useLeaveWhenGone(data != null && routine == null);
  if (!data || !routine) return <EditorScreen />;

  const mine = data.routineStretches.filter((rs) => rs.routineId === id).sort((a, b) => a.sortOrder - b.sortOrder);
  const library = data.stretches.filter((s) => !s.archived);

  return (
    <EditorScreen title={routine.name} trailing={<Capsule text="Done" onPress={() => router.back()} />}>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={routine.name} onChange={(name) => repo.patchRoutine(id, { name })} label="Routine name" />
        </FieldRow>
        <ToggleRow label="Active" value={routine.active} onChange={(on) => repo.setRoutineActive(id, on)} />
      </GroupedList>
      <SectionTitle text="Stretches" trailing={<PlusButton label="Add a stretch" onPress={() => setPicking(true)} />} style={styles.section} />
      <GroupedList>
        {mine.map((rs, i) => {
          const st = data.stretches.find((s) => s.id === rs.stretchId);
          return (
            <NumberRow
              key={rs.id}
              label={st?.name ?? 'Missing stretch'}
              note={st?.sided ? 'per side' : undefined}
              value={entrySeconds(rs, st)}
              unit="s"
              step={5}
              decimals={0}
              divider={i > 0}
              onChange={(v) => repo.patchRoutineStretch(rs.id, { seconds: clampStretchSeconds(v) })}
            />
          );
        })}
        {mine.length === 0 ? <Empty text="No stretches yet" /> : null}
      </GroupedList>
      {mine.length > 0 ? (
        <>
          <SectionTitle text="Order" style={styles.section} />
          <RoutineOrderList
            entries={mine}
            stretches={data.stretches}
            onMove={(i, dir) => repo.saveRoutineStretches(moveInOrder(mine, i, dir))}
            onRemove={removal.ask}
          />
        </>
      ) : null}
      <BottomActions>
        <GroupedList>
          <ActionRow icon={ACTION_ICONS.delete} title="Delete routine" color={K.Red} last onPress={() => setConfirming(true)} />
        </GroupedList>
      </BottomActions>
      <ChoiceSheet
        visible={picking}
        title="Add a stretch"
        choices={library.map((s) => ({ id: s.id, title: s.name }))}
        isSelected={(stretchId) => mine.some((rs) => rs.stretchId === stretchId)}
        onPick={(stretchId) => {
          repo.saveRoutineStretches([{ id: newId(), routineId: id, stretchId, sortOrder: nextSortOrder(mine), seconds: null }]);
          setPicking(false);
        }}
        onDismiss={() => setPicking(false)}
        searchable
      />
      <ConfirmSheet
        visible={removal.open}
        title={`Remove ${data.stretches.find((st) => st.id === removal.item?.stretchId)?.name ?? 'this stretch'}?`}
        body="It leaves this routine; the stretch stays in the library."
        action="Remove"
        onDismiss={removal.close}
        onConfirm={() => {
          removal.close();
          if (removal.item) repo.deleteRoutineStretch(removal.item.id);
        }}
      />
      <ConfirmSheet
        visible={confirming}
        title="Delete this routine?"
        body="Its stretch history stays."
        action="Delete"
        onDismiss={() => setConfirming(false)}
        onConfirm={async () => {
          setConfirming(false);
          await repo.deleteRoutine(id);
          leave();
        }}
      />
    </EditorScreen>
  );
}

const styles = StyleSheet.create({
  section: { marginTop: 20 },
});
