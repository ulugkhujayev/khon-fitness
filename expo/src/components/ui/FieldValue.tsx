import { StyleSheet, Text } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { Font, K, Type } from '@/theme';
import { Chevron } from './Chevron';

/**
 * A select's current value in a field row, with a chevron: the row opens a sheet. Locked, the value is muted
 * and a small lock takes the chevron's place, so the row does not look like it opens anything.
 */
export function FieldValue({ text, locked = false }: { text: string; locked?: boolean }) {
  return (
    <>
      <Text style={[styles.text, locked && styles.muted]} numberOfLines={2}>
        {text}
      </Text>
      {locked ? <SymbolView name={{ ios: 'lock.fill', android: 'lock', web: 'lock' }} tintColor={K.Dim} size={13} style={styles.lock} /> : <Chevron />}
    </>
  );
}

const styles = StyleSheet.create({
  text: { ...Type.body, fontFamily: Font.semibold, flexShrink: 1, textAlign: 'right' },
  muted: { color: K.Muted },
  lock: { marginLeft: 8 },
});
