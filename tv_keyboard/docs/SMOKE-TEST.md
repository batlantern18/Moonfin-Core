# Android TV keyboard smoke test

Manual pass on an Android TV image (1080p AVD or a real device). Robolectric cannot
exercise input-method activation, voice, or the system clipboard, so run this checklist
before a release.

## Setup

1. Build the APK: `cd tv_keyboard && ./gradlew :app:assembleDebug`.
2. Install it: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
3. Open **Moonfin Keyboard** from the TV launcher.

## Activation and status

- [ ] The launcher reports the keyboard as **Disabled / Not selected** before setup.
- [ ] **Enable** opens the system input-method settings screen.
- [ ] **Choose** opens the keyboard picker; Moonfin Keyboard appears in the list.
- [ ] Returning to the launcher refreshes the status to **Enabled / Selected**.

## Typing and navigation

- [ ] Open a text field in any app; the keyboard appears.
- [ ] The D-pad moves a clearly visible focus between keys, including diagonals.
- [ ] Center types the focused character.
- [ ] Space, backspace, and left/right cursor movement behave correctly.
- [ ] Touch or mouse taps type the tapped key.
- [ ] Back hides the keyboard; re-entering the field reopens it without losing text.

## Shift, caps, symbols, numbers

- [ ] Shift then a letter types uppercase, then returns to lowercase.
- [ ] Long-press Shift locks Caps; Shift again releases it.
- [ ] The mode key switches alphabet → symbols → numbers → alphabet.
- [ ] Every visible symbol and number key inserts its glyph.
- [ ] A numeric or phone field opens the number layout automatically.
- [ ] Moving to a different field resets shift and layout.

## Accents

- [ ] Long-press `e` opens the accent popup.
- [ ] Left/right/up/down move within the popup; center inserts the accent.
- [ ] The popup closes and focus returns to the key that opened it.
- [ ] Shift + accent inserts an uppercase accent.
- [ ] A key without accents never opens an empty popup.

## Languages, cycling, and RTL

- [ ] The world key cycles only the enabled layouts.
- [ ] The space bar shows the active language.
- [ ] Long-press the world key or space opens the language picker.
- [ ] The picker is D-pad navigable and closes with Back.
- [ ] Arabic, Hebrew, and Persian type the correct characters and navigate right-to-left.
- [ ] The selected language survives an app or device restart.
- [ ] A broken or missing layout falls back instead of crashing.

## Suggestions

- [ ] The suggestions row sits above the keys and is reachable with Up from the top row.
- [ ] Email and web-address fields offer common domain suggestions.
- [ ] Apps that provide completions show them.
- [ ] Left/right moves between chips, center inserts, and Down returns to the keyboard.
- [ ] Accepting a suggestion replaces the partial word instead of duplicating it.
- [ ] Auto-space appends a space when enabled.
- [ ] Disabling suggestions removes the row and Up never traps focus in it.

## Editor action

- [ ] The action button label follows the focused field (Search, Send, Next, Done, Go).
- [ ] Right from the rightmost key reaches the action button; Left returns.
- [ ] Center performs the field's editor action.
- [ ] A multiline field with no action shows **Enter** and inserts a newline.

## Voice input

- [ ] The voice key starts recognition and shows a listening state.
- [ ] The recognised text is inserted at the cursor; auto-space is honoured.
- [ ] Denying the microphone shows guidance and a route to settings.
- [ ] A device without a recogniser shows a clear unavailable message.
- [ ] Cancelling recognition returns to the keyboard without inserting text.
- [ ] Disabling dictation makes the key explain why it is unavailable.

## Clipboard

- [ ] The clipboard key opens an overlay of recent text clips.
- [ ] Up/down moves the selection; center pastes at the cursor.
- [ ] Back closes the overlay and returns focus to the keyboard.
- [ ] Image and URI clips are excluded.
- [ ] An empty clipboard shows a clear empty state.

## Themes and styles

- [ ] All four themes are selectable and show a preview: **Moonfin**, **Neon Pulse**,
      **Glass**, **8-bit Hero**.
- [ ] All three styles are selectable and show a preview: **Classic**, **Glass**, **Docked**.
- [ ] Changing a theme or style updates the keyboard immediately without restarting.
- [ ] Keys, focus ring, glow, icons, suggestions, action button, and popups all follow
      the active theme.
- [ ] Accent text stays readable in every theme.
- [ ] The selection survives a restart.

## Settings options

- [ ] **Keep on screen** keeps the keyboard visible while editing.
- [ ] **Enlarge keyboard** increases the key height immediately.
- [ ] **Cyclic navigation** wraps focus at the edges immediately.
- [ ] **Hide for physical keyboard** hides the IME when a hardware keyboard is connected.
- [ ] Every row and control is reachable and operable with the D-pad, with visible focus.
- [ ] Back returns to the previous screen without losing focus.

## Language enablement

- [ ] Settings lists every vendored language with its enabled state.
- [ ] Toggling a language persists and changes the world-key cycle immediately.
- [ ] The last enabled language cannot be disabled, and the refusal is explained.
- [ ] The preview shows a layout before it is enabled.
- [ ] Focus returns to the toggled row.

## About

- [ ] Version, build type, IME status, project link, and LeanKeyboard MIT attribution
      are shown.
