import { FieldRow, FieldValue, NumberRow } from '@/components/ui';
import { CardioField, fieldValue, NUMBER_FIELDS, runPace, swimFromLaps, withFieldValue } from '@/domain/cardio';
import { Session } from '@/domain/types';
import { ComputedRow } from './ComputedRow';

/**
 * The cardio fields of a session in the exercise's order, each a number widget (the stroke opens a sheet),
 * then what follows from them: the pace from distance, the distance and pace of a swim from its laps.
 */
export function CardioFieldRows({
  draft,
  fields,
  onChange,
  onStroke,
}: {
  draft: Session;
  fields: CardioField[];
  onChange: (draft: Session) => void;
  onStroke: () => void;
}) {
  const pace = fields.includes('distance') ? runPace(draft.timeSec, draft.distanceM) : null;
  const swim = fields.includes('laps') ? swimFromLaps(draft.laps, draft.poolLength, draft.timeSec) : null;
  return (
    <>
      {fields.map((f) =>
        f === 'stroke' ? (
          <FieldRow key={f} label="Stroke" onPress={onStroke}>
            <FieldValue text={draft.stroke ?? 'Freestyle'} />
          </FieldRow>
        ) : (
          <NumberRow
            key={f}
            label={NUMBER_FIELDS[f].label}
            value={fieldValue(draft, f)}
            unit={NUMBER_FIELDS[f].unit}
            step={NUMBER_FIELDS[f].step}
            decimals={NUMBER_FIELDS[f].decimals}
            onChange={(v) => onChange(withFieldValue(draft, f, v))}
          />
        ),
      )}
      {pace ? <ComputedRow label="Pace" value={pace} /> : null}
      {swim ? <ComputedRow label="Distance" value={swim.distance} /> : null}
      {swim?.pace ? <ComputedRow label="Pace" value={swim.pace} /> : null}
    </>
  );
}
