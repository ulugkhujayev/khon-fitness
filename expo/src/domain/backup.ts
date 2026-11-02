import {
  Block, BlockExercise, Bodyweight, DayOverride, Defaults, EatingWindow, Exercise, IntervalLog, Modality, Program,
  RoutineStretch, Session, SetLog, Stretch, StretchRoutine, StretchSession, WeekPlan, WindowDay,
} from './types';

/** Same keys as the Kotlin Backup class (data/Repository.kt). */
export interface Backup {
  version: number;
  exercises: Exercise[];
  modalities: Modality[];
  programs: Program[];
  blocks: Block[];
  blockExercises: BlockExercise[];
  sessions: Session[];
  setLogs: SetLog[];
  intervalLogs: IntervalLog[];
  bodyweights: Bodyweight[];
  weekPlan: WeekPlan[];
  overrides: DayOverride[];
  stretches: Stretch[];
  stretchRoutines: StretchRoutine[];
  routineStretches: RoutineStretch[];
  stretchSessions: StretchSession[];
  eatingWindow: EatingWindow | null;
  windowDays: WindowDay[];
}

export class BackupError extends Error {}

type Row = Record<string, unknown>;

function list<T>(raw: Row, key: string, defaults: Partial<T>, required: string[], optional = false): T[] {
  const v = raw[key];
  if (v === undefined || v === null) {
    if (optional) return [];
    throw new BackupError(`Not a Khon backup: "${key}" is missing`);
  }
  if (!Array.isArray(v)) throw new BackupError(`Not a Khon backup: "${key}" is not a list`);
  return v.map((item, i) => {
    if (typeof item !== 'object' || item === null) throw new BackupError(`Not a Khon backup: ${key}[${i}] is not an object`);
    for (const r of required) {
      if ((item as Row)[r] === undefined || (item as Row)[r] === null) {
        throw new BackupError(`Not a Khon backup: ${key}[${i}] has no "${r}"`);
      }
    }
    // Unknown keys are dropped, missing keys take the Kotlin default, like Json { ignoreUnknownKeys = true }.
    const out: Row = { ...(defaults as Row) };
    for (const k of Object.keys(item as Row)) out[k] = (item as Row)[k];
    return out as T;
  });
}

/** Parses backup JSON from either app. Throws BackupError when it is not a Khon backup. */
export function parseBackup(text: string): Backup {
  let raw: unknown;
  try {
    raw = JSON.parse(text);
  } catch {
    throw new BackupError('Not a Khon backup: the file is not JSON');
  }
  if (typeof raw !== 'object' || raw === null || Array.isArray(raw)) throw new BackupError('Not a Khon backup');
  const r = raw as Row;
  const D = Defaults;
  const ew = r.eatingWindow;
  return {
    version: typeof r.version === 'number' ? r.version : 1,
    exercises: list<Exercise>(r, 'exercises', D.exercise, ['id', 'name', 'kind']),
    modalities: list<Modality>(r, 'modalities', D.modality, ['id', 'name', 'color']),
    programs: list<Program>(r, 'programs', D.program, ['id', 'name']),
    blocks: list<Block>(r, 'blocks', D.block, ['id', 'programId', 'sortOrder']),
    blockExercises: list<BlockExercise>(r, 'blockExercises', D.blockExercise, ['id', 'blockId', 'slot', 'exerciseId']),
    sessions: list<Session>(r, 'sessions', D.session, ['id', 'date', 'startedAt', 'itemType']),
    setLogs: list<SetLog>(r, 'setLogs', D.setLog, [
      'id', 'sessionId', 'blockIndex', 'blockSuperset', 'slot', 'setIndex', 'order', 'exerciseId', 'exerciseName', 'minReps', 'maxReps', 'stepKg',
    ]),
    intervalLogs: list<IntervalLog>(r, 'intervalLogs', D.intervalLog, ['id', 'sessionId', 'index']),
    bodyweights: list<Bodyweight>(r, 'bodyweights', D.bodyweight, ['date', 'kg']),
    weekPlan: list<WeekPlan>(r, 'weekPlan', D.weekPlan, ['weekday', 'itemType']),
    overrides: list<DayOverride>(r, 'overrides', D.dayOverride, ['date', 'itemType']),
    stretches: list<Stretch>(r, 'stretches', D.stretch, ['id', 'name'], true),
    stretchRoutines: list<StretchRoutine>(r, 'stretchRoutines', D.stretchRoutine, ['id', 'name'], true),
    routineStretches: list<RoutineStretch>(r, 'routineStretches', D.routineStretch, ['id', 'routineId', 'stretchId', 'sortOrder'], true),
    stretchSessions: list<StretchSession>(r, 'stretchSessions', D.stretchSession, [
      'id', 'routineId', 'routineName', 'date', 'startedAt', 'totalSec', 'completed', 'skipped',
    ], true),
    eatingWindow: ew && typeof ew === 'object' ? ({ ...D.eatingWindow, ...(ew as Row), id: 1 } as EatingWindow) : null,
    windowDays: list<WindowDay>(r, 'windowDays', D.windowDay, ['date'], true),
  };
}

/** "3 sessions · 0 stretch sessions · 1 bodyweight entry · 1 program · 25 exercises" */
export function describeBackup(b: Backup): string {
  const n = (c: number, one: string, many: string) => `${c} ${c === 1 ? one : many}`;
  return [
    n(b.sessions.length, 'session', 'sessions'),
    n(b.stretchSessions.length, 'stretch session', 'stretch sessions'),
    n(b.bodyweights.length, 'bodyweight entry', 'bodyweight entries'),
    n(b.programs.length, 'program', 'programs'),
    n(b.exercises.length, 'exercise', 'exercises'),
  ].join(' · ');
}
