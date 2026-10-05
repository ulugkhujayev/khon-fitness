/**
 * Port of KhonRepository (data/Repository.kt). Reads return plain arrays; screens wrap them in useLive.
 * Every write goes through write(), which runs in one transaction and notifies the tables it touched.
 */
import { today as todayIso } from '@/domain/dates';
import { Backup, describeBackup, parseBackup } from '@/domain/backup';
import { itemFor, itemId, PlanData, PlanRules } from '@/domain/plan';
import {
  SEED_BLOCK_EXERCISES, SEED_BLOCKS, SEED_EATING_WINDOW, SEED_EXERCISES, SEED_MODALITIES, SEED_PROGRAM,
  SEED_ROUTINE, SEED_ROUTINE_STRETCHES, SEED_STRETCHES, SEED_WEEK_PLAN,
} from '@/domain/seed';
import { buildSetLogs, previousByKey, ProgramDetail } from '@/domain/session';
import {
  Block, BlockExercise, Bodyweight, DayOverride, Defaults, EatingWindow, Exercise, IntervalLog, ItemType, Modality,
  Program, RoutineStretch, Session, SetLog, SetStatus, Stretch, StretchRoutine, StretchSession, WeekPlan, WindowDay,
} from '@/domain/types';
import { all, count, first, Tx, write } from './database';
import { newId } from './id';
import { ALL_TABLES, TABLES as T } from './schema';

// ---------------------------------------------------------------- reads

export const exercises = () => all<Exercise>(T.exercises, '', [], 'name');
export const exercise = (id: string) => first<Exercise>(T.exercises, 'id = ?', [id]);
export const modalities = () => all<Modality>(T.modalities, '', [], 'name');
export const programs = () => all<Program>(T.programs, '', [], 'sortOrder, name');
export const program = (id: string) => first<Program>(T.programs, 'id = ?', [id]);
export const blocks = () => all<Block>(T.blocks, '', [], 'sortOrder');
export const blocksFor = (programId: string) => all<Block>(T.blocks, 'programId = ?', [programId], 'sortOrder');
export const blockExercises = () => all<BlockExercise>(T.blockExercises, '', [], 'slot');
export const blockExercisesFor = (blockId: string) => all<BlockExercise>(T.blockExercises, 'blockId = ?', [blockId], 'slot');
export const sessions = () => all<Session>(T.sessions, '', [], 'date DESC, startedAt DESC');
export const session = (id: string) => first<Session>(T.sessions, 'id = ?', [id]);
export const unfinishedSession = async () =>
  (await all<Session>(T.sessions, 'finishedAt IS NULL', [], 'startedAt DESC LIMIT 1'))[0] ?? null;
export const setLogs = (sessionId: string) => all<SetLog>(T.setLogs, 'sessionId = ?', [sessionId], '`order`');
export const allSetLogs = () => all<SetLog>(T.setLogs);
export const intervalLogs = (sessionId: string) => all<IntervalLog>(T.intervalLogs, 'sessionId = ?', [sessionId], '`index`');
export const allIntervalLogs = () => all<IntervalLog>(T.intervalLogs);
export const bodyweights = () => all<Bodyweight>(T.bodyweights, '', [], 'date');
export const weekPlan = () => all<WeekPlan>(T.weekPlan, '', [], 'weekday');
export const overrides = () => all<DayOverride>(T.overrides);
export const stretches = () => all<Stretch>(T.stretches, '', [], 'name');
export const stretchRoutines = () => all<StretchRoutine>(T.stretchRoutines, '', [], 'sortOrder, name');
export const routineStretches = () => all<RoutineStretch>(T.routineStretches, '', [], 'sortOrder');
export const stretchSessions = () => all<StretchSession>(T.stretchSessions, '', [], 'date DESC, startedAt DESC');
export const eatingWindow = async (): Promise<EatingWindow> =>
  (await first<EatingWindow>(T.eatingWindow, 'id = 1')) ?? { ...Defaults.eatingWindow };
export const windowDays = () => all<WindowDay>(T.windowDays, '', [], 'date');
export const windowDay = (date: string) => first<WindowDay>(T.windowDays, 'date = ?', [date]);

/** Tables a resolved plan depends on, for useLive. */
export const PLAN_TABLES = [T.weekPlan, T.overrides, T.programs, T.exercises] as const;

export async function planData(): Promise<PlanData> {
  const [plan, ov, progs, exs] = await Promise.all([weekPlan(), overrides(), programs(), exercises()]);
  return { plan, overrides: ov, programs: progs, exercises: exs };
}

export async function programDetail(programId: string): Promise<ProgramDetail | null> {
  const p = await program(programId);
  if (!p) return null;
  const byId = new Map((await all<Exercise>(T.exercises)).map((e) => [e.id, e]));
  const bs = await blocksFor(programId);
  const out = [];
  for (const block of bs) {
    const bes = await blockExercisesFor(block.id);
    out.push({
      block,
      exercises: bes.flatMap((be) => {
        const ex = byId.get(be.exerciseId);
        return ex ? [{ be, exercise: ex }] : [];
      }),
    });
  }
  return { program: p, blocks: out };
}

export async function exerciseUsage(id: string): Promise<number> {
  const n = await Promise.all([
    count('SELECT COUNT(*) AS n FROM set_logs WHERE exerciseId = ?', [id]),
    count('SELECT COUNT(*) AS n FROM sessions WHERE exerciseId = ?', [id]),
    count('SELECT COUNT(*) AS n FROM block_exercises WHERE exerciseId = ?', [id]),
    count("SELECT COUNT(*) AS n FROM week_plan WHERE itemType = 'cardio' AND itemId = ?", [id]),
    count("SELECT COUNT(*) AS n FROM day_overrides WHERE itemType = 'cardio' AND itemId = ?", [id]),
  ]);
  return n.reduce((a, b) => a + b, 0);
}

export const stretchUseCount = (id: string) => count('SELECT COUNT(*) AS n FROM routine_stretches WHERE stretchId = ?', [id]);

export async function importedSourceIds(): Promise<string[]> {
  const rows = await all<Session>(T.sessions, 'sourceId IS NOT NULL');
  return rows.flatMap((s) => (s.sourceId ?? '').split(','));
}

// ---------------------------------------------------------------- exercises, modalities

export const saveExercise = (e: Exercise) => write((tx) => tx.upsert(T.exercises, e));
export const saveModality = (m: Modality) => write((tx) => tx.upsert(T.modalities, m));

export async function deleteExercise(id: string) {
  if ((await exerciseUsage(id)) > 0) return;
  await write((tx) => tx.run(T.exercises, 'DELETE FROM exercises WHERE id = ?', [id]));
}

/** Moves every use of `from` onto `into`, then deletes `from`. */
export const mergeExercise = (from: string, into: string) =>
  write(async (tx) => {
    await tx.run(T.setLogs, 'UPDATE set_logs SET exerciseId = ? WHERE exerciseId = ?', [into, from]);
    await tx.run(T.sessions, 'UPDATE sessions SET exerciseId = ? WHERE exerciseId = ?', [into, from]);
    await tx.run(T.blockExercises, 'UPDATE block_exercises SET exerciseId = ? WHERE exerciseId = ?', [into, from]);
    await tx.run(T.weekPlan, "UPDATE week_plan SET itemId = ? WHERE itemType = 'cardio' AND itemId = ?", [into, from]);
    await tx.run(T.overrides, "UPDATE day_overrides SET itemId = ? WHERE itemType = 'cardio' AND itemId = ?", [into, from]);
    await tx.run(T.exercises, 'DELETE FROM exercises WHERE id = ?', [from]);
  });

// ---------------------------------------------------------------- programs and blocks

export const saveProgram = (p: Program) => write((tx) => tx.upsert(T.programs, p));
export const setActiveProgram = (id: string) =>
  write((tx) => tx.run(T.programs, 'UPDATE programs SET active = (id = ?)', [id]));

export const deleteProgram = (id: string) =>
  write(async (tx) => {
    await tx.run(T.blockExercises, 'DELETE FROM block_exercises WHERE blockId IN (SELECT id FROM blocks WHERE programId = ?)', [id]);
    await tx.run(T.blocks, 'DELETE FROM blocks WHERE programId = ?', [id]);
    await tx.run(T.programs, 'DELETE FROM programs WHERE id = ?', [id]);
  });

export const saveBlock = (b: Block) => write((tx) => tx.upsert(T.blocks, b));
export const saveBlocks = (list: Block[]) => write((tx) => tx.upsert(T.blocks, list));
export const deleteBlock = (id: string) =>
  write(async (tx) => {
    await tx.run(T.blockExercises, 'DELETE FROM block_exercises WHERE blockId = ?', [id]);
    await tx.run(T.blocks, 'DELETE FROM blocks WHERE id = ?', [id]);
  });
export const saveBlockExercise = (be: BlockExercise) => write((tx) => tx.upsert(T.blockExercises, be));
export const deleteBlockExercise = (id: string) =>
  write((tx) => tx.run(T.blockExercises, 'DELETE FROM block_exercises WHERE id = ?', [id]));

// ---------------------------------------------------------------- plan

/**
 * Changes a weekday in the template. Past dates of that weekday first get auto overrides with the
 * old value, so history keeps its plan.
 */
export async function saveWeekPlan(weekday: number, itemType: string, id: string | null, today = todayIso()) {
  const plan = await weekPlan();
  const old = plan.find((w) => w.weekday === weekday);
  if ((old?.itemType ?? ItemType.REST) === itemType && (old?.itemId ?? null) === id) return;
  const [ss, sts, bws, wds, ovs] = await Promise.all([sessions(), stretchSessions(), bodyweights(), windowDays(), overrides()]);
  const dates = [...ss, ...sts, ...bws, ...wds, ...ovs].map((x) => x.date);
  const from = PlanRules.freezeFrom(dates, today);
  const frozen: DayOverride[] = PlanRules.datesToFreeze(weekday, from, today, new Set(ovs.map((o) => o.date))).map((d) => ({
    date: d, itemType: old?.itemType ?? ItemType.REST, itemId: old?.itemId ?? null, auto: true,
  }));
  await write(async (tx) => {
    await tx.upsert(T.overrides, frozen);
    await tx.upsert(T.weekPlan, { weekday, itemType, itemId: id });
  });
}

export const saveOverride = (date: string, itemType: string, id: string | null) =>
  write((tx) => tx.upsert(T.overrides, { date, itemType, itemId: id, auto: false }));
export const clearOverride = (date: string) =>
  write((tx) => tx.run(T.overrides, 'DELETE FROM day_overrides WHERE date = ?', [date]));

export async function swapDays(a: string, b: string) {
  const data = await planData();
  const ia = itemFor(data, a);
  const ib = itemFor(data, b);
  await write(async (tx) => {
    await tx.upsert(T.overrides, [
      { date: a, itemType: ib.itemType, itemId: itemId(ib), auto: false },
      { date: b, itemType: ia.itemType, itemId: itemId(ia), auto: false },
    ]);
  });
}

// ---------------------------------------------------------------- bodyweight

export const saveBodyweight = (date: string, kg: number) => write((tx) => tx.upsert(T.bodyweights, { date, kg }));

// ---------------------------------------------------------------- sessions

async function previousLogs(excludeSessionId: string): Promise<Map<string, SetLog>> {
  const finished = await all<Session>(T.sessions, 'finishedAt IS NOT NULL AND itemType = ? AND id != ?', [ItemType.PROGRAM, excludeSessionId]);
  const withLogs = await Promise.all(finished.map(async (s) => ({ startedAt: s.startedAt, logs: await setLogs(s.id) })));
  return previousByKey(withLogs);
}

/** Starts a program session: every block snapshotted into set logs, prefilled from last time. */
export async function startProgramSession(programId: string, date = todayIso()): Promise<Session | null> {
  const detail = await programDetail(programId);
  if (!detail) return null;
  const s: Session = {
    ...Defaults.session, id: newId(), date, startedAt: Date.now(), itemType: ItemType.PROGRAM,
    programId, programName: detail.program.name,
  };
  const logs = buildSetLogs(detail, s.id, await previousLogs(s.id), newId);
  await write(async (tx) => {
    await tx.upsert(T.sessions, s);
    await tx.upsert(T.setLogs, logs);
  });
  return s;
}

export async function startCardioSession(ex: Exercise, date = todayIso()): Promise<Session> {
  const s: Session = {
    ...Defaults.session, id: newId(), date, startedAt: Date.now(), itemType: ItemType.CARDIO,
    exerciseId: ex.id, exerciseName: ex.name,
  };
  await write(async (tx) => {
    await tx.upsert(T.sessions, s);
    if (ex.intervals) {
      const logs: IntervalLog[] = Array.from({ length: ex.rounds }, (_, i) => ({ id: newId(), sessionId: s.id, index: i, avgHr: null, watts: null }));
      await tx.upsert(T.intervalLogs, logs);
    }
  });
  return s;
}

export const updateSetLog = (l: SetLog) => write((tx) => tx.upsert(T.setLogs, l));
export const updateSession = (s: Session) => write((tx) => tx.upsert(T.sessions, s));
export const saveIntervals = (list: IntervalLog[]) => write((tx) => tx.upsert(T.intervalLogs, list));

/** Pending sets become skipped, then the session gets its finish time. */
export const finishSession = (sessionId: string) =>
  write(async (tx) => {
    await tx.run(T.setLogs, 'UPDATE set_logs SET status = ? WHERE sessionId = ? AND status = ?', [SetStatus.SKIPPED, sessionId, SetStatus.PENDING]);
    await tx.run(T.sessions, 'UPDATE sessions SET finishedAt = ? WHERE id = ?', [Date.now(), sessionId]);
  });

export const deleteSession = (sessionId: string) =>
  write(async (tx) => {
    await tx.run(T.setLogs, 'DELETE FROM set_logs WHERE sessionId = ?', [sessionId]);
    await tx.run(T.intervalLogs, 'DELETE FROM interval_logs WHERE sessionId = ?', [sessionId]);
    await tx.run(T.sessions, 'DELETE FROM sessions WHERE id = ?', [sessionId]);
  });

/** Swaps an exercise inside a running session, and in the program too when asked. */
export const swapExerciseInSession = (sessionId: string, fromExerciseId: string, to: Exercise, updateProgram: boolean, programId: string | null) =>
  write(async (tx) => {
    await tx.run(T.setLogs, 'UPDATE set_logs SET exerciseId = ?, exerciseName = ?, stepKg = ? WHERE sessionId = ? AND exerciseId = ?', [
      to.id, to.name, to.stepKg, sessionId, fromExerciseId,
    ]);
    if (updateProgram && programId) {
      await tx.run(
        T.blockExercises,
        'UPDATE block_exercises SET exerciseId = ? WHERE exerciseId = ? AND blockId IN (SELECT id FROM blocks WHERE programId = ?)',
        [to.id, fromExerciseId, programId],
      );
    }
  });

// ---------------------------------------------------------------- stretching

export const saveStretch = (s: Stretch) => write((tx) => tx.upsert(T.stretches, s));
export const deleteStretch = (id: string) => write((tx) => tx.run(T.stretches, 'DELETE FROM stretches WHERE id = ?', [id]));
export const saveRoutine = (r: StretchRoutine) =>
  write(async (tx) => {
    await tx.upsert(T.stretchRoutines, r);
    if (r.active) await tx.run(T.stretchRoutines, 'UPDATE stretch_routines SET active = (id = ?)', [r.id]);
  });
export const deleteRoutine = (id: string) =>
  write(async (tx) => {
    await tx.run(T.routineStretches, 'DELETE FROM routine_stretches WHERE routineId = ?', [id]);
    await tx.run(T.stretchRoutines, 'DELETE FROM stretch_routines WHERE id = ?', [id]);
  });
export const saveRoutineStretches = (list: RoutineStretch[]) => write((tx) => tx.upsert(T.routineStretches, list));
export const deleteRoutineStretch = (id: string) =>
  write((tx) => tx.run(T.routineStretches, 'DELETE FROM routine_stretches WHERE id = ?', [id]));
export const saveStretchSession = (s: StretchSession) => write((tx) => tx.upsert(T.stretchSessions, s));
export const deleteStretchSession = (id: string) =>
  write((tx) => tx.run(T.stretchSessions, 'DELETE FROM stretch_sessions WHERE id = ?', [id]));

// ---------------------------------------------------------------- eating window

export const saveEatingWindow = (w: EatingWindow) => write((tx) => tx.upsert(T.eatingWindow, { ...w, id: 1 }));

export async function openWindowNow(date: string, now = Date.now()) {
  const d = (await windowDay(date)) ?? { ...Defaults.windowDay, date };
  await write((tx) => tx.upsert(T.windowDays, { ...d, openedAt: now, closedAt: null }));
}

export async function closeWindowNow(date: string, now = Date.now()) {
  const d = (await windowDay(date)) ?? { ...Defaults.windowDay, date };
  await write((tx) => tx.upsert(T.windowDays, { ...d, closedAt: now }));
}

// ---------------------------------------------------------------- seed

async function applySeed(tx: Tx) {
  await tx.upsert(T.modalities, SEED_MODALITIES);
  await tx.upsert(T.exercises, SEED_EXERCISES);
  await tx.upsert(T.programs, SEED_PROGRAM);
  await tx.upsert(T.blocks, SEED_BLOCKS);
  await tx.upsert(T.blockExercises, SEED_BLOCK_EXERCISES);
  await tx.upsert(T.weekPlan, SEED_WEEK_PLAN);
}

/** Stretches, the daily routine and the eating window, each only when missing. */
async function applyStretchSeed(tx: Tx) {
  await tx.upsert(T.stretches, SEED_STRETCHES);
  if ((await count('SELECT COUNT(*) AS n FROM stretch_routines')) === 0) {
    await tx.upsert(T.stretchRoutines, SEED_ROUTINE);
    await tx.upsert(T.routineStretches, SEED_ROUTINE_STRETCHES);
  }
  if ((await count('SELECT COUNT(*) AS n FROM eating_window')) === 0) await tx.upsert(T.eatingWindow, SEED_EATING_WINDOW);
}

/** First start: TP v3, the exercise library, the week plan and the stretching routine. */
export async function seedIfEmpty() {
  const empty = (await count('SELECT COUNT(*) AS n FROM programs')) === 0 && (await count('SELECT COUNT(*) AS n FROM exercises')) === 0;
  const noStretches = (await count('SELECT COUNT(*) AS n FROM stretches')) === 0;
  if (!empty && !noStretches) return;
  await write(async (tx) => {
    if (empty) await applySeed(tx);
    if (noStretches) await applyStretchSeed(tx);
  });
}

export const resetToSeed = () =>
  write(async (tx) => {
    for (const t of ALL_TABLES) await tx.clear(t);
    await applySeed(tx);
    await applyStretchSeed(tx);
  });

// ---------------------------------------------------------------- backup

export async function exportBackup(): Promise<Backup> {
  const [
    exs, mods, progs, bls, bes, ss, sls, ils, bws, wp, ovs, sts, srs, rss, sss, ew, wds,
  ] = await Promise.all([
    all<Exercise>(T.exercises), all<Modality>(T.modalities), all<Program>(T.programs), all<Block>(T.blocks),
    all<BlockExercise>(T.blockExercises), all<Session>(T.sessions), all<SetLog>(T.setLogs), all<IntervalLog>(T.intervalLogs),
    all<Bodyweight>(T.bodyweights), all<WeekPlan>(T.weekPlan), all<DayOverride>(T.overrides), all<Stretch>(T.stretches),
    all<StretchRoutine>(T.stretchRoutines), all<RoutineStretch>(T.routineStretches), all<StretchSession>(T.stretchSessions),
    first<EatingWindow>(T.eatingWindow, 'id = 1'), all<WindowDay>(T.windowDays),
  ]);
  return {
    version: 1, exercises: exs, modalities: mods, programs: progs, blocks: bls, blockExercises: bes, sessions: ss,
    setLogs: sls, intervalLogs: ils, bodyweights: bws, weekPlan: wp, overrides: ovs, stretches: sts, stretchRoutines: srs,
    routineStretches: rss, stretchSessions: sss, eatingWindow: ew, windowDays: wds,
  };
}

/** Pretty JSON with the same keys as the Kotlin export; either app can read it. */
export async function exportJson(): Promise<string> {
  return JSON.stringify(await exportBackup(), null, 2);
}

/** What a backup holds, for the confirm step. Throws BackupError when the text is not a Khon backup. */
export const describeBackupText = (text: string) => describeBackup(parseBackup(text));

/** Imports a backup. `replace` clears everything first; otherwise rows with the same id are overwritten. */
export async function importJson(text: string, replace: boolean) {
  const b = parseBackup(text);
  await write(async (tx) => {
    if (replace) for (const t of ALL_TABLES) await tx.clear(t);
    await tx.upsert(T.modalities, b.modalities);
    await tx.upsert(T.exercises, b.exercises);
    await tx.upsert(T.programs, b.programs);
    await tx.upsert(T.blocks, b.blocks);
    await tx.upsert(T.blockExercises, b.blockExercises);
    await tx.upsert(T.sessions, b.sessions);
    await tx.upsert(T.setLogs, b.setLogs);
    await tx.upsert(T.intervalLogs, b.intervalLogs);
    await tx.upsert(T.bodyweights, b.bodyweights);
    await tx.upsert(T.weekPlan, b.weekPlan);
    await tx.upsert(T.overrides, b.overrides);
    await tx.upsert(T.stretches, b.stretches);
    await tx.upsert(T.stretchRoutines, b.stretchRoutines);
    await tx.upsert(T.routineStretches, b.routineStretches);
    await tx.upsert(T.stretchSessions, b.stretchSessions);
    if (b.eatingWindow) await tx.upsert(T.eatingWindow, b.eatingWindow);
    await tx.upsert(T.windowDays, b.windowDays);
    // An old backup without stretches still gets the routine, like seedStretchesIfEmpty after import.
    if (replace && b.stretches.length === 0) await applyStretchSeed(tx);
  });
}

