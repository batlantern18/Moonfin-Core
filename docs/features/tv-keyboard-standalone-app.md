# Standalone Android TV Keyboard App with Theming

## Problem Statement

Moonfin-Core has a sophisticated Android TV D-pad/keyboard focus system that works well inside the app, but there is no standalone, themable application that demonstrates and exercises this keyboard functionality independently. Developers, testers, and reviewers need a self-contained app they can run on an Android TV device or emulator to evaluate, debug, and customize the D-pad navigation experience without building or launching the full Moonfin application. The app should be fully themable so different visual styles can be previewed and compared.

## Solution

Build a standalone Flutter application (separate from Moonfin-Core's main app) that replicates the core D-pad/keyboard focus infrastructure extracted from Moonfin-Core and presents it through a set of demo screens. The app will:

1. Replicate the input mode detection, key event handling, and focus management from Moonfin-Core's focus system.
2. Provide demo screens that exercise every aspect of the keyboard system: list navigation, horizontal rows, grid layouts, dialogs, context menus, long-press actions, back key handling, and scrollable content.
3. Include a theming system with a theme model, theme registry, and multiple built-in themes (light, dark, high-contrast, tvOS-style).
4. Allow runtime theme switching via D-pad navigation in a settings screen.
5. Be a single, self-contained Flutter project that can be built and installed on Android TV, Fire TV, or any D-pad-equipped device.

The app will be a separate Flutter project (e.g., `tv_keyboard_demo/`) at the repository root, not embedded in Moonfin-Core's `lib/` directory.

## User Stories

1. As a developer, I want to launch the app on an Android TV device using only the D-pad, so that I can test the keyboard navigation without a mouse or touchscreen.
2. As a developer, I want the app to detect whether I am using a D-pad/keyboard or a mouse/touch, so that it can switch between traditional focus visuals and automatic focus behavior.
3. As a developer, I want the app to handle all standard D-pad keys (up, down, left, right, select/enter, back, context menu), so that every common remote button works as expected.
4. As a developer, I want the app to handle gamepad inputs (D-pad on a game controller, A/B/X buttons, analog sticks), so that gamepad-equipped remotes also work.
5. As a developer, I want to see a visible focus ring around the currently focused widget, so that I can verify focus is where I expect it.
6. As a developer, I want the focus ring to scale up the focused widget slightly, so that focus is visually obvious.
7. As a developer, I want horizontal rows of items (like Moonfin's "Trending" rows) where I can navigate left/right with D-pad arrows and the row scrolls smoothly, so that I can test the LockedFocusRow behavior.
8. As a developer, I want the app to remember my position in each row when I leave and return, so that the HubFocusMemory behavior is verifiable.
9. As a developer, I want to navigate vertically between rows using up/down, so that I can test the RowFocusCoordinator behavior.
10. As a developer, I want long-press on the select key to open a context menu, so that I can test the context action system.
11. As a developer, I want the context menu to be navigable with D-pad arrows and closable with back, so that the full context menu flow works.
12. As a developer, I want a grid layout (like a library grid) where I can navigate with D-pad arrows in both directions, so that I can test the GridFocusNodeMixin behavior.
13. As a developer, I want dialogs to be navigable with D-pad, so that I can test focus in modal contexts.
14. As a developer, I want the back key to navigate back through the screen stack, so that I can test the back key release handling.
15. As a developer, I want the app to have a settings screen where I can switch between themes using D-pad, so that I can test runtime theme switching.
16. As a developer, I want at least four built-in themes (light, dark, high-contrast, tvOS-style), so that I can compare different visual styles.
17. As a developer, I want the focus ring color to change per theme, so that theme switching is visually obvious.
18. As a developer, I want the focus animation duration to be configurable per theme, so that I can test different animation speeds.
19. As a developer, I want a theme model that defines colors, border radius, animation duration, and focus style (border-only, solid-fill, glass), so that themes are composable.
20. As a developer, I want the app to work on Android TV, Fire TV, and standard Android devices with a mouse, so that testing is flexible.
21. As a developer, I want the app to be a single Flutter project with no dependencies on Moonfin-Core's internal packages, so that it is truly standalone.
22. As a developer, I want the app to include a "demo data" mode that generates sample content (long names, many items, empty states), so that I can test edge cases.
23. As a developer, I want the app to show the current input mode (keyboard vs pointer) on screen, so that I can verify mode detection.
24. As a developer, I want the app to show the current focused widget's label, so that I can verify focus position.
25. As a developer, I want the app to handle edge cases (first/last item in a row, empty rows, single-item grids), so that I can test boundary conditions.
26. As a developer, I want the app to support RTL (right-to-left) layout, so that I can test RTL D-pad behavior.
27. As a developer, I want the app to have a "glide" mode where the row scrolls smoothly with a spring animation as I navigate, so that I can test the RowGlide behavior.
28. As a developer, I want the app to support step-scroll in list views (D-pad up/down scrolls the list by a fixed step), so that I can test stepScrollWithinContextBounds.
29. As a developer, I want the app to have a settings screen where I can toggle features (glide on/off, animation speed, focus style), so that I can experiment.
30. As a tester, I want to run automated tests against the app using a test framework that simulates D-pad key events, so that I can verify keyboard behavior programmatically.
31. As a reviewer, I want to be able to build and install the app on an emulator without an Android TV device, so that I can review the keyboard behavior remotely.
32. As a designer, I want to preview how different themes look with the focus system, so that I can iterate on visual design.
33. As a developer, I want the app to have a "focus mode" toggle that forces traditional focus visuals even when a mouse is active, so that I can test TV focus behavior with a mouse attached.
34. As a developer, I want the app to handle key repeat events (holding a D-pad arrow), so that I can test repeat behavior.
35. As a developer, I want the app to handle the "context menu" key (long-press on select or dedicated context menu button), so that I can test context menu invocation.
36. As a developer, I want the app to have a list screen with D-pad-selectable items (like Moonfin's DpadListTile), so that I can test the list tile focus system.
37. As a developer, I want the app to have a switch/radio group screen with D-pad-selectable options, so that I can test the DpadSwitchListTile and DpadRadioGroup behavior.
38. As a developer, I want the app to have a search screen with a text field that is navigable with D-pad, so that I can test text input on TV.
39. As a developer, I want the app to have a "home" screen that acts as the root, with multiple rows and a bottom navigation bar, so that I can test the full navigation hierarchy.
40. As a developer, I want the app to have a "about" screen that lists all the keyboard features and their status, so that I can verify everything is working.

## Implementation Decisions

### Architecture

- The app will be a **separate Flutter project** at the repository root (e.g., `tv_keyboard_demo/`), not embedded in Moonfin-Core.
- It will use Flutter's built-in focus system and be written in pure Dart/Flutter with no dependency on Moonfin-Core's internal packages.
- The app will be structured as a monolithic project with a clear separation between the focus infrastructure, the demo screens, and the theming system.

### Focus Infrastructure

The app will replicate the following core components from Moonfin-Core:

1. **InputModeTracker** — A root widget that detects whether the user is using a keyboard/D-pad or a pointer/mouse. It sets Flutter's `FocusHighlightStrategy` to `alwaysTraditional` for keyboard input and `automatic` for pointer input. On TV devices, it defaults to keyboard mode. It provides an inherited widget (`InputModeProvider`) that descendant widgets can query.

2. **Key Definitions** — A set of key extensions and constants that define which keys are "actionable" (KeyDownEvent, KeyRepeatEvent), which keys are "select" (Enter, Space, GameButtonA), which are "back" (Escape, GoBack, GameButtonB), which are "context menu" (ContextMenu, GameButtonX), which are "directional" (arrows), and which are "page forward/back" (FastForward, TrackNext, Rewind, TrackPrevious).

3. **Key Event Handlers** — Functions that map key events to callbacks:
   - `dpadKeyHandler()` — maps up/down/left/right/select to callbacks.
   - `handleOneShotSelect()` — handles single-tap select and suppresses the key-up event.
   - `handleBackKeyAction()` — handles back key press and release.
   - `LongPressSelectKeyHandler` — handles long-press on select (500ms threshold) and context menu key.
   - `arrowScrollHandler()` — maps up/down to scroll steps.
   - `consumeIfEdge()` — blocks edge keys (e.g., left at the first item).

4. **Focus Widgets** — Reusable widgets that wrap content with focus behavior:
   - `FocusableWrapper` — wraps any child in a Focus node with D-pad key handling, auto-scroll, scale animation, and optional solid background focus.
   - `DpadListTile` — a ListTile with TV focus highlight and D-pad select.
   - `DpadSwitchListTile` — a SwitchListTile with D-pad toggle.
   - `DpadRadioGroup` / `DpadRadioListTile` — radio group with D-pad navigation.
   - `GlassFocusHalo` — tvOS-style focus ring (scale + glow + ring).
   - `LockedFocusRow<T>` — a horizontal row with one focused item, value notifier broadcast, long-press support, and vertical navigation delegation.
   - `RowGlide` — critically damped spring for smooth row scrolling.
   - `HubFocusMemory` — static map that remembers last focused index per hub.

5. **Focus Navigation** — Infrastructure for coordinating focus between widgets:
   - `FocusRouteObserver` — a NavigatorObserver that grants initial focus to the first content descendant of a new route.
   - `RowFocusCoordinator` — coordinates vertical D-pad navigation between rows.
   - `GridFocusNodeMixin` — mixin for grid layouts with per-item FocusNodes and focus restoration.

6. **Scroll Utilities** — Helper functions for D-pad scrolling:
   - `revealInNearestScrollable()` — scrolls a widget into view.
   - `stepScrollWithinContextBounds()` — steps scroll by a fixed amount within viewport bounds.

### Theming System

The app will have a theming system with the following components:

1. **ThemeModel** — A data class that defines:
   - `name` — human-readable name.
   - `id` — unique identifier.
   - `focusColor` — color of the focus ring/border.
   - `focusBackgroundColor` — background color when focus is solid-fill.
   - `focusForegroundColor` — text/icon color on solid-fill background.
   - `focusBorderRadius` — border radius for focus decorations.
   - `focusAnimationDuration` — duration of focus animations.
   - `focusScale` — scale factor for focused widgets.
   - `focusStyle` — enum: `borderOnly`, `solidFill`, `glass`.
   - `brightness` — light or dark.
   - `primaryColor`, `accentColor`, `surfaceColor`, `onSurfaceColor` — standard theme colors.

2. **ThemeRegistry** — A singleton that holds the current theme and provides a `ThemeModel` inherited widget. It will have methods to switch themes and listen for changes.

3. **Built-in Themes**:
   - **Light** — Standard light theme with accent-colored focus rings.
   - **Dark** — Standard dark theme with accent-colored focus rings.
   - **HighContrast** — High contrast theme with thick, bright focus rings and solid-fill style.
   - **tvOSStyle** — tvOS-inspired theme with white hairline rings, glow effects, and glass style.

4. **Theme Switching** — A settings screen where the user can D-pad-navigate through available themes and select one. The selection is persisted to shared_preferences.

### Demo Screens

The app will include the following demo screens to exercise the focus system:

1. **HomeScreen** — The root screen with:
   - Multiple horizontal rows (LockedFocusRow) of items.
   - A bottom navigation bar.
   - Vertical navigation between rows.
   - A "Settings" entry that navigates to the settings screen.

2. **ListScreen** — A vertical list of items (DpadListTile) with:
   - D-pad up/down navigation.
   - D-pad select to "open" an item.
   - Long-press for context menu.
   - Scroll handling.

3. **GridScreen** — A grid of items (using GridFocusNodeMixin) with:
   - D-pad up/down/left/right navigation.
   - D-pad select to "open" an item.
   - Long-press for context menu.

4. **DialogScreen** — A modal dialog with:
   - D-pad navigable buttons.
   - D-pad select to confirm/cancel.
   - Back key to dismiss.

5. **SettingsScreen** — A settings screen with:
   - Theme switching (D-pad navigable list).
   - Feature toggles (glide on/off, animation speed, focus style).
   - Input mode display (keyboard vs pointer).
   - Current focused widget label display.

6. **AboutScreen** — An about screen listing all keyboard features and their status.

### Platform Detection

The app will use Flutter's `PlatformDetection` (or equivalent) to detect whether it is running on a TV device and adjust behavior accordingly. On TV, it defaults to keyboard input mode and traditional focus highlights. On non-TV devices, it defaults to pointer mode but allows switching.

### Demo Data

The app will include a "demo data" generator that creates:
- Items with short and long names.
- Items with special characters and emoji.
- Empty lists and single-item lists.
- Grids with varying row lengths.
- RTL layout test data.

### Testing Infrastructure

The app will include:
- Unit tests for key event handlers (dpadKeyHandler, handleOneShotSelect, etc.).
- Widget tests for FocusableWrapper, DpadListTile, LockedFocusRow, GlassFocusHalo.
- Integration tests that simulate D-pad key sequences and verify focus movement.
- A test helper that simulates key events and reads focus state.

## Testing Decisions

### What Makes a Good Test

- Tests should only verify external behavior: focus moves to the expected widget, select triggers the expected callback, back key navigates back, context menu opens on long-press.
- Tests should not verify implementation details: internal state of FocusNode, exact animation values, internal spring calculations.
- Tests should be runnable on a headless CI runner (no Android TV device required).

### Modules to Test

1. **Key Event Handlers** — Unit tests for `dpadKeyHandler()`, `handleOneShotSelect()`, `handleBackKeyAction()`, `LongPressSelectKeyHandler`, `arrowScrollHandler()`, `consumeIfEdge()`.
2. **Focus Widgets** — Widget tests for `FocusableWrapper`, `DpadListTile`, `LockedFocusRow`, `GlassFocusHalo`.
3. **Focus Navigation** — Widget tests for `RowFocusCoordinator`, `GridFocusNodeMixin`.
4. **Input Mode Detection** — Widget tests that simulate pointer events and verify mode switching.
5. **Theming** — Widget tests that verify theme switching updates focus colors.
6. **Integration** — Integration tests that simulate a full D-pad navigation sequence (navigate rows, open context menu, long-press, navigate back).

### Prior Art

- Moonfin-Core's existing tests: `test/tv_keyboard_test.dart` and `test/tv_keyboard_back_test.dart`.
- Flutter's built-in focus testing utilities (`TestUtils`, `WidgetTester`).

## Out of Scope

- Integration with Moonfin-Core's server backends (Jellyfin, Emby, Plex, Silo).
- Actual media playback functionality.
- User authentication or account management.
- Offline support or caching.
- Gamepad mapping customization (basic gamepad support is in scope, but not remapping).
- Custom theme creation/editing (only built-in themes and switching are in scope).
- Accessibility features beyond standard D-pad focus (screen reader, magnification).
- Multi-language/localization (the app will be in English only).
- Building or distributing the app (APK generation, Play Store publishing).
- Performance optimization for low-end devices.
- Custom input method editor (IME) integration.

## Further Notes

- The app should be structured so that the focus infrastructure can be extracted into a separate package (e.g., `tv_keyboard_focus/`) if desired in the future.
- The theming system should be designed to be extensible: adding a new theme should only require adding a `ThemeModel` entry, not modifying the infrastructure.
- The app should be buildable with `flutter build apk` for Android TV and `flutter build ios` for iOS simulator (for non-TV testing).
- Consider using `androidTv` as the default target platform in the Flutter project's `pubspec.yaml`.
- The app should include a `README.md` that explains how to build, run, and test it.
- The app should be added to `.gitignore` for any build artifacts (build/, .dart_tool/).
- Consider adding a `tv_keyboard_demo/` directory at the repository root with a standard Flutter project structure.
