import { Spacer, Text, VStack } from '@expo/ui/swift-ui';
import { accessibilityLabel, background, clipShape, containerBackground, font, foregroundStyle, frame, lineLimit, widgetURL } from '@expo/ui/swift-ui/modifiers';
import { createWidget } from 'expo-widgets';
import { TodayCard } from '@/domain/widgets';
import { WidgetColors } from './colors';

export type TodayWidgetProps = TodayCard & { link: string; colors: WidgetColors };

/**
 * The Today home-screen widget (TodayWidget.kt): what today holds and its button; a tap starts, resumes or opens.
 * Sizes are the Kotlin widget's (12 / 20 / 16 pt); the 'widget' body runs apart from the app and sees only its props.
 */
function TodayView(props: TodayWidgetProps) {
  'widget';
  const c = props.colors;
  return (
    <VStack alignment="leading" spacing={2} modifiers={[containerBackground(c.surface, 'widget'), widgetURL(props.link), accessibilityLabel(props.description)]}>
      <Text modifiers={[font({ size: 12 }), foregroundStyle(c.muted)]}>{props.sub}</Text>
      <Text modifiers={[font({ size: 20, weight: 'bold' }), foregroundStyle(c.text), lineLimit(2)]}>{props.title}</Text>
      {props.stretch ? <Text modifiers={[font({ size: 12 }), foregroundStyle(props.stretched ? c.green : c.muted)]}>{props.stretch}</Text> : null}
      <Spacer />
      <Text modifiers={[font({ size: 16, weight: 'bold' }), foregroundStyle(c.accentInk), frame({ maxWidth: 1000, height: 36 }), background(c.accent), clipShape('capsule')]}>
        {props.action}
      </Text>
    </VStack>
  );
}

export const TodayWidget = createWidget<TodayWidgetProps>('TodayWidget', TodayView);
