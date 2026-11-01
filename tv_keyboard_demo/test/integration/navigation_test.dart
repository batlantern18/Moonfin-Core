import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/main.dart';
import 'package:tv_keyboard_demo/src/theme/theme_registry.dart';
import 'package:tv_keyboard_demo/src/focus/platform_detection.dart';
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
}
