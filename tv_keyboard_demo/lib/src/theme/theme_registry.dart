import 'package:flutter/widgets.dart';

import 'builtin_themes.dart';
import 'theme_model.dart';

/// Holds the app's current theme and makes it switchable and observable.
///
/// The registry is a [ChangeNotifier] (Listenable) plus an inherited widget
/// provider: listeners are notified on every theme switch, and widgets under
/// a [ThemeScope] rebuild whenever the current theme changes.
class ThemeRegistry extends ChangeNotifier {
  /// Creates a registry.
  ///
  /// [themes] defaults to [BuiltInThemes.all]; [initialThemeId] selects the
  /// starting theme (defaults to [BuiltInThemes.dark], the TV-typical look).
  ThemeRegistry({List<ThemeModel>? themes, String? initialThemeId})
      : _themes = List<ThemeModel>.unmodifiable(themes ?? BuiltInThemes.all),
        _current = _initialThemeOf(
          themes: themes,
          initialThemeId: initialThemeId,
        );

  /// The process-wide registry used by the app.
  static final ThemeRegistry instance = ThemeRegistry();

  final List<ThemeModel> _themes;
  ThemeModel _current;

  /// The currently active theme.
  ThemeModel get current => _current;

  /// All themes available for switching.
  List<ThemeModel> get themes => List<ThemeModel>.unmodifiable(_themes);

  /// Looks up a registered theme by [id], or null if none matches.
  ThemeModel? themeById(String id) {
    for (final ThemeModel theme in _themes) {
      if (theme.id == id) {
        return theme;
      }
    }
    return null;
  }

  /// Switches the current theme to the one registered under [id].
  ///
  /// Throws [ArgumentError] when no theme with that id is registered, and
  /// does nothing (no notification) when that theme is already current.
  void switchTheme(String id) {
    final ThemeModel theme = themeById(id) ??
        (throw ArgumentError.value(id, 'id', 'Not a registered theme id'));
    if (theme == _current) {
      return;
    }
    _current = theme;
    notifyListeners();
  }

  static ThemeModel _initialThemeOf({
    List<ThemeModel>? themes,
    String? initialThemeId,
  }) {
    final List<ThemeModel> all = themes ?? BuiltInThemes.all;
    if (initialThemeId != null) {
      for (final ThemeModel theme in all) {
        if (theme.id == initialThemeId) {
          return theme;
        }
      }
    }
    for (final ThemeModel theme in all) {
      if (theme == BuiltInThemes.dark) {
        return theme;
      }
    }
    return all.first;
  }
}

/// Inherited widget exposing the registry's current [ThemeModel].
///
/// Wrap the app root with a [ThemeScope] and read the theme anywhere with
/// [ThemeModel.of]; dependents rebuild whenever the registry notifies.
class ThemeScope extends InheritedNotifier<ThemeRegistry> {
  /// Creates a scope bound to [registry].
  const ThemeScope({super.key, required ThemeRegistry registry, required super.child})
      : super(notifier: registry);

  /// Returns the registry's current theme, registering the calling widget
  /// as a dependent so it rebuilds on theme switches.
  static ThemeModel of(BuildContext context) {
    final ThemeScope? scope =
        context.dependOnInheritedWidgetOfExactType<ThemeScope>();
    assert(
      scope != null,
      'ThemeModel.of() called outside a ThemeScope; wrap the app root.',
    );
    return scope!.notifier.current;
  }
}
