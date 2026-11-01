import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/main.dart';

void main() {
  testWidgets('home screen shows its rows and bottom navigation', (
    tester,
  ) async {
    await tester.pumpWidget(const TvKeyboardDemoApp());
    await tester.pumpAndSettle();

    expect(find.text('Moonfin TV Keyboard Demo'), findsOneWidget);
    expect(find.text('Featured'), findsOneWidget);
    expect(find.text('Continue Watching'), findsOneWidget);
    expect(find.text('Home'), findsOneWidget);
    expect(find.text('Settings'), findsOneWidget);
  });

  testWidgets('selecting a focused home card opens its details dialog', (
    tester,
  ) async {
    await tester.pumpWidget(const TvKeyboardDemoApp());
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pumpAndSettle();

    expect(find.byType(AlertDialog), findsOneWidget);
    expect(find.text('The Grand Adventure'), findsNWidgets(2));
  });
}
