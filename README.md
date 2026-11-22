# Moonfin TV Keyboard

A standalone, native Kotlin Android TV Keyboard IME app designed for D-pad navigation, remote controls, and Leanback devices.

## Features

- **LeanKeyboard Layout Parity**: Clean, fully rendered on-screen keyboard optimized for Android TV.
- **D-pad & Remote Navigation**: Full edge-aware focus transitions between keyboard, suggestions row, and action buttons.
- **Accent & Symbol Popups**: Long-press key mini-keyboard popups with D-pad navigation.
- **Held-Key Repeat**: Continuous key repeat for Delete and cursor keys.
- **Clipboard Overlay**: Dedicated D-pad navigable clipboard history panel.
- **Voice Input**: Integrated speech recognition with inline status banners.
- **Word Completions**: Bundled offline prefix completions and domain suggestions.
- **TV Settings App**: Live theme & style preview card, preference toggles, and safe one-touch factory reset.
- **Haptic & Sound Feedback**: Tactile confirmation on key press and popup selection.

## Building & Testing

Requires JDK 21 and Android SDK (API 35):

```bash
cd tv_keyboard
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```
