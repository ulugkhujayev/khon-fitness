import { BOOL_COLUMNS, COLUMNS, Table } from './schema';

type Row = Record<string, unknown>;

/** SQLite row -> entity: 0/1 back to booleans for the table's boolean columns. */
export function fromRow<T>(table: Table, row: Row): T {
  const bools = BOOL_COLUMNS[table];
  if (bools.length === 0) return row as T;
  const out: Row = { ...row };
  for (const c of bools) out[c] = out[c] === 1 || out[c] === true;
  return out as T;
}

function bind(table: Table, column: string, v: unknown): string | number | null {
  if (v === undefined || v === null) return null;
  if (typeof v === 'boolean') return v ? 1 : 0;
  if (typeof v === 'number' || typeof v === 'string') return v;
  throw new Error(`${table}.${column}: unsupported value ${String(v)}`);
}

/** Entity -> bind values in COLUMNS order. Booleans become 0/1, undefined becomes null. */
export function toParams(table: Table, entity: object): (string | number | null)[] {
  const e = entity as Row;
  return COLUMNS[table].map((c) => bind(table, c, e[c]));
}

/**
 * UPDATE of only the given columns of one row by id, so a screen holding an older copy of the row
 * never writes back fields it did not change.
 */
export function patchStatement(table: Table, id: string, patch: object): { sql: string; params: (string | number | null)[] } {
  const keys = Object.keys(patch).filter((k) => k !== 'id');
  for (const k of keys) if (!COLUMNS[table].includes(k)) throw new Error(`${table}: no column ${k}`);
  if (keys.length === 0) throw new Error(`${table}: empty patch`);
  return {
    sql: `UPDATE \`${table}\` SET ${keys.map((k) => `\`${k}\` = ?`).join(', ')} WHERE id = ?`,
    params: [...keys.map((k) => bind(table, k, (patch as Row)[k])), id],
  };
}

/** INSERT OR REPLACE with every column, quoted (order and index are SQL keywords). */
export function upsertSql(table: Table): string {
  const cols = COLUMNS[table];
  return `INSERT OR REPLACE INTO \`${table}\` (${cols.map((c) => `\`${c}\``).join(', ')}) VALUES (${cols.map(() => '?').join(', ')})`;
}
