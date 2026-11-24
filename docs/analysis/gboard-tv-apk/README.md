# Gboard TV APK — reverse-engineering artifacts

This folder holds the locally re-verified artifacts from inspecting
`/home/prana/gboardtv.apk`, the shipped Gboard for Android TV build used as the
product reference for Moonfin. Nothing here executes the APK.

## Source and reproduction

| Fact | Value |
| --- | --- |
| Artifact | `/home/prana/gboardtv.apk` |
| SHA-256 | `4a9f9804686492d2a716301f210e54d1a799199540fd8b2b0a89bbfbbc536f2e` |
| Package | `com.google.android.inputmethod.latin` |
| Version | `17.3.11.902587967-tv_release-x86_64` (`175783352`) |
| SDK | min 29, target 36 |
| Decompiler | JADX headless MCP 0.7.1 (via REA) |
| Resource decoder | apktool 3.0.3 |

The original analysis used REA with its JADX provider for classes, methods and
decompiled bodies, and REA's apktool bridge for resources. REA is no longer
connected in the session that produced this folder, so the numbers below were
**re-verified locally** with a fresh apktool decode and a standard-library DEX
parser (`verify_gboard_apk.py`). The saved REA evidence bundle is included so
the decompiled bodies remain inspectable.

```bash
python3 verify_gboard_apk.py /home/prana/gboardtv.apk .
apktool d -f --no-src -o /tmp/gboardtv-res /home/prana/gboardtv.apk
```

## Re-verification results

| Earlier claim | Local re-verification | Verdict |
| --- | --- | --- |
| APK SHA-256 `4a9f98…536f2e` | identical | **Match** |
| 41,411 raw DEX string_ids | 41,411 (`classes.dex` 1,999; `classes2.dex` 37,009; `classes3.dex` 2,403) | **Match** |
| 38,984 unique strings | 38,984 | **Match** |
| 36,441 printable strings | 36,090 | **Corrected** — the earlier count split embedded newlines when reading a line-based file; the direct parse is authoritative |
| 1,988 default-locale string resources | 1,988 `<string>` entries | **Match** |
| 92 locales | 95 locale-like `values-*` dirs, plus 18 non-locale qualifiers (`values-sw600dp`, `values-v35`, …) | **Scope difference** — REA counted resource-table locales; apktool emits qualifier dirs too |
| 8,422 top-level classes (JADX) | 8,766 DEX `class_defs`, of which 592 contain `$` (nested/anonymous) | **Scope difference** — JADX's `top_level` view is not DEX `class_defs`; equality is not expected |

All five input-handling classes and all eleven input-handling literals
(reported by REA) were found directly in the DEX tables. The APK digest is
unchanged, so these artifacts describe the same bytes REA analysed.

## Artifacts

- [`verify_gboard_apk.py`](verify_gboard_apk.py) — reproducible DEX class/string verifier, no third-party dependencies.
- [`dex-strings.jsonl`](dex-strings.jsonl) — 38,984 unique DEX string literals, JSON-escaped one per line (lossless for embedded newlines).
- [`dex-strings-printable.txt`](dex-strings-printable.txt) — 36,090 printable ASCII literals for browsing.
- [`decoded-strings.xml`](decoded-strings.xml) — apktool-decoded default string resources (1,988 entries).
- [`decoded-manifest.xml`](decoded-manifest.xml) — apktool-decoded manifest.
- [`rea-evidence-bundle.json`](rea-evidence-bundle.json) — 64 REA Evidence records, including the decompiled method bodies cited below.

## Functions

The build is R8-obfuscated: most of the 8,422 JADX top-level classes are named
`defpackage.a`, `defpackage.aa`, … and no `mapping.txt` ships with the APK.
TV and keyboard classes kept readable names. The input-relevant inventory:

| Class | Notable methods |
| --- | --- |
| `com.android.inputmethod.latin.LatinIME` | `onKeyDown`, `onKeyUp`, `onUpdateSelection`, `onBindInput`, `onDisplayCompletions`, `onShowInputRequested`, `requestHideSelf` |
| `com.google.android.apps.inputmethod.tv.LatinTVIme` | `c(dsw)`, `d(EditorInfo,ete)`, `e(EditorInfo,ijq)`, `cB(dye,boolean)`, `i()`, `p(int)` |
| `com.google.android.apps.inputmethod.libs.tv.keyboard.handler.TVMotionEventHandler` | `q(dsw)`, `E(dsw,bdo)`, `p(int,dsw)`, `v(erh)`, `g(MotionEvent)`, `onClick`, `onLongClick`, `onFocusChange`, `C(EditorInfo)` |
| `com.google.android.apps.inputmethod.tv.keyboard.handler.TvOnScreenRemoteEventHandler` | `q(dsw)`, `y(esd,erh)`, `z(dsw)`, `C(EditorInfo)`, `E(dsw,bdo)` |
| `com.google.android.apps.inputmethod.libs.tv.keyboard.TVKeyboard` | `g(dsw)`, `q(dsw)`, `f(List,dye,boolean)`, `h(SoftKeyboardView,etj)`, `l(int)` |

Evidence IDs in the bundle: package/manifest `ev_7fe9e1dbfe79f060b2c6c14f42f9d2e02040e670cf3aba5a87ecfe519dfce133`;
class inventory `ev_636945573fa4bca185eac0ce0337932b24016388d5040a7a5674db5b01bfb374`;
`LatinIME` `ev_cbd8da0969c63c595b475dc3262b93485a11c64357572150bf571931c5dbbcb4`;
`LatinTVIme` `ev_b78de3b8e02a0cc95012381b5c757c1e3f6e133c278ae2f1a75df9495529ac69`;
`TVMotionEventHandler` `ev_2798056bed371dc57f3f69ad2f212075c0fed3fccf414eddcd42364523a6a65c`;
`TvOnScreenRemoteEventHandler` `ev_7597065b707f3c9ed7eb818da645bb8c03dc6babb56ac5492585ce97c132897d`;
resources `ev_ddcdfffced204765afdfdaa104d741ed37c276579feba430c6339ef1d9a02a40`.

## Strings

Representative decoded resources: `ime_name` = "Gboard";
`content_desc_dpad_up/down/left/right/center` = "Dpad up/down/left/right/center";
`keyboard_row` = "keyboard row %1$d"; `enter_key_content_desc` = "Enter";
`onscreen_remote_ime_service_name` = "onscreen_software_remote";
`onscreen_remote_not_available_for_package_toast` = "The on-screen remote is not supported in this app.";
`direct_to_dictation_disabled_toast` = "Seamless voice typing is not available in this input field".

## How user input is handled

Static decompilation only — no execution or runtime dispatch was observed.
Source line positions are unknown; obfuscated `dsw`, `esd`, `erh`, `cir` types
are event wrappers inferred from call sites.

1. **Hardware keys — `LatinIME.onKeyDown/onKeyUp`.** `onKeyDown` clears a pending
   `hfw` state, then intercepts key code **212** on API 34+: if `au()` permits it,
   it logs `"OnScreen Remote invoked"` and calls `requestShowSelf(0)`; otherwise
   it shows `onscreen_remote_not_available_for_package_toast` and logs
   `"OnScreen Remote not available for package. Showing a Toast."`. Other keys
   fall through to `super`. `onKeyUp` only clears the pending flag.
   (`ev_b7546dd7d1e82746e03ea83aae3ffa381c1781e202c4bfdc0dc2ccaeda86e7d3`,
   `ev_67569a80bb88a5cbf9bcbfbbf0c25ce2743df0558d4da516bdafa2a808eb551b`.)

2. **TV IME state — `LatinTVIme`.** `d(EditorInfo,ete)` is the start-input hook:
   for a blocklisted field it fills `this.z.b` with entries from a per-app table;
   `c(dsw)` clears `this.z.a` on non-special keys; `i()` resets that state and
   drops the list when input ends. (`ev_da65e7c6ad93091d0385d814785a0e390a56880edf0a53671be61380fc2a5a0a`.)

3. **On-screen remote presses — `TVMotionEventHandler`.** `onClick(View)` calls
   `v(erh.a)` and `v(erh.i)`; `onLongClick` calls `v(erh.a)` and sets `p = true`.
   `v(erh)` wraps the event into a `dsw` and calls `q(dsw)`. `q` ignores events
   from the handler's own token, handles internal codes `-10004`, `-10002`,
   `-10041`, `-10128`, reports `cis.DPAD_BUTTON_PRESSED` telemetry, delegates
   to `E(dsw,U)` → `bdo.a(dsw)`, and records press timing for long-press
   detection. `p(int,dsw)` ("moveFocus") accepts key codes **17, 33, 66, 130**,
   logs `"Invalid keycode: %d"` otherwise, and posts a synthetic event delayed
   **200 ms** when focus is elsewhere.
   (`ev_a13e7d874793aac9a956a7b7269e60e367da1e433f4a429ca604a3f47b8a370f`,
   `ev_94da230391a1e01b085f2f643961b6c5a8f0012e28d63c6383bce38e8c564f91`,
   `ev_a944a7a75fe9ad8aee43853af6bc9ae9c1fa66e74cd9a5e610d856ac1665cadc`,
   `ev_f61a9ae64052426541930dd0c90381cbad1552ba58b92fde1622284f39097d38`,
   `ev_f4e27782a229b989f3058b84489ffacaaec1671786c40577730a178babf7cc3b`.)

4. **TV override — `TvOnScreenRemoteEventHandler`.** Its `q(dsw)` calls
   `super.q(dsw)` and, when the event was consumed and flag `u` is set, hides
   the remote via `gxk.e(this.z)` and `J()`. Its `y(esd,erh)` adds an
   accessibility guard: for D-pad codes **20, 21, 22, 19** (and `3`) with
   TalkBack flag `y` set, it logs `"Talkback enabled for blocklist apps, hiding keyboard"`
   and calls `A()`. (`ev_841dba556d3976935be25afa51c9a5b108f173df978d503a9183902953de0267`,
   `ev_a44bc70d381a839dc73950964562c894d7ad4f3a741494f7ad76c56426ec4db2`.)

## Limitations

- Static analysis never established runtime behaviour; reflection, JNI and
  dynamic dispatch remain unknown.
- R8 removed original names for most methods; no `mapping.txt` exists.
- Incoming-reference totals count JADX "using nodes", not individual invokes.
- The REA resource view returned 300 of 1,988 entries inline; the complete
  decoded table is in `decoded-strings.xml`.
- APK signatures were not verified.
