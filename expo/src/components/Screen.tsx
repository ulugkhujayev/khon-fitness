import { ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView } from 'react-native-safe-area-context';
import { K, Space } from '@/theme';

/** Height the floating tab bar takes over content: 60 bar + 10 gap + 6 top padding, plus breathing room. */
export const TAB_BAR_SPACE = 96;

/** A scrolling tab screen: black background, 16 pt margins, content clears the floating tab bar and the keyboard. */
export function TabScreen({ children }: { children: ReactNode }) {
  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <KeyboardAwareScrollView
        bottomOffset={24}
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}>
        {children}
      </KeyboardAwareScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  content: { paddingHorizontal: Space.margin, paddingBottom: TAB_BAR_SPACE },
});
