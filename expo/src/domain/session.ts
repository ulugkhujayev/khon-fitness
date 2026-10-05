import { Block, BlockExercise, Exercise, Program, SetLog, SetStatus } from './types';

export interface BlockDetail {
  block: Block;
  exercises: { be: BlockExercise; exercise: Exercise }[];
}

export interface ProgramDetail {
  program: Program;
  blocks: BlockDetail[];
}

/** Key that matches a set with the same set of the same exercise last time. */
export const setKey = (l: Pick<SetLog, 'exerciseId' | 'setIndex'>) => `${l.exerciseId}:${l.setIndex}`;

/** Estimated one-rep max, Epley: weight x (1 + reps / 30). */
export const epley = (weight: number, reps: number) => weight * (1 + reps / 30);

/**
 * Snapshots every block of a program into set logs in execution order.
 * A superset with two or more exercises alternates A1 B1 A2 B2; anything else runs straight through.
 * Each set is prefilled from the previous done set with the same key, or gets reps = minReps.
 */
export function buildSetLogs(
  detail: ProgramDetail,
  sessionId: string,
  previous: Map<string, SetLog>,
  newId: () => string,
): SetLog[] {
  const logs: SetLog[] = [];
  let order = 0;
  const make = (bi: number, superset: boolean, slot: number, s: number, be: BlockExercise, ex: Exercise): SetLog => ({
    id: newId(),
    sessionId,
    blockIndex: bi,
    blockSuperset: superset,
    slot,
    setIndex: s,
    order: order++,
    exerciseId: ex.id,
    exerciseName: ex.name,
    minReps: be.minReps,
    maxReps: be.maxReps,
    stepKg: ex.stepKg,
    weightKg: null,
    reps: null,
    status: SetStatus.PENDING,
  });

  detail.blocks.forEach((bd, bi) => {
    const exs = bd.exercises;
    if (bd.block.superset && exs.length > 1) {
      const maxSets = Math.max(...exs.map((x) => x.be.sets));
      for (let s = 0; s < maxSets; s++) {
        exs.forEach(({ be, exercise }, slot) => {
          if (s < be.sets) logs.push(make(bi, true, slot, s, be, exercise));
        });
      }
    } else {
      exs.forEach(({ be, exercise }, slot) => {
        for (let s = 0; s < be.sets; s++) logs.push(make(bi, bd.block.superset, slot, s, be, exercise));
      });
    }
  });

  return logs.map((l) => {
    const p = previous.get(setKey(l));
    return p ? { ...l, weightKg: p.weightKg, reps: p.reps } : { ...l, reps: l.minReps };
  });
}

/**
 * Last done set for each key, newest finished session first.
 * `sessions` must already be finished program sessions, excluding the current one.
 */
export function previousByKey(sessions: { startedAt: number; logs: SetLog[] }[]): Map<string, SetLog> {
  const result = new Map<string, SetLog>();
  const sorted = [...sessions].sort((a, b) => b.startedAt - a.startedAt);
  for (const s of sorted) {
    for (const l of s.logs) {
      const k = setKey(l);
      if (l.status === SetStatus.DONE && !result.has(k)) result.set(k, l);
    }
  }
  return result;
}
