/**
 * The launcher asks for a widget (added, resized, every 30 minutes) through a headless task, even when the app
 * is not running; each answer reads the database afresh. Taps open links, so clicks need no handling here.
 */
import { registerWidgetTaskHandler } from 'react-native-android-widget';
import { renderWidget } from './android/render';
import { widgetData } from './data';

export function registerWidgets() {
  registerWidgetTaskHandler(async ({ widgetInfo, widgetAction, renderWidget: draw }) => {
    if (widgetAction === 'WIDGET_DELETED' || widgetAction === 'WIDGET_CLICK') return;
    draw(renderWidget(widgetInfo.widgetName, await widgetData()));
  });
}
