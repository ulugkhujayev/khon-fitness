import { ReactNode, useEffect, useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { K, Type } from '@/theme';
import { getDb } from './database';
import { seedIfEmpty } from './repository';

/** Opens and migrates the database and seeds a fresh install before any screen reads from it. */
export function DataProvider({ children, onReady }: { children: ReactNode; onReady?: () => void }) {
  const [state, setState] = useState<'opening' | 'ready' | Error>('opening');

  useEffect(() => {
    getDb()
      .then(() => seedIfEmpty())
      .then(() => setState('ready'))
      .catch((e: unknown) => setState(e instanceof Error ? e : new Error(String(e))));
  }, []);

  useEffect(() => {
    if (state !== 'opening') onReady?.();
  }, [state, onReady]);

  if (state === 'opening') return null;
  if (state instanceof Error) {
    return (
      <View style={styles.failed}>
        <Text style={styles.title}>The database did not open</Text>
        <Text style={styles.message}>{state.message}</Text>
      </View>
    );
  }
  return <>{children}</>;
}

const styles = StyleSheet.create({
  failed: { flex: 1, backgroundColor: K.Bg, justifyContent: 'center', padding: 16 },
  title: { ...Type.headline, marginBottom: 8 },
  message: { ...Type.footnote, color: K.Muted },
});
