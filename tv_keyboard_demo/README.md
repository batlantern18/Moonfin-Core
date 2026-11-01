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

## Run on an Android TV device or emulator

```sh
flutter run
```

With no device attached, create an Android TV emulator (Android TV image,
1080p, e.g. via Android Studio → Device Manager → Create Virtual Device → TV)
and then `flutter run` again. The app declares `LEANBACK_LAUNCHER` in its
manifest, so it appears in the TV launcher alongside regular launchers.

## Build a release APK

```sh
flutter build apk --release
```

The APK lands in `build/app/outputs/flutter-apk/app-release.apk`; install it
with `adb install -r build/app/outputs/flutter-apk/app-release.apk`.

## Current status

The focus infrastructure from the spec has landed under `lib/src/focus/` —
importable as one file:

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

Tests live in `test/focus/`; they run with the standard headless `flutter test`.

The theming system and demo screens from the spec land in follow-up tickets.

## Project layout

| Path | What it is |
| --- | --- |
| `lib/main.dart` | App entry point and the placeholder D-pad home screen |
| `lib/src/focus/` | The replicated focus infrastructure (see above) |
| `test/` | `flutter test` suite (focus unit/widget tests plus the home smoke test) |
| `android/` | Android/TV platform scaffolding (Gradle, manifest with leanback launcher) |
| `analysis_options.yaml` | Analyzer configuration, based on `flutter_lints` |
