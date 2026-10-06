import { TextStyle } from 'react-native';

/**
 * iOS dark-appearance semantic palette (HIG Color and Dark Mode).
 * Same names and values as K in the Kotlin app (ui/theme/Theme.kt).
 */
export const K = {
  Bg: '#000000',
  Surface: '#1C1C1E',
  Surface2: '#2C2C2E',
  Surface3: '#3A3A3C',
  Divider: '#38383A',
  Text: '#FFFFFF',
  Muted: '#8E8E93',
  Dim: '#636366',
  Accent: '#FF9F0A',
  AccentInk: '#FFFFFF',
  AccentSoft: 'rgba(255,159,10,0.2)',
  Green: '#30D158',
  GreenInk: '#FFFFFF',
  Blue: '#0A84FF',
  Red: '#FF453A',
  Elevated: '#2C2C2E',
  // Alpha copies the Kotlin screens build inline (K.Green.copy(alpha = …) and the like), named here.
  /** A logged set row: systemGreen at 5 percent. */
  GreenTint: 'rgba(48,209,88,0.05)',
  /** Dimmed screen behind a sheet. */
  Scrim: 'rgba(0,0,0,0.55)',
  /** 1 pt edge of the floating tab bar: white at 6 percent. */
  Hairline: 'rgba(255,255,255,0.06)',
  Shadow: '#000000',
  /** Toggle thumb and the ring around the chosen color swatch. */
  White: '#FFFFFF',
  // Interval timer backgrounds (TimerScreen.kt): warm-up gray, work green, rest blue; text on them in white.
  PhaseWarmup: '#3A3A40',
  PhaseWork: '#14532D',
  PhaseRest: '#1E3A5F',
  OnPhaseMuted: 'rgba(255,255,255,0.75)',
  OnPhaseDim: 'rgba(255,255,255,0.6)',
  /** The step instruction in the stretch player and editor (white at 90 percent). */
  OnPhaseText: 'rgba(255,255,255,0.9)',
  /** Underline of an unselected demonstration step (white at 20 percent). */
  OnPhaseLine: 'rgba(255,255,255,0.2)',
  /** Studio background of the stretch demonstration renders (assets/stretch_demos/README.md). */
  DemoBackdrop: '#F5F5F5',
  /** Text on the white primary button of the stretch player. */
  Black: '#000000',
} as const;

/** Inter, bundled as the stand-in for SF Pro. Keys must match useFonts in the root layout. */
export const Font = {
  regular: 'Inter-Regular',
  medium: 'Inter-Medium',
  semibold: 'Inter-SemiBold',
  bold: 'Inter-Bold',
} as const;

const t = (size: number, family: string, line: number, spacing = 0): TextStyle => ({
  fontFamily: family,
  fontSize: size,
  lineHeight: line,
  letterSpacing: spacing,
  color: K.Text,
});

/** Tabular figures for numbers. */
export const tabular: TextStyle = { fontVariant: ['tabular-nums'] };

/** HIG text styles at the default size (DESIGN.md, HIG mapping). */
export const Type = {
  largeTitle: t(34, Font.bold, 41, -0.4),
  title1: t(28, Font.bold, 34, -0.3),
  title2: t(22, Font.bold, 28, -0.2),
  headline: t(17, Font.semibold, 22, -0.2),
  body: t(17, Font.regular, 22, -0.2),
  subheadline: t(15, Font.regular, 20, -0.1),
  footnote: t(13, Font.regular, 18),
  caption1: t(12, Font.regular, 16),
  caption2: t(11, Font.regular, 13),
  /**
   * Value in the number widget: bold tabular figures (NumberField in Components.kt, 19 sp).
   * No line height: it sits in a TextInput, where one shifts the text off center.
   */
  value: { fontFamily: Font.bold, fontSize: 19, color: K.Text, ...tabular },
  /**
   * The big figure above a ruler picker (Today's bodyweight, DESIGN.md: 48/700). New to the Expo app.
   * No line height for the same reason as `value`.
   */
  figure: { fontFamily: Font.bold, fontSize: 48, color: K.Text, letterSpacing: -1, ...tabular },
  /** The − and + glyphs of the number widget (StepButton in Components.kt, 22 sp medium). */
  stepGlyph: t(22, Font.medium, 28),
  /** The countdown of the interval timer (TimerScreen.kt: 96 sp bold, tight tracking). */
  countdown: { ...t(96, Font.bold, 112, -5), ...tabular },
  /** "Round 2 of 4" under the countdown (24 sp semibold). */
  round: t(24, Font.semibold, 30),
  /** The stretch player's countdown (StretchScreen.kt: 88 sp bold, 72 on screens under 680 dp). */
  stretchCountdown: { ...t(88, Font.bold, 100), ...tabular },
  stretchCountdownCompact: { ...t(72, Font.bold, 84), ...tabular },
} as const;

export const Space = {
  margin: 16,
  rhythm: 8,
  groupRadius: 20,
  rowMin: 48,
  tapMin: 44,
  buttonHeight: 50,
} as const;
