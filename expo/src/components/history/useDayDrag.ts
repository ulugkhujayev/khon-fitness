import { useRef, useState } from 'react';
import { Animated, GestureResponderEvent, View } from 'react-native';
import { cellAt, dropAction } from '@/domain/history';

/** Where a lifted day is: the date it came from and the date under the finger. */
export interface DayDrag {
  from: string;
  over: string | null;
}

/**
 * Long press a day, then drag it (HistoryScreen.kt): released on its own day it opens the plan, on another
 * day ahead it swaps the two. Moves come from raw touch events on the grid, which keep arriving while the
 * pressed cell holds the touch. The page position is measured when the press starts, since the grid may have
 * scrolled; the caller pauses scrolling through `onActive` while a day is lifted.
 */
export function useDayDrag({
  dates,
  rows,
  rowHeight,
  today,
  onPlan,
  onSwap,
  onActive,
}: {
  /** Every cell of the grid, row by row. */
  dates: string[];
  rows: number;
  rowHeight: number;
  today: string;
  onPlan: (date: string) => void;
  onSwap: (a: string, b: string) => void;
  onActive: (active: boolean) => void;
}) {
  const grid = useRef<View>(null);
  const frame = useRef({ x: 0, y: 0, width: 0, startX: 0, startY: 0 });
  const [drag, setDrag] = useState<DayDrag | null>(null);
  const [offset] = useState(() => new Animated.ValueXY());

  const overAt = (pageX: number, pageY: number) => {
    const f = frame.current;
    const i = cellAt(pageX - f.x, pageY - f.y, f.width, rowHeight, rows);
    return i >= 0 ? dates[i] : null;
  };

  const begin = (date: string, e: GestureResponderEvent) => {
    const { pageX, pageY } = e.nativeEvent;
    frame.current.startX = pageX;
    frame.current.startY = pageY;
    grid.current?.measureInWindow((x, y, width) => {
      frame.current = { ...frame.current, x, y, width };
    });
    offset.setValue({ x: 0, y: 0 });
    setDrag({ from: date, over: date });
    onActive(true);
  };

  const move = (e: GestureResponderEvent) => {
    if (!drag) return;
    const { pageX, pageY } = e.nativeEvent;
    offset.setValue({ x: pageX - frame.current.startX, y: pageY - frame.current.startY });
    const over = overAt(pageX, pageY);
    if (over !== drag.over) setDrag({ ...drag, over });
  };

  const end = (cancelled: boolean) => {
    if (!drag) return;
    const action = cancelled ? null : dropAction(drag.from, drag.over, today);
    if (action === 'plan') onPlan(drag.from);
    else if (action === 'swap' && drag.over) onSwap(drag.from, drag.over);
    setDrag(null);
    onActive(false);
  };

  return {
    drag,
    offset,
    begin,
    gridProps: {
      ref: grid,
      onTouchMove: move,
      onTouchEnd: () => end(false),
      onTouchCancel: () => end(true),
    },
  };
}
