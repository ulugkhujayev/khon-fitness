import WidgetKit
import SwiftUI
internal import ExpoWidgets

struct TodayWidget: Widget {
  let name: String = "TodayWidget"

  var body: some WidgetConfiguration {
    StaticConfiguration(kind: name, provider: WidgetsTimelineProvider(name: name)) { entry in
      WidgetsEntryView(entry: entry)
    }
    .configurationDisplayName("Today")
    .description("Workout of the day with a Start button")
    .supportedFamilies([.systemSmall])
  }
}