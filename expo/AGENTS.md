This is an Expo/React Native mobile application. Prioritize mobile-first patterns, performance, and cross-platform compatibility.

## Never edit `android/` or `ios/`

`android/` and `ios/` in this repo are **generated output**, not source. `bunx expo prebuild` writes them from `app.json`, config plugins and the installed packages. They are committed only because the owner asked for it. `bunx expo prebuild --clean` deletes both folders and writes them again, so **any hand edit there is silently lost** on the next prebuild.

- Do not create, edit, rename or delete anything under `android/` or `ios/`, with any tool: editors, `sed`, scripts, generators. That covers `Info.plist`, `*.entitlements`, `project.pbxproj`, `Podfile`, `AppDelegate.swift`, `AndroidManifest.xml`, `build.gradle`, `gradle.properties`, `MainApplication.kt`, `res/` and everything else in there.
- Change native behaviour at the source instead:
  - `app.json`: `expo.ios` (`infoPlist`, `entitlements`, `bundleIdentifier`), `expo.android` (`permissions`, `package`), `expo.plugins` and its plugin options;
  - the package's own config plugin;
  - a local config plugin in `plugins/`, registered in `app.json`.

  Then run `bunx expo prebuild --clean` and commit the regenerated folders together with the source change.
- Changes that tools make there are fine to commit: `bunx expo prebuild`, `pod install` (`Podfile.lock`, `project.pbxproj`) and autolinking.
- If a change seems to need a native edit that no config or plugin can express, stop and ask the owner. Do not patch the generated files.
- Claude Code enforces this with `Edit(/android/**)` and `Edit(/ios/**)` deny rules in `.claude/settings.json`. Other agents have no such guard, so they follow this section themselves.
- Paths like `android/app/src/main/java/dev/mirzohidkhon/...` in `CLAUDE.md` belong to the original Kotlin app, a separate repo cloned to `../khon-fitness`. Read them, never write them.

## Code quality: write it like a senior engineer

The owner reviews for this. Code that works but ignores these rules is not done.

- **Reuse before writing.** Before building UI, read `src/components/` and use what is there (rows, field rows, sheets, number widget, buttons, chart). If two screens need the same piece, or a piece is described in `docs/DESIGN.md`, it is a shared component. Never copy a block of JSX from one screen into another; extract it.
- **Thin route files.** A file in `src/app/` reads its params, gets data from a hook and composes components. It holds only its default export; sub-components (rows, sheets, forms, sections) live in `src/components/<feature>/` (for example `src/components/session/SetRow.tsx`). Keep a route file under about 150 lines.
- **Small, single-purpose components.** One exported component per file, the file named after it. Split a component that grows past about 150 lines or does two jobs. Props get an explicit type; prefer a few clear props to a config object.
- **Logic out of components.** Rules, calculations, formatting and parsing go to `src/domain/` (pure TypeScript) with a `bun test` case. Components render and call handlers.
- **Data through hooks.** Screens read with named hooks (`useSession(id)`, `usePlan()`) built on `useLive`, and write through `src/db/repository.ts`. No SQL and no chains of repository calls inside a component.
- **Theme tokens only.** Colors, type and spacing come from `src/theme` (`K`, `Type`, `Space`). No raw hex values or ad-hoc font sizes in components; a size from `docs/DESIGN.md` that has no token gets a short comment naming the rule.
- **Strict types.** No `any`, no unchecked `as` casts, no `!` unless the line above proves the value exists. No dead code, unused exports or commented-out blocks.
- **Accessible by default.** Every pressable has a role and a label, touch targets are at least 44 pt, state (selected, checked, disabled) is exposed.
- **Comments say why.** A one-line doc comment on each exported component or function; inline comments only for decisions that are not obvious from the code.

## Expo has changed — do not trust your training data

Expo ships breaking changes every SDK release. APIs you remember are likely renamed, moved, or removed. Before writing any code that touches an Expo, EAS, or React Native API:

1. Read the major version of the `expo` package in `package.json`.
2. Fetch the matching versioned docs: `https://docs.expo.dev/versions/v<major>.0.0/`
3. For anything else, fetch https://docs.expo.dev/llms.txt — an index of all Expo docs with corrections to common LLM misconceptions. Follow its links to the specific page you need; never answer from memory.

## Commands

This project uses Bun: always `bun`/`bunx`, never npm/npx.

```bash
bunx expo install <package>  # ALWAYS use instead of npm/yarn/pnpm/bun add — resolves SDK-compatible versions
bunx expo start              # start the dev server
bunx expo lint               # lint
bunx tsc --noEmit            # typecheck
bunx expo-doctor             # diagnose dependency and config issues
bunx expo install --fix      # fix incompatible package versions
```

Run lint and typecheck before declaring any task done.

## Navigation & Routing

- Use **Expo Router** for all navigation. Routes live in `src/app/` — every file there is a screen, `_layout.tsx` files define navigators. Keep non-route code (components, hooks, utils) outside `src/app/`.
- Import `Link`, `router`, and `useLocalSearchParams` from `expo-router`.
- Docs: https://docs.expo.dev/router/introduction.md

## Building with EAS

Use EAS to build, sign, and submit the app in the cloud (`eas build`, `eas submit`) and to ship over-the-air updates (`eas update`) — no local Xcode or Android Studio required. Run EAS CLI as `bunx eas-cli <command>` in Bun projects, or `bunx eas-cli@latest <command>` otherwise; substitute that for bare `eas` in docs examples.
Docs: https://docs.expo.dev/eas/index.md

## Rules

- `ios/` and `android/` are generated by prebuild (Continuous Native Generation). They exist and are committed here, but they are still never edited by hand: see "Never edit `android/` or `ios/`" above.
- Expo Go only includes its bundled native modules. After adding a library with native code, the app needs a development build: `bunx expo run:ios|android` locally, or `eas build --profile development`.
- Prefer recommended Expo modules over third-party libraries, and check your available skills before adding dependencies. Docs: https://docs.expo.dev/versions/latest/index.md
