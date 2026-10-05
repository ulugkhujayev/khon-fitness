import { Circle, HStack, Spacer, Text, VStack } from '@expo/ui/swift-ui';
import { accessibilityLabel, containerBackground, font, foregroundStyle, frame, widgetURL } from '@expo/ui/swift-ui/modifiers';
import { createWidget } from 'expo-widgets';
import { WindowCard } from '@/domain/widgets';
import { WidgetColors } from './colors';

export type WindowWidgetProps = WindowCard & { link: string; colors: WidgetColors };

/**
 * The eating-window widget (WindowWidget.kt, DESIGN.md Eating window): a 12 pt dot, Open or Closed at 17 pt and
 * the next change at 13 pt. A tap opens Today with the window sheet.
 */
function WindowView(props: WindowWidgetProps) {
  'widget';
  const c = props.colors;
  return (
    <HStack spacing={12} modifiers={[containerBackground(c.surface, 'widget'), widgetURL(props.link), accessibilityLabel(props.description)]}>
      <Circle modifiers={[frame({ width: 12, height: 12 }), foregroundStyle(props.open ? c.green : c.dim)]} />
      <VStack alignment="leading" spacing={2}>
        <Text modifiers={[font({ size: 17, weight: 'bold' }), foregroundStyle(c.text)]}>{props.status}</Text>
        <Text modifiers={[font({ size: 13 }), foregroundStyle(c.muted)]}>{props.text}</Text>
      </VStack>
      <Spacer />
    </HStack>
  );
}

export const WindowWidget = createWidget<WindowWidgetProps>('WindowWidget', WindowView);
