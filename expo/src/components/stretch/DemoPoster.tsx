import { Image, StyleSheet, Text } from 'react-native';
import { figureName } from '@/domain/stretch';
import { K, Type } from '@/theme';
import { demoPoster } from './demoAssets';

/** Library thumbnail: the held pose on the studio background (DemoPoster in StretchDemonstration.kt). */
export function DemoPoster({ figure, size = 36 }: { figure: string; size?: number }) {
  const source = demoPoster(figure);
  if (!source) return <Text style={styles.none}>No demo</Text>;
  return (
    <Image
      source={source}
      fadeDuration={0}
      resizeMode="contain"
      accessibilityLabel={figureName(figure)}
      style={[styles.poster, { width: size, height: size }]}
    />
  );
}

const styles = StyleSheet.create({
  // Apart from the row's value text before it and its chevron after it.
  poster: { borderRadius: 6, backgroundColor: K.DemoBackdrop, marginLeft: 10, marginRight: 6 },
  none: { ...Type.caption1, color: K.Muted, marginLeft: 10, marginRight: 6 },
});
