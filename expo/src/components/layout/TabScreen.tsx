import { ReactNode } from 'react';
import { StyleSheet } from 'react-native';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { K, Space } from '@/theme';
import { TAB_BAR_HEIGHT } from './TabBar';

/** Room between the end of the content and the floating tab bar, so the last row scrolls clear of it. */
const BAR_CLEARANCE = 32;

/** A scrolling tab screen: black background, 16 pt margins, content clears the floating tab bar and the keyboard. */
export function TabScreen({ children, scrollEnabled = true }: { children: ReactNode; /** Off while a drag inside owns the touch. */ scrollEnabled?: boolean }) {
  const insets = useSafeAreaInsets();
  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <KeyboardAwareScrollView
        bottomOffset={24}
        scrollEnabled={scrollEnabled}
        // The bar floats over the screen's bottom edge, above the home indicator, so the inset counts too.
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + TAB_BAR_HEIGHT + BAR_CLEARANCE }]}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}>
        {children}
      </KeyboardAwareScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  content: { paddingHorizontal: Space.margin },
});
