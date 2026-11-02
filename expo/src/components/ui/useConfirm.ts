import { useState } from 'react';

/**
 * What a ConfirmSheet asks about: `ask(item)` opens it, `close()` lets it slide out while `item` stays, so
 * the sheet keeps its text until it is gone (the open flag next to the data, as sheets want it).
 */
export function useConfirm<T>() {
  const [item, setItem] = useState<T | null>(null);
  const [open, setOpen] = useState(false);
  return {
    item,
    open,
    ask: (next: T) => {
      setItem(next);
      setOpen(true);
    },
    close: () => setOpen(false),
  };
}
