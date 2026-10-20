import { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Animated, { Extrapolation, interpolate, SharedValue, useAnimatedStyle, useSharedValue } from 'react-native-reanimated';
import { K, Type } from '@/theme';
import { BackButton } from './BackButton';

/** Scroll distance over which the hairline fades in. */
const FADE = 16;

/**
 * Navigation bar (DESIGN.md): 56 pt, the round Back button, the title centered in Headline, the action trailing.
 * Given the list under it (`scrollY`), a hairline fades in on its bottom edge once content scrolls beneath.
 */
export function NavBar({
  title,
  trailing,
  onBack,
  backLabel,
  scrollY,
}: {
  title: string;
  trailing?: ReactNode;
  onBack?: () => void;
  /** Spoken name of Back when it leads somewhere specific, e.g. "Today". */
  backLabel?: string;
  scrollY?: SharedValue<number>;
}) {
  const still = useSharedValue(0);
  const y = scrollY ?? still;
  const watching = scrollY != null;
  const hairline = useAnimatedStyle(() => ({ opacity: watching ? interpolate(y.get(), [0, FADE], [0, 1], Extrapolation.CLAMP) : 0 }));

  return (
    <View style={styles.bar}>
      <View style={styles.side}>
        <BackButton onPress={onBack} label={backLabel} />
      </View>
      <Text style={styles.title} numberOfLines={1} accessibilityRole="header">
        {title}
      </Text>
      <View style={[styles.side, styles.trailing]}>{trailing}</View>
      <Animated.View style={[styles.hairline, hairline]} />
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { height: 56, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16 },
  /** Equal sides keep the title centered; wide enough for a "Done" capsule. */
  side: { width: 88 },
  trailing: { alignItems: 'flex-end' },
  title: { ...Type.headline, flex: 1, textAlign: 'center' },
  hairline: { position: 'absolute', left: 0, right: 0, bottom: 0, height: StyleSheet.hairlineWidth, backgroundColor: K.Divider },
});
