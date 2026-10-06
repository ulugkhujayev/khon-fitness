import { StyleSheet, Text, View } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { noResults } from '@/domain/editors';
import { K, Type } from '@/theme';

/** Size of the magnifying glass (not in DESIGN.md; the system no-results view's symbol). */
const GLASS = 32;

/**
 * What a list under a search shows when nothing matches (iOS ContentUnavailableView.search): a magnifying
 * glass, "No Results for “row”" and what to try. Used in screens and in pick sheets alike.
 */
export function SearchEmpty({ query }: { query: string }) {
  const { title, hint } = noResults(query);
  return (
    <View style={styles.box} accessible accessibilityLabel={`${title}. ${hint}`}>
      <SymbolView name={{ ios: 'magnifyingglass', android: 'search', web: 'search' }} tintColor={K.Muted} size={GLASS} />
      <Text style={styles.title}>{title}</Text>
      <Text style={styles.hint}>{hint}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  box: { alignItems: 'center', paddingVertical: 28, paddingHorizontal: 24 },
  title: { ...Type.headline, textAlign: 'center', marginTop: 10 },
  hint: { ...Type.subheadline, color: K.Muted, textAlign: 'center', marginTop: 4 },
});
