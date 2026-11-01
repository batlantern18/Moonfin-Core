import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/main.dart';
import 'package:tv_keyboard_demo/src/theme/theme_registry.dart';
import 'package:tv_keyboard_demo/src/theme/builtin_themes.dart';
import 'package:tv_keyboard_demo/src/focus/platform_detection.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../helpers/dpad_test_utils.dart';

void main() {
  setUp(TvPlatformDetection.reset);
  tearDown(TvPlatformDetection.reset);

  testWidgets('D-pad moves between rows, opens a card, and backs out', (
    tester,
  ) async {
    final registry = ThemeRegistry();
    await tester.pumpWidget(TvKeyboardDemoApp(registry: registry));
    await tester.pumpAndSettle();

    await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
    expect(find.text('Focused: Episode 4'), findsOneWidget);

    await pressDpadKey(tester, LogicalKeyboardKey.select);
    expect(find.byType(AlertDialog), findsOneWidget);
    expect(find.text('Episode 4'), findsNWidgets(2));

    await pressDpadBack(tester);
    expect(find.byType(AlertDialog), findsNothing);
    expect(find.text('Focused: Episode 4'), findsOneWidget);
  });

  testWidgets('list context key and held select open navigable menus', (
    tester,
  ) async {
    TvPlatformDetection.setTvMode(true);
    await tester.pumpWidget(TvKeyboardDemoApp(registry: ThemeRegistry()));
    await tester.pumpAndSettle();

    await tester.tap(find.widgetWithText(OutlinedButton, 'Lists'));
    await tester.pumpAndSettle();
    expect(find.text('List navigation'), findsOneWidget);

    await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
    expect(
      focusedTextLabel(),
      'A Very Long Feature Title That Exercises Text Wrapping',
    );
    await pressDpadKey(tester, LogicalKeyboardKey.arrowUp);

    await tester.sendKeyDownEvent(LogicalKeyboardKey.contextMenu);
    await tester.pumpAndSettle();
    expect(find.text('Context menu actions'), findsOneWidget);
    await pressDpadBack(tester);

    await holdDpadSelect(tester);
    expect(find.text('Context menu actions'), findsOneWidget);

    await pressDpadBack(tester);
    expect(find.text('Context menu actions'), findsNothing);
    await pressDpadBack(tester);
  });

  testWidgets('grid moves in both axes and supports select and long-press', (
    tester,
  ) async {
    TvPlatformDetection.setTvMode(true);
    await tester.pumpWidget(TvKeyboardDemoApp(registry: ThemeRegistry()));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(OutlinedButton, 'Grid'));
    await tester.pumpAndSettle();

    await pressDpadKey(tester, LogicalKeyboardKey.arrowRight);
    expect(
      focusedTextLabel(),
      'A Very Long Feature Title That Exercises Text Wrapping',
    );
    await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
    expect(focusedTextLabel(), 'After the Rain');

    await holdDpadSelect(tester);
    expect(find.text('Grid context menu'), findsOneWidget);
    await pressDpadBack(tester);
    expect(find.text('Grid context menu'), findsNothing);

    await pressDpadKey(tester, LogicalKeyboardKey.select);
    expect(find.byType(AlertDialog), findsOneWidget);
    await pressDpadBack(tester);
    expect(find.byType(AlertDialog), findsNothing);
  });

  testWidgets(
    'D-pad opens a long-press menu, backs out, then switches theme in settings',
    (tester) async {
      tester.view.physicalSize = const Size(800, 600);
      tester.view.devicePixelRatio = 1;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      TvPlatformDetection.setTvMode(true);
      SharedPreferences.setMockInitialValues({});
      final registry = ThemeRegistry();
      await tester.pumpWidget(TvKeyboardDemoApp(registry: registry));
      await tester.pumpAndSettle();

      await pressDpadKey(tester, LogicalKeyboardKey.arrowUp);
      await holdDpadSelect(tester);
      expect(find.text('Home row context menu'), findsOneWidget);
      await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
      expect(focusedTextLabel(), 'Close menu');
      await pressDpadKey(tester, LogicalKeyboardKey.arrowUp);
      expect(focusedTextLabel(), 'Open details');
      await pressDpadBack(tester);
      expect(find.text('Home row context menu'), findsNothing);

      for (var i = 0; i < 3; i++) {
        await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
      }
      for (var i = 0; i < 3; i++) {
        await pressDpadKey(tester, LogicalKeyboardKey.arrowRight);
      }
      await pressDpadKey(tester, LogicalKeyboardKey.arrowRight);
      expect(focusedTextLabel(), 'Settings');
      await pressDpadKey(tester, LogicalKeyboardKey.select);
      expect(find.text('Settings'), findsOneWidget);

      var focused = focusedTextLabel();
      for (var i = 0; i < 32 && focused != 'High Contrast'; i++) {
        await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
        focused = focusedTextLabel();
      }
      expect(focused, 'High Contrast');
      await pressDpadKey(tester, LogicalKeyboardKey.select);
      expect(registry.current, BuiltInThemes.highContrast);

      final preferences = await SharedPreferences.getInstance();
      expect(preferences.getString('themeId'), BuiltInThemes.highContrast.id);

      focused = focusedTextLabel();
      for (var i = 0; i < 16 && focused != 'About'; i++) {
        await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
        focused = focusedTextLabel();
      }
      expect(focused, 'About');
      await pressDpadKey(tester, LogicalKeyboardKey.select);
      expect(find.text('Moonfin TV Keyboard Demo'), findsOneWidget);
      await pressDpadBack(tester);

      focused = focusedTextLabel();
      for (var i = 0; i < 24 && focused != 'Dialog examples'; i++) {
        await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
        focused = focusedTextLabel();
      }
      expect(focused, 'Dialog examples');
      await pressDpadKey(tester, LogicalKeyboardKey.select);
      expect(find.text('Dialogs'), findsOneWidget);
      await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
      await pressDpadKey(tester, LogicalKeyboardKey.arrowDown);
      expect(focusedTextLabel(), 'Show confirmation dialog');
      await pressDpadKey(tester, LogicalKeyboardKey.select);
      expect(find.text('Confirm action'), findsOneWidget);
      await pressBackKey(tester);
      expect(find.text('Confirm action'), findsNothing);
      expect(find.text('Dialogs'), findsOneWidget);
    },
  );
}
