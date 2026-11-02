import { Choice } from '@/components/ui';
import { modalityColor } from '@/components/plan/colors';
import { asCardio, asStrength, cap, CARDIO_FIELDS, fieldLabel, toggleIn } from '@/domain/editors';
import { Exercise, Intensity, Kind, Modality, MUSCLE_GROUPS, splitList } from '@/domain/types';

/** The selects of the exercise editor, each opened as a choice sheet. */
export type ExerciseSelect = 'kind' | 'primary' | 'secondary' | 'modality' | 'intensity' | 'fields' | 'merge';

export interface SelectConfig {
  title: string;
  choices: Choice[];
  isSelected: (id: string) => boolean;
  /** Multi selects toggle and stay open; single selects close after a pick. */
  multi: boolean;
  /** The draft after picking `id`. Merge is an action, not a draft change, so it has none. */
  apply?: (draft: Exercise, id: string) => Exercise;
}

const INTENSITIES = [Intensity.LOW, Intensity.MODERATE, Intensity.HIGH];

/** Title, options and pick rule of one select (the sheets in ExerciseEditorScreen, Editors.kt). */
export function selectConfig(select: ExerciseSelect, draft: Exercise, modalities: Modality[], mergeTargets: Exercise[]): SelectConfig {
  switch (select) {
    case 'kind':
      return {
        title: 'Kind',
        choices: [
          { id: Kind.STRENGTH, title: 'Strength' },
          { id: Kind.CARDIO, title: 'Cardio' },
        ],
        isSelected: (id) => draft.kind === id,
        multi: false,
        apply: (d, id) => (id === Kind.STRENGTH ? asStrength(d) : asCardio(d, modalities[0]?.id ?? null)),
      };
    case 'primary':
      return {
        title: 'Primary muscle',
        choices: MUSCLE_GROUPS.map((m) => ({ id: m, title: cap(m) })),
        isSelected: (id) => draft.primaryMuscle === id,
        multi: false,
        apply: (d, id) => ({ ...d, primaryMuscle: id }),
      };
    case 'secondary':
      return {
        title: 'Secondary muscles',
        choices: MUSCLE_GROUPS.filter((m) => m !== draft.primaryMuscle).map((m) => ({ id: m, title: cap(m) })),
        isSelected: (id) => splitList(draft.secondaryMuscles).includes(id),
        multi: true,
        apply: (d, id) => ({ ...d, secondaryMuscles: toggleIn(d.secondaryMuscles, id) }),
      };
    case 'modality':
      return {
        title: 'Modality',
        choices: modalities.map((m) => ({ id: m.id, title: m.name, dot: modalityColor(m.id, modalities) })),
        isSelected: (id) => draft.modalityId === id,
        multi: false,
        apply: (d, id) => ({ ...d, modalityId: id }),
      };
    case 'intensity':
      return {
        title: 'Intensity',
        choices: INTENSITIES.map((v) => ({ id: v, title: cap(v) })),
        isSelected: (id) => draft.intensity === id,
        multi: false,
        apply: (d, id) => ({ ...d, intensity: id }),
      };
    case 'fields':
      return {
        title: 'Fields',
        choices: CARDIO_FIELDS.map((f) => ({ id: f, title: fieldLabel(f) })),
        isSelected: (id) => splitList(draft.fields).includes(id),
        multi: true,
        apply: (d, id) => ({ ...d, fields: toggleIn(d.fields, id) }),
      };
    case 'merge':
      return {
        title: `Merge ${draft.name} into`,
        choices: mergeTargets.map((e) => ({ id: e.id, title: e.name })),
        isSelected: () => false,
        multi: false,
      };
  }
}
