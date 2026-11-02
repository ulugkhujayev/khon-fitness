import { useEffect, useRef } from 'react';
import { REPEAT_AFTER, repeatDelay } from '@/domain/stepper';

/**
 * Press handlers for a − or + button: a tap calls `onTap`; holding it calls `onRepeat` again and again,
 * faster the longer it is held, and `onRelease` once the finger lifts. Spread the result on a Pressable.
 */
export function useRepeatPress(onTap: () => void, onRepeat: () => void, onRelease: () => void) {
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const holding = useRef(false);

  const stop = () => {
    if (timer.current) clearTimeout(timer.current);
    timer.current = null;
  };
  // A screen that goes away mid-hold must not keep stepping.
  useEffect(() => () => stop(), []);

  const repeat = (n: number) => {
    onRepeat();
    timer.current = setTimeout(() => repeat(n + 1), repeatDelay(n));
  };

  return {
    onPress: onTap,
    onLongPress: () => {
      holding.current = true;
      repeat(0);
    },
    onPressOut: () => {
      if (!holding.current) return;
      holding.current = false;
      stop();
      onRelease();
    },
    delayLongPress: REPEAT_AFTER,
  };
}
