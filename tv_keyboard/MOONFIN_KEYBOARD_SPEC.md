# Moonfin in-app TV keyboard: recreation spec

Source build: `Moonfin_Android_v3.1.1.apk` (`org.moonfin.androidtv`, versionCode 30000165, minSdk 24, targetSdk 36).
The Android app is Flutter (`io.flutter.embedding` in `classes.dex`, Dart AOT in `lib/x86_64/libapp.so`), so this keyboard is an **in-app widget** (`CustomKeyboard` / `CustomKeyboardState`), not an IME service. The APK manifest declares no `android.view.inputmethod.InputMethod` service.

Evidence screenshots live in `docs/keyboard-analysis/`.

## 1. Why this spec exists

The native Kotlin app (`com.moonfin.tvkeyboard`) is an IME. Moonfin's keyboard is an overlay drawn by the app. The goal is to reproduce the Moonfin keyboard's look, layout, and behaviour in Kotlin, either as the IME's input view (Docked-style window) or as an in-app overlay for screens that want to keep the keyboard inside the app.

Keep both targets in mind: the geometry below is the IME-window variant, because that is the one Android actually composites over other apps.

## 2. Styles

`KeyboardStyle` has four values, persisted under the `keyboardStyle` preference. Strings in the APK:

| Value | In-app copy | Notes |
| :--- | :--- | :--- |
| `native` | "Native uses your TV's own keyboard." | Hands focus to the system IME; no Moonfin keyboard drawn. |
| `classic` | "Classic is the original Moonfin keyboard." | Dark panel, no blur. |
| `glass` | "Glass is a frosted panel with larger keys." | Blurred/frosted surface, larger keys. |
| `docked` | "Docked fills the bottom of the screen and shows what you type above the keys." | **Default in 3.1.1** (Settings shows Docked selected and this is what the screenshots show). |

Settings also exposes the style as a live preview (`LIVE PREVIEW` panel, Search tab), so a style change is visible before leaving settings.

Location: **Settings → General → Input → Keyboard Style** (a card with the four-line description above, a `Docked` value chip, and a detail panel with `Native / Classic / Glass / Docked` chips plus a live preview of the Search screen). In automation, tapping a chip focused the panel but did not persist the change, so only the default Docked style was captured at full size; treat the Classic and Glass geometry as described by the copy until verified by hand.

## 3. Anatomy (Docked, 1920x1080)

Compact panel anchored to the bottom edge and centred horizontally, about 60% of the screen width and 56% of the height on a 1920x1080 TV — a card above the bottom edge, not a bar across the screen (matching the Gboard for TV reference).

Top to bottom:

1. **Echo line** (`x = 270`, baseline `y ≈ 575`): the current field text at ~34 px semibold white, with a 2 px cyan caret. For password fields it renders bullets (`••••`), never the text.
2. **Chrome row** (`y ≈ 586`), right aligned: `OK` chip (1349), label `hold for accents` (1437), `close` chip with a back arrow (1592) and `close` label (1627).
3. **Hint strip** (`y ≈ 658`): context hints, grey ~22 px, no chips/borders: `https://` `http://` `www.` `jellyfin` `.com` `.org` `.net` `:8096` `/`. This row is address/URL specific; password fields show no hint strip.
4. **Key grid**:
   - Left rail `x ≈ 330`: mode badge (`abc` when letters are active, `123` when symbols are active), `ABC` shift (y 829), `123` symbols (y 923), keyboard glyph (y 1016) for closing/keeping the keyboard.
   - Letters: three rows, pitch 100 px, first key at `x = 470`, last at `x = 1367`, rows at `y = 735 / 829 / 923`. Key circles 64 px, label 32 px white. Each letter has a 4 px dot beneath it, the affordance for the accent long-press.
   - Row 1: `q w e r t y u i o p`
   - Row 2: `a s d f g h j k l '` (apostrophe key; shift renders `"`)
   - Row 3: `z x c v b n m , : @`
   - Right column `x = 1400 … 1647`: `Delete` (y 691…879, 247 x 188) with a backspace glyph, and `Done` (y 894…1057) in a dark teal pill with a check glyph. The action key label follows context: `Done` for address/password, `Search` with a magnifier for search fields (see the live preview).
5. **Bottom row** (`y ≈ 1016`): `space` bar (x 434…1081, 1 px outline, no fill), cursor-left `‹` (1137), cursor-right `›` (1229), clipboard glyph (1321).

### Focus treatment

The focused key is the only strongly coloured element on the panel: a 4 px cyan ring with a soft glow and a slightly teal-tinted fill (`#00A4DC` at full opacity on the ring). Focus starts on the first key, not on the top strip.

## 4. Colour tokens (sampled from screenshots)

| Token | Value | Where |
| :--- | :--- | :--- |
| Page background | `#0B0C0F` | behind/around the panel |
| Panel surface | `#17181B` | keyboard background |
| Key fill | transparent over the panel | circles read as `#1A1C20`-ish at 1x |
| Focus/accent | `#00A4DC` | focus ring, caret, primary buttons (Jellyfin blue) |
| Done surface | `#09212C` | Done pill, teal border |
| Accent chip | `#191A1D` | long-press variant chips |
| Hint text | grey ~`#8A8F94` | hint strip, labels like `space`, `hold for accents` |
| Error text | red | "Invalid username or password" |

The keyboard inherits the app's dark theme; no cream/light variant was observed.

## 5. Behaviour

### Verified on the emulator (API 34 TV, 1920x1080)

- **Typing**: tap a key commits the character; the echo line mirrors the field and keeps the caret at the end.
- **Hardware input**: `adb shell input text` inserts into the focused field and the echo line updates — the widget is a real text field, not a canvas-only writer.
- **Shift**: `ABC` is a one-shot shift; the key stays highlighted for one character (`qwert` became `QWERT` after pressing ABC) and rows render uppercase with `"` replacing `'`.
- **Symbols**: `123` swaps the three letter rows for `1 2 3 4 5 6 7 8 9 0`, `! @ # $ % ^ & * ( )`, `- _ = + : ' ? / ; :`. The mode badge flips to `abc`/`ABC` to return. One symbol page only; there is no `#+=` second page.
- **Accents**: long-press (held ~1 s) on `a` opens a horizontal strip directly over the row above: `à á â ä ã å æ` in rounded chips, base letter highlighted, first variant selected by default. Releasing inserts the currently selected variant (the base letter is inserted if nothing was moved to).
- **Context hints**: URL/address fields show `https://` `http://` `www.` `jellyfin` `.com` `.org` `.net` `:8096` `/`. Tapping a hint inserts it.
- **Password**: no hint strip, echo line shows bullets, layout otherwise identical. **Auto-shift is active in password fields** (ABC turns on when the field gains focus), which is wrong for passwords and should not be copied.
- **Action key**: `Done` in address/password contexts, `Search` with a magnifier in search contexts.
- **Delete**: deletes one character per activation; no auto-repeat was observed from a single tap.
- **Cursor keys**: `‹` / `›` move the caret inside the echo line without closing the keyboard.
- **Clipboard**: the clipboard glyph takes focus (cyan ring); the sheet itself was not confirmed.
- **Close**: the `close` chip is meant to dismiss the keyboard; a plain tap on its label did not dismiss it during testing (the label may sit outside the hit area). Dismissal was reliably achieved with Back.

### Not yet verified

- `classic` and `glass` visuals (only `docked` was captured).
- Clipboard sheet contents and paste behaviour.
- Haptics and key-click sound (both are present in the AOT build via `HapticFeedback.vibrate`; the emulator has no vibrator and was started with audio off).
- Plain-text suggestions/prediction: Moonfin has no dictionary UI in this build, and no word suggestions appeared in any field.
- Number-only and email layouts: no such fields were reachable at the login screen.
- RTL / non-Latin layouts: no evidence either way.

## 6. Design intent worth keeping

- The keyboard is a single dark slab that replaces the bottom half of the screen, not a floating card. It reads as part of the page, so the app behind it stays visible above the fold.
- Only two elements carry colour: the focused key and the primary action. Everything else is greys on greys; this makes D-pad position unmistakable from three metres away.
- The echo line is the "type here" surface for TV: the actual field may be scrolled off or dimmed behind the dialog, so the keyboard repeats the text above the keys.
- Hint chips are text-only (no pills), which keeps the row from competing with the key grid.
- Accent affordance is a 4 px dot, quiet enough not to clutter the grid yet enough to suggest the long-press.

## 7. Kotlin implementation plan (this repo)

Target: `com.moonfin.tvkeyboard`, a `InputMethodService` with a custom input view, minSdk 21.

### 7.1 Window

- `Docked` maps to a compact IME window pinned to the bottom and centred, hold on. Use ~52–60% width, wrap-content height, no full-bleed bar.
- Use `FLAG_NOT_FOCUSABLE` off (IME windows must be focusable), `softInputMode` `adjustResize` from the host.
- Draw the panel background from the theme (`#17181B` in dark), no elevation/shadow at the bottom edge.
- Keep the echo line and chrome inside the IME view so the host never has to draw a preview.

### 7.2 View structure

```
KeyboardRootView (FrameLayout)
├── EchoBar         — text + caret, reads state, never edits
├── ChromeRow       — OK chip, hint label, close chip
├── CueRow          — contextual hint chips (URL, email, password)
└── KeyboardGrid
    ├── ModeRail    — abc/ABC badge, shift, symbols, keyboard glyph
    ├── Keys 3x10   — QWERTY / symbols, dot affordance on letters
    ├── ActionColumn— Delete (top), action key (bottom)
    └── BottomRow   — space, cursor left, cursor right, clipboard
```

Keep `KeyboardView` as a single custom `View` if the existing focus engine already lives there; add the rail, action column, and bottom row as hit regions inside its existing `hitTest`/`draw` pipeline rather than new child views, so the D-pad traversal stays in one place.

### 7.3 State

- `mode: Letters | Symbols` (one symbol page only), `shift: Off | OneShot | Locked` with the existing 500 ms timer reused for the accent hold.
- `echoText` comes from `InputConnection.getExtractedText` on every commit/delete/cursor move; passwords render as `•` for `InputType.TYPE_TEXT_VARIATION_PASSWORD`.
- Hint providers by `EditorInfo.inputType` / `imeOptions`:
  - URL/URI (`TYPE_TEXT_VARIATION_URI`): `https://`, `http://`, `www.`, `jellyfin`, `.com`, `.org`, `.net`, `:8096`, `/`
  - password: no cues
  - search: no cues, action key label `Search`
- Action key label from `imeOptions`: `Search` for `IME_ACTION_SEARCH`, `Done` for `IME_ACTION_DONE`, `Next`/`Go` as the existing Gboard-style mapping already does.

### 7.4 Accents

Port the observed table for a first pass: `a → à á â ä ã å æ`, plus vowels and `n`/`c`/`y` sets. Long-press opens a horizontal strip directly above the source key, base letter selected, D-pad Left/Right moves selection while held, release commits. If the hold ends without movement, commit the base letter.

### 7.5 Password safeguard

Do **not** copy Moonfin's auto-shift on password fields: force `shift = Off` and `isPassword = true`, hide cues, hide the accent dots, and disable the clipboard key.

### 7.6 Reuse from the existing app

- D-pad traversal, edge wrap, repeat timers, accent popup plumbing, clipboard overlay, and action-key dispatch already exist in `KeyboardView`/`KeyboardRootView`; this is mostly re-skinning plus the rail/action/bottom rows and the echo/cue strips.
- Add a `KeyboardStyle` preference (default `Docked`) so `Classic` (no blur, smaller keys) and `Glass` (blur + larger keys) can follow later; `Native` should delegate to the system IME.

## 8. Acceptance checklist

- [ ] Full-width panel, 56% height, no rounding, no shadow.
- [ ] Echo line mirrors the field, bullets for passwords, caret visible.
- [ ] URL cues in URI fields only; hidden for passwords.
- [ ] 3 x 10 letter grid at 100 px pitch and 64 px keys on a 1080p TV profile.
- [ ] Left rail modes, `Delete` column, `Done`/`Search` action pill.
- [ ] Bottom space bar + cursor keys + clipboard.
- [ ] One-shot shift, one symbol page, held accent strip over the row above.
- [ ] Focus ring `#00A4DC`, 4 px, with glow; focus starts on the first key.
- [ ] Password fields never auto-shift and never show text in echo.
- [ ] Back dismisses the keyboard and returns focus to the host field.

## 9. Evidence

Screenshots in `docs/keyboard-analysis/`:

| File | Shows |
| :--- | :--- |
| `m4_ime.png` | Docked keyboard, empty URL field, cue strip, focus on `q` |
| `m5_typed.png` | Typed text in the echo line, Delete/Done column, space and cursor keys |
| `m6_shift.png` | One-shot `ABC` active, uppercase rows, `"` instead of `'` |
| `m7_symbols.png` | `123` page: digits row, symbol rows, mode badge back to `abc` |
| `m10_accent_hold.png` | Held `a`: accent strip `à á â ä ã å æ` over the row above |
| `m12_clipboard.png` | Clipboard key focused (cyan ring), no sheet |
| `t2_pwfield.png` | Password field: bullets in the echo line, no cue strip |
| `t3_pwtyped.png` | Password field after typing: auto-shift on, error state red text behind |
| `x2_down.png`, `z1_kbstyle.png` | Settings → Keyboard Style with description, value chip and live preview |

