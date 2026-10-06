import { useEffect, useRef, useState } from 'react';
import { LayoutAnimation, LayoutChangeEvent } from 'react-native';
import type { KeyboardAwareScrollViewRef } from 'react-native-keyboard-controller';
import { useAnimatedReaction, useAnimatedScrollHandler, useAnimatedStyle, useSharedValue } from 'react-native-reanimated';
import { scheduleOnRN } from 'react-native-worklets';
import { BlockSpan, blockSpans, Frame, pinnedHeader } from '@/domain/workout';

/** How long a block takes to open or close (ms). */
const DURATION = 220;
/** How long to wait for a block's new frame before scrolling to the one it has (ms). */
const SETTLE = 80;

/**
 * Scrolling of a session's blocks. Headers and sets report their frames; from those and the scroll offset
 * the UI thread works out which open block's header has gone under the top, and `pinStyle` moves a pinned
 * copy of it there until its card's end pushes it off (pinnedHeader). The list's own sticky headers kept
 * a stale offset after blocks opened or closed, so this replaces them. Also: blocks opening and closing
 * animate, an opened block scrolls up to its header, and a block closed while the list is inside it keeps
 * its header in view instead of dropping the list into the blocks below.
 */
export function useBlockScroll(open: boolean[]) {
  const scrollRef = useRef<KeyboardAwareScrollViewRef>(null);
  const headers = useRef(new Map<number, Frame>());
  const bodies = useRef(new Map<number, Frame>());
  const openRef = useRef(open);
  const wanted = useRef<number | null>(null);
  const scrollY = useSharedValue(0);
  const spans = useSharedValue<BlockSpan[]>([]);
  // The block the pinned copy shows now: the copy stays hidden until it shows the block the UI pins.
  const shown = useSharedValue(-1);
  const [pinned, setPinned] = useState<number | null>(null);

  const publish = () => spans.set(blockSpans(openRef.current, headers.current, bodies.current));

  // Every render may open or close a block; a closed one's sets report nothing, so republish here.
  useEffect(() => {
    openRef.current = open;
    publish();
  });
  useEffect(() => {
    shown.set(pinned ?? -1);
  }, [pinned, shown]);

  useAnimatedReaction(
    () => pinnedHeader(spans.get(), scrollY.get())?.index ?? -1,
    (index, previous) => {
      if (index !== previous) scheduleOnRN(setPinned, index < 0 ? null : index);
    },
  );
  const pinStyle = useAnimatedStyle(() => {
    const pin = pinnedHeader(spans.get(), scrollY.get());
    return { opacity: pin != null && pin.index === shown.get() ? 1 : 0, transform: [{ translateY: pin?.shift ?? 0 }] };
  });
  const onScroll = useAnimatedScrollHandler((e) => {
    scrollY.set(e.contentOffset.y);
  });

  const scrollToTop = (y: number) => {
    wanted.current = null;
    scrollRef.current?.scrollTo({ y: Math.max(0, y), animated: true });
  };

  return {
    scrollRef,
    onScroll,
    pinned,
    pinStyle,
    /** Animates the next change of the blocks' layout. */
    animate: () => LayoutAnimation.configureNext(LayoutAnimation.create(DURATION, LayoutAnimation.Types.easeInEaseOut, LayoutAnimation.Properties.opacity)),
    /** Brings a block's header to the top: at its next layout, or where it is if nothing moves it. */
    reveal: (index: number) => {
      wanted.current = index;
      setTimeout(() => {
        const header = headers.current.get(index);
        if (wanted.current === index && header) scrollToTop(header.y);
      }, SETTLE);
    },
    /** Before a block closes: if the list is scrolled into it, its header comes back to the top. */
    keepInView: (index: number) => {
      const header = headers.current.get(index);
      if (header && scrollY.get() > header.y) scrollToTop(header.y);
    },
    onHeaderLayout: (index: number, e: LayoutChangeEvent) => {
      const { y, height } = e.nativeEvent.layout;
      headers.current.set(index, { y, height });
      publish();
      if (wanted.current === index) scrollToTop(y);
    },
    onBodyLayout: (index: number, e: LayoutChangeEvent) => {
      const { y, height } = e.nativeEvent.layout;
      bodies.current.set(index, { y, height });
      publish();
      // The sets follow their header, so this finds the header's top even before its own layout arrives.
      if (wanted.current === index) scrollToTop(y - (headers.current.get(index)?.height ?? 0));
    },
  };
}
