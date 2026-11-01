import 'package:flutter/material.dart';

import 'glass_press_scale.dart';

/// tvOS-style focus and hover treatment: a gentle scale pop plus a hairline
/// ring and soft glow, and a grow while a pointer is down. It only uses
/// borders and BoxShadows, never blur filters, so it's safe on every tier
/// including Android TV sheen. Adapted from Moonfin-Core's glass_focus_halo
/// with the theme's primary color standing in for its accent and a static
/// flag ([appleStyleActive]) standing in for the glass capability.
///
/// Under non-glass looks the ring falls back to the primary color so themes
/// keep their identity.
class GlassFocusHalo extends StatelessWidget {
  static const double _borderWidth = 2;

  const GlassFocusHalo({
    super.key,
    required this.focused,
    required this.child,
    this.borderRadius,
    this.scale = 1.05,
    this.pressGrowth = 17.0,
    this.padding,
    this.ringOverPaints = false,
    this.ringColor,
    this.backgroundColor,
    this.duration = const Duration(milliseconds: 120),
  });

  final bool focused;
  final Widget child;
  final BorderRadius? borderRadius;

  /// Scale applied while focused. 1.0 disables the pop, for toolbar icons and
  /// anything else that shouldn't grow.
  final double scale;

  /// Logical pixels the longest side gains while a pointer is down. 0
  /// disables it, for surfaces that carry their own press response.
  final double pressGrowth;
  final EdgeInsetsGeometry? padding;

  /// Paint the ring over the child rather than around it.
  ///
  /// The ring is laid out whether or not the halo has focus, so drawn around
  /// it widens the child on every side for a state it may never reach. A
  /// surface that hugs its contents wants it painted over instead.
  final bool ringOverPaints;
  final Color? ringColor;
  final Color? backgroundColor;
  final Duration duration;

  /// Whether the tvOS white-halo language applies right now.
  ///
  /// Moonfin-Core derives this from its glass capability; the demo exposes a
  /// static the theming ticket can drive from its theme registry.
  static bool appleStyleActive = false;

  @override
  Widget build(BuildContext context) {
    final radius = borderRadius ?? BorderRadius.circular(12);
    final apple = appleStyleActive;
    final ring =
        ringColor ?? (apple ? Colors.white : Theme.of(context).primaryColor);
    final bg = backgroundColor ??
        (apple
            ? Colors.white.withValues(alpha: 0.10)
            : Theme.of(context).primaryColor.withValues(alpha: 0.18));

    final border = Border.all(
      color: focused ? ring : Colors.transparent,
      width: _borderWidth,
    );

    final content = AnimatedContainer(
      duration: duration,
      curve: Curves.easeOut,
      padding: padding ?? EdgeInsets.zero,
      foregroundDecoration: ringOverPaints
          ? BoxDecoration(borderRadius: radius, border: border)
          : null,
      decoration: BoxDecoration(
        borderRadius: radius,
        border: ringOverPaints ? null : border,
        color: focused ? bg : Colors.transparent,
        boxShadow: focused && apple
            ? const [
                BoxShadow(
                  color: Color(0x40FFFFFF),
                  blurRadius: 14,
                  spreadRadius: 1,
                ),
                BoxShadow(
                  color: Color(0x59000000),
                  blurRadius: 18,
                  offset: Offset(0, 8),
                ),
              ]
            : null,
      ),
      child: child,
    );

    final pressed = GlassPressScale(growth: pressGrowth, child: content);
    if (scale == 1.0) return pressed;
    return AnimatedScale(
      scale: focused ? scale : 1.0,
      duration: duration,
      curve: Curves.easeOutCubic,
      child: pressed,
    );
  }
}
