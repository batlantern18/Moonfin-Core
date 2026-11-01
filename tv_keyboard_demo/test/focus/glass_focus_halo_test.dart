import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/glass_focus_halo.dart';

GlassFocusHalo _halo({bool focused = false, bool? ringOverPaints, double? scale}) =>
    SizedBox(
      width: 120,
      height: 48,
      child: ColoredBox(
        color: Colors.blue,
        child: GlassFocusHalo(
          focused: focused,
          ringOverPaints: ringOverPaints ?? false,
          scale: scale ?? 1.05,
          child: const SizedBox.expand(),
        ),
      ),
    );

BoxDecoration _ringDecoration(WidgetTester tester) {
  final container = tester.widget<AnimatedContainer>(
    find.descendant(
      of: find.byType(GlassFocusHalo),
      matching: find.byType(AnimatedContainer),
    ),
  );
  return container.decoration! as BoxDecoration;
}

void main() {
  setUp(() => GlassFocusHalo.appleStyleActive = false);
  tearDown(() => GlassFocusHalo.appleStyleActive = false);

  testWidgets('an unfocused halo draws no ring and no scale pop',
      (tester) async {
    await tester.pumpWidget(MaterialApp(home: _halo()));
    await tester.pumpAndSettle();

    final decoration = _ringDecoration(tester);
    expect((decoration.border as Border).top.color, Colors.transparent);

    final scale = tester.widget<AnimatedScale>(find.byType(AnimatedScale));
    expect(scale.scale, 1.0);
  });

  testWidgets('a focused halo rings with the theme primary and pops',
      (tester) async {
    await tester.pumpWidget(MaterialApp(home: _halo(focused: true)));
    await tester.pumpAndSettle();

    final decoration = _ringDecoration(tester);
    final ring = (decoration.border as Border).top.color;
    expect(ring.value, isNot(equals(Colors.transparent.value)));
    expect(decoration.color!.alpha, greaterThan(0));

    final scale = tester.widget<AnimatedScale>(find.byType(AnimatedScale));
    expect(scale.scale, 1.05);
  });

  testWidgets('the apple look adds the white ring and glow',
      (tester) async {
    GlassFocusHalo.appleStyleActive = true;
    await tester.pumpWidget(MaterialApp(home: _halo(focused: true)));
    await tester.pumpAndSettle();

    final decoration = _ringDecoration(tester);
    expect((decoration.border as Border).top.color, Colors.white);
    expect(
      decoration.boxShadow,
      isNotNull,
      reason: 'the tvOS look is built on the soft glow',
    );
  });

  testWidgets('ringOverPaints moves the ring to the foreground decoration',
      (tester) async {
    await tester.pumpWidget(MaterialApp(home: _halo(focused: true)));
    await tester.pumpAndSettle();
    expect(_ringDecoration(tester).border, isNotNull);

    await tester.pumpWidget(
      MaterialApp(home: _halo(focused: true, ringOverPaints: true)),
    );
    await tester.pumpAndSettle();
    expect(_ringDecoration(tester).border, isNull);
    expect(_ringDecoration(tester).color, Colors.transparent);
  });

  testWidgets('scale 1.0 skips the scale pop entirely', (tester) async {
    await tester.pumpWidget(MaterialApp(home: _halo(focused: true, scale: 1.0)));
    await tester.pumpAndSettle();
    expect(find.byType(AnimatedScale), findsNothing);
  });

  testWidgets(
    'the pointer press grows the halo when the apple look is on, and is '
    'inert when it is off', (tester) async {
    GlassFocusHalo.appleStyleActive = true;
    await tester.pumpWidget(MaterialApp(home: _halo(focused: false)));
    await tester.pumpAndSettle();

    await tester.sendEventToBinding(
      const PointerDownEvent(position: Offset(60, 24)),
    );
    await tester.pump(const Duration(milliseconds: 50));
    final pressed = tester.widget<AnimatedScale>(find.byType(AnimatedScale).last);
    expect(pressed.scale, greaterThan(1.0));

    await tester.sendEventToBinding(
      const PointerUpEvent(position: Offset(60, 24)),
    );
    await tester.pumpAndSettle();
    expect(
      tester.widget<AnimatedScale>(find.byType(AnimatedScale).last).scale,
      1.0,
    );

    GlassFocusHalo.appleStyleActive = false;
    await tester.pumpWidget(MaterialApp(home: _halo(focused: false)));
    await tester.pumpAndSettle();
    await tester.sendEventToBinding(
      const PointerDownEvent(position: Offset(60, 24)),
    );
    await tester.pump();
    // Only the focus pop scale remains, resting at 1.0.
    expect(
      tester.widget<AnimatedScale>(find.byType(AnimatedScale).last).scale,
      1.0,
    );
  });
}
