/// Platform detection for the standalone TV keyboard demo.
///
/// The demo app only ships to Android TV, but the focus widgets borrow a
/// couple of TV/non-TV branches from Caller-Core's own behavior. A static
/// flag keeps that distinction testable on a headless runner: production
/// builds run with the compile-time constant, tests flip [setTvMode].
///
/// There deliberately are no Moonfin-Core imports here.
library;

class TvPlatformDetection {
  TvPlatformDetection._();

  /// Compile-time TV switch: pass
  /// `--dart-define=TV_KEYBOARD_TV_MODE=true` on the build command.
  static const bool isTvBuild = bool.fromEnvironment('TV_KEYBOARD_TV_MODE');

  static bool _forcedTv = false;

  /// Overrides runtime detection. Used in tests, and the demo app sets it at
  /// startup because for this app the TV layout is not an option.
  static void setTvMode(bool value) => _forcedTv = value;

  static void reset() => _forcedTv = false;

  static bool get isTV => isTvBuild || _forcedTv;
}
