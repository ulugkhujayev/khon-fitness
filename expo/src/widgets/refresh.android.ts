import { requestWidgetUpdate } from 'react-native-android-widget';
import { renderWidget, ANDROID_WIDGETS } from './android/render';
import { widgetData } from './data';

/** Redraws every placed widget from the current data (TodayWidget.refresh / WindowWidget.refresh in Kotlin). */
export async function refreshWidgets() {
  const data = await widgetData();
  await Promise.all(Object.values(ANDROID_WIDGETS).map((widgetName) => requestWidgetUpdate({ widgetName, renderWidget: () => renderWidget(widgetName, data) })));
}
