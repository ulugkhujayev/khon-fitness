import { K } from '@/theme';

/** Widget code runs in its own runtime and cannot import the theme, so the colors travel in its props. */
export const WIDGET_COLORS = { surface: K.Surface, text: K.Text, muted: K.Muted, dim: K.Dim, accent: K.Accent, accentInk: K.AccentInk, green: K.Green };

export type WidgetColors = typeof WIDGET_COLORS;
