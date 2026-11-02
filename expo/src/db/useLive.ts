import { DependencyList, useEffect, useRef, useState } from 'react';
import { subscribe } from './database';
import { Table } from './schema';

/**
 * Runs a query and runs it again whenever a write touches one of the tables.
 * The React counterpart of collecting a Room Flow. `data` is undefined until the first result.
 */
export function useLive<T>(query: () => Promise<T>, tables: readonly Table[], deps: DependencyList = []) {
  const [data, setData] = useState<T | undefined>(undefined);
  const [error, setError] = useState<Error | null>(null);
  const run = useRef(0);

  useEffect(() => {
    let alive = true;
    const load = () => {
      const id = ++run.current;
      query()
        .then((v) => {
          // Drop results that a newer run already replaced.
          if (alive && id === run.current) {
            setData(v);
            setError(null);
          }
        })
        .catch((e: unknown) => {
          if (alive && id === run.current) setError(e instanceof Error ? e : new Error(String(e)));
        });
    };
    load();
    const off = subscribe(tables, load);
    return () => {
      alive = false;
      off();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tables.join(','), ...deps]);

  return { data, error, loading: data === undefined && error === null };
}
