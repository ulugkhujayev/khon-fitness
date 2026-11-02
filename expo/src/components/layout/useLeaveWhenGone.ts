import { useEffect, useRef } from 'react';
import { router } from 'expo-router';

/**
 * Leaves the screen once its record is gone (deleted here or elsewhere, or a backup replaced everything).
 * Returns `leave` for the screen's own exits, so a delete it started does not leave twice.
 */
export function useLeaveWhenGone(gone: boolean) {
  const leaving = useRef(false);
  const leave = () => {
    if (leaving.current) return;
    leaving.current = true;
    if (router.canGoBack()) router.back();
    else router.replace('/');
  };
  useEffect(() => {
    if (gone) leave();
  }, [gone]);
  return leave;
}
