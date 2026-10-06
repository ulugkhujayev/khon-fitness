import { ReactNode } from 'react';
import { StyleSheet, Text } from 'react-native';
import { KeyboardAwareScrollView } from 'react-native-keyboard-controller';
import { SafeAreaView, useSafeAreaInsets } from 'react-native-safe-area-context';
import { K, Space, Type } from '@/theme';
import { NavBar } from './NavBar';
import { useScrollY } from './useScrollY';

/** The content's top padding, and the offset at which a large title (41 pt line) has gone under the bar. */
const TOP = 8;
const LARGE_AT = TOP + (Type.largeTitle.lineHeight ?? 41);

/**
 * Full-screen editor or list (DESIGN.md, Editors): Back at the left, the title centered, the primary action
 * trailing; the content scrolls and keeps the focused field above the keyboard, and a hairline shows under
 * the bar once content scrolls beneath it. `large` (lists such as Exercises) puts the title in Large Title
 * over the content instead, and the bar's title fades in as it scrolls under, as iOS does.
 * Without children it is the empty frame shown while the screen's data loads.
 */
export function EditorScreen({
  title = '',
  trailing,
  large = false,
  children,
}: {
  title?: string;
  trailing?: ReactNode;
  large?: boolean;
  children?: ReactNode;
}) {
  const insets = useSafeAreaInsets();
  const { scrollY, onScroll } = useScrollY();
  return (
    <SafeAreaView style={styles.safe} edges={['top']}>
      <NavBar title={title} trailing={trailing} scrollY={scrollY} titleAt={large ? LARGE_AT : undefined} />
      <KeyboardAwareScrollView
        bottomOffset={24}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
        onScroll={onScroll}
        contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 32 }]}>
        {/* The bar's own title is the one screen readers hear. */}
        {large ? (
          <Text style={[Type.largeTitle, styles.large]} importantForAccessibility="no" accessibilityElementsHidden>
            {title}
          </Text>
        ) : null}
        {children}
      </KeyboardAwareScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: K.Bg },
  content: { paddingHorizontal: Space.margin, paddingTop: TOP },
  large: { marginBottom: 12 },
});
