import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/scroll_utils.dart';

final GlobalKey _revealProbe = GlobalKey();
final GlobalKey _stepProbe = GlobalKey();

/// Items are 100 tall in a 300 viewport, so max scroll is 700 and item 5
/// spans offsets 500..600. [_revealProbe] sits on item 7; when the caller
/// needs the bottom item instead, [pumpScrollerWithBottomProbe] attaches the
/// same global key to item 9 (spans 900..1000).
Future<void> pumpScroller(
  WidgetTester tester, {
  required ScrollController controller,
}) async {
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
                  key: i == 7 ? _revealProbe : (i == 5 ? _stepProbe : null),
                  height: 100,
                  child: Builder(
                    builder: (context) => Text(
                      'item $i',
                      textDirection: TextDirection.ltr,
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> pumpScrollerWithBottomProbe(
  WidgetTester tester,
  ScrollController controller,
  GlobalKey probe,
) async {
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
                  key: i == 9 ? probe : null,
                  height: 100,
                  child: Text(
                    'item $i',
                    textDirection: TextDirection.ltr,
                  ),
                ),
            ],
          ),
        ),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('revealInNearestScrollable brings the probe into view',
      (tester) async {
    final controller = ScrollController();
    await pumpScroller(tester, controller: controller);
    expect(controller.offset, 0);

    revealInNearestScrollable(
      _revealProbe.currentContext!,
      alignment: 0.15,
      duration: const Duration(milliseconds: 120),
    );
    await tester.pumpAndSettle();

    // Item 7 aligned to 15% of the viewport.
    final expected = (700 - 0.15 * (300 - 100)).clamp(
      0.0,
      controller.position.maxScrollExtent,
    );
    expect(controller.offset, closeTo(expected, 2));
  });

  testWidgets(
    'revealInNearestScrollable clamps to the scrollable bounds',
    (tester) async {
      // Item 9 spans 900..1000 and could want offset 870; the list maxes
      // out at 700 and the reveal clamps there.
      final bottomProbe = _revealProbe;
      final controller = ScrollController();
      await pumpScrollerWithBottomProbe(tester, controller, bottomProbe);
      revealInNearestScrollable(
        bottomProbe.currentContext!,
        alignment: 0.15,
        duration: const Duration(milliseconds: 120),
      );
      await tester.pumpAndSettle();
      expect(controller.offset, closeTo(controller.position.maxScrollExtent, 2));
    },
  );

  testWidgets(
    'revealInNearestScrollable does nothing without a scrollable ancestor',
    (tester) async {
      final probe = GlobalKey();
      await tester.pumpWidget(MaterialApp(
        home: Builder(
          key: probe,
          builder: (context) => const Text('no scroller'),
        ),
      ));
      // Must not throw and must stay put.
      revealInNearestScrollable(probe.currentContext!, alignment: 0.5);
      await tester.pumpAndSettle();
    },
  );

  testWidgets('stepScrollWithinContextBounds stops at the probe bounds',
      (tester) async {
    final controller = ScrollController();
    await pumpScroller(tester, controller: controller);
    expect(controller.offset, 0);

    final context = _stepProbe.currentContext!;
    var steps = 0;
    while (stepScrollWithinContextBounds(
      context,
      down: true,
      step: 64,
      duration: const Duration(milliseconds: 20),
    )) {
      steps++;
      await tester.pumpAndSettle();
      expect(steps, lessThan(50), reason: 'the step never hit its bound');
    }
    await tester.pumpAndSettle();

    // The probe spans 500..600, so stepping clamps to 600 and then returns
    // false: the key is free to hand on past this widget.
    expect(controller.offset, closeTo(600, 1));
    expect(steps, 10);
  });

  testWidgets('stepScrollWithinContextBounds steps back up', (tester) async {
    final controller = ScrollController();
    await pumpScroller(tester, controller: controller);

    final context = _stepProbe.currentContext!;
    controller.jumpTo(600);
    await tester.pumpAndSettle();

    expect(
      stepScrollWithinContextBounds(
        context,
        down: false,
        step: 64,
        duration: const Duration(milliseconds: 20),
      ),
      isTrue,
    );
    await tester.pumpAndSettle();
    expect(controller.offset, closeTo(536, 1));
  });

  testWidgets('stepScrollWithinContextBounds refuses without a scrollable',
      (tester) async {
    final probe = GlobalKey();
    await tester.pumpWidget(MaterialApp(
      home: Builder(
        key: probe,
        builder: (context) => const Text('no scroller'),
      ),
    ));
    expect(
      stepScrollWithinContextBounds(probe.currentContext!, down: true),
      isFalse,
    );
  });

  testWidgets('scrollFocusIntoView walks a whole page minus one peek',
      (tester) async {
    final controller = ScrollController();
    final probe = GlobalKey();
    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: SizedBox(
          height: 300,
          child: ListView(
            controller: controller,
            children: <Widget>[
              for (var i = 0; i < 10; i++)
                SizedBox(
                  key: i == 6 ? probe : null,
                  height: 100,
                  child: Text('item $i'),
                ),
            ],
          ),
        ),
      ),
    ));
    await tester.pumpAndSettle();

    scrollFocusIntoView(probe.currentContext!);
    await tester.pumpAndSettle();
    expect(controller.offset, closeTo(600 - 30, 2));
  });
}
