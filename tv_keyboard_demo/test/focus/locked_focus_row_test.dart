import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/hub_focus_memory.dart';
import 'package:tv_keyboard_demo/src/focus/locked_focus_row.dart';

class _Options {
  _Options({
    this.onTap,
    this.onLongPress,
    this.onVerticalNavigation,
    this.onBack,
    this.onRightEdge,
    this.items,
  });

  final void Function(int index, int item)? onTap;
  final void Function(int index, int item)? onLongPress;
  final LockedFocusVerticalNav? onVerticalNavigation;
  final VoidCallback? onBack;
  final VoidCallback? onRightEdge;
  final List<int>? items;
}

final GlobalKey<LockedFocusRowState<int>> _rowKey =
    GlobalKey<LockedFocusRowState<int>>();

Future<void> _pumpRow(
  WidgetTester tester, {
  _Options? options,
  int count = 6,
  TextDirection direction = TextDirection.ltr,
}) async {
  options ??= _Options();
  await tester.pumpWidget(
    MaterialApp(
      home: Directionality(
        textDirection: direction,
        child: Scaffold(
          body: LockedFocusRow<int>(
            key: _rowKey,
            hubKey: 'test-row',
            itemExtent: 100,
            itemSpacing: 10,
            height: 80,
            items: options.items ?? List<int>.generate(count, (i) => i),
            onTap: options.onTap,
            onLongPress: options.onLongPress,
            onVerticalNavigation: options.onVerticalNavigation,
            onRightEdge: options.onRightEdge,
            onBack: options.onBack,
            itemBuilder: (context, item, index, focused) => Text(
              'Card $index${focused ? '<' : ''}',
              textDirection: TextDirection.ltr,
            ),
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  setUp(HubFocusMemory.clearAll);
  tearDown(HubFocusMemory.clearAll);

  testWidgets('taking focus parks on index 0 and marks the card focused', (
    tester,
  ) async {
    await _pumpRow(tester);
    final row = _rowKey.currentState!;
    row.requestFocusAt(0);
    await tester.pumpAndSettle();

    expect(row.hasFocusedItem, isTrue);
    expect(find.text('Card 0<'), findsOneWidget);
    expect(find.text('Card 1<'), findsNothing);
  });

  testWidgets('right and left keys move the focused card', (tester) async {
    await _pumpRow(tester);
    final row = _rowKey.currentState!;
    row.requestFocusAt(0);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowRight);
    await tester.pumpAndSettle();
    expect(row.focusedIndex, 1);
    expect(find.text('Card 1<'), findsOneWidget);

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowLeft);
    await tester.pumpAndSettle();
    expect(row.focusedIndex, 0);
  });

  testWidgets('RTL reverses horizontal row navigation', (tester) async {
    await _pumpRow(tester, direction: TextDirection.rtl);
    final row = _rowKey.currentState!;
    row.requestFocusAt(0);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowLeft);
    await tester.pumpAndSettle();
    expect(row.focusedIndex, 1);

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowRight);
    await tester.pumpAndSettle();
    expect(row.focusedIndex, 0);
  });

  testWidgets('moving leaves the row focused and never releases it', (
    tester,
  ) async {
    await _pumpRow(tester);
    final row = _rowKey.currentState!;
    row.requestFocusAt(2);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowRight);
    await tester.pumpAndSettle();
    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowLeft);
    await tester.pumpAndSettle();

    expect(row.hasFocusedItem, isTrue);
    expect(row.focusedIndex, 2);
  });

  testWidgets('select taps the focused card exactly once', (tester) async {
    final tapped = <int>[];
    await _pumpRow(
      tester,
      options: _Options(onTap: (i, item) => tapped.add(item)),
    );
    _rowKey.currentState!.requestFocusAt(3);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump();
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(tapped, [3]);
  });

  testWidgets('holding select long-presses; the up does not tap', (
    tester,
  ) async {
    final tapped = <int>[];
    final longPressed = <int>[];
    await _pumpRow(
      tester,
      options: _Options(
        onTap: (i, item) => tapped.add(item),
        onLongPress: (i, item) => longPressed.add(item),
      ),
    );
    _rowKey.currentState!.requestFocusAt(1);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump(const Duration(milliseconds: 550));
    expect(longPressed, [1]);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(tapped, isEmpty);
  });

  testWidgets('the context menu key long-presses immediately', (tester) async {
    final longPressed = <int>[];
    await _pumpRow(
      tester,
      options: _Options(onLongPress: (i, item) => longPressed.add(item)),
    );
    _rowKey.currentState!.requestFocusAt(4);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.contextMenu);
    await tester.pump();
    expect(longPressed, [4]);
  });

  testWidgets('the last card consumes right and fires the right edge', (
    tester,
  ) async {
    var edge = 0;
    await _pumpRow(tester, options: _Options(onRightEdge: () => edge++));
    final row = _rowKey.currentState!;
    row.requestFocusAt(5);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowRight);
    await tester.pumpAndSettle();
    expect(row.focusedIndex, 5);
    expect(edge, 1);
  });

  testWidgets('the first card consumes left when no edge callback is given', (
    tester,
  ) async {
    await _pumpRow(tester);
    final row = _rowKey.currentState!;
    row.requestFocusAt(0);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowLeft);
    await tester.pumpAndSettle();
    expect(row.focusedIndex, 0);
  });

  testWidgets('up/down navigation delegates to the screen', (tester) async {
    final calls = <bool>[];
    await _pumpRow(
      tester,
      options: _Options(
        onVerticalNavigation: (up) {
          calls.add(up);
          return up;
        },
      ),
    );
    _rowKey.currentState!.requestFocusAt(0);
    await tester.pumpAndSettle();

    // The screen claims the up key: handled, focus stays on the row.
    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowUp);
    await tester.pumpAndSettle();
    expect(calls, [true]);
    expect(_rowKey.currentState!.hasFocusedItem, isTrue);

    // The screen declines the down key: the event is handed back to
    // traversal instead of stopping on the row.
    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowDown);
    await tester.pumpAndSettle();
    expect(calls, [true, false]);
  });

  testWidgets('back is handled through the row', (tester) async {
    var backs = 0;
    await _pumpRow(tester, options: _Options(onBack: () => backs++));
    _rowKey.currentState!.requestFocusAt(0);
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.escape);
    await tester.pump();
    await tester.sendKeyUpEvent(LogicalKeyboardKey.escape);
    await tester.pump();
    expect(backs, 1);
  });

  testWidgets('focus comes back to the hub slot it left', (tester) async {
    await _pumpRow(tester);
    final row = _rowKey.currentState!;
    row.requestFocusAt(4);
    await tester.pumpAndSettle();

    // Park focus elsewhere, then return to the row.
    final decoy = Focus(debugLabel: 'decoy', child: const SizedBox());
    await tester.pumpWidget(MaterialApp(home: Scaffold(body: decoy)));
    await tester.pumpAndSettle();

    expect(HubFocusMemory.peek('test-row'), 4);
    await _pumpRow(tester);
    _rowKey.currentState!.requestFocusFromMemory();
    await tester.pumpAndSettle();
    expect(_rowKey.currentState!.focusedIndex, 4);
    expect(find.text('Card 4<'), findsOneWidget);
  });

  testWidgets('an empty row does not break keys or focus', (tester) async {
    await _pumpRow(tester, options: _Options(items: const []));
    final row = _rowKey.currentState!;
    row.requestFocusFromMemory();
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowRight);
    await tester.pumpAndSettle();
    expect(row.focusedIndex, 0);
  });
}
