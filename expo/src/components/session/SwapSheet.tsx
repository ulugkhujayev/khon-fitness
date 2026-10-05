import { ChoiceSheet, SearchField } from '@/components/ui';
import { searchByName } from '@/domain/editors';
import { Exercise, SetLog } from '@/domain/types';

/** Where the swap sheet is: the set being swapped, the search text, and the replacement once chosen. */
export interface SwapState {
  log: SetLog;
  query: string;
  to: Exercise | null;
}

/** The kept list stays short, like the Kotlin sheet; search narrows it. */
const LIMIT = 12;
const SCOPES = [
  { id: 'session', title: 'This session only' },
  { id: 'program', title: 'Update the program too' },
];

/** Swap an exercise (SwapSheet in SessionScreen.kt): pick a replacement, then the scope, session only or the program too. */
export function SwapSheet({
  visible,
  state,
  exercises,
  onChange,
  onPick,
  onDismiss,
}: {
  visible: boolean;
  state: SwapState | null;
  /** Strength exercises that are not archived. */
  exercises: Exercise[];
  onChange: (state: SwapState) => void;
  onPick: (to: Exercise, updateProgram: boolean) => void;
  onDismiss: () => void;
}) {
  const to = state?.to ?? null;
  if (state && to) {
    return (
      <ChoiceSheet visible={visible} title={`Swap to ${to.name}`} choices={SCOPES} onPick={(id) => onPick(to, id === 'program')} onDismiss={onDismiss} />
    );
  }
  const found = state ? searchByName(exercises, state.query).filter((e) => e.id !== state.log.exerciseId).slice(0, LIMIT) : [];
  return (
    <ChoiceSheet
      visible={visible}
      title={state ? `Swap ${state.log.exerciseName}` : undefined}
      choices={found.map((e) => ({ id: e.id, title: e.name }))}
      onPick={(id) => state && onChange({ ...state, to: found.find((e) => e.id === id) ?? null })}
      onDismiss={onDismiss}
      header={state ? <SearchField value={state.query} onChange={(query) => onChange({ ...state, query })} elevated /> : null}
    />
  );
}
