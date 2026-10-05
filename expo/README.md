# Khon Fitness (Expo)

Cross-platform rewrite of the Khon Fitness Android app
(github.com/ulugkhujayev/khon-fitness, Kotlin + Compose + Room) for Android
and iOS from one codebase. `docs/SPEC.md` and `docs/DESIGN.md` bind every
screen.

Stack: Bun, Expo SDK 57, TypeScript, Expo Router, expo-sqlite.

## Run

Everything goes through Bun. Do not use npm or yarn; `bun.lock` is the lock file.

```sh
bun install
bun run start         # scan the QR code with Expo Go
bun run typecheck
bun run add <pkg>     # bunx expo install: SDK-compatible versions
bun run doctor
```

The Android package id is `dev.mirzohidkhon.khonfitness.expo`, so this
build installs next to the Kotlin app instead of colliding with its
signed release.

## Layout

- `src/app/` routes. `(tabs)/` holds Today, History, Programs.
- `src/theme/` HIG palette, Inter text styles, spacing (mirrors `Theme.kt`).
- `src/components/` shared UI: floating tab bar, grouped lists, buttons.
- `src/db/` SQLite schema, seed, repository (stage 1).

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
