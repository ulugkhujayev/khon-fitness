import { useState } from 'react';
import * as repo from '@/db/repository';
import { SetLog, SetStatus } from '@/domain/types';
import { afterCheck, groupBlocks, openBlocks } from '@/domain/workout';
import { useBlockReveal } from './useBlockReveal';

/**
 * Which blocks of a session are open and what a tap on a set's circle does. Blocks with work left are
 * open and finished ones closed, so scrolling runs from one block to the next; a tap on a header opens or
 * closes that block. A check saves the set and focus moves to the set afterCheck names; a block's last
 * set closes it and brings the next block with work to the top. Changes animate (useBlockReveal).
 */
export function useBlockFlow(logs: SetLog[], focusField: (id: string) => void) {
  // Blocks the user or a finished block opened or closed, by position; the rest follow openBlocks.
  const [chosen, setChosen] = useState<Record<number, boolean>>({});
  const { scrollRef, animate, reveal, onHeaderLayout, onBodyLayout } = useBlockReveal();
  const blocks = groupBlocks(logs);
  const open = openBlocks(blocks, chosen);

  const toggleBlock = (i: number) => {
    animate();
    setChosen((c) => ({ ...c, [i]: !open[i] }));
    if (!open[i]) reveal(i);
  };

  const toggleSet = (log: SetLog) => {
    const next = log.status === SetStatus.DONE ? SetStatus.PENDING : SetStatus.DONE;
    repo.patchSetLog(log.id, { status: next });
    if (next !== SetStatus.DONE) return;
    const here = blocks.findIndex((b) => b.some((l) => l.id === log.id));
    const after = afterCheck(blocks, log);
    if (after.open !== here) {
      animate();
      setChosen((c) => (after.open == null ? { ...c, [here]: false } : { ...c, [here]: false, [after.open]: true }));
      if (after.open != null) reveal(after.open);
    }
    if (after.focus) focusField(after.focus.id);
  };

  return { blocks, open, toggleBlock, toggleSet, scrollRef, onHeaderLayout, onBodyLayout };
}
