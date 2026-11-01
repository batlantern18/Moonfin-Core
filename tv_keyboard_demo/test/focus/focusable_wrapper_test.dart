import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/focusable_wrapper.dart';
import 'package:tv_keyboard_demo/src/focus/glass_focus_halo.dart';
import 'package:tv_keyboard_demo/src/focus/input_mode_tracker.dart';

class _Counter extends StatelessWidget {
  const _Counter({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) =>
      Text(label, textDirection: TextDirection.ltr);
}

void main() {
  setUp(() {
    // Focus visuals follow cursor policy; reset the one switchable knob.
    GlassFocusHalo.appleStyleActive = false;
  });

  testWidgets('select triggers the callback exactly once per press', (
    tester,
  ) async {
    var selected = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: FocusableWrapper(
          autofocus: true,
          onSelect: () => selected++,
          child: const _Counter(label: 'item'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    expect(selected, 1);
    // The key up is suppressed: no repeat fire.
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(selected, 1);
  });

  testWidgets('arrow keys fire their direction callbacks', (tester) async {
    final path = <String>[];
    await tester.pumpWidget(
      MaterialApp(
        home: FocusableWrapper(
          autofocus: true,
          onNavigateUp: () => path.add('up'),
          onNavigateDown: () => path.add('down'),
          onNavigateLeft: () => path.add('left'),
          onNavigateRight: () => path.add('right'),
          child: const _Counter(label: 'item'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    for (final key in <LogicalKeyboardKey>[
      LogicalKeyboardKey.arrowUp,
      LogicalKeyboardKey.arrowDown,
      LogicalKeyboardKey.arrowLeft,
      LogicalKeyboardKey.arrowRight,
    ]) {
      await tester.sendKeyDownEvent(key);
      await tester.pump();
    }
    expect(path, ['up', 'down', 'left', 'right']);
  });

  testWidgets('back fires on key down and the key up does nothing', (
    tester,
  ) async {
    var backs = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: FocusableWrapper(
          autofocus: true,
          onBack: () => backs++,
          child: const _Counter(label: 'item'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.sendKeyDownEvent(LogicalKeyboardKey.escape);
    await tester.pump();
    expect(backs, 1);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.escape);
    await tester.pump();
    expect(backs, 1);
  });

  testWidgets('long-press fires on context menu key without select', (
    tester,
  ) async {
    var selected = 0;
    Widget build(VoidCallback? onLongPress) => MaterialApp(
      home: FocusableWrapper(
        autofocus: true,
        onSelect: () => selected++,
        onLongPress: onLongPress,
        child: const _Counter(label: 'item'),
      ),
    );

    // Without enableLongPress a context-menu press isn't mapped to select.
    await tester.pumpWidget(build(null));
    await tester.pumpAndSettle();
    await tester.sendKeyDownEvent(LogicalKeyboardKey.contextMenu);
    await tester.pump();
    expect(selected, 0);

    // With enableLongPress the context key long-presses directly.
    var longPressed = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: FocusableWrapper(
          autofocus: true,
          enableLongPress: true,
          onSelect: () => selected++,
          onLongPress: () => longPressed++,
          child: const _Counter(label: 'item'),
        ),
      ),
    );
    await tester.pumpAndSettle();
    selected = 0;
    await tester.sendKeyDownEvent(LogicalKeyboardKey.contextMenu);
    await tester.pump();
    expect(longPressed, 1);
    expect(selected, 0);
  });

  testWidgets('holding select long-presses; a quick press selects', (
    tester,
  ) async {
    var selected = 0;
    var longPressed = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: FocusableWrapper(
          autofocus: true,
          enableLongPress: true,
          onSelect: () => selected++,
          onLongPress: () => longPressed++,
          child: const _Counter(label: 'item'),
        ),
      ),
    );
    await tester.pumpAndSettle();

    // Hold past the threshold.
    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump(const Duration(milliseconds: 550));
    expect(longPressed, 1);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(selected, 0);

    // A fresh quick press selects.
    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump(const Duration(milliseconds: 100));
    await tester.sendKeyUpEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(selected, 1);
    expect(longPressed, 1);
  });

  testWidgets('onFocusChange reports gains and losses', (tester) async {
    final node = FocusNode();
    final events = <bool>[];
    await tester.pumpWidget(
      MaterialApp(
        home: FocusableWrapper(
          focusNode: node,
          onFocusChange: events.add,
          child: const _Counter(label: 'item'),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(events, isEmpty);

    node.requestFocus();
    await tester.pumpAndSettle();
    expect(events, [true]);

    node.unfocus();
    await tester.pumpAndSettle();
    expect(events, [true, false]);
  });

  testWidgets('autoScroll brings a focused child below the fold into view', (
    tester,
  ) async {
    final controller = ScrollController();
    final nodes = List<FocusNode>.generate(10, (_) => FocusNode());
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: SizedBox(
            height: 300,
            child: ListView(
              controller: controller,
              children: <Widget>[
                for (var i = 0; i < 10; i++)
                  SizedBox(
                    height: 60,
                    child: FocusableWrapper(
                      focusNode: nodes[i],
                      autoScroll: true,
                      child: _Counter(label: 'row $i'),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(controller.offset, 0);

    // Focus a row past the fold; autoScroll must bring it into view.
    nodes[8].requestFocus();
    await tester.pumpAndSettle();
    expect(controller.offset, greaterThan(0));
  });

  testWidgets('useBackgroundFocus only shows the solid fill in keyboard mode', (
    tester,
  ) async {
    final node = FocusNode();
    Widget build() => MaterialApp(
      home: InputModeTracker(
        child: FocusableWrapper(
          focusNode: node,
          autofocus: true,
          useBackgroundFocus: true,
          child: const _Counter(label: 'solid'),
        ),
      ),
    );

    await tester.pumpWidget(build());
    await tester.pumpAndSettle();
    // Non-TV defaults to pointer mode, so focus alone does not light the
    // solid fill.
    expect(
      FocusableWrapper.isHighlighted(tester.element(find.text('solid'))),
      isFalse,
    );

    // A key press switches to keyboard mode, and the fill appears.
    await tester.sendKeyDownEvent(LogicalKeyboardKey.select);
    await tester.pump();
    expect(
      FocusableWrapper.isHighlighted(tester.element(find.text('solid'))),
      isTrue,
    );
  });
}
