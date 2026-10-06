import { ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { K, Space } from '@/theme';
import { NavBar } from './NavBar';
import { useScrollY } from './useScrollY';

/**
 * Full-screen editor or list (DESIGN.md, Editors): Back at the left, the title centered, the primary action
 * trailing; the content scrolls and keeps the focused field above the keyboard, and a hairline shows under
 * the bar once content scrolls beneath it. Without children it is the empty frame shown while the screen's
 * data loads.
 */
export function EditorScreen({ title = '', trailing, children }: { title?: string; trailing?: ReactNode; children?: ReactNode }) {
  const insets = useSafeAreaInsets();
  const { scrollY, onScroll } = useScrollY();
  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <NavBar title={title} trailing={trailing} scrollY={scrollY} />
      <KeyboardAwareScrollView
        bottomOffset={24}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
        onScroll={onScroll}
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 32 }]}>
        {children}
      </KeyboardAwareScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  content: { paddingHorizontal: Space.margin, paddingTop: 8 },
});
