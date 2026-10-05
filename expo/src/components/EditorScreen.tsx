import { ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { K, Space } from '@/theme';
import { NavBar } from './NavBar';

/**
 * Full-screen editor (DESIGN.md, Editors): Back at the left, the title centered, the primary action
 * trailing; the content scrolls and keeps the focused field above the keyboard.
 */
export function EditorScreen({
  title,
  trailing,
  onBack,
  children,
}: {
  title: string;
  trailing?: ReactNode;
  onBack?: () => void;
  children: ReactNode;
}) {
  const insets = useSafeAreaInsets();
  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <NavBar title={title} trailing={trailing} onBack={onBack} />
      <KeyboardAwareScrollView
        bottomOffset={24}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
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
