import 'package:flutter/widgets.dart';
import '../theme/theme_model.dart';

enum DemoAnimationSpeed { slow, normal, fast }

enum DemoDataMode { full, boundary, empty }

/// Shared controls used by the demo screens so settings take effect in place.
class DemoSettings extends ChangeNotifier {
  static final DemoSettings instance = DemoSettings();

  bool rowGlideEnabled = true;
  bool rtlEnabled = false;
  DemoAnimationSpeed animationSpeed = DemoAnimationSpeed.normal;
  DemoDataMode dataMode = DemoDataMode.full;
  FocusStyle? focusStyleOverride;

  double get animationFactor => switch (animationSpeed) {
    DemoAnimationSpeed.slow => 1.8,
    DemoAnimationSpeed.normal => 1,
    DemoAnimationSpeed.fast => 0.55,
  };

  void setRowGlideEnabled(bool value) {
    if (rowGlideEnabled == value) return;
    rowGlideEnabled = value;
    notifyListeners();
  }

  void setRtlEnabled(bool value) {
    if (rtlEnabled == value) return;
    rtlEnabled = value;
    notifyListeners();
  }

  void setAnimationSpeed(DemoAnimationSpeed value) {
    if (animationSpeed == value) return;
    animationSpeed = value;
    notifyListeners();
  }

  void setDataMode(DemoDataMode value) {
    if (dataMode == value) return;
    dataMode = value;
    notifyListeners();
  }

  void setFocusStyleOverride(FocusStyle? value) {
    if (focusStyleOverride == value) return;
    focusStyleOverride = value;
    notifyListeners();
  }
}

class DemoSettingsScope extends InheritedNotifier<DemoSettings> {
  const DemoSettingsScope({
    super.key,
    required DemoSettings settings,
    required super.child,
  }) : super(notifier: settings);

  static DemoSettings of(BuildContext context) {
    final scope = context
        .dependOnInheritedWidgetOfExactType<DemoSettingsScope>();
    assert(scope != null, 'DemoSettingsScope is missing above this widget.');
    return scope!.notifier!;
  }
}
