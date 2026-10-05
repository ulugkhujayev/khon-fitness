/**
 * The demonstration images (assets/stretch_demos/README.md): a sprite sheet per guide and camera angle,
 * and a poster of the held pose for thumbnails. Metro bundles only literal requires, so each is listed.
 */
import { ImageSourcePropType } from 'react-native';

const SHEETS: Record<string, readonly [ImageSourcePropType, ImageSourcePropType]> = {
  catcow: [require('../../../assets/stretch_demos/catcow-0.webp'), require('../../../assets/stretch_demos/catcow-1.webp')],
  elephant: [require('../../../assets/stretch_demos/elephant-0.webp'), require('../../../assets/stretch_demos/elephant-1.webp')],
  fold: [require('../../../assets/stretch_demos/fold-0.webp'), require('../../../assets/stretch_demos/fold-1.webp')],
  hipflexor: [require('../../../assets/stretch_demos/hipflexor-0.webp'), require('../../../assets/stretch_demos/hipflexor-1.webp')],
  needle: [require('../../../assets/stretch_demos/needle-0.webp'), require('../../../assets/stretch_demos/needle-1.webp')],
  ninety: [require('../../../assets/stretch_demos/ninety-0.webp'), require('../../../assets/stretch_demos/ninety-1.webp')],
  plow: [require('../../../assets/stretch_demos/plow-0.webp'), require('../../../assets/stretch_demos/plow-1.webp')],
  shoulderir: [require('../../../assets/stretch_demos/shoulderir-0.webp'), require('../../../assets/stretch_demos/shoulderir-1.webp')],
  wgs: [require('../../../assets/stretch_demos/wgs-0.webp'), require('../../../assets/stretch_demos/wgs-1.webp')],
};

const POSTERS: Record<string, ImageSourcePropType> = {
  catcow: require('../../../assets/stretch_demos/catcow-0-poster.webp'),
  elephant: require('../../../assets/stretch_demos/elephant-0-poster.webp'),
  fold: require('../../../assets/stretch_demos/fold-0-poster.webp'),
  hipflexor: require('../../../assets/stretch_demos/hipflexor-0-poster.webp'),
  needle: require('../../../assets/stretch_demos/needle-0-poster.webp'),
  ninety: require('../../../assets/stretch_demos/ninety-0-poster.webp'),
  plow: require('../../../assets/stretch_demos/plow-0-poster.webp'),
  shoulderir: require('../../../assets/stretch_demos/shoulderir-0-poster.webp'),
  wgs: require('../../../assets/stretch_demos/wgs-0-poster.webp'),
};

/** Camera angle: 0 or 1 ("Change view"). */
export type DemoView = 0 | 1;

export const demoSheet = (figure: string, view: DemoView): ImageSourcePropType | null => SHEETS[figure]?.[view] ?? null;
export const demoPoster = (figure: string): ImageSourcePropType | null => POSTERS[figure] ?? null;
