import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/theme/builtin_themes.dart';
import 'package:tv_keyboard_demo/src/theme/theme_model.dart';

void main() {
  group('ThemeModel', () {
    test('exposes the fields described by the spec', () {
      const ThemeModel theme = ThemeModel(
        id: 'test',
        name: 'Test',
        brightness: Brightness.dark,
        primaryColor: Color(0xFF000001),
        accentColor: Color(0xFF000002),
        surfaceColor: Color(0xFF000003),
        onSurfaceColor: Color(0xFF000004),
        focusColor: Color(0xFF000005),
        focusBackgroundColor: Color(0xFF000006),
        focusForegroundColor: Color(0xFF000007),
      );

      expect(theme.id, 'test');
      expect(theme.name, 'Test');
      expect(theme.brightness, Brightness.dark);
      expect(theme.primaryColor, const Color(0xFF000001));
      expect(theme.accentColor, const Color(0xFF000002));
      expect(theme.surfaceColor, const Color(0xFF000003));
      expect(theme.onSurfaceColor, const Color(0xFF000004));
      expect(theme.focusColor, const Color(0xFF000005));
      expect(theme.focusBackgroundColor, const Color(0xFF000006));
      expect(theme.focusForegroundColor, const Color(0xFF000007));
      expect(theme.focusStyle, FocusStyle.borderOnly);
      expect(theme.focusBorderRadius, 12.0);
      expect(theme.focusAnimationDuration, const Duration(milliseconds: 150));
      expect(theme.focusScale, 1.08);
    });

    test('two models with identical fields are equal', () {
      const ThemeModel builtIn = BuiltInThemes.light;
      const ThemeModel literal = ThemeModel(
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

      expect(literal, builtIn);
      expect(literal.hashCode, builtIn.hashCode);
    });

    test('models that differ in one field are not equal', () {
      const ThemeModel base = BuiltInThemes.light;
      const ThemeModel differentScale = ThemeModel(
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
        focusScale: 1.2,
        focusBorderWidth: 2.0,
      );

      expect(differentScale, isNot(equals(base)));
      expect(differentScale.hashCode, isNot(base.hashCode));
    });
  });

  group('BuiltInThemes', () {
    test('ships the four described themes with unique ids and names', () {
      final Set<String> ids = BuiltInThemes.all.map((t) => t.id).toSet();
      final Set<String> names = BuiltInThemes.all.map((t) => t.name).toSet();

      expect(BuiltInThemes.all, hasLength(4));
      expect(ids, hasLength(4));
      expect(names, hasLength(4));
      expect(
        ids,
        containsAll(<String>['light', 'dark', 'highContrast', 'tvosStyle']),
      );
    });

    test('looks themes up by id', () {
      expect(BuiltInThemes.byId('highContrast'), BuiltInThemes.highContrast);
      expect(BuiltInThemes.byId('nope'), isNull);
    });

    test('standard themes use accent-colored borderOnly focus rings', () {
      for (final ThemeModel standard in <ThemeModel>[
        BuiltInThemes.light,
        BuiltInThemes.dark,
      ]) {
        expect(standard.focusColor, standard.accentColor);
        expect(standard.focusStyle, FocusStyle.borderOnly);
      }
      expect(BuiltInThemes.light.brightness, Brightness.light);
      expect(BuiltInThemes.dark.brightness, Brightness.dark);
    });

    test('highContrast uses a thick solid-fill focus', () {
      expect(BuiltInThemes.highContrast.focusStyle, FocusStyle.solidFill);
      expect(
        BuiltInThemes.highContrast.focusBorderWidth,
        greaterThan(BuiltInThemes.dark.focusBorderWidth),
      );
    });

    test('tvOS style uses hairline glass focus with a glow', () {
      expect(BuiltInThemes.tvosStyle.focusStyle, FocusStyle.glass);
      expect(BuiltInThemes.tvosStyle.focusColor, Colors.white);
      expect(
        BuiltInThemes.tvosStyle.focusBorderWidth,
        lessThan(BuiltInThemes.dark.focusBorderWidth),
      );
      expect(BuiltInThemes.tvosStyle.focusGlowColor, isNotNull);
    });

    test('themes map to matching Material color schemes', () {
      expect(
        BuiltInThemes.dark.toMaterialTheme().colorScheme.primary,
        BuiltInThemes.dark.primaryColor,
      );
      expect(
        BuiltInThemes.light.toMaterialTheme().colorScheme.surface,
        BuiltInThemes.light.surfaceColor,
      );
    });
  });
}
