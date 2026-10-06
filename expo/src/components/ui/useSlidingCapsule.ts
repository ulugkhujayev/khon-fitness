import { useEffect, useRef, useState } from 'react';
import { LayoutChangeEvent } from 'react-native';
import { useAnimatedStyle, useSharedValue, withSpring } from 'react-native-reanimated';
import { Motion } from '@/theme';

/**
 * A selection marker that slides between the equal slots of a row (the tab bar, a segmented control): pass
 * `onLayout` to the row and draw the marker `slot` wide with `style`. Its first place is set at once; later
 * changes slide on Motion.slide, on the UI thread. `slot` is 0 until the row is measured or with nothing chosen.
 */
export function useSlidingCapsule(index: number, count: number) {
  const [width, setWidth] = useState(0);
  const slot = width > 0 && count > 0 && index >= 0 ? width / count : 0;
  const x = useSharedValue(0);
  const placed = useRef(false);

  useEffect(() => {
    if (slot === 0) return;
    const target = index * slot;
    x.set(placed.current ? withSpring(target, Motion.slide) : target);
    placed.current = true;
  }, [index, slot, x]);

  const style = useAnimatedStyle(() => ({ transform: [{ translateX: x.get() }] }));
  return { slot, style, onLayout: (e: LayoutChangeEvent) => setWidth(e.nativeEvent.layout.width) };
}
