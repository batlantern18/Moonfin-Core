import 'package:flutter/material.dart';

import 'theme_model.dart';

/// The four built-in themes of the TV keyboard demo app.
///
/// Adding a new theme only requires defining another [ThemeModel] constant
/// and adding it to [all] — no changes anywhere else.
abstract final class BuiltInThemes {
  const BuiltInThemes._();

  /// Standard light theme with accent-colored focus rings.
  static const ThemeModel light = ThemeModel(
    id: 'light',
    name: 'Light',
    brightness: Brightness.light,
    primaryColor: Color(0xFF1565C0),
    accentColor: Color(0xFF03A9F4),
    surfaceColor: Color(0xFFFAFAFA),
    onSurfaceColor: Color(0xFF202124),
    focusColor: Color(0xFF03A9F4),
    focusBackgroundColor: Color(0xFF03A9F4),
    focusForegroundColor: Color(0xFFFFFFFF),
    focusStyle: FocusStyle.borderOnly,
    focusBorderRadius: 12.0,
    focusAnimationDuration: Duration(milliseconds: 150),
    focusScale: 1.08,
    focusBorderWidth: 2.0,
  );

  /// Standard dark theme with accent-colored focus rings.
  static const ThemeModel dark = ThemeModel(
    id: 'dark',
    name: 'Dark',
    brightness: Brightness.dark,
    primaryColor: Color(0xFF42A5F5),
    accentColor: Color(0xFF4FC3F7),
    surfaceColor: Color(0xFF121212),
    onSurfaceColor: Color(0xFFE8EAED),
    focusColor: Color(0xFF4FC3F7),
    focusBackgroundColor: Color(0xFF01579B),
    focusForegroundColor: Color(0xFFFFFFFF),
    focusStyle: FocusStyle.borderOnly,
    focusBorderRadius: 12.0,
    focusAnimationDuration: Duration(milliseconds: 150),
    focusScale: 1.08,
    focusBorderWidth: 2.0,
  );

  /// High contrast theme with thick, bright focus rings and solid-fill style.
  static const ThemeModel highContrast = ThemeModel(
    id: 'highContrast',
    name: 'High Contrast',
    brightness: Brightness.dark,
    primaryColor: Color(0xFFFFFFFF),
    accentColor: Color(0xFFFFD740),
    surfaceColor: Color(0xFF000000),
    onSurfaceColor: Color(0xFFFFFFFF),
    focusColor: Color(0xFFFFFFFF),
    focusBackgroundColor: Color(0xFFFFD740),
    focusForegroundColor: Color(0xFF000000),
    focusStyle: FocusStyle.solidFill,
    focusBorderRadius: 8.0,
    focusAnimationDuration: Duration(milliseconds: 100),
    focusScale: 1.12,
    focusBorderWidth: 4.0,
  );

  /// tvOS-inspired theme with white hairline rings, glow effects, and glass
  /// style.
  static const ThemeModel tvosStyle = ThemeModel(
    id: 'tvosStyle',
    name: 'TV OS Style',
    brightness: Brightness.dark,
    primaryColor: Color(0xFF0A84FF),
    accentColor: Color(0xFF0A84FF),
    surfaceColor: Color(0xFF121214),
    onSurfaceColor: Color(0xFFFBFBFD),
    focusColor: Color(0xFFFFFFFF),
    focusBackgroundColor: Color(0xFFFFFFFF),
    focusForegroundColor: Color(0xFFFFFFFF),
    focusStyle: FocusStyle.glass,
    focusBorderRadius: 16.0,
    focusAnimationDuration: Duration(milliseconds: 200),
    focusScale: 1.10,
    focusBorderWidth: 1.0,
    focusGlowColor: Color(0x40FFFFFF),
  );

  /// All built-in themes, in display order.
  static const List<ThemeModel> all = <ThemeModel>[
    light,
    dark,
    highContrast,
    tvosStyle,
  ];

  /// Looks up a built-in theme by [id], or returns null if none matches.
  static ThemeModel? byId(String id) {
    for (final ThemeModel theme in all) {
      if (theme.id == id) {
        return theme;
      }
    }
    return null;
  }
}
