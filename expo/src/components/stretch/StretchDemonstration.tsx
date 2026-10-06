import { useEffect, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { DemoClock, exerciseFrame, forDemoSide, guideFor, nearestStep, previewFrame, stepAt, StretchDemo, timelineFill } from '@/domain/stretch';
import { K, Type } from '@/theme';
import { DemoView } from './demoAssets';
import { DemoControl } from './DemoControl';
import { DemoFrame } from './DemoFrame';
import { StepTimeline } from './StepTimeline';

/**
 * A lesson before Start, a clock-driven demonstration during the stretch (StretchDemonstration.kt).
 * Before Start the guide plays its steps on its own; a step tab holds that position, Play demo starts over.
 * During the stretch a hold shows the held pose and reps follow `exerciseProgress` (0..1..0 per rep).
 */
export function StretchDemonstration({
  figure,
  mirror = false,
  preview = true,
  exerciseProgress = null,
  compact = false,
  style,
}: {
  figure: string;
  mirror?: boolean;
  preview?: boolean;
  exerciseProgress?: number | null;
  compact?: boolean;
  style?: ViewStyle;
}) {
  const guide = guideFor(figure);
  if (!guide) {
    return (
      <View style={style}>
        <Text style={styles.none}>No demonstration for this stretch</Text>
      </View>
    );
  }
  // A new guide or side starts its own lesson, as rememberSaveable(figure, mirror) does in Kotlin.
  return (
    <Demonstration
      key={`${figure}-${mirror}`}
      figure={figure}
      guide={guide}
      mirror={mirror}
      preview={preview}
      exerciseProgress={exerciseProgress}
      compact={compact}
      style={style}
    />
  );
}

function Demonstration({
  figure,
  guide,
  mirror,
  preview,
  exerciseProgress,
  compact,
  style,
}: {
  figure: string;
  guide: StretchDemo;
  mirror: boolean;
  preview: boolean;
  exerciseProgress: number | null;
  compact: boolean;
  style?: ViewStyle;
}) {
  const [view, setView] = useState<DemoView>(0);
  const [playing, setPlaying] = useState(true);
  const [playFrame, setPlayFrame] = useState(() => previewFrame(guide, 0));
  const [manualFrame, setManualFrame] = useState(0);
  const running = playing && preview;

  // The sprites hold 13-20 poses a second; the display clock only picks the index, and an unchanged index renders nothing.
  useEffect(() => {
    if (!running) return;
    const clock = new DemoClock(0);
    let handle = requestAnimationFrame(function onFrame(time) {
      setPlayFrame(previewFrame(guide, clock.elapsedMs(time)));
      handle = requestAnimationFrame(onFrame);
    });
    return () => cancelAnimationFrame(handle);
  }, [running, guide]);

  const held = exerciseFrame(guide, exerciseProgress);
  const frame = !preview ? held : playing ? playFrame : manualFrame;
  const selected = preview ? stepAt(guide, frame) : nearestStep(guide, held);
  const instruction = forDemoSide(guide.steps[selected].instruction, mirror);

  const togglePlay = () => {
    if (playing) setManualFrame(playFrame);
    else setPlayFrame(previewFrame(guide, 0));
    setPlaying(!playing);
  };

  return (
    <View style={style}>
      <View>
        <DemoFrame
          figure={figure}
          guide={guide}
          view={view}
          frame={frame}
          mirror={mirror}
          height={compact ? 208 : 272}
          description={`${guide.name}. ${guide.steps[selected].title}. ${instruction}`}
        />
        {/* Over the image's bottom corners, like a video player's controls. */}
        <View style={styles.controls}>
          {preview ? (
            <DemoControl
              icon={playing ? { ios: 'pause.fill', android: 'pause', web: 'pause' } : { ios: 'play.fill', android: 'play_arrow', web: 'play_arrow' }}
              label={playing ? 'Pause demo' : 'Play demo'}
              onPress={togglePlay}
            />
          ) : (
            <View />
          )}
          <DemoControl
            icon={{ ios: 'rotate.3d', android: '3d_rotation', web: '3d_rotation' }}
            label={`Change camera angle. View ${view + 1} of 2`}
            onPress={() => setView(view === 0 ? 1 : 0)}
          />
        </View>
      </View>
      {preview ? (
        <StepTimeline
          style={styles.tabs}
          steps={guide.steps}
          selected={selected}
          fill={timelineFill(guide, frame)}
          onSelect={(i) => {
            setPlaying(false);
            setManualFrame(guide.steps[i].frame);
          }}
        />
      ) : null}
      <Text style={styles.instruction}>{instruction}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  none: { ...Type.body, color: K.Muted },
  controls: { position: 'absolute', left: 10, right: 10, bottom: 10, flexDirection: 'row', justifyContent: 'space-between', pointerEvents: 'box-none' },
  tabs: { marginTop: 14 },
  // Three lines reserved, so the controls below never jump between steps.
  instruction: { ...Type.body, color: K.OnPhaseText, minHeight: 66, marginTop: 12, marginBottom: 8 },
});
