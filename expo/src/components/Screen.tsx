import { ReactNode } from 'react';
import { ScrollView, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { K, Space } from '@/theme';

/** Height the floating tab bar takes over content: 60 bar + 10 gap + 6 top padding, plus breathing room. */
export const TAB_BAR_SPACE = 96;

/** A scrolling tab screen: black background, 16 pt margins, content clears the floating tab bar. */
export function TabScreen({ children }: { children: ReactNode }) {
  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <ScrollView
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}>
        {children}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  content: { paddingHorizontal: Space.margin, paddingBottom: TAB_BAR_SPACE },
});
