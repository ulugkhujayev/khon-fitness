import { WidgetData, widgetData } from './data';
import { WIDGET_COLORS } from './ios/colors';
import { TodayWidget } from './ios/TodayWidget';
import { WindowWidget } from './ios/WindowWidget';
import { WIDGET_LINKS } from './links';

/**
 * Hands the widgets their cards for now and ahead: iOS redraws a widget only from its timeline, so Today gets
 * tomorrow's card at midnight and the window one card per change for two days.
 */
export async function refreshWidgets() {
  const data: WidgetData = await widgetData();
  TodayWidget.updateTimeline(
    data.today.map(({ at, card }) => ({ date: at, props: { ...card, link: card.starts ? WIDGET_LINKS.start : WIDGET_LINKS.open, colors: WIDGET_COLORS } })),
  );
  WindowWidget.updateTimeline(data.window.map(({ at, card }) => ({ date: at, props: { ...card, link: WIDGET_LINKS.window, colors: WIDGET_COLORS } })));
}
