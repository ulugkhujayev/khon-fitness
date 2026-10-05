/** Where a widget tap goes; routes in src/app handle them (start.tsx, window.tsx). */
export const WIDGET_LINKS = {
  /** Start or resume today's item (ACTION_START_TODAY in Kotlin). */
  start: 'khonfitness://start',
  /** Today with the eating-window sheet open (ACTION_OPEN_WINDOW). */
  window: 'khonfitness://window',
  /** Just the app. */
  open: 'khonfitness://',
} as const;
