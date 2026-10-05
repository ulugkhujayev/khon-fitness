import { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SymbolView } from 'expo-symbols';
import { router } from 'expo-router';
import { K, Type } from '@/theme';

/**
 * Navigation bar (DESIGN.md): 56 pt, a round 36 pt Back button with a chevron and no text,
 * the title centered in Headline, the primary action trailing.
 */
export function NavBar({ title, trailing, onBack }: { title: string; trailing?: ReactNode; onBack?: () => void }) {
  return (
    <View style={styles.bar}>
      <View style={styles.side}>
        <Pressable
          onPress={onBack ?? (() => router.back())}
          accessibilityRole="button"
          accessibilityLabel="Back"
          hitSlop={4}
          style={({ pressed }) => [styles.back, pressed && { opacity: 0.6 }]}>
          <SymbolView name={{ ios: 'chevron.left', android: 'arrow_back_ios_new', web: 'arrow_back_ios_new' }} tintColor={K.Text} size={17} />
        </Pressable>
      </View>
      <Text style={[Type.headline, styles.title]} numberOfLines={1} accessibilityRole="header">
        {title}
      </Text>
      <View style={[styles.side, { alignItems: 'flex-end' }]}>{trailing}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { height: 56, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16 },
  side: { width: 88 },
  back: { width: 36, height: 36, borderRadius: 18, backgroundColor: K.Surface2, alignItems: 'center', justifyContent: 'center' },
  title: { flex: 1, textAlign: 'center' },
});
