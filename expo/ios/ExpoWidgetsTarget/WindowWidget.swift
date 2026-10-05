import WidgetKit
import SwiftUI
internal import ExpoWidgets

struct WindowWidget: Widget {
  let name: String = "WindowWidget"

  var body: some WidgetConfiguration {
    StaticConfiguration(kind: name, provider: WidgetsTimelineProvider(name: name)) { entry in
      WidgetsEntryView(entry: entry)
    }
    .configurationDisplayName("Eating window")
    .description("Eating window: open or closed, and the next change time")
    .supportedFamilies([.systemSmall])
  }
}