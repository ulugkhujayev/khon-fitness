import { StyleSheet } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { K } from '@/theme';

/** Small trailing chevron in tertiary label: the row opens something. */
export function Chevron() {
  return (
    <SymbolView name={{ ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }} tintColor={K.Dim} size={14} style={styles.icon} />
  );
}

const styles = StyleSheet.create({
  icon: { marginLeft: 8 },
});
