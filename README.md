# Khon Fitness

A gym and cardio log for one person, on Android, with no account and no
server. It replaced a Notion page that needed a template copy per
session and four edits per exercise swap, and tracked no weights.

What it does now:

- One item per day: a program (TP v3 by default), a cardio exercise, or
  rest. Weekly template plus per-date overrides.
- Live session with supersets, prefilled from last time, one tap to log a
  set, focus jumps to the next kg field, green delta when you beat last
  time.
- History calendar, bodyweight chart, estimated 1RM chart per exercise.
- Exercise library with muscle groups, archive, merge, notes.
- JSON export and import.
- Check for update from Settings: downloads the latest release APK from
  this repo and installs it over the old build.

Layout:

- `android/` the app. Kotlin, Jetpack Compose, Room. `minSdk 28`.
- `mockup/` the HTML mockup that set the design. Open `index.html`.
- `SPEC.md` decisions. `DESIGN.md` layout rules. Both bind the app.

Build:

```sh
cd android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleRelease
```

Release signing reads `~/.khon/keystore.properties`. Keep that key; the
in-app updater only installs builds signed with it.

Install on a phone: download the APK from the latest release, open it,
allow installs from that source once.
