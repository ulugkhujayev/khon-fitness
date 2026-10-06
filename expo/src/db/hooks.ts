/**
 * Named reads for the screens, each a `useLive` query with the tables it depends on.
 * Screens call these instead of querying the repository themselves.
 */
import { useEffect, useState } from 'react';
import { preview } from '@/domain/format';
import { importedIds } from '@/domain/health';
import { doneSessionFor, itemFor } from '@/domain/plan';
import { activeRoutine, routineSummary } from '@/domain/stretch';
import { ItemType, Kind, SetLog } from '@/domain/types';
import { doneCount, doneLine } from '@/domain/workout';
import * as repo from './repository';
import { TABLES as T } from './schema';
import { useLive } from './useLive';

/**
 * Today: the plan around the date, its resolved item, a running session with its sets done of total (a
 * program) and the library for its color, the done line, bodyweights,
 * the active stretch routine (green once stretched today), the eating window with its days, and the
 * health record ids already imported (for the band row).
 */
export function useToday(date: string) {
  return useLive(
    async () => {
      const [plan, modalities, blocks, blockExercises, bodyweights, unfinished, sessions, routines, routineStretches, stretches, stretchSessions, eatingWindow, windowDays, exercises] =
        await Promise.all([
          repo.planData(), repo.modalities(), repo.blocks(), repo.blockExercises(), repo.bodyweights(), repo.unfinishedSession(), repo.sessions(),
          repo.stretchRoutines(), repo.routineStretches(), repo.stretches(), repo.stretchSessions(), repo.eatingWindow(), repo.windowDays(), repo.exercises(),
        ]);
      const running = unfinished?.itemType === ItemType.PROGRAM ? await repo.setLogs(unfinished.id) : null;
      const item = itemFor(plan, date);
      const done = doneSessionFor(sessions, item, date);
      const routine = activeRoutine(routines);
      return {
        plan, modalities, exercises, bodyweights, unfinished, item, eatingWindow, windowDays,
        progress: running && { done: doneCount(running), total: running.length },
        imported: importedIds(sessions),
        line: preview(item, blocks, blockExercises, modalities),
        doneText: done ? doneLine(done, await repo.setLogs(done.id)) : null,
        routine: routine && {
          id: routine.id,
          name: routine.name,
          line: routineSummary(routine.id, routineStretches, stretches),
          doneToday: stretchSessions.some((r) => r.date === date),
        },
      };
    },
    [
      ...repo.PLAN_TABLES, T.modalities, T.blocks, T.blockExercises, T.bodyweights, T.sessions, T.setLogs,
      T.stretchRoutines, T.routineStretches, T.stretches, T.stretchSessions, T.eatingWindow, T.windowDays,
    ],
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

/** Modalities, and the exercises that use them for the list's counts. */
export function useModalities() {
  return useLive(
    async () => {
      const [modalities, exercises] = await Promise.all([repo.modalities(), repo.exercises()]);
      return { modalities, exercises };
    },
    [T.modalities, T.exercises],
  );
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

/** A count read once per id; 0 for a new item (null id). */
function useCountOf(id: string | null, read: (id: string) => Promise<number>): number {
  const [n, setN] = useState(0);
  useEffect(() => {
    if (!id) return;
    let alive = true;
    read(id).then((v) => alive && setN(v));
    return () => {
      alive = false;
    };
  }, [id, read]);
  return n;
}

/** How many sessions, programs and plan days use an exercise. Delete is allowed only at 0. */
export const useExerciseUsage = (id: string | null) => useCountOf(id, repo.exerciseUsage);

/** How many routine rows use a stretch. Delete is allowed only at 0. */
export const useStretchUseCount = (id: string | null) => useCountOf(id, repo.stretchUseCount);

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

/** Stretching screens and the player: the routines, their stretches in order, the stretch library. */
export function useStretching() {
  return useLive(
    async () => {
      const [routines, routineStretches, stretches] = await Promise.all([repo.stretchRoutines(), repo.routineStretches(), repo.stretches()]);
      return { routines, routineStretches, stretches };
    },
    [T.stretchRoutines, T.routineStretches, T.stretches],
  );
}

/** Health import: the cardio library and plan for default exercises, modality colors, and the record ids already imported. */
export function useImportData() {
  return useLive(
    async () => {
      const [exercises, modalities, plan, imported] = await Promise.all([repo.exercises(), repo.modalities(), repo.planData(), repo.importedSourceIds()]);
      return { cardio: exercises.filter((e) => e.kind === Kind.CARDIO && !e.archived), modalities, plan, imported };
    },
    [T.sessions, T.exercises, T.modalities, ...repo.PLAN_TABLES],
  );
}
