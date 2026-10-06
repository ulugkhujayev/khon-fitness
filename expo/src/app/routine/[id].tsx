import { useState } from 'react';
import { router, useLocalSearchParams } from 'expo-router';
import { EditorScreen, useLeaveWhenGone } from '@/components/layout';
import { RoutineStretches } from '@/components/stretch/RoutineStretches';
import { ACTION_ICONS, ActionRow, BottomActions, Capsule, ChoiceSheet, ConfirmSheet, FieldRow, GroupedList, GroupFooter, InlineTextField, ToggleRow } from '@/components/ui';
import { useConfirm } from '@/components/ui/useConfirm';
import { moveInOrder, nextSortOrder } from '@/domain/editors';
import { newRoutine } from '@/domain/stretch';
import { RoutineStretch } from '@/domain/types';
import { useStretching } from '@/db/hooks';
import { newId } from '@/db/id';
import * as repo from '@/db/repository';
import { K } from '@/theme';

/**
 * Routine editor (RoutineEditorScreen in StretchEditors.kt): name, active, the stretches with their seconds,
 * Add stretch, their order, delete. Edits save live. A new routine (`new`) is a draft until Done or its first
 * stretch saves it, so backing out leaves nothing behind.
 */
export default function RoutineEditor() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const isNew = id === 'new';
  const { data } = useStretching();
  const [picking, setPicking] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const removal = useConfirm<RoutineStretch>();
  // A draft's name and active choice; a saved routine writes them as they change.
  const [draftName, setDraftName] = useState<string | null>(null);
  const [draftActive, setDraftActive] = useState<boolean | null>(null);
  const saved = data?.routines.find((r) => r.id === id) ?? null;
  const leave = useLeaveWhenGone(!isNew && data != null && saved == null);
  const draft = data && isNew ? newRoutine(id, data.routines) : null;
  const routine = draft ? { ...draft, name: draftName ?? draft.name, active: draftActive ?? draft.active } : saved;
  if (!data || !routine) return <EditorScreen />;

  const mine = data.routineStretches.filter((rs) => rs.routineId === id).sort((a, b) => a.sortOrder - b.sortOrder);
  const library = data.stretches.filter((s) => !s.archived);

  const create = (first: string | null) => {
    const routineId = newId();
    const entry = first ? { id: newId(), routineId, stretchId: first, sortOrder: 0, seconds: null } : null;
    repo.createRoutine({ ...routine, id: routineId, name: routine.name.trim() || 'New routine' }, entry);
    return routineId;
  };

  const pick = (stretchId: string) => {
    setPicking(false);
    if (isNew) router.replace({ pathname: '/routine/[id]', params: { id: create(stretchId) } });
    else repo.saveRoutineStretches([{ id: newId(), routineId: id, stretchId, sortOrder: nextSortOrder(mine), seconds: null }]);
  };

  return (
    <EditorScreen
      title={isNew ? 'New routine' : routine.name}
      trailing={
        <Capsule
          text="Done"
          onPress={() => {
            if (isNew) create(null);
            router.back();
          }}
        />
      }>
      <GroupedList>
        <FieldRow label="Name" divider={false}>
          <InlineTextField value={routine.name} onChange={(name) => (isNew ? setDraftName(name) : repo.patchRoutine(id, { name }))} label="Routine name" />
        </FieldRow>
        <ToggleRow label="Active" value={routine.active} onChange={(on) => (isNew ? setDraftActive(on) : repo.setRoutineActive(id, on))} />
      </GroupedList>
      <GroupFooter text="The active routine is the one Today offers to stretch." />
      <RoutineStretches
        entries={mine}
        stretches={data.stretches}
        onSeconds={(rs, seconds) => repo.patchRoutineStretch(rs.id, { seconds })}
        onAdd={() => setPicking(true)}
        onMove={(i, dir) => repo.saveRoutineStretches(moveInOrder(mine, i, dir))}
        onRemove={removal.ask}
      />
      {!isNew ? (
        <BottomActions>
          <GroupedList>
            <ActionRow icon={ACTION_ICONS.delete} title="Delete routine" color={K.Red} last onPress={() => setConfirming(true)} />
          </GroupedList>
        </BottomActions>
      ) : null}
      <ChoiceSheet
        visible={picking}
        title="Add a stretch"
        choices={library.map((s) => ({ id: s.id, title: s.name }))}
        isSelected={(stretchId) => mine.some((rs) => rs.stretchId === stretchId)}
        onPick={pick}
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

