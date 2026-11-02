import { ReactNode } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { K, tabular, Type } from '@/theme';

/**
 * Headline of a section with an optional value or action on the right: "Bodyweight  80.4 kg",
 * "Stretching  3 days · avg 12 min", a chart title in secondary label.
 */
export function TitleRow({
  title,
  muted = false,
  value,
  valueLabel,
  large = false,
  trailing,
  style,
}: {
  title: string;
  /** Title in secondary label, for a chart title. */
  muted?: boolean;
  value?: string;
  /** Spoken in place of the value, e.g. "No entry yet" for a dash. */
  valueLabel?: string;
  /** The value as the big figure of the section (Title 2), else in secondary label. */
  large?: boolean;
  trailing?: ReactNode;
  style?: ViewStyle;
}) {
  return (
    <View style={[styles.row, style]}>
      <Text style={[styles.title, muted && styles.muted]} accessibilityRole="header">
        {title}
      </Text>
      {value ? (
        <Text style={large ? styles.large : styles.value} accessibilityLabel={valueLabel}>
          {value}
        </Text>
      ) : null}
      {trailing}
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center' },
  title: { ...Type.headline, flex: 1, paddingRight: 12 },
  muted: { color: K.Muted },
  large: { ...Type.title2, ...tabular },
  value: { ...Type.subheadline, color: K.Muted },
});
