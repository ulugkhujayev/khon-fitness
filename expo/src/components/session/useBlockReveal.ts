import { useRef } from 'react';
import { LayoutAnimation, LayoutChangeEvent } from 'react-native';
import type { KeyboardAwareScrollViewRef } from 'react-native-keyboard-controller';

/** Room left above an opened block once it has scrolled to the top. */
const ABOVE = 8;
/** How long a block takes to open or close (ms). */
const DURATION = 250;

/**
 * Opens and closes a session's blocks smoothly: the change animates (LayoutAnimation, every frame that
 * moves), and a block that opens scrolls to the top of the list once its new frame is known, so its sets
 * are in view without hunting for them. `cover` is how much of the list's top a header over it hides.
 */
export function useBlockReveal(cover: number) {
  const scrollRef = useRef<KeyboardAwareScrollViewRef>(null);
  const wanted = useRef<number | null>(null);

  return {
    scrollRef,
    /** Call just before the open block changes, with the block that will be open (null: none). */
    reveal: (open: number | null) => {
      LayoutAnimation.configureNext(LayoutAnimation.create(DURATION, LayoutAnimation.Types.easeInEaseOut, LayoutAnimation.Properties.opacity));
      wanted.current = open;
    },
    /** From a block's onLayout: an opening block changes height, so this sees its final place. */
    onBlockLayout: (index: number, e: LayoutChangeEvent) => {
      if (wanted.current !== index) return;
      wanted.current = null;
      scrollRef.current?.scrollTo({ y: Math.max(0, e.nativeEvent.layout.y - cover - ABOVE), animated: true });
    },
  };
}
