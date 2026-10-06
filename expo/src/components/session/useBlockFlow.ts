import { useState } from 'react';
import * as repo from '@/db/repository';
import { SetLog, SetStatus } from '@/domain/types';
import { afterCheck, firstOpenBlock, groupBlocks } from '@/domain/workout';
import { useBlockReveal } from './useBlockReveal';

/**
 * Which block of a session is open and what a tap on a set's circle does. The first block with a pending
 * set is open until the user picks one. A check saves the set; its block stays open until its last set,
 * then the next block with work opens and focus goes to the set afterCheck names. Every change of the open
 * block animates, and a block that opens scrolls to the top (useBlockReveal).
 */
export function useBlockFlow(logs: SetLog[], focusField: (id: string) => void) {
  // Undefined until the user opens or closes a block, or a check moves on.
  const [chosen, setChosen] = useState<number | null | undefined>(undefined);
  const { scrollRef, reveal, onBlockLayout } = useBlockReveal();
  const blocks = groupBlocks(logs);
  const expanded = chosen !== undefined ? chosen : blocks.length > 0 ? firstOpenBlock(blocks) : null;

  const openBlock = (i: number | null) => {
    if (i !== expanded) reveal(i);
    setChosen(i);
  };

  const toggleSet = (log: SetLog) => {
    const next = log.status === SetStatus.DONE ? SetStatus.PENDING : SetStatus.DONE;
    repo.patchSetLog(log.id, { status: next });
    if (next !== SetStatus.DONE) return;
    const { open, focus } = afterCheck(blocks, log);
    openBlock(open);
    if (focus) focusField(focus.id);
  };

  return { blocks, expanded, openBlock, toggleSet, scrollRef, onBlockLayout };
}
