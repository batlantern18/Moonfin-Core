import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

/// Sends a complete D-pad press and lets resulting focus/route changes settle.
Future<void> pressDpadKey(
  WidgetTester tester,
  LogicalKeyboardKey key, {
  Duration settle = const Duration(milliseconds: 250),
}) async {
  await tester.sendKeyEvent(key);
  await tester.pumpAndSettle(settle);
}

/// Holds select long enough to trigger remote context-menu behavior.
Future<void> holdDpadSelect(
  WidgetTester tester, {
  Duration duration = const Duration(milliseconds: 550),
}) async {
  await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
  await tester.pump(duration);
  await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
  await tester.pumpAndSettle();
}

/// Uses the platform back action and waits for the previous route to resume.
Future<void> pressDpadBack(WidgetTester tester) async {
  await tester.binding.handlePopRoute();
  await tester.pumpAndSettle();
}
