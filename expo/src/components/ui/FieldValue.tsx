import { StyleSheet, Text } from 'react-native';
import { Font, K, Type } from '@/theme';
import { Chevron } from './Chevron';

/** A select's current value in a field row, with a chevron: the row opens a sheet. Muted when locked. */
export function FieldValue({ text, muted = false }: { text: string; muted?: boolean }) {
  return (
    <>
      <Text style={[styles.text, muted && styles.muted]} numberOfLines={2}>
        {text}
      </Text>
      <Chevron />
    </>
  );
}

const styles = StyleSheet.create({
  text: { ...Type.body, fontFamily: Font.semibold, flexShrink: 1, textAlign: 'right' },
  muted: { color: K.Muted },
});
