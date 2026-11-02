import { FlexWidget, TextWidget } from 'react-native-android-widget';
import { TodayCard } from '@/domain/widgets';
import { K } from '@/theme';
import { WIDGET_LINKS } from '../links';

/**
 * The Today launcher widget (TodayWidget.kt): what today holds and a Start, Resume or Open button.
 * Sizes are the Kotlin widget's (12 / 20 / 16 sp, 20 dp corners, a 40 dp button); widgets have no theme text styles.
 */
export function TodayWidget({ card }: { card: TodayCard }) {
  const link = { uri: card.starts ? WIDGET_LINKS.start : WIDGET_LINKS.open };
  return (
    <FlexWidget
      clickAction="OPEN_URI"
      clickActionData={link}
      accessibilityLabel={card.description}
      style={{ width: 'match_parent', height: 'match_parent', flexDirection: 'column', justifyContent: 'center', padding: 14, borderRadius: 20, backgroundColor: K.Surface }}>
      <TextWidget text={card.sub} style={{ fontSize: 12, color: K.Muted }} />
      <TextWidget text={card.title} maxLines={2} truncate="END" style={{ fontSize: 20, fontWeight: 'bold', color: K.Text }} />
      {card.stretch ? <TextWidget text={card.stretch} style={{ fontSize: 12, color: card.stretched ? K.Green : K.Muted }} /> : null}
      <FlexWidget
        clickAction="OPEN_URI"
        clickActionData={link}
        style={{ width: 'match_parent', height: 40, marginTop: 10, justifyContent: 'center', alignItems: 'center', borderRadius: 20, backgroundColor: K.Accent }}>
        <TextWidget text={card.action} style={{ fontSize: 16, fontWeight: 'bold', color: K.AccentInk }} />
      </FlexWidget>
    </FlexWidget>
  );
}
