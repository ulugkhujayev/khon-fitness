import { useRef } from 'react';
import { LayoutAnimation, LayoutChangeEvent } from 'react-native';
import type { KeyboardAwareScrollViewRef } from 'react-native-keyboard-controller';

/** How long a block takes to open or close (ms). */
const DURATION = 220;

/**
 * Opens and closes a session's blocks smoothly: the change animates (LayoutAnimation, every frame that
 * moves), and a block that opens scrolls up until its header is at the top, so its sets are in view.
 * The sets' frame is the one to watch: they mount as the block opens. Headers are sticky, so their own
 * frames are measured inside their sticky wrapper; only their heights are kept.
 */
export function useBlockReveal() {
  const scrollRef = useRef<KeyboardAwareScrollViewRef>(null);
  const wanted = useRef<number | null>(null);
  const headerHeights = useRef(new Map<number, number>());

  return {
    scrollRef,
    /** Call just before the open block changes, with the block that will be open (null: none). */
    reveal: (open: number | null) => {
      LayoutAnimation.configureNext(LayoutAnimation.create(DURATION, LayoutAnimation.Types.easeInEaseOut, LayoutAnimation.Properties.opacity));
      wanted.current = open;
    },
    onHeaderLayout: (index: number, e: LayoutChangeEvent) => {
      headerHeights.current.set(index, e.nativeEvent.layout.height);
    },
    onBodyLayout: (index: number, e: LayoutChangeEvent) => {
      if (wanted.current !== index) return;
      wanted.current = null;
      const top = e.nativeEvent.layout.y - (headerHeights.current.get(index) ?? 0);
      scrollRef.current?.scrollTo({ y: Math.max(0, top), animated: true });
    },
  };
}
