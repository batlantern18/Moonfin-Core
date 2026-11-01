import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/main.dart';
import 'package:tv_keyboard_demo/src/theme/builtin_themes.dart';
import 'package:tv_keyboard_demo/src/theme/theme_model.dart';
import 'package:tv_keyboard_demo/src/theme/theme_registry.dart';

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

  group('ThemeRegistry', () {
    test('starts on the dark theme and lists all built-in themes', () {
      final ThemeRegistry registry = ThemeRegistry();

      expect(registry.current, BuiltInThemes.dark);
      expect(registry.themes, BuiltInThemes.all);
      expect(registry.themeById('dark'), BuiltInThemes.dark);
      expect(registry.themeById('nope'), isNull);
    });

    test('switching notifies listeners and changes current', () {
      final ThemeRegistry registry = ThemeRegistry();
      expect(registry.current.id, BuiltInThemes.dark.id);

      var notifications = 0;
      final void Function() listener = () => notifications++;
      registry.addListener(listener);

      registry.switchTheme(BuiltInThemes.light.id);
      registry.switchTheme(BuiltInThemes.light.id);

      expect(registry.current, BuiltInThemes.light);
      expect(notifications, 1);
    });

    test('switching to an unregistered id throws', () {
      final ThemeRegistry registry = ThemeRegistry();

      expect(
        () => registry.switchTheme('does-not-exist'),
        throwsArgumentError,
      );
      expect(registry.current.id, BuiltInThemes.dark.id);
    });

    test('a new theme works with no infrastructure changes', () {
      const ThemeModel custom = ThemeModel(
        id: 'custom',
        name: 'Custom',
        brightness: Brightness.dark,
        primaryColor: Color(0xFF00E676),
        accentColor: Color(0xFFFFEA00),
        surfaceColor: Color(0xFF001F1F),
        onSurfaceColor: Color(0xFFB9F6CA),
        focusColor: Color(0xFFFFEA00),
        focusBackgroundColor: Color(0xFF00695C),
        focusForegroundColor: Color(0xFFFFFFFF),
        focusStyle: FocusStyle.solidFill,
        focusBorderRadius: 20.0,
        focusAnimationDuration: Duration(milliseconds: 90),
        focusScale: 1.15,
        focusBorderWidth: 3.0,
      );
      final ThemeRegistry registry = ThemeRegistry(
        themes: <ThemeModel>[...BuiltInThemes.all, custom],
        initialThemeId: BuiltInThemes.dark.id,
      );

      registry.switchTheme('custom');

      expect(registry.current, custom);
    });
  });

  group('ThemeScope', () {
    testWidgets('dependents observe the current theme and rebuild on switch',
        (WidgetTester tester) async {
      final ThemeRegistry registry = ThemeRegistry();
      ThemeModel? observed;

      await tester.pumpWidget(
        ThemeScope(
          registry: registry,
          child: Builder(
            builder: (BuildContext context) {
              observed = ThemeModel.of(context);
              return const SizedBox.shrink();
            },
          ),
        ),
      );
      expect(observed!.id, BuiltInThemes.dark.id);

      registry.switchTheme(BuiltInThemes.highContrast.id);
      await tester.pump();

      expect(observed!.id, BuiltInThemes.highContrast.id);
    });
  });

  group('app focus visuals follow the theme', () {
    BoxDecoration focusedTileDecoration(WidgetTester tester) {
      final AnimatedContainer container = tester.widget<AnimatedContainer>(
        find.descendant(
          of: find.byType(DemoTile),
          matching: find.byType(AnimatedContainer),
        ).first,
      );
      return container.decoration as BoxDecoration;
    }

    AnimatedScale focusedTileScale(WidgetTester tester) {
      return tester.widget<AnimatedScale>(
        find.descendant(
          of: find.byType(DemoTile),
          matching: find.byType(AnimatedScale),
        ).first,
      );
    }

    testWidgets('switching themes re-skins the focused tile everywhere',
        (WidgetTester tester) async {
      final ThemeRegistry registry = ThemeRegistry();
      await tester.pumpWidget(TvKeyboardDemoApp(registry: registry));
      await tester.pumpAndSettle();

      // Light: borderOnly ring in the accent color, fill matches surface.
      registry.switchTheme(BuiltInThemes.light.id);
      await tester.pumpAndSettle();
      BoxDecoration decoration = focusedTileDecoration(tester);
      final Border lightBorder = decoration.border as Border;
      expect(lightBorder.top.color, BuiltInThemes.light.accentColor);
      expect(lightBorder.top.width, BuiltInThemes.light.focusBorderWidth);
      expect(decoration.color, BuiltInThemes.light.surfaceColor);
      expect(
        decoration.borderRadius,
        BorderRadius.circular(BuiltInThemes.light.focusBorderRadius),
      );

      AnimatedScale animatedScale = focusedTileScale(tester);
      expect(animatedScale.scale, BuiltInThemes.light.focusScale);
      expect(animatedScale.duration, BuiltInThemes.light.focusAnimationDuration);

      // HighContrast: solid fill, thick bright ring, bigger scale, faster.
      registry.switchTheme(BuiltInThemes.highContrast.id);
      await tester.pumpAndSettle();
      decoration = focusedTileDecoration(tester);
      final Border contrastBorder = decoration.border as Border;
      expect(decoration.color, BuiltInThemes.highContrast.focusBackgroundColor);
      expect(
        contrastBorder.top.color,
        BuiltInThemes.highContrast.focusColor,
      );
      expect(
        contrastBorder.top.width,
        BuiltInThemes.highContrast.focusBorderWidth,
      );
      expect(decoration.borderRadius,
          BorderRadius.circular(BuiltInThemes.highContrast.focusBorderRadius));

      animatedScale = focusedTileScale(tester);
      expect(animatedScale.scale, BuiltInThemes.highContrast.focusScale);
      expect(
        animatedScale.duration,
        BuiltInThemes.highContrast.focusAnimationDuration,
      );

      // tvOS: translucent glass fill, hairline ring, glow.
      registry.switchTheme(BuiltInThemes.tvosStyle.id);
      await tester.pumpAndSettle();
      decoration = focusedTileDecoration(tester);
      final Border tvosBorder = decoration.border as Border;
      expect(decoration.color!.a, lessThan(1.0));
      expect(tvosBorder.top.color, BuiltInThemes.tvosStyle.focusColor);
      expect(tvosBorder.top.width, BuiltInThemes.tvosStyle.focusBorderWidth);
      expect(decoration.boxShadow, isNotEmpty);

      animatedScale = focusedTileScale(tester);
      expect(animatedScale.scale, BuiltInThemes.tvosStyle.focusScale);
      expect(
        animatedScale.duration,
        BuiltInThemes.tvosStyle.focusAnimationDuration,
      );
    });

    testWidgets('focused tile uses the focus foreground color',
        (WidgetTester tester) async {
      final ThemeRegistry registry = ThemeRegistry();
      await tester.pumpWidget(TvKeyboardDemoApp(registry: registry));
      await tester.pumpAndSettle();

      registry.switchTheme(BuiltInThemes.highContrast.id);
      await tester.pumpAndSettle();

      final Text text = tester.widget<Text>(
        find.descendant(
          of: find.byType(DemoTile),
          matching: find.text('Demo tile 1'),
        ),
      );
      expect(
        text.style?.color,
        BuiltInThemes.highContrast.focusForegroundColor,
      );
    });
  });
}
