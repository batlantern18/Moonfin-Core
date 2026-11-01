# tv_keyboard_demo

Standalone Android TV keyboard demo app. A self-contained Flutter project at
the root of the Moonfin-Core repository, completely independent of Moonfin-Core's
`lib/` and internal packages — it replicates and exercises the D-pad/keyboard
focus infrastructure described in
[`docs/features/tv-keyboard-standalone-app.md`](../docs/features/tv-keyboard-standalone-app.md).

## Prerequisites

- Flutter (the repository CI pins **3.47.2**; any recent stable Flutter with a
  matching Dart SDK will do)
- An Android TV device or emulator for on-device testing

## Getting started

```sh
cd tv_keyboard_demo
flutter pub get
```

## Analyze & test

```sh
flutter analyze
flutter test
```

The complete test command includes headless D-pad navigation flows for rows,
dialogs, list context menus, theme changes, and edge-case data. CI runs the same
`flutter analyze` and `flutter test` commands in
`.github/workflows/tv-keyboard-demo.yml`.

## Run on an Android TV device or emulator

```sh
flutter run --dart-define=TV_KEYBOARD_TV_MODE=true
```

With no device attached, create an Android TV emulator (Android TV image,
1080p, e.g. via Android Studio → Device Manager → Create Virtual Device → TV)
and then run the same command. The app declares `LEANBACK_LAUNCHER` in its
manifest, so it appears in the TV launcher alongside regular launchers.

## Build a release APK

```sh
flutter build apk --release
```

The APK lands in `build/app/outputs/flutter-apk/app-release.apk`; install it
with `adb install -r build/app/outputs/flutter-apk/app-release.apk`.

## Features

The focus infrastructure lives under `lib/src/focus/` — importable as one
file:

```dart
import 'package:tv_keyboard_demo/src/focus/focus.dart';
```

It replicates Moonfin-Core's focus system with zero Moonfin-Core imports:

- **Inputmode tracking** — `InputModeTracker` (+ the `InputMode`/`InputModeProvider`
  inherited widget) switches keyboard/D-pad vs pointer mode and pins keyboard on
  TV (`TvPlatformDetection`).
- **Key definitions** — `dpadKeys` key sets: select (Enter, Space, GameButtonA,
  ...), back (Escape, GoBack, ...), context menu, directional, and page
  transport keys.
- **Key handlers** — `dpadKeyHandler`, `handleOneShotSelect`,
  `handleBackKeyAction`, `LongPressSelectKeyHandler`, `arrowScrollHandler`,
  `consumeIfEdge`.
- **Focus widgets** — `FocusableWrapper`, `DpadListTile`,
  `DpadSwitchListTile`, `DpadRadioGroup`/`DpadRadioListTile`,
  `GlassFocusHalo` (+ `GlassPressScale`), `LockedFocusRow`, `RowGlide` and
  `HubFocusMemory`.
- **Focus navigation** — `FocusRouteObserver`, `RowFocusCoordinator`,
  `GridFocusNodeMixin`.
- **Scroll utilities** — `revealInNearestScrollable`,
  `stepScrollWithinContextBounds`, `scrollListToIndex`, `scrollFocusIntoView`.

The app includes horizontal home rows with spring glide, list, grid, and search
navigation, navigable context menus and dialogs, runtime theme switching with
persistent selection, focus-mode controls, RTL preview, edge-case sample data,
and an About screen. Tests live in `test/` and run with the standard headless
`flutter test`.

## Project layout

| Path | What it is |
| --- | --- |
| `lib/main.dart` | App entry point and theme registry wiring |
| `lib/src/focus/` | The replicated focus infrastructure (see above) |
| `lib/src/screens/` | Home, list/grid, search, settings, dialog, and about screens |
| `lib/src/theme/` | Theme model, registry, and four built-in themes |
| `test/` | Focus, theme, screen, and integration tests |
| `android/` | Android/TV platform scaffolding (Gradle, manifest with leanback launcher) |
| `analysis_options.yaml` | Analyzer configuration, based on `flutter_lints` |
