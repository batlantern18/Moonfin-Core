import 'package:flutter/material.dart';

import 'glass_focus_halo.dart';

/// Visual constants for the TV focus treatment.
///
/// Moonfin-Core reads these from its ThemeRegistry; the demo resolves them
/// from [Theme.of] instead, and the theming ticket later feeds its ThemeModel
/// through the same surface.
class FocusTheme {
  FocusTheme._();

  static const double scale = 1.02;
  static const double defaultBorderRadius = 8.0;
  static const Duration animationDuration = Duration(milliseconds: 150);
  static const double borderWidth = 2.0;

  static List<BoxShadow> focusGlow(Color color) => <BoxShadow>[
    BoxShadow(color: color.withValues(alpha: 0.35), blurRadius: 12,
        spreadRadius: 1),
  ];

  static BorderSide focusBorder(BuildContext context, {Color? color}) =>
      BorderSide(
        color: resolveColor(context, color),
        width: borderWidth,
      );

  static BoxDecoration focusDecoration({
    required BuildContext context,
    required bool isFocused,
    double radius = defaultBorderRadius,
    Color? color,
    Color? backgroundColor,
    bool suppressFocusGlow = false,
  }) {
    return BoxDecoration(
      color: backgroundColor,
      borderRadius: BorderRadius.circular(radius),
      border: isFocused
          ? Border.fromBorderSide(focusBorder(context, color: color))
          : null,
      boxShadow: isFocused && !suppressFocusGlow
          ? focusGlow(resolveColor(context, color))
          : null,
      // The border is always reserved once a color is set, so content
      // doesn't shift by a pixel when focus arrives.
    );
  }

  /// A focused row filled solid in the theme's on-surface color, with the
  /// focus border and glow on top.
  static BoxDecoration solidFocusDecoration({
    required BuildContext context,
    required bool active,
    double radius = 10,
  }) {
    return BoxDecoration(
      color: active
          ? Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.9)
          : null,
      borderRadius: BorderRadius.circular(radius),
      border: Border.fromBorderSide(
        active
            ? focusBorder(context).copyWith(width: 1)
            : const BorderSide(color: Colors.transparent, width: 1),
      ),
      boxShadow: active ? focusGlow(resolveColor(context, null)) : null,
    );
  }

  /// Text and icon color on a [solidFocusDecoration] fill.
  static Color solidFocusForeground(BuildContext context) {
    final onSurface = Theme.of(context).colorScheme.onSurface;
    return ThemeData.estimateBrightnessForColor(onSurface) == Brightness.light
        ? Colors.black.withValues(alpha: 0.87)
        : Colors.white;
  }

  static Color resolveColor(BuildContext context, Color? override) =>
      override ??
      (GlassFocusHalo.appleStyleActive
          ? Colors.white
          : Theme.of(context).colorScheme.primary);
}
