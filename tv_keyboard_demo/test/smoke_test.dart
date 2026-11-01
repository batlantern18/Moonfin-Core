import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/main.dart';

void main() {
  testWidgets('home screen shows the placeholder content', (tester) async {
    await tester.pumpWidget(const TvKeyboardDemoApp());
    await tester.pump();

    expect(find.text('TV Keyboard Demo'), findsOneWidget);
    expect(find.textContaining('Placeholder home screen'), findsOneWidget);
    expect(find.byType(DemoTile), findsNWidgets(9));
  });

  testWidgets('the first tile grabs autofocus on launch', (tester) async {
    await tester.pumpWidget(const TvKeyboardDemoApp());
    await tester.pump();

    expect(find.text('Focused: Demo tile 1'), findsOneWidget);
  });

  testWidgets('pressing select on a focused tile opens the demo dialog',
      (tester) async {
    await tester.pumpWidget(const TvKeyboardDemoApp());
    await tester.pump();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pumpAndSettle();

    expect(find.byType(AlertDialog), findsOneWidget);
    expect(find.text('Demo tile 1'), findsOneWidget);
  });
}
