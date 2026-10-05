import { FlexWidget, TextWidget } from 'react-native-android-widget';
import { WindowCard } from '@/domain/widgets';
import { K } from '@/theme';
import { WIDGET_LINKS } from '../links';

/**
 * The 2x1 eating-window widget (WindowWidget.kt, DESIGN.md Eating window): a 12 dp dot, Open or Closed at 17 sp,
 * the next change at 13 sp. A tap opens Today with the window sheet.
 */
export function WindowWidget({ card }: { card: WindowCard }) {
  return (
    <FlexWidget
      clickAction="OPEN_URI"
      clickActionData={{ uri: WIDGET_LINKS.window }}
      accessibilityLabel={card.description}
      style={{ width: 'match_parent', height: 'match_parent', flexDirection: 'row', alignItems: 'center', paddingHorizontal: 14, borderRadius: 20, backgroundColor: K.Surface }}>
      <FlexWidget style={{ width: 12, height: 12, borderRadius: 6, backgroundColor: card.open ? K.Green : K.Dim }} />
      <FlexWidget style={{ flexDirection: 'column', marginLeft: 12 }}>
        <TextWidget text={card.status} style={{ fontSize: 17, fontWeight: 'bold', color: K.Text }} />
        <TextWidget text={card.text} style={{ fontSize: 13, color: K.Muted }} />
      </FlexWidget>
    </FlexWidget>
  );
}
