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
} as const;

/** Tabular figures for numbers. */
export const tabular: TextStyle = { fontVariant: ['tabular-nums'] };

export const Space = {
  margin: 16,
  rhythm: 8,
  groupRadius: 20,
  rowMin: 48,
  tapMin: 44,
  buttonHeight: 50,
} as const;
