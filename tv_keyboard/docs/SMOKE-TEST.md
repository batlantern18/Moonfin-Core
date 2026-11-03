# Android TV Keyboard Smoke Test

## Prerequisites
- Android TV emulator or device (API 35, x86_64)
- Moonfin-Core debug APK installed

## Test Steps

### Activation
1. Enable the keyboard in Settings > Languages & Input > Current Keyboard.
2. Verify the keyboard appears in text fields.

### Basic Typing
- Type 'Hello World' using D-pad navigation.
- Verify all characters appear correctly.

### Shift/Caps
- Press Shift once, type 'TEST'.
- Verify uppercase letters.
- Press Shift again, type 'test'.
- Verify lowercase letters.
- Long-press Shift to lock Caps, type 'CAPS'.
- Verify locked uppercase.

### Symbols/Numbers
- Switch to symbols layout, type '!,@'.
- Switch to number layout, type '123'.

### Accents
- Long-press 'e', select 'é' from popup.
- Verify 'é' is inserted.

### Language Switching
- Cycle languages with the globe key.
- Verify space bar shows active language.

### Suggestions
- Type 'examp', verify 'example' appears in suggestions.
- Select 'example' with D-pad.

### Voice Input
- Press voice key, speak a phrase.
- Verify transcription appears (or error if unavailable).

### Clipboard
- Copy text, press clipboard key.
- Select clipboard entry, verify paste.

### Themes
- Switch to Light/Dark/High Contrast/tvOS themes.
- Verify keyboard appearance updates.

### Options
- Toggle 'Enlarge keyboard' and 'Cyclic navigation'.
- Verify visual and behavioral changes.
