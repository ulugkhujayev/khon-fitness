import * as SQLite from 'expo-sqlite';
import { fromRow, patchStatement, toParams, upsertSql } from './rows';
import { CREATE_SQL, INDEX_SQL, SCHEMA_VERSION, Table } from './schema';

type Param = string | number | null;

/** Same file name as Room uses, so a future native migration can pick it up. */
const DB_NAME = 'khon.db';

let opening: Promise<SQLite.SQLiteDatabase> | null = null;

async function open(): Promise<SQLite.SQLiteDatabase> {
  const db = await SQLite.openDatabaseAsync(DB_NAME);
  await db.execAsync('PRAGMA journal_mode = WAL;');
  const row = await db.getFirstAsync<{ user_version: number }>('PRAGMA user_version');
  const version = row?.user_version ?? 0;
  if (version < SCHEMA_VERSION) {
    // Fresh install: the whole v4 schema. Later versions add their steps here, keyed on `version`.
    await db.withTransactionAsync(async () => {
      await db.execAsync(CREATE_SQL);
      await db.execAsync(INDEX_SQL);
      await db.execAsync(`PRAGMA user_version = ${SCHEMA_VERSION}`);
    });
  }
  return db;
}

export function getDb(): Promise<SQLite.SQLiteDatabase> {
  if (!opening) opening = open();
  return opening;
}

// ---------- change notifications (what Room's Flow gave the Kotlin app) ----------

type Listener = () => void;
const listeners = new Map<Table, Set<Listener>>();

export function subscribe(tables: readonly Table[], fn: Listener): () => void {
  for (const t of tables) {
    if (!listeners.has(t)) listeners.set(t, new Set());
    listeners.get(t)!.add(fn);
  }
  return () => {
    for (const t of tables) listeners.get(t)?.delete(fn);
  };
}

function notify(tables: Iterable<Table>) {
  const fns = new Set<Listener>();
  for (const t of tables) listeners.get(t)?.forEach((f) => fns.add(f));
  fns.forEach((f) => f());
}

// ---------- reads ----------

export async function all<T>(table: Table, where = '', params: Param[] = [], orderBy = ''): Promise<T[]> {
  const db = await getDb();
  const sql = `SELECT * FROM \`${table}\`${where ? ` WHERE ${where}` : ''}${orderBy ? ` ORDER BY ${orderBy}` : ''}`;
  const rows = await db.getAllAsync<Record<string, unknown>>(sql, params);
  return rows.map((r) => fromRow<T>(table, r));
}

export async function first<T>(table: Table, where: string, params: Param[] = []): Promise<T | null> {
  const rows = await all<T>(table, where, params);
  return rows[0] ?? null;
}

export async function count(sql: string, params: Param[] = []): Promise<number> {
  const db = await getDb();
  const row = await db.getFirstAsync<{ n: number }>(sql, params);
  return row?.n ?? 0;
}

// ---------- writes ----------

/** Writes inside one transaction. `touched` collects the tables to notify once it commits. */
export interface Tx {
  upsert(table: Table, rows: object | object[]): Promise<void>;
  /** Sets only the given columns of the row with this id. */
  patch(table: Table, id: string, patch: object): Promise<void>;
  run(table: Table, sql: string, params?: Param[]): Promise<void>;
  clear(table: Table): Promise<void>;
}

let queue: Promise<unknown> = Promise.resolve();

/**
 * Runs writes one at a time in a transaction, then notifies listeners of the tables it touched.
 * Serialized because expo-sqlite's withTransactionAsync does not stop a second caller from interleaving.
 */
export function write<R>(fn: (tx: Tx) => Promise<R>): Promise<R> {
  const job = queue.then(async () => {
    const db = await getDb();
    const touched = new Set<Table>();
    const tx: Tx = {
      async upsert(table, rows) {
        const list = Array.isArray(rows) ? rows : [rows];
        if (list.length === 0) return;
        const stmt = await db.prepareAsync(upsertSql(table));
        try {
          for (const r of list) await stmt.executeAsync(toParams(table, r));
        } finally {
          await stmt.finalizeAsync();
        }
        touched.add(table);
      },
      async patch(table, id, patch) {
        const { sql, params } = patchStatement(table, id, patch);
        await db.runAsync(sql, params);
        touched.add(table);
      },
      async run(table, sql, params = []) {
        await db.runAsync(sql, params);
        touched.add(table);
      },
      async clear(table) {
        await db.runAsync(`DELETE FROM \`${table}\``);
        touched.add(table);
      },
    };
    let result!: R;
    await db.withTransactionAsync(async () => {
      result = await fn(tx);
    });
    notify(touched);
    return result;
  });
  queue = job.catch(() => undefined);
  return job;
}
