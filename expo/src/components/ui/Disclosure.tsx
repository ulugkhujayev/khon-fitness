import { useEffect } from 'react';
import { SymbolView } from 'expo-symbols';
import Animated, { useAnimatedStyle, useSharedValue, withTiming } from 'react-native-reanimated';
import { K } from '@/theme';

/** How long the chevron takes to turn (ms): as long as the section it opens (useBlockReveal). */
const TURN = 250;

/** Disclosure chevron: points right while its section is closed and turns down as it opens. Decorative. */
export function Disclosure({ open }: { open: boolean }) {
  const angle = useSharedValue(open ? 90 : 0);
  useEffect(() => {
    angle.set(withTiming(open ? 90 : 0, { duration: TURN }));
  }, [open, angle]);
  const turn = useAnimatedStyle(() => ({ transform: [{ rotate: `${angle.get()}deg` }] }));

  return (
    <Animated.View style={turn}>
      <SymbolView name={{ ios: 'chevron.right', android: 'chevron_right', web: 'chevron_right' }} tintColor={K.Dim} size={14} />
    </Animated.View>
  );
}
