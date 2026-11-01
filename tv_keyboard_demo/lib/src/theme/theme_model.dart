import 'package:flutter/material.dart';

/// How a focused widget is visually emphasized.
enum FocusStyle {
  /// Only a colored ring is drawn around the focused widget.
  borderOnly,

  /// The focused widget's background is filled with the theme's focus
  /// background color, with the focus ring drawn on top.
  solidFill,

  /// A translucent frosted fill with a hairline focus ring, optionally
  /// accompanied by a glow. tvOS-style.
  glass,
}

/// Data-only description of a TV keyboard theme.
///
/// A theme is defined entirely by this class (and the [FocusStyle] enum):
/// adding a new theme to the app only requires a new [ThemeModel] entry in
/// [pkg:tv_keyboard_demo/src/theme/builtin_themes.dart] — no infrastructure
/// changes elsewhere.
@immutable
class ThemeModel {
  /// Creates a theme configuration.
  const ThemeModel({
    required this.id,
    required this.name,
    required this.brightness,
    required this.primaryColor,
    required this.accentColor,
    required this.surfaceColor,
    required this.onSurfaceColor,
    required this.focusColor,
    required this.focusBackgroundColor,
    required this.focusForegroundColor,
    this.focusStyle = FocusStyle.borderOnly,
    this.focusBorderRadius = 12.0,
    this.focusAnimationDuration = const Duration(milliseconds: 150),
    this.focusScale = 1.08,
    this.focusBorderWidth = 2.0,
    this.focusGlowColor,
  });

  /// Stable unique identifier used by the theme registry and persistence.
  final String id;

  /// Human-readable theme name, shown in the theme picker.
  final String name;

  /// Light or dark, used to derive perceptual defaults for Material widgets.
  final Brightness brightness;

  /// Primary brand color.
  final Color primaryColor;

  /// Accent color (focus rings default to this on the standard themes).
  final Color accentColor;

  /// Background color of screens.
  final Color surfaceColor;

  /// Default text/icon color on top of [surfaceColor].
  final Color onSurfaceColor;

  /// Color of the focus ring/border.
  final Color focusColor;

  /// Background color when focus is drawn as a solid fill.
  final Color focusBackgroundColor;

  /// Text/icon color drawn on top of a solid-fill focus background.
  final Color focusForegroundColor;

  /// How focused widgets are emphasized.
  final FocusStyle focusStyle;

  /// Corner radius of focus decorations, in logical pixels.
  final double focusBorderRadius;

  /// Duration of focus enter/leave animations.
  final Duration focusAnimationDuration;

  /// Scale factor applied to a focused widget (1.0 means no scale).
  final double focusScale;

  /// Stroke width of the focus ring, in logical pixels.
  ///
  /// Allows themes to describe ring thickness: hairline rings (tvOS-style)
  /// and thick rings (HighContrast) are both expressed with this field.
  final double focusBorderWidth;

  /// Glow color drawn around focused widgets, or null for no glow.
  final Color? focusGlowColor;

  /// Color used for text/icons on the surface while focused.
  Color resolveForegroundColor({required bool focused}) {
    return focused ? focusForegroundColor : onSurfaceColor;
  }

  /// Decoration for a widget in its current focus state, built from this
  /// theme's focus configuration.
  BoxDecoration focusDecoration({required bool focused}) {
    if (!focused) {
      return BoxDecoration(
        color: Color.lerp(surfaceColor, onSurfaceColor, 0.08)!,
        borderRadius: BorderRadius.circular(focusBorderRadius),
      );
    }

    final BorderRadius radius = BorderRadius.circular(focusBorderRadius);
    switch (focusStyle) {
      case FocusStyle.borderOnly:
        return BoxDecoration(
          color: surfaceColor,
          borderRadius: radius,
          border: Border.fromBorderSide(
            BorderSide(color: focusColor, width: focusBorderWidth),
          ),
        );
      case FocusStyle.solidFill:
        return BoxDecoration(
          color: focusBackgroundColor,
          borderRadius: radius,
          border: Border.fromBorderSide(
            BorderSide(color: focusColor, width: focusBorderWidth),
          ),
        );
      case FocusStyle.glass:
        return BoxDecoration(
          color: focusBackgroundColor.withValues(alpha: 0.2),
          borderRadius: radius,
          border: Border.fromBorderSide(
            BorderSide(color: focusColor, width: focusBorderWidth),
          ),
          boxShadow: focusGlowColor == null
              ? null
              : <BoxShadow>[
                  BoxShadow(
                    color: focusGlowColor!,
                    blurRadius: 24.0,
                    spreadRadius: 1.0,
                  ),
                ],
        );
    }
  }

  /// Material theme data built from this theme, so the surrounding app
  /// (surfaces, text, Material widgets) matches the focus styling.
  ThemeData toMaterialTheme() {
    final bool isDark = brightness == Brightness.dark;
    final Color onPrimary = isDark ? Colors.black : Colors.white;
    final Color onAccent = isDark ? Colors.black : Colors.white;

    return ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme(
        brightness: brightness,
        primary: primaryColor,
        onPrimary: onPrimary,
        secondary: accentColor,
        onSecondary: onAccent,
        error: const Color(0xFFCF6679),
        onError: Colors.black,
        surface: surfaceColor,
        onSurface: onSurfaceColor,
        onSurfaceVariant: Color.lerp(onSurfaceColor, surfaceColor, 0.30)!,
        outline: Color.lerp(onSurfaceColor, surfaceColor, 0.40)!,
        surfaceContainerHighest: Color.lerp(surfaceColor, onSurfaceColor, 0.12)!,
      ),
      scaffoldBackgroundColor: surfaceColor,
    );
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) {
      return true;
    }
    return other is ThemeModel &&
        other.id == id &&
        other.name == name &&
        other.brightness == brightness &&
        other.primaryColor == primaryColor &&
        other.accentColor == accentColor &&
        other.surfaceColor == surfaceColor &&
        other.onSurfaceColor == onSurfaceColor &&
        other.focusColor == focusColor &&
        other.focusBackgroundColor == focusBackgroundColor &&
        other.focusForegroundColor == focusForegroundColor &&
        other.focusStyle == focusStyle &&
        other.focusBorderRadius == focusBorderRadius &&
        other.focusAnimationDuration == focusAnimationDuration &&
        other.focusScale == focusScale &&
        other.focusBorderWidth == focusBorderWidth &&
        other.focusGlowColor == focusGlowColor;
  }

  @override
  int get hashCode => Object.hash(
        id,
        name,
        brightness,
        primaryColor,
        accentColor,
        surfaceColor,
        onSurfaceColor,
        focusColor,
        focusBackgroundColor,
        focusForegroundColor,
        focusStyle,
        focusBorderRadius,
        focusAnimationDuration,
        focusScale,
        focusBorderWidth,
        focusGlowColor,
      );
}
