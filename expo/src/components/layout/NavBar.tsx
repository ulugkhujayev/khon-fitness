import { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Type } from '@/theme';
import { BackButton } from './BackButton';

/** Navigation bar (DESIGN.md): 56 pt, the round Back button, the title centered in Headline, the action trailing. */
export function NavBar({
  title,
  trailing,
  onBack,
  backLabel,
}: {
  title: string;
  trailing?: ReactNode;
  onBack?: () => void;
  /** Spoken name of Back when it leads somewhere specific, e.g. "Today". */
  backLabel?: string;
}) {
  return (
    <View style={styles.bar}>
      <View style={styles.side}>
        <BackButton onPress={onBack} label={backLabel} />
      </View>
      <Text style={styles.title} numberOfLines={1} accessibilityRole="header">
        {title}
      </Text>
      <View style={[styles.side, styles.trailing]}>{trailing}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { height: 56, flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16 },
  /** Equal sides keep the title centered; wide enough for a "Done" capsule. */
  side: { width: 88 },
  trailing: { alignItems: 'flex-end' },
  title: { ...Type.headline, flex: 1, textAlign: 'center' },
});
