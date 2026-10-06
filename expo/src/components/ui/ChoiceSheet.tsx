import { ReactNode, useState } from 'react';
import { StyleSheet, View } from 'react-native';
import { matchesQuery } from '@/domain/editors';
import { ChoiceList } from './ChoiceList';
import { ChoiceRow } from './ChoiceRow';
import { SearchEmpty } from './SearchEmpty';
import { SearchField } from './SearchField';
import { Sheet } from './Sheet';

/** One option of a ChoiceSheet. */
export interface Choice {
  id: string;
  title: string;
  dot?: string;
  dotFilled?: boolean;
}

/**
 * A sheet that lists choices with a check on the selected ones (Selects in DESIGN.md, Editors).
 * It never closes by itself: a single select closes in `onPick`, a multi select leaves it open.
 * `searchable` adds a search field over the list that narrows it by title, empty again each time the
 * sheet opens, and the no-results view when nothing matches.
 */
export function ChoiceSheet({
  visible,
  title,
  choices,
  isSelected = () => false,
  onPick,
  onDismiss,
  header,
  empty,
  searchable = false,
}: {
  visible: boolean;
  title?: string;
  choices: Choice[];
  isSelected?: (id: string) => boolean;
  onPick: (id: string) => void;
  onDismiss: () => void;
  /** Shown above the list, e.g. a search field. */
  header?: ReactNode;
  /** Shown in the list when it has no choices, e.g. SearchEmpty under a search field. */
  empty?: ReactNode;
  searchable?: boolean;
}) {
  const [query, setQuery] = useState('');
  // Each opening starts with an empty search; the text stays while the sheet slides out.
  const [openedFor, setOpenedFor] = useState(visible);
  if (visible !== openedFor) {
    setOpenedFor(visible);
    if (visible) setQuery('');
  }
  const shown = searchable ? choices.filter((c) => matchesQuery(c.title, query)) : choices;

  return (
    <Sheet visible={visible} title={title} onDismiss={onDismiss}>
      {header ? <View style={styles.header}>{header}</View> : null}
      {searchable ? (
        <View style={styles.header}>
          <SearchField value={query} onChange={setQuery} elevated />
        </View>
      ) : null}
      <ChoiceList>
        {shown.length === 0 ? (empty ?? (searchable ? <SearchEmpty query={query} /> : null)) : null}
        {shown.map((c, i) => (
          <ChoiceRow
            key={c.id}
            title={c.title}
            dot={c.dot}
            dotFilled={c.dotFilled}
            selected={isSelected(c.id)}
            last={i === shown.length - 1}
            onPress={() => onPick(c.id)}
          />
        ))}
      </ChoiceList>
    </Sheet>
  );
}

const styles = StyleSheet.create({
  header: { marginBottom: 12 },
});
