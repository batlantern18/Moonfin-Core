import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:tv_keyboard_demo/main.dart';
import 'package:tv_keyboard_demo/src/theme/builtin_themes.dart';
import 'package:tv_keyboard_demo/src/theme/theme_registry.dart';
import 'package:tv_keyboard_demo/src/screens/demo_settings.dart';
import 'package:tv_keyboard_demo/src/theme/theme_model.dart';

void main() {
  testWidgets('settings switch themes and open dialogs and about', (
    tester,
  ) async {
    SharedPreferences.setMockInitialValues({});
    final registry = ThemeRegistry();
    final settings = DemoSettings();
    await tester.pumpWidget(
      TvKeyboardDemoApp(registry: registry, settings: settings),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(OutlinedButton, 'Settings'));
    await tester.pumpAndSettle();
    expect(find.text('Current input mode: pointer'), findsOneWidget);

    await tester.tap(find.byType(SwitchListTile).first);
    await tester.pumpAndSettle();
    expect(find.text('Current input mode: keyboard'), findsOneWidget);

    await tester.tap(find.byType(SwitchListTile).at(1));
    await tester.pumpAndSettle();
    expect(settings.rowGlideEnabled, isFalse);

    await tester.tap(find.byType(SwitchListTile).at(2));
    await tester.pumpAndSettle();
    expect(settings.rtlEnabled, isTrue);

    await tester.scrollUntilVisible(
      find.text('Fast'),
      160,
      scrollable: find.byType(Scrollable).last,
    );
    await tester.tap(find.text('Fast'));
    await tester.pumpAndSettle();
    expect(settings.animationSpeed, DemoAnimationSpeed.fast);

    await tester.scrollUntilVisible(
      find.text('Glass'),
      160,
      scrollable: find.byType(Scrollable).last,
    );
    await tester.tap(find.text('Glass'));
    await tester.pumpAndSettle();
    expect(settings.focusStyleOverride, FocusStyle.glass);

    await tester.scrollUntilVisible(
      find.text('Single long item'),
      160,
      scrollable: find.byType(Scrollable).last,
    );
    await tester.tap(find.text('Single long item'));
    await tester.pumpAndSettle();
    expect(settings.dataMode, DemoDataMode.boundary);

    await tester.scrollUntilVisible(
      find.text('High Contrast'),
      160,
      scrollable: find.byType(Scrollable).last,
    );

    await tester.tap(find.text('High Contrast'));
    await tester.pumpAndSettle();
    expect(registry.current, BuiltInThemes.highContrast);
    final preferences = await SharedPreferences.getInstance();
    expect(preferences.getString('themeId'), BuiltInThemes.highContrast.id);

    await tester.scrollUntilVisible(
      find.text('Dialog examples'),
      160,
      scrollable: find.byType(Scrollable).last,
    );
    await tester.tap(find.text('Dialog examples'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Show confirmation dialog'));
    await tester.pumpAndSettle();
    expect(find.byType(AlertDialog), findsOneWidget);
    await tester.tap(find.text('Cancel'));
    await tester.pumpAndSettle();
    expect(find.byType(AlertDialog), findsNothing);

    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();
    await tester.scrollUntilVisible(
      find.text('About'),
      160,
      scrollable: find.byType(Scrollable).last,
    );
    await tester.tap(find.text('About'));
    await tester.pumpAndSettle();
    expect(find.text('Moonfin TV Keyboard Demo'), findsOneWidget);
  });

  testWidgets('list and grid handle empty and single-item boundary data', (
    tester,
  ) async {
    final settings = DemoSettings()..setDataMode(DemoDataMode.empty);
    await tester.pumpWidget(TvKeyboardDemoApp(settings: settings));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(OutlinedButton, 'Lists'));
    await tester.pumpAndSettle();
    expect(find.text('No list items in this demo data mode.'), findsOneWidget);
    await tester.binding.handlePopRoute();
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(OutlinedButton, 'Grid'));
    await tester.pumpAndSettle();
    expect(find.text('No grid items in this demo data mode.'), findsOneWidget);

    settings.setDataMode(DemoDataMode.boundary);
    await tester.pumpAndSettle();
    expect(find.textContaining('Very Long Grid Tile Title'), findsOneWidget);
  });

  testWidgets('empty home rows still hand D-pad focus to the next row', (
    tester,
  ) async {
    final settings = DemoSettings()..setDataMode(DemoDataMode.empty);
    await tester.pumpWidget(TvKeyboardDemoApp(settings: settings));
    await tester.pumpAndSettle();

    await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
    await tester.pumpAndSettle();
    expect(find.text('Focused: No Continue Watching items'), findsOneWidget);

    await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
    await tester.pumpAndSettle();
    expect(find.text('Focused: No Popular Now items'), findsOneWidget);
  });
}
