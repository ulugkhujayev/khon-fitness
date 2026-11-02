import { useState } from 'react';
import { Image, PixelRatio, StyleSheet, View } from 'react-native';
import { StretchDemo } from '@/domain/stretch';
import { K } from '@/theme';
import { demoSheet, DemoView } from './demoAssets';

/**
 * One frame of a demonstration, fitted into the box and centered on the studio background (DemoFrame in
 * StretchDemonstration.kt). The whole sheet is laid out at the frame's scale inside a clip one frame in size,
 * and shifted so the wanted frame shows: a new frame only moves it, the image is never decoded again.
 */
export function DemoFrame({
  figure,
  guide,
  view,
  frame,
  mirror,
  height,
  description,
}: {
  figure: string;
  guide: StretchDemo;
  view: DemoView;
  frame: number;
  /** The right side: the image flips, as the instruction does. */
  mirror: boolean;
  height: number;
  description: string;
}) {
  const [width, setWidth] = useState(0);
  const source = demoSheet(figure, view);
  const ratio = Math.min(width / guide.frameWidth, height / guide.frameHeight);
  // Whole pixels per frame, so neighboring frames never bleed into the edges.
  const w = PixelRatio.roundToNearestPixel(guide.frameWidth * ratio);
  const h = PixelRatio.roundToNearestPixel(guide.frameHeight * ratio);
  const index = Math.min(Math.max(0, frame), guide.frameCount - 1);
  const col = index % guide.columns;
  const row = Math.floor(index / guide.columns);
  const rows = Math.ceil(guide.frameCount / guide.columns);

  return (
    <View
      style={[styles.box, { height }]}
      onLayout={(e) => setWidth(e.nativeEvent.layout.width)}
      accessible
      accessibilityRole="image"
      accessibilityLabel={description}>
      {source && w > 0 ? (
        <View style={[{ width: w, height: h }, styles.clip, mirror && styles.mirror]}>
          <Image
            source={source}
            fadeDuration={0}
            resizeMode="stretch"
            resizeMethod="scale"
            style={{ width: w * guide.columns, height: h * rows, transform: [{ translateX: -col * w }, { translateY: -row * h }] }}
          />
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  box: { alignSelf: 'stretch', alignItems: 'center', justifyContent: 'center', borderRadius: 16, overflow: 'hidden', backgroundColor: K.DemoBackdrop },
  clip: { overflow: 'hidden' },
  mirror: { transform: [{ scaleX: -1 }] },
});
