/**
 * Runs the real schema in SQLite (bun:sqlite) and pushes every seed row through the same
 * upsertSql / toParams / fromRow the app uses, then a Kotlin-style backup through parseBackup.
 */
import { Database } from 'bun:sqlite';
import { describe, expect, test } from 'bun:test';
import { parseBackup } from '../src/domain/backup';
import * as S from '../src/domain/seed';
import { fromRow, patchStatement, toParams, upsertSql } from '../src/db/rows';
import { ALL_TABLES, COLUMNS, CREATE_SQL, INDEX_SQL, Table, TABLES as T } from '../src/db/schema';

function fresh() {
  const db = new Database(':memory:');
  db.exec(CREATE_SQL);
  db.exec(INDEX_SQL);
  return db;
}

function put(db: Database, table: Table, rows: object[]) {
  const stmt = db.prepare(upsertSql(table));
  for (const r of rows) stmt.run(...toParams(table, r));
}

const read = <X>(db: Database, table: Table) => (db.query(`SELECT * FROM \`${table}\``).all() as Record<string, unknown>[]).map((r) => fromRow<X>(table, r));

describe('schema', () => {
  test('creates all 17 tables with the columns the code writes', () => {
    const db = fresh();
    for (const t of ALL_TABLES) {
      const cols = (db.query(`PRAGMA table_info(\`${t}\`)`).all() as { name: string }[]).map((c) => c.name);
      expect(cols).toEqual(COLUMNS[t]);
    }
    expect(ALL_TABLES.length).toBe(17);
  });

  test('seed rows survive a write and read with booleans and nulls intact', () => {
    const db = fresh();
    const sets: [Table, object[]][] = [
      [T.modalities, S.SEED_MODALITIES], [T.exercises, S.SEED_EXERCISES], [T.programs, [S.SEED_PROGRAM]], [T.blocks, S.SEED_BLOCKS],
      [T.blockExercises, S.SEED_BLOCK_EXERCISES], [T.weekPlan, S.SEED_WEEK_PLAN], [T.stretches, S.SEED_STRETCHES],
      [T.stretchRoutines, [S.SEED_ROUTINE]], [T.routineStretches, S.SEED_ROUTINE_STRETCHES], [T.eatingWindow, [S.SEED_EATING_WINDOW]],
    ];
    for (const [t, rows] of sets) {
      put(db, t, rows);
      const key = COLUMNS[t][0];
      const sort = (xs: object[]) => [...xs].sort((a, b) => String((a as never)[key]).localeCompare(String((b as never)[key])));
      expect(sort(read(db, t))).toEqual(sort(rows));
    }
  });

  test('upsert replaces by primary key, and the quoted order and index columns work', () => {
    const db = fresh();
    const log = {
      id: 'x', sessionId: 's', blockIndex: 0, blockSuperset: true, slot: 0, setIndex: 0, order: 3, exerciseId: 'bench', exerciseName: 'Bench',
      minReps: 8, maxReps: 12, stepKg: 2.5, weightKg: null, reps: 8, status: 'pending',
    };
    put(db, T.setLogs, [log, { ...log, weightKg: 62.5, status: 'done' }]);
    expect(read(db, T.setLogs)).toEqual([{ ...log, weightKg: 62.5, status: 'done' }]);
    put(db, T.intervalLogs, [{ id: 'i', sessionId: 's', index: 2, avgHr: 150, watts: null }]);
    expect(db.query('SELECT * FROM interval_logs ORDER BY `index`').all()).toEqual([{ id: 'i', sessionId: 's', index: 2, avgHr: 150, watts: null }]);
  });

  test('a patch writes only its own columns, so an older copy of the row cannot undo a newer change', () => {
    const db = fresh();
    put(db, T.sessions, [{ id: 's', date: '2026-10-05', startedAt: 1, itemType: 'program', note: '', effort: null }]);
    const effort = patchStatement(T.sessions, 's', { effort: 7 });
    db.prepare(effort.sql).run(...effort.params);
    const note = patchStatement(T.sessions, 's', { note: 'felt good' });
    db.prepare(note.sql).run(...note.params);
    expect(read<{ note: string; effort: number }>(db, T.sessions)[0]).toMatchObject({ note: 'felt good', effort: 7 });
    const order = patchStatement(T.setLogs, 'x', { order: 1, blockSuperset: false });
    expect(order.sql).toBe('UPDATE `set_logs` SET `order` = ?, `blockSuperset` = ? WHERE id = ?');
    expect(order.params).toEqual([1, 0, 'x']);
    expect(() => patchStatement(T.sessions, 's', { nope: 1 })).toThrow();
  });
});

describe('Kotlin backup', () => {
  /** What kotlinx.serialization writes: default values left out, colors as ARGB longs. */
  const kotlinJson = JSON.stringify({
    exercises: [
      { id: 'bench', name: 'Barbell Bench Press', kind: 'strength', builtin: true, primaryMuscle: 'chest', secondaryMuscles: 'shoulders,triceps' },
      { id: 'tabata', name: 'Tabata bike', kind: 'cardio', builtin: true, modalityId: 'bike', intensity: 'high', intervals: true, workSec: 20, restSec: 10, rounds: 8, fields: 'time,avgHr,watts' },
    ],
    modalities: [{ id: 'bike', name: 'Bike', color: 4289961471 }],
    programs: [{ id: 'tpv3', name: 'TP v3', active: true }],
    blocks: [{ id: 'tpv3-b0', programId: 'tpv3', sortOrder: 0 }],
    blockExercises: [{ id: 'tpv3-b0-a', blockId: 'tpv3-b0', slot: 0, exerciseId: 'bench' }],
    sessions: [{ id: 'a1b2c3d4', date: '2026-09-22', startedAt: 1790000000000, finishedAt: 1790003600000, itemType: 'program', programId: 'tpv3', programName: 'TP v3' }],
    setLogs: [{ id: 'l1', sessionId: 'a1b2c3d4', blockIndex: 0, blockSuperset: true, slot: 0, setIndex: 0, order: 0, exerciseId: 'bench', exerciseName: 'Barbell Bench Press', minReps: 8, maxReps: 12, stepKg: 2.5, weightKg: 60.0, reps: 10, status: 'done' }],
    intervalLogs: [],
    bodyweights: [{ date: '2026-09-22', kg: 78.4 }],
    weekPlan: [{ weekday: 2, itemType: 'program', itemId: 'tpv3' }, { weekday: 7, itemType: 'rest' }],
    overrides: [],
    eatingWindow: { enabled: true, startMinute: 660 },
    unknownFutureKey: 1,
  });

  test('fills Kotlin defaults, keeps given values and writes cleanly into SQLite', () => {
    const b = parseBackup(kotlinJson);
    expect(b.exercises[0]).toMatchObject({ stepKg: 2.5, notes: '', archived: false, intervals: false, modalityId: null, fields: '' });
    expect(b.exercises[1]).toMatchObject({ intervals: true, rounds: 8, warmupSec: 0, primaryMuscle: null });
    expect(b.programs[0]).toMatchObject({ active: true, sortOrder: 0 });
    expect(b.blocks[0].superset).toBe(true);
    expect(b.blockExercises[0]).toMatchObject({ sets: 3, minReps: 8, maxReps: 12 });
    expect(b.sessions[0]).toMatchObject({ note: '', effort: null, sourceId: null });
    expect(b.weekPlan[1].itemId).toBeNull();
    expect(b.eatingWindow).toEqual({ id: 1, enabled: true, startMinute: 660, endMinute: 1200, remindBeforeMin: 15 });
    expect(b.stretches).toEqual([]);

    const db = fresh();
    put(db, T.exercises, b.exercises);
    put(db, T.modalities, b.modalities);
    put(db, T.sessions, b.sessions);
    put(db, T.setLogs, b.setLogs);
    put(db, T.weekPlan, b.weekPlan);
    put(db, T.eatingWindow, [b.eatingWindow!]);
    expect(read(db, T.exercises)).toEqual(b.exercises);
    expect(read(db, T.modalities)).toEqual(b.modalities);
    expect(read(db, T.setLogs)).toEqual(b.setLogs);
  });

  test('our export reads back to the same data', () => {
    const b = parseBackup(kotlinJson);
    expect(parseBackup(JSON.stringify(b))).toEqual(b);
  });

  test('rejects files that are not a Khon backup', () => {
    expect(() => parseBackup('not json')).toThrow('not JSON');
    expect(() => parseBackup('[]')).toThrow('Not a Khon backup');
    expect(() => parseBackup('{"exercises": []}')).toThrow('"modalities" is missing');
    expect(() => parseBackup(kotlinJson.replace('"kind":"strength",', ''))).toThrow('exercises[0] has no "kind"');
  });
});
