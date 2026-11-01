import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/input_mode_tracker.dart';
import 'package:tv_keyboard_demo/src/focus/platform_detection.dart';

/// Pumps the tracker with a probe that records each distinct reported mode.
/// The probe renders `<showFocusVisuals>:<pointerIsLive>` for a focused
/// widget, so the visible text is the whole decision consumers depend on.
///
/// A focused-flag of `true` is handed to [InputModeTracker.showFocusVisuals]
/// so the first line of the rendered text is the visuals decision.
Future<void> pumpTracker(
  WidgetTester tester, {
  required List<String> recorded,
}) async {
  await tester.pumpWidget(
    InputModeTracker(
      child: Directionality(
        textDirection: TextDirection.ltr,
        child: Builder(
          builder: (context) {
            final current = InputModeTracker.of(context);
            if (recorded.isEmpty || recorded.last != current.name) {
              recorded.add(current.name);
            }
            return Focus(
              autofocus: true,
              child: SizedBox.square(
                dimension: 100,
                child: Text(
                  '${InputModeTracker.showFocusVisuals(context, true)}:'
                  '${InputModeTracker.pointerIsLive(context)}',
                  textDirection: TextDirection.ltr,
                ),
              ),
            );
          },
        ),
      ),
    ),
  );
}

void main() {
  setUp(TvPlatformDetection.reset);
  tearDown(TvPlatformDetection.reset);

  testWidgets('defaults to pointer away from a TV', (tester) async {
    final modes = <String>[];
    await pumpTracker(tester, recorded: modes);
    expect(modes, ['pointer']);
    expect(find.byText('false:false'), findsOneWidget);
  });

  testWidgets('defaults to keyboard on a TV device', (tester) async {
    TvPlatformDetection.setTvMode(true);
    final modes = <String>[];
    await pumpTracker(tester, recorded: modes);
    expect(modes, ['keyboard']);
  });

  testWidgets('a key press makes the UI keyboard-driven', (tester) async {
    final modes = <String>[];
    await pumpTracker(tester, recorded: modes);
    expect(modes, ['pointer']);
    expect(find.byText('false:false'), findsOneWidget);

    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowDown);
    await tester.pump();
    expect(modes.last, 'keyboard');
    expect(find.byText('true:false'), findsOneWidget);
  });

  testWidgets('a pointer event makes the UI pointer-driven', (tester) async {
    final modes = <String>[];
    await pumpTracker(tester, recorded: modes);

    // A key press first, so there is a keyboard state to leave.
    await tester.sendKeyDownEvent(LogicalKeyboardKey.arrowDown);
    await tester.pump();
    expect(modes.last, 'keyboard');

    // A hovering mouse is a pointer being used right now.
    await tester.sendEventToBinding(
      const PointerHoverEvent(position: Offset(60, 50)),
    );
    await tester.pump();
    expect(modes, ['pointer', 'keyboard', 'pointer']);
    expect(find.byText('false:true'), findsOneWidget);
  });

  testWidgets('on TV, pointer events never leave keyboard mode',
      (tester) async {
    TvPlatformDetection.setTvMode(true);
    final modes = <String>[];
    await pumpTracker(tester, recorded: modes);
    expect(modes, ['keyboard']);

    await tester.sendEventToBinding(
      const PointerHoverEvent(position: Offset(60, 50)),
    );
    await tester.pump();
    expect(modes, ['keyboard']);
    // The mouse is still drawn hover state even though focus visuals stay.
    expect(find.byText('true:true'), findsOneWidget);
  });

  testWidgets('a pointer down position is remembered', (tester) async {
    final modes = <String>[];
    await pumpTracker(tester, recorded: modes);

    await tester.sendEventToBinding(
      const PointerDownEvent(position: Offset(50, 50)),
    );
    await tester.pump();
    expect(
      InputModeTracker.lastPointerDownPosition,
      const Offset(50, 50),
    );
  });
}
