import { Pressable, StyleSheet, TextInput, View } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { K, Type } from '@/theme';

/** Search field: magnifier, the text, a clear button once there is text. */
export function SearchField({ value, onChange, elevated = false }: { value: string; onChange: (v: string) => void; elevated?: boolean }) {
  return (
    <View style={[styles.box, elevated && styles.elevated]}>
      <SymbolView name={{ ios: 'magnifyingglass', android: 'search', web: 'search' }} tintColor={K.Dim} size={18} />
      <TextInput
        value={value}
        onChangeText={onChange}
        placeholder="Search"
        placeholderTextColor={K.Dim}
        autoCorrect={false}
        autoCapitalize="none"
        returnKeyType="search"
        keyboardAppearance="dark"
        selectionColor={K.Accent}
        accessibilityLabel="Search"
        style={styles.input}
      />
      {value ? (
        <Pressable onPress={() => onChange('')} accessibilityRole="button" accessibilityLabel="Clear search" hitSlop={8}>
          <SymbolView name={{ ios: 'xmark.circle.fill', android: 'cancel', web: 'cancel' }} tintColor={K.Muted} size={18} />
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  box: { flexDirection: 'row', alignItems: 'center', height: 44, borderRadius: 10, paddingHorizontal: 12, gap: 8, backgroundColor: K.Surface },
  /** Inside a sheet the field sits on the elevated fill. */
  elevated: { backgroundColor: K.Elevated },
  input: { ...Type.body, flex: 1, paddingVertical: 0 },
});
