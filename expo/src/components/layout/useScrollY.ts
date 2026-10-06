import { useAnimatedScrollHandler, useSharedValue } from 'react-native-reanimated';

/** The scroll offset of a list on the UI thread, for headers that fold as it scrolls. Pass `onScroll` to the list. */
export function useScrollY() {
  const scrollY = useSharedValue(0);
  const onScroll = useAnimatedScrollHandler((e) => {
    scrollY.set(e.contentOffset.y);
  });
  return { scrollY, onScroll };
}
