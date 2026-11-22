# Gboard on Android TV feature specification

This reference defines the architecture, layout, input handling, and system integrations required to recreate [Gboard for Android TV](https://developer.android.com/training/tv/get-started/onscreen-keyboard).

## System architecture and IME lifecycle

Gboard on Android TV operates as a standard [Android Input Method Editor (IME)](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method) packaged under `com.google.android.inputmethod.latin`. You implement the keyboard service by extending `InputMethodService`.

- **Communication channel**: The service communicates with the focused host application through `BaseInputConnection`.
- **Text operations**: The service uses `BaseInputConnection` to read text around the cursor, compose candidate text, commit final text, and dispatch raw `KeyEvent` objects.
- **Host attributes**: On input start, the service inspects `EditorInfo` to read `inputType`, `imeOptions`, and `privateImeOptions`.
- **Audio permission isolation**: The IME owns the `RECORD_AUDIO` permission so host applications do not request microphone access for [speech-to-text](https://developer.android.com/training/tv/get-started/onscreen-keyboard).
- **Latency budget**: Input response and cold-start rendering must stay under [200 milliseconds](https://websiddu.com/work/g-board-for-tv).

## Window placement and layout geometry

Gboard on Android TV renders as a compact, floating overlay rather than a full-width bar, as documented in the [Android TV on-screen keyboard guide](https://developer.android.com/training/tv/get-started/onscreen-keyboard). This compact footprint reduces eye travel across large television screens.

- **Default alignment**: The keyboard window aligns to the horizontal center of the bottom screen edge when no alignment option is present.
- **Custom horizontal alignment**: Host applications control placement through the `horizontalAlignment` key inside `android:privateImeOptions`.
- **Supported alignment values**: The `horizontalAlignment` attribute accepts `center`, `left`, and `right`.
- **Host layout insets**: Applications set `android:windowSoftInputMode="adjustResize"` so the host UI adjusts padding when the keyboard appears, as described in the [Android edge-to-edge setup guide](https://developer.android.com/develop/ui/compose/system/setup-e2e).

Example host XML configuration for right alignment:

```xml
<EditText
	android:layout_width="match_parent"
	android:layout_height="wrap_content"
	android:imeOptions="actionNext"
	android:privateImeOptions="horizontalAlignment=right" />
```

## Input types and keyboard modes

The IME selects its active layout and top bar mode from `EditorInfo.inputType`. Gboard for Android TV supports four primary [input types](https://developer.android.com/training/tv/get-started/onscreen-keyboard).

| Input type | Main grid | Top bar content | Voice input | Post-selection behavior |
| :--- | :--- | :--- | :--- | :--- |
| Text | QWERTY | Word suggestions or voice plate | Enabled | Remains focused on keyboard |
| Email | QWERTY | Domain suggestions and saved emails | Enabled | Moves focus to primary action key |
| Password | QWERTY | Numbers and symbols row | Disabled | Remains focused on keyboard |
| Numeric | Number pad | None | Disabled | Remains focused on keyboard |

### Primary action key mapping

The primary action key on the right edge of the keyboard adapts to `EditorInfo.imeOptions`.

- **Search**: Displays a magnifying glass icon for `IME_ACTION_SEARCH`.
- **Next**: Displays a right arrow or **Next** label for `IME_ACTION_NEXT`.
- **Done**: Displays a checkmark or **Done** label for `IME_ACTION_DONE`.
- **Go**: Displays a **Go** label for `IME_ACTION_GO`.

## Keyboard anatomy and key grid

The floating window contains a top utility bar and a tactile key grid, as detailed in the [Gboard for TV design specification](https://websiddu.com/work/g-board-for-tv).

### Top utility bar

The top bar occupies a single horizontal row directly above the main key grid.

- **Word suggestion strip**: Displays three or more predictive text candidates during standard text entry.
- **Voice plate**: Replaces the suggestion strip with a waveform and live transcription banner when voice typing is active.
- **Password number and symbol strip**: Displays digits `0` through `9` and common symbols during password entry so users do not switch layouts.
- **Email domain strip**: Displays common domain suffixes and previously used email addresses during email entry.

### Main QWERTY grid

The main grid uses a [standard QWERTY arrangement](https://websiddu.com/work/g-board-for-tv) rather than an alphabetical grid. Each key renders as a distinct, rounded button with a high-contrast focus ring.

- **Alphabet keys**: Three rows of letter keys in standard QWERTY order.
- **Shift key**: Toggles uppercase for the next character, or locks uppercase on a double press or long press.
- **Backspace key**: Deletes one character to the left of the cursor.
- **Symbols key**: Switches the main grid to punctuation and special characters (`?123`).
- **Language switcher key**: Displays a globe icon when two or more languages are enabled.
- **Spacebar**: Inserts a space character.
- **Voice input key**: Displays a microphone icon, or a Google Assistant icon on supported devices.
- **Primary action key**: Submits the field or advances focus to the next form control.

## D-pad navigation and key interactions

Users operate the keyboard with a directional pad (D-pad) and center select button on a television remote.

### Directional focus and wrap-around

- **Grid traversal**: Pressing **Up**, **Down**, **Left**, or **Right** moves focus one key in that direction.
- **Edge wrap-around**: Pressing **Left** on the leftmost key wraps focus to the rightmost key in the same row, and pressing **Right** on the rightmost key wraps focus to the leftmost key, as noted by [Android TV keyboard users](https://www.reddit.com/r/AndroidTV/comments/j1gg0x/new_keyboard_layout_just_showed_up_seems_like/).
- **Vertical repeat**: Holding a directional arrow repeats focus movement across keys.

### Long-press interactions

Holding the D-pad center button on specific keys triggers secondary actions, as documented in the [Gboard for TV case study](https://websiddu.com/work/g-board-for-tv).

- **Letter keys**: Opens a popup menu with accented characters, alternate symbols, or capitalized forms.
- **Backspace key**: Triggers continuous character deletion until you release the button.
- **Spacebar**: Opens the system input method picker to switch to another installed keyboard.
- **Language switcher key**: Opens the Gboard language settings dialog.

### Back key dismissal

Pressing the remote **Back** button (`KeyEvent.KEYCODE_BACK`) or **Escape** key dismisses the on-screen keyboard and clears text field focus without closing the underlying host screen, as required in the [Android TV navigation guidelines](https://developer.android.com/agents/skills/tv/leanback-to-compose-tv-migration/skill).

## Voice typing and speech-to-text

Gboard provides integrated [speech-to-text](https://developer.android.com/training/tv/get-started/onscreen-keyboard) across supported languages without requiring host application permissions.

1. The user selects the **Microphone** key, or presses and holds a dedicated voice button.
2. The top bar transitions to the listening state and displays a clear active microphone indicator.
3. As the user speaks, the top bar enters the speaking state and streams recognized text into the focused field.
4. When dictation finishes, focus moves automatically to the keyboard primary action key so the user can submit immediately, as described in the [Gboard for TV design case study](https://websiddu.com/work/g-board-for-tv).

For security, Gboard disables voice typing whenever the focused field specifies a password `inputType`.

## Predictive text and email suggestions

- **Word prediction**: During standard text entry, the IME calls `setComposingText` as characters arrive and updates the top suggestion strip. Selecting a suggestion calls `commitText` and appends a trailing space.
- **Email completion**: During email entry, the IME suggests common domains and previously entered email addresses. Selecting an email suggestion commits the full address and shifts D-pad focus to the primary action key.

## Language and internationalization support

Gboard manages multiple languages through [Android IME subtypes](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method). Each subtype defines a locale and keyboard layout.

- **Single press on Language switcher**: Cycles to the next enabled language subtype.
- **Long press on Language switcher**: Opens the language configuration dialog.

The [Android TV on-screen keyboard documentation](https://developer.android.com/training/tv/get-started/onscreen-keyboard) lists the following supported languages and regional variants:

| Region or group | Supported languages |
| :--- | :--- |
| English | English (US), English (Australia), English (Canada), English (UK) |
| Western Europe | Dutch (Belgium), Dutch (Nederlands), French, French (Belgium), French (Canada), French (Switzerland), German, German (Austria), German (Belgium), German (Switzerland), Irish, Italian, Italian (Switzerland), Portuguese, Spanish (Mexico), Spanish (Spain) |
| Northern Europe | Danish, Estonian, Finnish, Lithuanian, Norwegian (Bokmål), Swedish |
| Central and Eastern Europe | Albanian, Bulgarian, Croatian, Czech, Greek, Hungarian, Macedonian, Polish, Romanian, Russian, Serbian, Slovakian, Slovenian, Ukrainian |
| Middle East and South Asia | Arabic, Bengali, Farsi, Hebrew, Hindi, Hindi (transliteration), Punjabi, Turkish, Urdu |
| East and Southeast Asia | Burmese, Chinese (Hong Kong), Chinese (Simplified), Chinese (Traditional), Filipino, Indonesian, Japanese (Japan), Korean, Malay, Thai, Vietnamese |
| Africa and Central Asia | Afrikaans, O'zbek |

## Companion remote keyboard integration

Gboard integrates with the **Android TV Remote Service** so users can type from a paired smartphone using the Google TV app or Google Home app, as described in the [Google TV remote support guide](https://support.google.com/chromecast/answer/11221499?hl=en-GB).

- **Trigger**: Focusing a text field on the television notifies the paired mobile client.
- **Transport**: Keystrokes and committed strings typed on the phone pass over the remote service bridge directly into the active `InputConnection`.
- **Requirement**: Gboard must be selected as the active system keyboard for the remote keyboard bridge to function, as noted in the [Google TV support documentation](https://support.google.com/googletv/thread/377486628/where-is-keyboard-option?hl=en).

## System settings and privacy controls

Users configure Gboard through the television system menu at **Settings** > **System** > **Keyboard** (or **Settings** > **Device Preferences** > **Keyboard** on earlier Android TV releases), as listed in [Google TV device documentation](https://www.t-mobile.com/support/devices/tvision-hub-googletv-software-how-tos).

### Gboard settings menu

- **Current version**: Displays the installed build number.
- **Languages**: Contains the **Use system languages** toggle and individual language switches.
- **Share usage statistics**: Controls anonymous telemetry reporting.
- **Legal and compliance**: Links to **Terms of service**, **Privacy policy**, and **Open source licenses**.

### Manage keyboards menu

- **Gboard**: Enables or disables the primary software keyboard.
- **Google voice typing**: Toggles system speech-to-text availability.
- **Virtual Remote Keyboard**: Toggles companion mobile keyboard input.

## Accessibility

Gboard integrates with **TalkBack** on Android TV. When TalkBack is active, moving D-pad focus across the grid announces the focused key label, capitalization state, or secondary popup character.

## Reference sources

- [Android Developers: On-screen keyboard for Android TV](https://developer.android.com/training/tv/get-started/onscreen-keyboard)
- [Android Developers: Create an input method](https://developer.android.com/develop/ui/views/touch-and-input/creating-input-method)
- [Android Developers: Migrate Android TV apps from Leanback to Jetpack Compose for TV](https://developer.android.com/agents/skills/tv/leanback-to-compose-tv-migration/skill)
- [Android Developers: Set up Edge-to-edge](https://developer.android.com/develop/ui/compose/system/setup-e2e)
- [Siddhartha Gudipati: GBoard for TV Design Case Study](https://websiddu.com/work/g-board-for-tv)
- [Google Help: Use your phone as the remote for your Chromecast or Google TV](https://support.google.com/chromecast/answer/11221499?hl=en-GB)
- [Google TV Community: Keyboard option and Gboard requirement](https://support.google.com/googletv/thread/377486628/where-is-keyboard-option?hl=en)
- [T-Mobile Support: TVision HUB with Google TV keyboard settings](https://www.t-mobile.com/support/devices/tvision-hub-googletv-software-how-tos)
- [Reddit r/AndroidTV: New keyboard layout wrap-around behavior](https://www.reddit.com/r/AndroidTV/comments/j1gg0x/new_keyboard_layout_just_showed_up_seems_like/)
