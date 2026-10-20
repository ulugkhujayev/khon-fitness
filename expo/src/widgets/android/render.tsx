import { WidgetData } from '../data';
import { TodayWidget } from './TodayWidget';
import { WindowWidget } from './WindowWidget';

/** Widget names as declared in app.json (react-native-android-widget plugin). */
export const ANDROID_WIDGETS = { today: 'Today', window: 'EatingWindow' } as const;

/** The widget a launcher asks for by name, drawn from fresh data. */
export function renderWidget(name: string, data: WidgetData) {
  return name === ANDROID_WIDGETS.window ? <WindowWidget card={data.window[0].card} /> : <TodayWidget card={data.today[0].card} />;
}
