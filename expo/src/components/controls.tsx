import { Pressable, StyleSheet, Text, TextInput, View } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { Font, K, Type } from '@/theme';

/** Search field: magnifier, the text, a clear button once there is text. */
export function SearchField({
  value,
  onChange,
  elevated = false,
  autoFocus = false,
}: {
  value: string;
  onChange: (v: string) => void;
  /** On the elevated fill inside a sheet. */
  elevated?: boolean;
  autoFocus?: boolean;
}) {
  return (
    <View style={[styles.search, { backgroundColor: elevated ? K.Elevated : K.Surface }]}>
      <SymbolView name={{ ios: 'magnifyingglass', android: 'search', web: 'search' }} tintColor={K.Dim} size={18} />
      <TextInput
        value={value}
        onChangeText={onChange}
        placeholder="Search"
        placeholderTextColor={K.Dim}
        autoFocus={autoFocus}
        autoCorrect={false}
        autoCapitalize="none"
        returnKeyType="search"
        keyboardAppearance="dark"
        selectionColor={K.Accent}
        accessibilityLabel="Search"
        style={[Type.body, styles.searchInput]}
      />
      {value ? (
        <Pressable onPress={() => onChange('')} accessibilityRole="button" accessibilityLabel="Clear search" hitSlop={8}>
          <SymbolView name={{ ios: 'xmark.circle.fill', android: 'cancel', web: 'cancel' }} tintColor={K.Muted} size={18} />
        </Pressable>
      ) : null}
    </View>
  );
}

/** Fixed-width segmented control: every option gets an equal share, nothing scrolls. */
export function Segmented({ options, selected, onSelect }: { options: string[]; selected: number; onSelect: (i: number) => void }) {
  return (
    <View style={styles.segmented} accessibilityRole="tablist">
      {options.map((label, i) => {
        const active = i === selected;
        return (
          <Pressable
            key={label}
            onPress={() => onSelect(i)}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            style={[styles.segment, active && { backgroundColor: K.Surface3 }]}>
            <Text style={[styles.segmentText, { color: active ? K.Text : K.Muted }]}>{label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  search: { flexDirection: 'row', alignItems: 'center', height: 44, borderRadius: 10, paddingHorizontal: 12, gap: 8 },
  searchInput: { flex: 1, paddingVertical: 0, color: K.Text },
  segmented: { flexDirection: 'row', height: 36, borderRadius: 10, backgroundColor: K.Surface, padding: 3 },
  segment: { flex: 1, borderRadius: 8, alignItems: 'center', justifyContent: 'center' },
  segmentText: { fontFamily: Font.medium, fontSize: 15 },
});
