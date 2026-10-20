import { useEffect, useRef } from 'react';
import { useNavigation } from 'expo-router';

/**
 * Deletes a record that was saved only so its screen could build on it (a new program saved for its first
 * block) when the screen goes away by any path, Back, a swipe or the hardware back, unless `keep()` was
 * called (Done) or it has gained content worth keeping.
 */
export function useDropOnLeave(provisional: boolean, hasContent: boolean, drop: () => void) {
  const navigation = useNavigation();
  const kept = useRef(false);
  const latest = useRef({ hasContent, drop });
  useEffect(() => {
    latest.current = { hasContent, drop };
  });

  useEffect(() => {
    if (!provisional) return;
    return navigation.addListener('beforeRemove', () => {
      if (!kept.current && !latest.current.hasContent) latest.current.drop();
    });
  }, [navigation, provisional]);

  return {
    keep: () => {
      kept.current = true;
    },
  };
}
