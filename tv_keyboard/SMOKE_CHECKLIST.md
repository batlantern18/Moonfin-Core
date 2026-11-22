# Moonfin TV Keyboard — Smoke Test Checklist

Target: Android TV emulator or hardware (API 21+, leanback launcher).

Run before merging to `spec/tv-keyboard-parity`.

## Setup
- [ ] Install the debug APK: `./gradlew :app:assembleDebug` and `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- [ ] Enable Moonfin TV Keyboard in Settings › System › Language & Input › Current Keyboard
- [ ] Open any text field (e.g., YouTube search bar or browser URL field)

## A — Basic Typing
- [ ] Type a-z on QWERTY layout; characters appear correctly
- [ ] Shift types uppercase for one character (one-shot Shift)
- [ ] Caps Lock (long-press Shift) types multiple uppercase until pressed again
- [ ] Delete removes the character before the cursor
- [ ] Space inserts a space (space bar shows the active language display name)

## B — Accent / Symbol Popups
- [ ] Long-press 'a' opens accent popup mini-keyboard; D-pad moves between choices; Center/Enter commits
- [ ] Back from accent popup closes it without inserting and returns focus to keyboard
- [ ] Symbols mode (?123 key) switches layout to symbols; long-press accent keys works

## C — Edge Navigation
- [ ] Up from top keyboard row moves focus to the suggestions row (when suggestions visible)
- [ ] Right from keyboard right edge moves focus to the editor-action button
- [ ] Down from keyboard bottom row moves focus to the editor-action button
- [ ] Down from suggestions row returns focus to keyboard
- [ ] Back from suggestions or action returns focus to keyboard
- [ ] Back from the keyboard hides the IME
- [ ] Action button (Search / Done / Send / Enter) triggers the editor action

## D — Held-Key Repeat
- [ ] Hold Delete: characters delete continuously after initial delay until released
- [ ] Hold Left arrow: cursor moves left continuously until released
- [ ] Hold Right arrow: cursor moves right continuously until released
- [ ] Quick tap Delete: exactly one character deleted (no double delete on release)

## E — Clipboard Overlay
- [ ] Copy text in another app; open keyboard; tap clipboard key (KeyCodes.CLIPBOARD)
- [ ] Overlay shows recent clips list; Up/Down moves selection
- [ ] Enter/Center pastes selected clip at cursor
- [ ] Back closes overlay without pasting; focus returns to keyboard
- [ ] Empty clipboard shows "Clipboard empty" message

## F — Voice Input
- [ ] Tap voice key: app requests RECORD_AUDIO permission on first use
- [ ] Deny permission: keyboard shows "Microphone permission not granted" inline status
- [ ] Grant permission and tap voice: keyboard shows "🎤 Listening…" banner
- [ ] Speak a word: recognized text is inserted at cursor
- [ ] Network/no-match errors show readable inline error message

## G — Word Completions
- [ ] Type prefix "ap" in a text field: suggestions row shows words like "apple", "application", "apply"
- [ ] Select suggestion from row: replaces the word prefix, adds trailing space if auto-space enabled
- [ ] Email field shows domain suggestions (@gmail.com etc.)
- [ ] Clearing the field clears suggestions

## H — Appearance Options (via Settings)
- [ ] Enable Enlarge: key height and spacing visibly increase
- [ ] Enable Keep on screen: IME stays visible in multiline notes fields
- [ ] Enable Hide for physical keyboard: on-screen keyboard suppresses when hardware keyboard attached
- [ ] Disable both: keyboard behaves normally

## I — Haptic and Sound Feedback
- [ ] Haptic toggle ON: pressing keys and popup selection produces vibration
- [ ] Sound toggle ON: pressing keys plays audio click sound
- [ ] Both toggles OFF: no vibration or sound

## J — Settings Preview and Reset
- [ ] Changing theme updates the preview card in Settings immediately
- [ ] Changing style updates the preview card
- [ ] "Reset to Defaults" restores all settings to defaults; preview reflects defaults
- [ ] Settings survive app restart

## K — Language Picker
- [ ] Enable multiple languages in Settings
- [ ] Long-press language/space key opens the language picker overlay
- [ ] D-pad selects a language; Enter switches layout
- [ ] Space bar displays the active language name

## L — RTL Layouts (if Arabic or Hebrew enabled)
- [ ] Arabic layout mirrors the keyboard and picker (RTL)
- [ ] D-pad navigation direction is correct for RTL
