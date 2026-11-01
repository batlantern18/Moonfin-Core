import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/locked_focus_row.dart';
import 'package:tv_keyboard_demo/src/focus/row_focus_coordinator.dart';

void main() {
  final GlobalKey<LockedFocusRowState<int>> _rowKey =
      GlobalKey<LockedFocusRowState<int>>();
  late FocusNode a;
  late FocusNode b;
  late FocusNode c;

  setUp(() {
    a = FocusNode(debugLabel: 'stop-a');
    b = FocusNode(debugLabel: 'stop-b');
    c = FocusNode(debugLabel: 'stop-c');
  });

  tearDown(() {
    // Detached nodes never hit the framework loop, safe to just drop.
    a.dispose();
    b.dispose();
    c.dispose();
  });

  Future<void> pump(
    WidgetTester tester, {
    bool bFocusable = true,
    bool listRow = false,
  }) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Column(
            children: <Widget>[
              Focus(focusNode: a, child: const SizedBox(height: 10)),
              Focus(
                focusNode: b,
                canRequestFocus: bFocusable,
                child: Container(
                  key: const ValueKey('mid'),
                  height: 10,
                ),
              ),
              Focus(focusNode: c, child: const SizedBox(height: 10)),
              if (listRow)
                LockedFocusRow<int>(
                  key: _rowKey,
                  hubKey: 'coord',
                  itemExtent: 50,
                  height: 40,
                  items: const [1, 2],
                  builder: (context, item, index, focused) => SizedBox.expand(),
                ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  testWidgets('moveVertical skips an unfocusable stop in the middle',
      (tester) async {
    await pump(tester, bFocusable: false);
    final coordinator = RowFocusCoordinator()
      ..entries = [
        RowFocusEntry.node(a),
        RowFocusEntry.node(b),
        RowFocusEntry.node(c),
      ];

    expect(coordinator.moveVertical(fromIndex: 0, isUp: false), isTrue);
    expect(FocusManager.instance.primaryFocus, c,
        reason: 'b cannot take focus, so c does');
  });

  testWidgets('moveVertical walks upward to a focusable stop',
      (tester) async {
    await pump(tester, bFocusable: false);
    final coordinator = RowFocusCoordinator()
      ..entries = [
        RowFocusEntry.node(a),
        RowFocusEntry.node(b),
      ];

    expect(coordinator.moveVertical(fromIndex: 1, isUp: true), isTrue);
    expect(FocusManager.instance.primaryFocus, a);
  });

  testWidgets('focusFirst lands on the first focusable entry', (tester) async {
    await pump(tester, bFocusable: false);
    final coordinator = RowFocusCoordinator()
      ..entries = [
        RowFocusEntry.node(b),
        RowFocusEntry.node(a),
      ];

    expect(coordinator.focusFirst(), isTrue);
    expect(FocusManager.instance.primaryFocus, a);

    // An empty coordinator fails politely.
    expect(RowFocusCoordinator().focusFirst(), isFalse);
  });

  testWidgets('row entries focus the remembered card', (tester) async {
    await pump(tester, listRow: true);
    final coordinator = RowFocusCoordinator()
      ..entries = [RowFocusEntry.row(_rowKey)];

    expect(coordinator.focusFirst(), isTrue);
    expect(
      FocusManager.instance.primaryFocus!.debugLabel,
      'LockedFocusRow:coord',
      reason: 'the row owns its node while a card holds the focus position',
    );
  });
}
