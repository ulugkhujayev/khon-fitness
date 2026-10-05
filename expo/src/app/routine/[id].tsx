import { useState } from 'react';
import { StyleSheet } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { EditorScreen, useLeaveWhenGone } from '@/components/layout';
import { RoutineOrderList } from '@/components/stretch/RoutineOrderList';
import {
  BottomActions, Capsule, ChoiceSheet, ConfirmSheet, Empty, FieldRow, GroupedList, InlineTextField, NumberRow, PlusButton, SectionTitle, TextButton,
  ToggleRow,
} from '@/components/ui';
import { moveInOrder, nextSortOrder } from '@/domain/editors';
import { clampStretchSeconds, entrySeconds } from '@/domain/stretch';
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
            onRemove={(rs) => repo.deleteRoutineStretch(rs.id)}
          />
        </>
      ) : null}
      <BottomActions>
        <TextButton text="Delete routine" color={K.Red} onPress={() => setConfirming(true)} />
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
