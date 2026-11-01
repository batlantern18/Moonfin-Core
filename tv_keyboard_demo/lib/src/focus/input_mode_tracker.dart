import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'platform_detection.dart';

enum InputMode { keyboard, pointer }

/// Root tracker for keyboard/D-pad versus pointer input.
///
/// Replicated from Moonfin-Core's `lib/util/focus/input_mode_tracker.dart`
/// with `TvPlatformDetection` standing in for its platform gate.
///
/// Any key press makes the UI keyboard-driven; a pointer event makes it
/// mouse/touch driven. Focus visuals only show for keyboard mode, and the
/// focus highlight strategy follows the mode. On a TV device the mode is
/// pinned to keyboard: a remote *is* keyboard input, so focus visuals can
/// never vanish for it.
class InputModeTracker extends StatefulWidget {
  final Widget child;
  const InputModeTracker({super.key, required this.child});

  static InputMode of(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<_InputModeProvider>()?.mode ??
      _fallbackMode();

  static bool showFocusVisuals(BuildContext context, bool isFocused) =>
      isFocused && of(context) == InputMode.keyboard;

  /// Whether a pointer is the input being used right now.
  ///
  /// Deliberately *not* suppressed on TV the way [of] is. A mouse attached to
  /// a TV box -- screen mirroring, or a USB mouse -- really is being used, and
  /// a widget that draws hover state has to know. [of] stays pinned to
  /// keyboard there so focus visuals can never vanish for a remote user.
  ///
  /// Kept in its own inherited widget, so a pointer arriving or leaving
  /// rebuilds only those widgets and not every caller of [showFocusVisuals].
  static bool pointerIsLive(BuildContext context) =>
      context
          .dependOnInheritedWidgetOfExactType<_PointerLiveProvider>()
          ?.pointerLive ??
      false;

  static InputMode _fallbackMode() =>
      TvPlatformDetection.isTV ? InputMode.keyboard : InputMode.pointer;

  static Offset? _lastPointerDownPosition;

  /// Where the pointer last went down, which is where a menu the pointer asked
  /// for belongs. Null until a pointer has been used, so a remote or a touch
  /// gets the centered menu it expects.
  static Offset? get lastPointerDownPosition => _lastPointerDownPosition;

  static _InputModeTrackerState? _instance;

  /// Whether focus visuals remain in keyboard mode while a pointer is used.
  static bool get focusModeForced => _instance?._forceKeyboardMode ?? false;

  /// Forces traditional focus visuals independently of the latest input.
  static void setFocusModeForced(bool value) =>
      _instance?._setFocusModeForced(value);

  @override
  State<InputModeTracker> createState() => _InputModeTrackerState();
}

class _InputModeTrackerState extends State<InputModeTracker> {
  late InputMode _mode;
  bool _pointerLive = false;
  bool _forceKeyboardMode = false;

  @override
  void initState() {
    super.initState();
    _mode = InputModeTracker._fallbackMode();
    InputModeTracker._instance = this;
    _applyHighlightStrategy(_mode);
    HardwareKeyboard.instance.addHandler(_onHardwareKey);
  }

  @override
  void dispose() {
    HardwareKeyboard.instance.removeHandler(_onHardwareKey);
    if (InputModeTracker._instance == this) {
      InputModeTracker._instance = null;
    }
    super.dispose();
  }

  bool _onHardwareKey(KeyEvent event) {
    if (event is KeyDownEvent || event is KeyRepeatEvent) {
      _setPointerLive(false);
      _setMode(InputMode.keyboard);
    }
    return false;
  }

  void _onPointer(PointerEvent event) {
    _setPointerLive(true);
    _setMode(InputMode.pointer);
  }

  void _setPointerLive(bool live) {
    if (_pointerLive == live) return;
    setState(() => _pointerLive = live);
  }

  void _onPointerDown(PointerDownEvent event) {
    InputModeTracker._lastPointerDownPosition = event.position;
    _onPointer(event);
  }

  void _setMode(InputMode mode) {
    if (TvPlatformDetection.isTV && mode == InputMode.pointer) return;
    if (_mode == mode) return;
    setState(() => _mode = mode);
    _applyHighlightStrategy(mode);
  }

  void _setFocusModeForced(bool value) {
    if (_forceKeyboardMode == value) return;
    setState(() => _forceKeyboardMode = value);
    _applyHighlightStrategy(value ? InputMode.keyboard : _mode);
  }

  void _applyHighlightStrategy(InputMode mode) {
    final effectiveMode = _forceKeyboardMode ? InputMode.keyboard : mode;
    FocusManager.instance.highlightStrategy =
        effectiveMode == InputMode.keyboard
        ? FocusHighlightStrategy.alwaysTraditional
        : FocusHighlightStrategy.automatic;
  }

  @override
  Widget build(BuildContext context) {
    return Listener(
      behavior: HitTestBehavior.translucent,
      onPointerDown: _onPointerDown,
      onPointerHover: _onPointer,
      onPointerSignal: _onPointer,
      child: _InputModeProvider(
        mode: _forceKeyboardMode ? InputMode.keyboard : _mode,
        child: _PointerLiveProvider(
          pointerLive: _pointerLive,
          child: widget.child,
        ),
      ),
    );
  }
}

/// Inherited widget descendant widgets read as [InputModeTracker.of] —
/// the InputModeProvider named in the spec.
class _InputModeProvider extends InheritedWidget {
  final InputMode mode;
  const _InputModeProvider({required this.mode, required super.child});

  @override
  bool updateShouldNotify(covariant _InputModeProvider oldWidget) =>
      oldWidget.mode != mode;
}

class _PointerLiveProvider extends InheritedWidget {
  final bool pointerLive;
  const _PointerLiveProvider({required this.pointerLive, required super.child});

  @override
  bool updateShouldNotify(covariant _PointerLiveProvider oldWidget) =>
      oldWidget.pointerLive != pointerLive;
}
