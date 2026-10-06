import { ReactNode } from 'react';
import { StyleSheet, View } from 'react-native';
import { ChoiceList } from './ChoiceList';
import { ChoiceRow } from './ChoiceRow';
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
}) {
  return (
    <Sheet visible={visible} title={title} onDismiss={onDismiss}>
      {header ? <View style={styles.header}>{header}</View> : null}
      <ChoiceList>
        {choices.length === 0 ? empty : null}
        {choices.map((c, i) => (
          <ChoiceRow
            key={c.id}
            title={c.title}
            dot={c.dot}
            dotFilled={c.dotFilled}
            selected={isSelected(c.id)}
            last={i === choices.length - 1}
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
