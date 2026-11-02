/**
 * The Room v4 schema (android/app/schemas/.../4.json), statement for statement.
 * Booleans are INTEGER 0/1 in SQLite; BOOL_COLUMNS lists them so rows convert back to true/false.
 */

export const SCHEMA_VERSION = 4;

export const TABLES = {
  exercises: 'exercises',
  modalities: 'modalities',
  programs: 'programs',
  blocks: 'blocks',
  blockExercises: 'block_exercises',
  sessions: 'sessions',
  setLogs: 'set_logs',
  intervalLogs: 'interval_logs',
  bodyweights: 'bodyweights',
  weekPlan: 'week_plan',
  overrides: 'day_overrides',
  stretches: 'stretches',
  stretchRoutines: 'stretch_routines',
  routineStretches: 'routine_stretches',
  stretchSessions: 'stretch_sessions',
  eatingWindow: 'eating_window',
  windowDays: 'window_days',
} as const;

export type Table = (typeof TABLES)[keyof typeof TABLES];
export const ALL_TABLES = Object.values(TABLES) as Table[];

export const BOOL_COLUMNS: Record<Table, string[]> = {
  exercises: ['archived', 'builtin', 'intervals'],
  modalities: ['archived'],
  programs: ['active'],
  blocks: ['superset'],
  block_exercises: [],
  sessions: [],
  set_logs: ['blockSuperset'],
  interval_logs: [],
  bodyweights: [],
  week_plan: [],
  day_overrides: ['auto'],
  stretches: ['sided', 'builtin', 'archived'],
  stretch_routines: ['active'],
  routine_stretches: [],
  stretch_sessions: [],
  eating_window: ['enabled'],
  window_days: [],
};

export const COLUMNS: Record<Table, string[]> = {
  exercises: ['id', 'name', 'kind', 'notes', 'archived', 'builtin', 'primaryMuscle', 'secondaryMuscles', 'stepKg', 'modalityId', 'intensity', 'intervals', 'warmupSec', 'workSec', 'restSec', 'rounds', 'fields'],
  modalities: ['id', 'name', 'color', 'archived'],
  programs: ['id', 'name', 'active', 'sortOrder'],
  blocks: ['id', 'programId', 'sortOrder', 'superset'],
  block_exercises: ['id', 'blockId', 'slot', 'exerciseId', 'sets', 'minReps', 'maxReps'],
  sessions: ['id', 'date', 'startedAt', 'finishedAt', 'itemType', 'programId', 'programName', 'exerciseId', 'exerciseName', 'note', 'effort', 'timeSec', 'distanceM', 'avgHr', 'watts', 'laps', 'poolLength', 'stroke', 'sourceId'],
  set_logs: ['id', 'sessionId', 'blockIndex', 'blockSuperset', 'slot', 'setIndex', 'order', 'exerciseId', 'exerciseName', 'minReps', 'maxReps', 'stepKg', 'weightKg', 'reps', 'status'],
  interval_logs: ['id', 'sessionId', 'index', 'avgHr', 'watts'],
  bodyweights: ['date', 'kg'],
  week_plan: ['weekday', 'itemType', 'itemId'],
  day_overrides: ['date', 'itemType', 'itemId', 'auto'],
  stretches: ['id', 'name', 'sided', 'mode', 'seconds', 'reps', 'figure', 'muscles', 'cue', 'link', 'builtin', 'archived'],
  stretch_routines: ['id', 'name', 'active', 'sortOrder'],
  routine_stretches: ['id', 'routineId', 'stretchId', 'sortOrder', 'seconds'],
  stretch_sessions: ['id', 'routineId', 'routineName', 'date', 'startedAt', 'totalSec', 'completed', 'skipped'],
  eating_window: ['id', 'enabled', 'startMinute', 'endMinute', 'remindBeforeMin'],
  window_days: ['date', 'openedAt', 'closedAt', 'note'],
};

export const CREATE_SQL = `
CREATE TABLE IF NOT EXISTS \`exercises\` (\`id\` TEXT NOT NULL, \`name\` TEXT NOT NULL, \`kind\` TEXT NOT NULL, \`notes\` TEXT NOT NULL, \`archived\` INTEGER NOT NULL, \`builtin\` INTEGER NOT NULL, \`primaryMuscle\` TEXT, \`secondaryMuscles\` TEXT NOT NULL, \`stepKg\` REAL NOT NULL, \`modalityId\` TEXT, \`intensity\` TEXT, \`intervals\` INTEGER NOT NULL, \`warmupSec\` INTEGER NOT NULL, \`workSec\` INTEGER NOT NULL, \`restSec\` INTEGER NOT NULL, \`rounds\` INTEGER NOT NULL, \`fields\` TEXT NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`modalities\` (\`id\` TEXT NOT NULL, \`name\` TEXT NOT NULL, \`color\` INTEGER NOT NULL, \`archived\` INTEGER NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`programs\` (\`id\` TEXT NOT NULL, \`name\` TEXT NOT NULL, \`active\` INTEGER NOT NULL, \`sortOrder\` INTEGER NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`blocks\` (\`id\` TEXT NOT NULL, \`programId\` TEXT NOT NULL, \`sortOrder\` INTEGER NOT NULL, \`superset\` INTEGER NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`block_exercises\` (\`id\` TEXT NOT NULL, \`blockId\` TEXT NOT NULL, \`slot\` INTEGER NOT NULL, \`exerciseId\` TEXT NOT NULL, \`sets\` INTEGER NOT NULL, \`minReps\` INTEGER NOT NULL, \`maxReps\` INTEGER NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`sessions\` (\`id\` TEXT NOT NULL, \`date\` TEXT NOT NULL, \`startedAt\` INTEGER NOT NULL, \`finishedAt\` INTEGER, \`itemType\` TEXT NOT NULL, \`programId\` TEXT, \`programName\` TEXT, \`exerciseId\` TEXT, \`exerciseName\` TEXT, \`note\` TEXT NOT NULL, \`effort\` INTEGER, \`timeSec\` INTEGER, \`distanceM\` INTEGER, \`avgHr\` INTEGER, \`watts\` INTEGER, \`laps\` INTEGER, \`poolLength\` INTEGER, \`stroke\` TEXT, \`sourceId\` TEXT, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`set_logs\` (\`id\` TEXT NOT NULL, \`sessionId\` TEXT NOT NULL, \`blockIndex\` INTEGER NOT NULL, \`blockSuperset\` INTEGER NOT NULL, \`slot\` INTEGER NOT NULL, \`setIndex\` INTEGER NOT NULL, \`order\` INTEGER NOT NULL, \`exerciseId\` TEXT NOT NULL, \`exerciseName\` TEXT NOT NULL, \`minReps\` INTEGER NOT NULL, \`maxReps\` INTEGER NOT NULL, \`stepKg\` REAL NOT NULL, \`weightKg\` REAL, \`reps\` INTEGER, \`status\` TEXT NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`interval_logs\` (\`id\` TEXT NOT NULL, \`sessionId\` TEXT NOT NULL, \`index\` INTEGER NOT NULL, \`avgHr\` INTEGER, \`watts\` INTEGER, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`bodyweights\` (\`date\` TEXT NOT NULL, \`kg\` REAL NOT NULL, PRIMARY KEY(\`date\`));
CREATE TABLE IF NOT EXISTS \`week_plan\` (\`weekday\` INTEGER NOT NULL, \`itemType\` TEXT NOT NULL, \`itemId\` TEXT, PRIMARY KEY(\`weekday\`));
CREATE TABLE IF NOT EXISTS \`day_overrides\` (\`date\` TEXT NOT NULL, \`itemType\` TEXT NOT NULL, \`itemId\` TEXT, \`auto\` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(\`date\`));
CREATE TABLE IF NOT EXISTS \`stretches\` (\`id\` TEXT NOT NULL, \`name\` TEXT NOT NULL, \`sided\` INTEGER NOT NULL, \`mode\` TEXT NOT NULL, \`seconds\` INTEGER NOT NULL, \`reps\` INTEGER NOT NULL, \`figure\` TEXT NOT NULL, \`muscles\` TEXT NOT NULL, \`cue\` TEXT NOT NULL, \`link\` TEXT NOT NULL, \`builtin\` INTEGER NOT NULL, \`archived\` INTEGER NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`stretch_routines\` (\`id\` TEXT NOT NULL, \`name\` TEXT NOT NULL, \`active\` INTEGER NOT NULL, \`sortOrder\` INTEGER NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`routine_stretches\` (\`id\` TEXT NOT NULL, \`routineId\` TEXT NOT NULL, \`stretchId\` TEXT NOT NULL, \`sortOrder\` INTEGER NOT NULL, \`seconds\` INTEGER, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`stretch_sessions\` (\`id\` TEXT NOT NULL, \`routineId\` TEXT NOT NULL, \`routineName\` TEXT NOT NULL, \`date\` TEXT NOT NULL, \`startedAt\` INTEGER NOT NULL, \`totalSec\` INTEGER NOT NULL, \`completed\` INTEGER NOT NULL, \`skipped\` INTEGER NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`eating_window\` (\`id\` INTEGER NOT NULL, \`enabled\` INTEGER NOT NULL, \`startMinute\` INTEGER NOT NULL, \`endMinute\` INTEGER NOT NULL, \`remindBeforeMin\` INTEGER NOT NULL, PRIMARY KEY(\`id\`));
CREATE TABLE IF NOT EXISTS \`window_days\` (\`date\` TEXT NOT NULL, \`openedAt\` INTEGER, \`closedAt\` INTEGER, \`note\` TEXT NOT NULL, PRIMARY KEY(\`date\`));
`;

/** Indexes the Kotlin schema lacks; the hot lookups by session and by block. */
export const INDEX_SQL = `
CREATE INDEX IF NOT EXISTS idx_set_logs_session ON set_logs(sessionId);
CREATE INDEX IF NOT EXISTS idx_set_logs_exercise ON set_logs(exerciseId);
CREATE INDEX IF NOT EXISTS idx_interval_logs_session ON interval_logs(sessionId);
CREATE INDEX IF NOT EXISTS idx_blocks_program ON blocks(programId);
CREATE INDEX IF NOT EXISTS idx_block_exercises_block ON block_exercises(blockId);
CREATE INDEX IF NOT EXISTS idx_sessions_date ON sessions(date);
`;
