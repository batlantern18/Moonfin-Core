import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/dpad_list_tile.dart';
import 'package:tv_keyboard_demo/src/focus/platform_detection.dart';

Future<void> pumpListOfTiles(
  WidgetTester tester, {
  int count = 3,
  ValueChanged<int>? onTapped,
  ValueChanged<int>? onLongPressed,
  bool enabled = true,
}) async {
  final nodes = List<FocusNode>.generate(count, (_) => FocusNode());
  await tester.pumpWidget(
    MaterialApp(
      home: Scaffold(
        body: Column(
          children: <Widget>[
            for (var i = 0; i < count; i++)
              DpadListTile(
                key: ValueKey('tile-$i'),
                focusNode: nodes[i],
                enabled: enabled,
                title: Text('Item $i', textDirection: TextDirection.ltr),
                onTap: onTapped == null ? null : () => onTapped(i),
                onLongPress: onLongPressed == null
                    ? null
                    : () => onLongPressed(i),
              ),
          ],
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

DpadListTile _tile(WidgetTester tester, int index) =>
    find.byKey(ValueKey('tile-$index')).evaluate().first.widget as DpadListTile;

FocusNode _tileNode(WidgetTester tester, int index) =>
    _tile(tester, index).focusNode!;

// The passed-in focus nodes; a typedef keeps the intent readable.
typedef TileFocusNode = FocusNode;

void main() {
  setUp(() {
    // The select-through-Focus path is the one TV production uses.
    TvPlatformDetection.setTvMode(true);
  });
  tearDown(TvPlatformDetection.reset);

  testWidgets('select fires onTap once per press', (tester) async {
    final tapped = <int>[];
    await pumpListOfTiles(tester, onTapped: tapped.add);

    _tileNode(tester, 0).requestFocus();
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump();
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(tapped, [0]);
  });

  testWidgets('arrows move focus along the rows', (tester) async {
    final tapped = <int>[];
    await pumpListOfTiles(tester, onTapped: tapped.add);

    _tileNode(tester, 0).requestFocus();
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowDown);
    await tester.pumpAndSettle();
    expect(FocusManager.instance.primaryFocus, _tileNode(tester, 1));

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowUp);
    await tester.pumpAndSettle();
    expect(FocusManager.instance.primaryFocus, _tileNode(tester, 0));

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(tapped, [0]);
  });

  testWidgets('a disabled tile ignores select', (tester) async {
    final tapped = <int>[];
    await pumpListOfTiles(tester, onTapped: tapped.add, enabled: false);
    _tileNode(tester, 0).requestFocus();
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(tapped, isEmpty);
  });

  testWidgets('a short select press taps rather than opening context menu', (
    tester,
  ) async {
    // The click-through is instant on TV: long-press via the remote is
    // carried by the context-menu key, not a select hold.
    final tapped = <int>[];
    final longPressed = <int>[];
    await pumpListOfTiles(
      tester,
      onTapped: tapped.add,
      onLongPressed: longPressed.add,
    );
    _tileNode(tester, 0).requestFocus();
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump();
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(tapped, [0]);
    expect(longPressed, isEmpty);
  });

  testWidgets('holding select invokes long-press instead of tap', (
    tester,
  ) async {
    final tapped = <int>[];
    final longPressed = <int>[];
    await pumpListOfTiles(
      tester,
      onTapped: tapped.add,
      onLongPressed: longPressed.add,
    );
    _tileNode(tester, 0).requestFocus();
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump(const Duration(milliseconds: 500));
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pump();

    expect(longPressed, [0]);
    expect(tapped, isEmpty);
  });

  testWidgets('context menu key invokes the long-press action', (tester) async {
    final longPressed = <int>[];
    await pumpListOfTiles(tester, onLongPressed: longPressed.add);
    _tileNode(tester, 0).requestFocus();
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.contextMenu);
    await tester.pump();
    expect(longPressed, [0]);
  });

  testWidgets('DpadSwitchListTile toggles on select', (tester) async {
    bool value = false;
    Widget build() => MaterialApp(
      home: Scaffold(
        body: StatefulBuilder(
          builder: (context, setState) => DpadSwitchListTile(
            autofocus: true,
            title: const Text('Switch', textDirection: TextDirection.ltr),
            value: value,
            onChanged: (v) => setState(() => value = v),
          ),
        ),
      ),
    );

    await tester.pumpWidget(build());
    await tester.pumpAndSettle();
    expect(value, isFalse);

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pumpAndSettle();
    expect(value, isTrue);
  });

  testWidgets('DpadRadioGroup commits the focused row on select', (
    tester,
  ) async {
    String? picked;
    Widget build(String? group) => MaterialApp(
      home: Scaffold(
        body: DpadRadioGroup<String>(
          groupValue: group,
          onChanged: (v) => picked = v,
          child: Column(
            children: <Widget>[
              DpadRadioListTile<String>(
                key: const ValueKey('radio-a'),
                value: 'a',
                autofocus: true,
                title: const Text('Alpha', textDirection: TextDirection.ltr),
              ),
              DpadRadioListTile<String>(
                key: const ValueKey('radio-b'),
                value: 'b',
                title: const Text('Beta', textDirection: TextDirection.ltr),
              ),
            ],
          ),
        ),
      ),
    );

    await tester.pumpWidget(build(null));
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pumpAndSettle();
    expect(picked, 'a');

    // 'a' now selected and shows the check trail.
    await tester.pumpWidget(build('a'));
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.check), findsOneWidget);
  });

  testWidgets('away from TV the family keeps native Material widgets', (
    tester,
  ) async {
    TvPlatformDetection.setTvMode(false);
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: DpadRadioGroup<String>(
            groupValue: null,
            onChanged: (_) {},
            child: const Column(
              children: <Widget>[
                DpadListTile(
                  key: ValueKey('plain-tile'),
                  title: Text('Plain', textDirection: TextDirection.ltr),
                  onTap: null,
                ),
                DpadRadioListTile<String>(
                  value: 'a',
                  title: Text('Alpha', textDirection: TextDirection.ltr),
                ),
                DpadSwitchListTile(
                  key: ValueKey('plain-switch'),
                  value: false,
                  title: Text('Switch', textDirection: TextDirection.ltr),
                  onChanged: null,
                ),
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(
      find.byType(RadioListTile<String>),
      findsOneWidget,
      reason: 'non-TV keeps the native radio tile',
    );
    expect(find.byType(SwitchListTile), findsOneWidget);
    // The TV-only outer Focus handler is absent off TV.
    expect(find.byKey(const ValueKey('plain-tile')), findsOneWidget);
  });
}
