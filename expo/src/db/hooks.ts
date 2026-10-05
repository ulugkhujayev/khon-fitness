/**
 * Named reads for the screens, each a `useLive` query with the tables it depends on.
 * Screens call these instead of querying the repository themselves.
 */
import { useEffect, useState } from 'react';
import { preview } from '@/domain/format';
import { doneSessionFor, itemFor } from '@/domain/plan';
import { SetLog } from '@/domain/types';
import { doneLine } from '@/domain/workout';
import * as repo from './repository';
import { TABLES as T } from './schema';
import { useLive } from './useLive';

/** Today: the plan around the date, its resolved item, a running session, the done line, bodyweights. */
export function useToday(date: string) {
  return useLive(
    async () => {
      const [plan, modalities, blocks, blockExercises, bodyweights, unfinished, sessions] = await Promise.all([
        repo.planData(), repo.modalities(), repo.blocks(), repo.blockExercises(), repo.bodyweights(), repo.unfinishedSession(), repo.sessions(),
      ]);
      const item = itemFor(plan, date);
      const done = doneSessionFor(sessions, item, date);
      return {
        plan, modalities, bodyweights, unfinished, item,
        line: preview(item, blocks, blockExercises, modalities),
        doneText: done ? doneLine(done, await repo.setLogs(done.id)) : null,
      };
    },
    [...repo.PLAN_TABLES, T.modalities, T.blocks, T.blockExercises, T.bodyweights, T.sessions, T.setLogs],
    [date],
  );
}

/** A session with its set logs in order, and the library for the swap sheet. `session` is null once it is gone. */
export function useSession(id: string) {
  return useLive(
    async () => {
      const [session, logs, exercises] = await Promise.all([repo.session(id), repo.setLogs(id), repo.exercises()]);
      return { session, logs, exercises };
    },
    [T.sessions, T.setLogs, T.exercises],
    [id],
  );
}

/** Last time's done set per exercise and set index, read once when a session opens. */
export function usePreviousLogs(sessionId: string): Map<string, SetLog> {
  const [previous, setPrevious] = useState<Map<string, SetLog>>(new Map());
  useEffect(() => {
    let alive = true;
    repo.previousLogs(sessionId).then((m) => alive && setPrevious(m));
    return () => {
      alive = false;
    };
  }, [sessionId]);
  return previous;
}

/** Programs tab: programs with their blocks, and counts for the library rows. */
export function usePrograms() {
  return useLive(
    async () => {
      const [programs, blocks, exercises, modalities] = await Promise.all([repo.programs(), repo.blocks(), repo.exercises(), repo.modalities()]);
      return { programs, blocks, exercises, modalities };
    },
    [T.programs, T.blocks, T.exercises, T.modalities],
  );
}

/** Program editor: the program (null once deleted), its blocks in order, and what they hold. */
export function useProgram(id: string) {
  return useLive(
    async () => {
      const [programs, blocks, blockExercises, exercises] = await Promise.all([
        repo.programs(), repo.blocksFor(id), repo.blockExercises(), repo.exercises(),
      ]);
      return { programCount: programs.length, program: programs.find((p) => p.id === id) ?? null, blocks, blockExercises, exercises };
    },
    [T.programs, T.blocks, T.blockExercises, T.exercises],
    [id],
  );
}

/** Block editor: the program's blocks in order, this block's exercises by slot, the library. */
export function useBlock(programId: string, blockId: string) {
  return useLive(
    async () => {
      const [siblings, mine, exercises] = await Promise.all([repo.blocksFor(programId), repo.blockExercisesFor(blockId), repo.exercises()]);
      return { siblings, mine, exercises };
    },
    [T.blocks, T.blockExercises, T.exercises],
    [programId, blockId],
  );
}

/** Exercise library and editor: every exercise and modality. */
export function useLibrary() {
  return useLive(
    async () => {
      const [exercises, modalities] = await Promise.all([repo.exercises(), repo.modalities()]);
      return { exercises, modalities };
    },
    [T.exercises, T.modalities],
  );
}

export function useModalities() {
  return useLive(() => repo.modalities(), [T.modalities]);
}

/** Week plan: the template with what it points at, and modality colors for the dots. */
export function usePlan() {
  return useLive(
    async () => {
      const [plan, modalities] = await Promise.all([repo.planData(), repo.modalities()]);
      return { plan, modalities };
    },
    [...repo.PLAN_TABLES, T.modalities],
  );
}

/** How many sessions, programs and plan days use an exercise; 0 for a new one. Delete is allowed only at 0. */
export function useExerciseUsage(id: string | null): number {
  const [usage, setUsage] = useState(0);
  useEffect(() => {
    if (!id) return;
    let alive = true;
    repo.exerciseUsage(id).then((n) => alive && setUsage(n));
    return () => {
      alive = false;
    };
  }, [id]);
  return usage;
}

/** History: every session with its sets and interval rows, stretch runs, bodyweights, and the plan for the calendar. */
export function useHistory() {
  return useLive(
    async () => {
      const [sessions, logs, intervals, stretches, bodyweights, plan, exercises, modalities] = await Promise.all([
        repo.sessions(), repo.allSetLogs(), repo.allIntervalLogs(), repo.stretchSessions(), repo.bodyweights(), repo.planData(), repo.exercises(), repo.modalities(),
      ]);
      return { sessions, logs, intervals, stretches, bodyweights, plan, exercises, modalities };
    },
    [T.sessions, T.setLogs, T.intervalLogs, T.stretchSessions, T.bodyweights, T.modalities, ...repo.PLAN_TABLES],
  );
}

/** A cardio session with its exercise (null when gone) and its interval rows. `session` is null once it is gone. */
export function useCardioSession(id: string) {
  return useLive(
    async () => {
      const [session, exercises, intervals] = await Promise.all([repo.session(id), repo.exercises(), repo.intervalLogs(id)]);
      return { session, exercise: exercises.find((e) => e.id === session?.exerciseId) ?? null, intervals };
    },
    [T.sessions, T.exercises, T.intervalLogs],
    [id],
  );
}
