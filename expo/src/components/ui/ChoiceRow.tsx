import { Check } from './Check';
import { ListRow } from './ListRow';

/** One choice in a sheet: optional color dot, the name, a check when selected. */
export function ChoiceRow({
  title,
  selected,
  dot,
  dotFilled = true,
  last = false,
  onPress,
}: {
  title: string;
  selected: boolean;
  dot?: string;
  dotFilled?: boolean;
  last?: boolean;
  onPress: () => void;
}) {
  return (
    <ListRow
      title={title}
      dot={dot}
      dotFilled={dotFilled}
      chevron={false}
      last={last}
      elevated
      selected={selected}
      trailing={selected ? <Check /> : null}
      onPress={onPress}
    />
  );
}
