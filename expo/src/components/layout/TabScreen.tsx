import { ReactNode, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { ScreenTitle } from '@/components/ui';
import { K, Space } from '@/theme';
import { TAB_BAR_HEIGHT } from './TabBar';

/** Room between the end of the content and the floating tab bar, so the last row scrolls clear of it. */
const BAR_CLEARANCE = 32;

/**
 * A tab screen: the large title with its bar items stays put at the top (a hairline under it once the
 * content scrolls beneath), the content scrolls with 16 pt margins and clears the floating tab bar and
 * the keyboard.
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
  const [scrolled, setScrolled] = useState(false);
  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <View style={[styles.header, scrolled && styles.scrolled]}>
        <ScreenTitle text={title} trailing={trailing} />
      </View>
      <KeyboardAwareScrollView
        bottomOffset={24}
        scrollEnabled={scrollEnabled}
        // The bar floats over the screen's bottom edge, above the home indicator, so the inset counts too.
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + TAB_BAR_HEIGHT + BAR_CLEARANCE }]}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
        // React skips the render while the flag stays the same, so this costs nothing per frame.
        onScroll={(e) => setScrolled(e.nativeEvent.contentOffset.y > 0)}>
        {children}
      </KeyboardAwareScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  // The hairline is always there, clear until the content scrolls, so the header never changes height.
  header: { paddingHorizontal: Space.margin, backgroundColor: K.Bg, borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: 'transparent' },
  scrolled: { borderBottomColor: K.Divider },
  content: { paddingHorizontal: Space.margin, paddingTop: 4 },
});
