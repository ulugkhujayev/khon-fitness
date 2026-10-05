import { ReactNode } from 'react';
import { GroupedList } from './GroupedList';

/** Choice list inside a sheet: a grouped list on the elevated fill. */
export function ChoiceList({ children }: { children: ReactNode }) {
  return <GroupedList elevated>{children}</GroupedList>;
}
