# Native Android TV Keyboard (LeanKeyboard parity)

> Supersedes [`tv-keyboard-standalone-app.md`](tv-keyboard-standalone-app.md). The Flutter
> D-pad focus demo is no longer the target. The app is now a real Android TV input method
> (IME) written in Kotlin.

## Problem Statement

Moonfin-Core's previous `tv_keyboard_demo/` project was a Flutter **focus demo**: it
exercised D-pad navigation widgets but could never replace the system keyboard on a TV.
Android TV users and developers still need a standalone, installable **on-screen
keyboard** that works with every app, is fully usable with a remote, and offers the same
breadth of layouts and input features as
[LeanKeyboard](https://github.com/yuliskov/leankeyboard).

The keyboard must:

- register as a real Android input method, so it appears on every text field on the TV;
- be navigable end-to-end with a D-pad, with optional touch/mouse support;
- support the language layouts, accent popups, symbols, voice input, clipboard, cursor
  movement, suggestions, and settings that LeanKeyboard users rely on;
- be themed (Light, Dark, High Contrast, tvOS) with live switching;
- ship as an **Android TV-only** app, not a phone app with a TV mode.

## Solution

Replace the Flutter demo with a native Kotlin Android TV application at `tv_keyboard/`,
built as a single Gradle `app` module with package `com.moonfin.tvkeyboard`.

The app will:

1. Implement `InputMethodService` (the IME) and its input view in Kotlin.
2. Vendor LeanKeyboard's MIT-licensed keyboard layout XML (26 languages, ~103 files)
   with attribution, and implement the parser, renderer, navigation, and behavior in
   Kotlin. No LeanKeyboard Java source is copied.
3. Provide a custom-drawn, remote-first keyboard surface with a single internal focus
   cursor, a mini accent keyboard, key repeat, and touch/mouse hit-testing.
4. Provide the full LeanKeyboard input feature set: shift/caps, symbols/numbers,
   language cycling and picker, accent popups, suggestions, voice, clipboard, cursor
   keys, editor action, auto-space, keep-on-screen, enlarge, cyclic navigation, RTL.
5. Ship a TV-friendly settings/activation app (launcher + leanback entry points) for
   enabling the IME, choosing layouts and themes, and toggling options.
6. Target Android TV only: `android.software.leanback` required, no touchscreen
   requirement, TV banner, landscape-first layout, D-pad as the primary input.
7. Be verified by JVM/Robolectric tests and an Android-TV-only CI workflow.

The app is standalone: it does not depend on Moonfin-Core's `lib/` or internal packages.

## User Stories

### Installation and activation

1. As an Android TV user, I want the app to appear as an input method in system settings,
   so that I can select it as my keyboard.
2. As an Android TV user, I want a launcher/settings app that shows whether the keyboard
   is enabled and selected, so that I can fix activation without hunting through menus.
3. As an Android TV user, I want an "Open keyboard settings" action, so that enabling the
   IME takes only a few remote presses.
4. As an Android TV user, I want the IME to appear automatically on any editable text
   field, so that I can type in every app.
5. As an Android TV user, I want the keyboard to stay on screen in landscape, so that the
   layout does not jump or disappear while typing.
6. As an Android TV user, I want the keyboard to hide cleanly when I press Back, so that I
   can dismiss it without leaving the field.

### Remote and controller input

7. As a remote user, I want to move between keys with the D-pad, so that I never need a
   mouse.
8. As a remote user, I want Up/Down to move between keyboard rows, so that navigation is
   predictable.
9. As a remote user, I want Left/Right to move along a row and wrap when cyclic navigation
   is enabled, so that long rows are quick to traverse.
10. As a remote user, I want the select/center key to type the focused key, so that typing
    is one press per character.
11. As a remote user, I want holding Backspace or the cursor keys to repeat, so that
    correcting text is fast.
12. As a remote user, I want the focused key to be clearly highlighted and animated, so
    that I always know where I am.
13. As a gamepad user, I want the A button to act as select and the D-pad/stick to move, so
    that a controller works like a remote.
14. As a mouse/touch user, I want to tap a key directly, so that the keyboard also works on
    touch-enabled TVs and emulators.
15. As a user, I want physical keyboard input to hide the on-screen keyboard when that
    option is on, so that the screen is not wasted.

### Typing and editing

16. As a user, I want Shift to type uppercase letters and Shift again to return to
    lowercase, so that typing is predictable.
17. As a user, I want long-pressing Shift to lock Caps, so that I can type several capitals
    without repeated presses.
18. As a user, I want the symbols/numbers key to switch layouts and the alphabet key to
    switch back, so that I can reach punctuation quickly.
19. As a user, I want a number layout for numeric fields, so that entering codes and
    numbers is efficient.
20. As a user, I want long-pressing a letter to open accent choices, so that I can type é,
    ü, ñ, č, and other accented characters.
21. As a user, I want accent choices to follow the current Shift state, so that uppercase
    accents are possible.
22. As a user, I want a space key, so that I can separate words.
23. As a user, I want Backspace to delete before the cursor, so that I can correct mistakes.
24. As a user, I want left/right cursor keys, so that I can move inside the text without a
    hardware keyboard.
25. As a user, I want the Enter/Search/Next/Done/Send action to match the focused field, so
    that submitting a form works as the app expects.
26. As a user, I want auto-space after a suggestion or voice result when enabled, so that
    dictation flows naturally.

### Languages and layout data

27. As a user, I want 26 language layouts (QWERTY and AZERTY variants), so that I can type
    in my language.
28. As a user, I want the language/world key to cycle through my enabled layouts, so that I
    can switch quickly.
29. As a user, I want long-pressing the language key (or the space bar) to open a language
    picker, so that I can jump directly to a layout.
30. As a user, I want my selected language and layout to persist across reboots, so that I
    do not reconfigure every time.
31. As a user, I want the space bar to show the active language name, so that I can see
    which layout is active.
32. As an RTL user, I want Arabic, Hebrew, and Persian layouts to read and navigate
    correctly, so that right-to-left typing is not broken.
33. As a user, I want the keyboard to fall back to a sensible default layout if a language
    resource is missing, so that it never fails to open.
34. As a developer, I want the app to optionally auto-detect the system language on first
    run, so that the default layout matches the device.

### Suggestions, voice, and clipboard

35. As a user, I want a suggestions row to show completions and common domains, so that I
    can enter text with fewer key presses.
36. As a user, I want to navigate the suggestions row with the D-pad and insert a
    suggestion with select, so that suggestions are usable without touch.
37. As a user, I want to turn the suggestions row off, so that the keyboard can be compact.
38. As a user, I want a voice key that starts speech recognition and inserts the result, so
    that I can dictate into a field.
39. As a user, I want a clear message if speech recognition or microphone permission is
    unavailable, so that the key never fails silently.
40. As a user, I want a clipboard key that opens recent clipboard text and pastes the
    selected item, so that copying between apps works on TV.
41. As a user, I want clipboard entries to be selectable, dismissable with Back, and limited
    to text, so that the overlay is safe and simple.

### Themes and appearance

42. As a user, I want Light, Dark, High Contrast, and tvOS themes, so that I can match my
    room and preferences.
43. As a user, I want theme changes to apply to the keyboard without restarting, so that
    switching is immediate.
44. As a user, I want the focused key, key text, icons, suggestion bar, action button, and
    popups to all follow the theme, so that nothing looks out of place.
45. As a user, I want an "Enlarge keyboard" option, so that keys are easier to read from a
    couch.
46. As a user, I want a "Keep on screen" option, so that the keyboard persists even when a
    physical keyboard is connected.
47. As a user, I want a "Cyclic navigation" option, so that D-pad focus wraps around the
    keyboard edges.
48. As a user, I want the chosen theme and options to persist, so that my setup survives
    restarts.

### Settings, diagnostics, and quality

49. As a developer, I want a settings screen with Activation, Layout, Theme, Options, and
    About sections, all navigable by D-pad, so that no mouse is required.
50. As a developer, I want an About section with version, build type, IME status, and
    LeanKeyboard/MIT attribution, so that licensing and support are clear.
51. As a developer, I want a layout preview in settings, so that I can verify a layout
    before selecting it.
52. As a tester, I want automated tests that parse every vendored layout, resolve every
    key action, verify navigation, themes, preferences, and suggestions, so that
    regressions are caught in CI.
53. As a tester, I want the app to build and the unit tests to run on a headless Linux
    runner, so that CI does not need a TV.
54. As a tester, I want a documented TV-emulator smoke test covering activation, typing,
    symbols, accents, language switching, voice, clipboard, and themes, so that release
    verification is repeatable.
55. As a maintainer, I want the APK to be Android TV-only (leanback required, no
    touchscreen requirement), so that the Play Store and sideloading see the correct
    device support.

## Implementation Decisions

### Project and toolchain

- **Location:** `tv_keyboard/` at the repository root. `tv_keyboard_demo/` is deleted.
- **Module:** single `:app` module, namespace/applicationId `com.moonfin.tvkeyboard`.
- **Toolchain:** Gradle 8.14.3, AGP 8.13.1, Kotlin 2.2.21, JDK 21 (compile target 17),
  `compileSdk = 35`, `minSdk = 21`, `targetSdk = 35`.
- **Dependencies:** Android framework only at runtime (no AppCompat, no Leanback library).
  Settings UI is built from stock `View`s so the method count and startup cost stay low.
  Tests add JUnit 4 and Robolectric.
- **Build variants:** `debug` and `release` (unminified for v1). Only Android TV device
  filters are emitted.

### Manifest and IME registration

- `TvKeyboardService extends InputMethodService`, exported with
  `android.permission.BIND_INPUT_METHOD`, intent filter `android.view.InputMethod`, and
  `meta-data android.view.im` pointing at `res/xml/method.xml`.
- `MainActivity` is the launcher/settings activity, registered for both `MAIN/LAUNCHER`
  and `MAIN/LEANBACK_LAUNCHER`.
- `<uses-feature android:name="android.software.leanback" android:required="true"/>` and
  `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`.
  This is the TV-only gate.
- Permissions: `RECORD_AUDIO` (voice), `RECEIVE_BOOT_COMPLETED` only if needed for state
  refresh. No storage permission; clipboard and voice go through framework APIs.
- Application has a TV banner, launcher icon, and `android:supportsRtl="true"`.

### Keyboard data

- Vendor the 103 LeanKeyboard layout XML files under `app/src/main/res/xml/`:
  `qwerty_*`, `azerty_*`, `sym_*`, `number`, `empty_kbd`, `popup_symbols`, `shift_*`,
  `accent_*`. Do **not** vendor LeanKeyboard's `method.xml` (we ship our own).
- Vendor `languages.xml` (26 entries in `Name|code[|azerty]` form).
- Add `NOTICE`/`THIRD_PARTY_LICENSES` with the LeanKeyboard MIT license and attribution.
- Layout resolution mirrors LeanKeyboard:
  - alphabet: `qwerty_<code>` unless the entry is AZERTY, then `azerty_<code>`;
  - symbols: `sym_en_us` (LeanKeyboard's choice for every language);
  - numbers: `number` for numeric input types.
- Parse with `android.inputmethodservice.Keyboard` and its public `Key` geometry,
  `codes`, `label`, `icon`, `edgeFlags`, and `popupResId`. This guarantees the vendored
  XML behaves the same as in LeanKeyboard.
- Key codes: `-1` shift, `-2` mode change, `-3`/`-4` cursor left/right, `-5` delete,
  `-7` voice, `-9` language, `-10` clipboard.

### Keyboard view

- A custom `View` (`KeyboardView`) draws all keys on a `Canvas`; it does **not** use
  Android per-key focus. One internal focus index is the source of truth.
- Geometry: all key rects are scaled uniformly from the parsed keyboard's pixel geometry
  to the view width. Main layouts fill the width; the 4-column number layout is centered
  at the same key size rather than stretched.
- Navigation: geometric nearest-key search per direction, weighted by the perpendicular
  offset and requiring overlap in the target row where possible. Space is treated as a
  wide key. Edges use `Key.edgeFlags`; Right from the right edge moves to the action
  button, Up from the top row moves to suggestions (or hides the keyboard).
- Cyclic navigation: when enabled and no candidate exists, wrap horizontally within the
  row or vertically to the opposite row.
- Focus/cursor: an animated highlight rect (short decelerate animation) drawn over the
  focused key. Pressed state flashes the accent fill.
- Press handling: center/Enter/A triggers the focused key; holding Backspace/Left/Right
  repeats after a short delay; holding a key with `popupKeyboard` opens accent choices;
  holding Shift locks Caps; holding Space or the language key opens the language picker.
- Accent popup: a mini keyboard rendered above the focused key, navigated with Left/Right
  and Up/Down, selected with center, dismissed with Back or Down at the bottom. Selected
  accents honor Shift and reset one-shot Shift.
- Touch/mouse: nearest-key hit testing on `ACTION_DOWN`, press on `ACTION_UP`, long-press
  detection with the same popup behavior. Hover moves the cursor on pointer devices.
- Rendering supports themed key backgrounds, text, icon tinting, focus fill/stroke,
  suggestion chips, action button, and popups. Icons are the vendored `ic_ime_*` PNGs,
  tinted per theme.

### Root view and input pipeline

- `KeyboardRootView` composes:
  - the suggestions row (horizontal, one chip per suggestion),
  - the editor-action button (label from `EditorInfo.imeOptions`: Go, Search, Send, Next,
    Done, or Enter),
  - the keyboard view,
  - overlays for the accent popup (inside the keyboard view), voice state, and clipboard.
- Focus zones are explicit: `KEYBOARD`, `SUGGESTIONS`, `ACTION`, `OVERLAY`. The root view
  intercepts D-pad navigation and moves focus between zones at the edges; Back dismisses
  overlays/popups before hiding the IME.
- Touch taps on suggestions/action use normal click handling; the keyboard view handles
  its own touch.
- `TvKeyboardService` owns the `InputConnection`:
  - inserts characters derived from the key label, uppercasing per Shift and the active
    layout's locale;
  - handles Backspace (`deleteSurroundingText`), cursor keys
    (`sendKeyEvent(KEYCODE_DPAD_LEFT/RIGHT)`), space, and Enter/editor actions
    (`performEditorAction` with the field's action id);
  - reads text before the cursor for suggestions and applies suggestions/voice text;
  - tracks Shift (off/on/locked), mode (alphabet/symbols/numbers), language index, and
    editor info per input session.
- Keep-on-screen: when enabled and the field is multiline, keep the IME visible; honor
  the option when physical keyboards are attached.
- Enlarge: scales key size/padding up by a fixed factor (drawn, not a new layout file).
- Fire TV/system state: accept the LeanKeyboard-style keyboard-state broadcast if present
  and refresh the IME; degrade gracefully when unavailable.

### Suggestions

- `SuggestionEngine` parses the text before the cursor, asks the input connection for
  completions, and supplies common domains for web/email fields (LeanKeyboard behavior).
- A `SuggestionsProvider` abstraction keeps the engine unit-testable without an IME.
- Suggestions are limited (about 10) and cleared on submit, focus changes, and after a
  short idle delay.
- Selecting a suggestion replaces the current word (or appends), optionally adds a space
  (auto-space), and returns focus to the keyboard.

### Voice and clipboard

- `VoiceInputController` uses `SpeechRecognizer` + `RecognizerIntent` with partial
  results. Results are inserted at the cursor; errors surface as a short inline message.
  If recognition is unavailable, a clear inline message is shown.
- Microphone permission is requested from the settings activity; the IME checks it before
  listening and routes the user to settings if missing.
- `ClipboardController` reads `ClipboardManager` text clips and shows them in an inline
  D-pad-navigable overlay. Selecting a clip commits its text; Back closes the overlay.

### Themes

- `KeyboardTheme` data class: id, display name, background, key, key pressed, key text,
  focus fill/stroke, focus text, suggestion bar, suggestion text, action color, popup
  background, border width, corner radius, animation duration, brightness.
- Built-ins: **Light**, **Dark**, **High Contrast**, **tvOS**. High Contrast uses thick
  bright strokes and opaque fills; tvOS uses a near-black surface, white focus ring, and
  subtle glow.
- `ThemeRepository` reads/writes the selection in `SharedPreferences`; both the IME and
  the settings preview observe changes and redraw without recreation.
- Theme is applied consistently to keys, icons, suggestions, action button, popups, and
  the settings preview card.

### Settings app

- `MainActivity` uses stock views and a TV-safe layout: a header with IME status, a
  vertically scrollable list of D-pad-focusable rows, and a preview card.
- Rows open sub-screens or dialogs:
  - **Activation:** IME enabled?, current IME, buttons to open
    `ACTION_INPUT_METHOD_SETTINGS` and `ACTION_INPUT_METHOD_SUBTYPE_SETTINGS`.
  - **Layout:** list of the 26 languages with checkboxes (enabled layouts) and a preview;
    at least one layout is always enabled.
  - **Theme:** radio list of the four themes with live preview.
  - **Options:** Keep on screen, Enlarge keyboard, Suggestions, Cyclic navigation,
    Auto-space, Hide when physical keyboard is used.
  - **About:** app version/build, IME status, LeanKeyboard attribution and MIT license,
    and project links.
- All rows use explicit focus highlighting, focus restoration on return, and Back
  navigation.

### Testing and CI

- **Unit (JVM):** layout resolution (qwerty/azerty/symbols/numbers), key action mapping,
  shift/case handling, language cycling and fallback, cyclic/geometric navigation,
  suggestion word replacement, preference defaults/persistence, theme defaults.
- **Robolectric:** parse all 103 vendored XML files and assert non-empty key sets, valid
  popup references, icon/string references, and no missing resources; measure the root
  view and keyboard view without crashing.
- **CI:** replace `.github/workflows/tv-keyboard-demo.yml` with an Android-TV-only
  workflow that runs `./gradlew :app:assembleDebug :app:testDebugUnitTest`, uploads the
  debug APK, and only triggers on `tv_keyboard/**`.
- **Manual smoke test:** documented steps on an Android TV emulator/image for activation,
  D-pad typing, shift/caps, symbols, number field, accents, language switching and picker,
  suggestions, voice (or graceful failure), clipboard, each theme, enlarge, cyclic, and
  keep-on-screen.

## Testing Decisions

### What makes a good test

- Test externally visible behavior: a direction moves focus to the expected key; select
  commits the expected text; shift changes the committed case; a layout parses and exposes
  usable keys; a theme resolves to the expected colors; a preference round-trips.
- Do not assert animation internals, exact pixel values, or private helper state.
- Every vendored layout is covered by a parsing test, so a broken XML shows up in CI.
- Tests must run headless; no TV, no emulator, no input method activation required.

### Modules to test

1. `KeyboardLayouts` — layout resource resolution and fallbacks.
2. `KeyboardAction` / key action mapping — all negative key codes and editor actions.
3. `KeyboardView` navigation model — a pure `KeyboardNavigator` used by the view, tested
   for directions, edges, cyclic wrap, wide space key, and small numeric layout.
4. `KeyboardPreferences` — defaults, persistence, language enablement invariant.
5. `KeyboardTheme` / `ThemeRepository` — four themes, selection persistence, color roles.
6. `SuggestionEngine` — word prefix replacement, domain suggestions, limits, auto-space.
7. `TvKeyboardService` text behavior — extracted into a testable `TextCommitter` /
   `EditorActionResolver`.
8. Layout parsing (Robolectric) — all 103 XML files, icons, popups, strings, dimensions.
9. Root view (Robolectric) — measure, focus-zone transitions, suggestion/action clicks.

### Prior art

- LeanKeyboard's manual behavior and layout XMLs (MIT) are the behavioral reference.
- The old Flutter widget tests are retained only as historical D-pad navigation checks;
  they do not apply to the Kotlin app.
- Android's `Keyboard`/`KeyboardView` source documents layout parsing semantics.

## Out of Scope

- Phone/tablet support, iOS, Flutter, and the old `tv_keyboard_demo/` project.
- LeanKeyboard's update checker, analytics, donation links, and its custom add-on stores.
- Full dictionary-based prediction, learning, and cloud sync.
- User-authored themes or custom theme import.
- Custom key/layout editing beyond enabling LeanKeyboard's built-in layouts.
- Non-text clipboards (images, URIs) and clipboard history beyond the system clip.
- Localization of the settings UI beyond English (keyboard layouts themselves stay
  multilingual).
- Play Store publishing, signing, and release distribution.
- Accessibility beyond D-pad focus (screen readers, magnification).
- Performance work for phones or low-memory devices.

## Delivery Plan

Proposed tickets, in dependency order:

| # | Ticket | Blocked by | Delivers |
|---|--------|-----------|----------|
| 1 | Gradle scaffolding and TV manifest | — | `tv_keyboard/` Gradle project, toolchain, TV-only manifest, method.xml, resources skeleton, CI stub |
| 2 | Layout data, parser, and navigation model | 1 | Vendored XML + NOTICE, 26-language registry, `KeyboardLayouts`, pure `KeyboardNavigator`, layout parsing tests |
| 3 | IME service and text pipeline | 1, 2 | `TvKeyboardService`, `InputConnection` commits, shift/caps, modes, editor actions, repeat, preferences, suggestions engine, voice/clipboard controllers |
| 4 | Keyboard view: rendering, D-pad, touch, accents | 2, 3 | Custom `KeyboardView`, focus animation, key icons/labels, accent popup, touch/mouse, key repeat |
| 5 | Root view: suggestions, action, overlays | 3, 4 | `KeyboardRootView`, focus zones, suggestion chips, action button, voice state, clipboard overlay |
| 6 | Themes | 4, 5 | `KeyboardTheme`, four built-ins, `ThemeRepository`, live redraw, settings preview |
| 7 | Settings and activation app | 1, 6 | `MainActivity`, activation/layout/theme/options/about screens, IME status, language enablement |
| 8 | TV-only build, CI, docs, attribution | 3, 7 | Android TV workflow, APK artifact, README, spec rewrite, MIT NOTICE, feature doc |
| 9 | Tests and emulator smoke pass | 2–8 | Robolectric layout suite, navigation/prefs/theme/suggestion tests, documented TV smoke test, CI green |

Dependency graph:

```
1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9
```

## Further Notes

- LeanKeyboard is MIT-licensed. The vendored layout data must keep the license text and
  attribution in `NOTICE` and in the About screen.
- The app must build with `./gradlew :app:assembleDebug`; the APK is the deliverable for
  sideloading on Android TV.
- Use a TV AVD (1080p, Android TV image) or a real device for the smoke test; the IME
  cannot be fully exercised in Robolectric.
- Keep the keyboard surface free of Android focus traversal: one internal cursor is easier
  to make deterministic than per-key `View` focus and matches LeanKeyboard's behavior.
- Prefer pure Kotlin models (`KeyboardNavigator`, `SuggestionEngine`, `EditorActionResolver`)
  so most behavior is tested on the JVM and the Android classes stay thin.
- Consider a follow-up ticket for additional LeanKeyboard translations of the settings UI,
  but do not block v1 on it.
