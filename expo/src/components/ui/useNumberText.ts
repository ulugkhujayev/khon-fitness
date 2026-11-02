import { useEffect, useRef, useState } from 'react';
import { fmt } from '@/domain/format';

/**
 * The text of a number input. While focused the input keeps its own text, so a value that comes back
 * from the database never moves the cursor or reformats "80." under the user; outside values land
 * only while it is not focused.
 */
export function useNumberText(value: number | null, decimals: number) {
  const [text, setText] = useState(fmt(value, decimals));
  const focused = useRef(false);

  useEffect(() => {
    if (!focused.current) setText(fmt(value, decimals));
  }, [value, decimals]);

  return {
    text,
    setText,
    focus: () => {
      focused.current = true;
    },
    /** Leaves typing and shows `shown`, the value the field stands for now. */
    blur: (shown: number | null) => {
      focused.current = false;
      setText(fmt(shown, decimals));
    },
  };
}
