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

/** Entity -> bind values in COLUMNS order. Booleans become 0/1, undefined becomes null. */
export function toParams(table: Table, entity: object): (string | number | null)[] {
  const e = entity as Row;
  return COLUMNS[table].map((c) => {
    const v = e[c];
    if (v === undefined || v === null) return null;
    if (typeof v === 'boolean') return v ? 1 : 0;
    if (typeof v === 'number' || typeof v === 'string') return v;
    throw new Error(`${table}.${c}: unsupported value ${String(v)}`);
  });
}

/** INSERT OR REPLACE with every column, quoted (order and index are SQL keywords). */
export function upsertSql(table: Table): string {
  const cols = COLUMNS[table];
  return `INSERT OR REPLACE INTO \`${table}\` (${cols.map((c) => `\`${c}\``).join(', ')}) VALUES (${cols.map(() => '?').join(', ')})`;
}
