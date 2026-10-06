import { useRef } from 'react';
import { LayoutAnimation, LayoutChangeEvent } from 'react-native';
import type { KeyboardAwareScrollViewRef } from 'react-native-keyboard-controller';

/** How long a block takes to open or close (ms). */
const DURATION = 220;
/** How long to wait for a block's new frame before scrolling to the one it has (ms). */
const SETTLE = 80;

/**
 * Animates a session's blocks opening and closing (LayoutAnimation, every frame that moves) and scrolls a
 * block up until its header is at the top. A block's sets report where they are: as they mount when it
 * opens, and whenever a block above opens or closes. Headers are sticky, so their own frames are measured
 * inside their sticky wrapper; only their heights are kept.
 */
export function useBlockReveal() {
  const scrollRef = useRef<KeyboardAwareScrollViewRef>(null);
  const wanted = useRef<number | null>(null);
  const headerHeights = useRef(new Map<number, number>());
  const bodyTops = useRef(new Map<number, number>());

  const scrollTo = (index: number) => {
    const y = bodyTops.current.get(index);
    if (y == null) return;
    wanted.current = null;
    scrollRef.current?.scrollTo({ y: Math.max(0, y - (headerHeights.current.get(index) ?? 0)), animated: true });
  };

  return {
    scrollRef,
    /** Animates the next change of the blocks' layout. */
    animate: () => LayoutAnimation.configureNext(LayoutAnimation.create(DURATION, LayoutAnimation.Types.easeInEaseOut, LayoutAnimation.Properties.opacity)),
    /** Brings a block to the top: at its sets' next layout, or where they are if nothing moves them. */
    reveal: (index: number) => {
      wanted.current = index;
      setTimeout(() => {
        if (wanted.current === index) scrollTo(index);
      }, SETTLE);
    },
    onHeaderLayout: (index: number, e: LayoutChangeEvent) => {
      headerHeights.current.set(index, e.nativeEvent.layout.height);
    },
    onBodyLayout: (index: number, e: LayoutChangeEvent) => {
      bodyTops.current.set(index, e.nativeEvent.layout.y);
      if (wanted.current === index) scrollTo(index);
    },
  };
}
