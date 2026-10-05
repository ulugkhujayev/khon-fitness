# Khon Fitness (Expo)

Cross-platform rewrite of the Android app in `../android`, for Android and
iOS from one codebase. `../SPEC.md` and `../DESIGN.md` still bind every
screen.

Stack: Expo SDK 57, TypeScript, Expo Router, expo-sqlite (from stage 1).

## Run

```sh
cd mobile
npm install
npx expo start        # scan the QR code with Expo Go
npx tsc --noEmit      # typecheck
```

The Android package id is `dev.mirzohidkhon.khonfitness.expo`, so this
build installs next to the Kotlin app instead of colliding with its
signed release.

## Layout

- `src/app/` routes. `(tabs)/` holds Today, History, Programs.
- `src/theme/` HIG palette, Inter text styles, spacing (mirrors `Theme.kt`).
- `src/components/` shared UI: floating tab bar, grouped lists, buttons.

## Stages

0. Base: navigation, theme, shared components. Done.
1. Data: Room schema on expo-sqlite, seed, repository, JSON import and export
   compatible with the Kotlin app's backup.
2. Gym: Today, live session, programs and exercise editors.
3. History: calendar, bodyweight chart, estimated 1RM per exercise.
4. Cardio: interval timer, cues, background notifications.
5. Stretching and eating window.
6. Platform parts: Health Connect and HealthKit, widgets, EAS Update in
   place of the APK updater.
7. EAS Build for Android and iOS.
