import { useEffect, useState } from 'react';

const currentTime = () => new Date();

/**
 * The time, renewed every minute so "closes in" stays right while the screen is open (minuteTick in TodayScreen.kt).
 * `refresh` renews it at once, after an action that changes what the time means.
 */
export function useNow(): [Date, () => void] {
  const [now, setNow] = useState(currentTime);
  useEffect(() => {
    const timer = setInterval(() => setNow(currentTime()), 60_000);
    return () => clearInterval(timer);
  }, []);
  return [now, () => setNow(currentTime())];
}
