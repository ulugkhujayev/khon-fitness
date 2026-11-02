import { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { K, Space } from '@/theme';
import { LARGE_TITLE_HEIGHT, LargeTitleHeader } from './LargeTitleHeader';
import { TAB_BAR_HEIGHT } from './TabBar';
import { useScrollY } from './useScrollY';

/** Room between the end of the content and the floating tab bar, so the last row scrolls clear of it. */
const BAR_CLEARANCE = 32;

/**
 * A tab screen: the large title with its bar items stays at the top and folds as the content scrolls under
 * it; the content scrolls with 16 pt margins and clears the floating tab bar and the keyboard.
 */
export function TabScreen({
  title,
  trailing,
  children,
  scrollEnabled = true,
}: {
  title: string;
  trailing?: ReactNode;
  children: ReactNode;
  /** Off while a drag inside owns the touch. */
  scrollEnabled?: boolean;
}) {
  const insets = useSafeAreaInsets();
  const { scrollY, onScroll } = useScrollY();

  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      {/* The header sits over this frame, not the safe area's padding, so its top is the inset's edge. */}
      <View style={styles.frame}>
        <KeyboardAwareScrollView
          bottomOffset={24}
          scrollEnabled={scrollEnabled}
          // The bar floats over the screen's bottom edge, above the home indicator, so the inset counts too.
          contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + TAB_BAR_HEIGHT + BAR_CLEARANCE }]}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
          onScroll={onScroll}>
          {children}
        </KeyboardAwareScrollView>
        <LargeTitleHeader title={title} trailing={trailing} scrollY={scrollY} />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  frame: { flex: 1 },
  content: { paddingHorizontal: Space.margin, paddingTop: LARGE_TITLE_HEIGHT + 4 },
});
