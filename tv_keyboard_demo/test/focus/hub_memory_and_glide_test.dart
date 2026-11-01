import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/hub_focus_memory.dart';
import 'package:tv_keyboard_demo/src/focus/row_glide.dart';

void main() {
  group('HubFocusMemory', () {
    setUp(HubFocusMemory.clearAll);
    tearDown(HubFocusMemory.clearAll);

    test('nothing stored lands on index 0', () {
      expect(HubFocusMemory.getForHub('hub', 5), 0);
      expect(HubFocusMemory.peek('hub'), isNull);
    });

    test('an empty hub clamps to a safe 0', () {
      HubFocusMemory.set('hub', 7);
      expect(HubFocusMemory.getForHub('hub', 0), 0);
      expect(HubFocusMemory.getForHub('hub', 3), 2);
      expect(HubFocusMemory.getForHub('hub', 9), 7);
    });

    test('negative stores are rejected', () {
      HubFocusMemory.set('hub', -1);
      expect(HubFocusMemory.peek('hub'), isNull);
    });

    test('clear removes one hub, clearAll all of them', () {
      HubFocusMemory.set('a', 2);
      HubFocusMemory.set('b', 3);
      HubFocusMemory.clear('a');
      expect(HubFocusMemory.peek('a'), isNull);
      expect(HubFocusMemory.peek('b'), 3);
      HubFocusMemory.clearAll();
      expect(HubFocusMemory.peek('b'), isNull);
    });
  });

  group('RowGlide', () {
    test('jumpTo pins the position and moveTo retargets', () {
      final glide = RowGlide();
      glide.jumpTo(3);
      expect(glide.target, 3);
      expect(glide.openness(3), 0, reason: 'nothing is open while inactive');

      glide.active = true;
      glide.tick(10.0); // long enough to settle
      expect(glide.openness(3), closeTo(1.0, 0.01));

      glide.moveTo(4);
      expect(glide.target, 4);
    });

    test('openness falls off away from the focus position', () {
      final glide = RowGlide(
        cardGrowDuration: Duration.zero,
        settleDuration: Duration.zero,
      );
      glide
        ..jumpTo(2)
        ..active = true;
      // cardGrowDuration zero makes the open spring instant.
      glide.tick(0.5);
      expect(glide.openness(2), closeTo(1.0, 0.01));
      expect(glide.openness(1), closeTo(0.0, 0.01));
      expect(glide.openness(3), closeTo(0.0, 0.01));
      expect(glide.openness(0), 0.0);
    });

    test('held folds the row back to plain cards', () {
      final glide = RowGlide(
        cardGrowDuration: Duration.zero,
        settleDuration: Duration.zero,
      );
      glide
        ..jumpTo(1)
        ..active = true
        ..held = true;
      glide.tick(0.5);
      expect(glide.openness(1), closeTo(0.0, 0.01));
    });

    test('tick settles: it returns false once nothing moves', () {
      final glide = RowGlide(
        cardGrowDuration: Duration.zero,
        settleDuration: Duration.zero,
      );
      glide
        ..jumpTo(5)
        ..active = true;
      var stillMoving = true;
      for (var i = 0; i < 50 && stillMoving; i++) {
        stillMoving = glide.tick(0.1);
      }
      expect(stillMoving, isFalse);
    });

    test('scrollOffset keeps the focus card at the leading edge', () {
      final glide =
          RowGlide(
              cardGrowDuration: Duration.zero,
              settleDuration: Duration.zero,
            )
            ..jumpTo(0)
            ..growth = 20;
      // With no gap and the position exactly on a card, the offset is the
      // card's own start.
      expect(glide.scrollOffset(leading: 50, stride: 100), 50.0);

      glide.moveTo(2);
      glide.tick(10.0);
      expect(glide.scrollOffset(leading: 50, stride: 100), 250.0);
    });

    test('a catch-up gap decays back to zero', () {
      final glide =
          RowGlide(
              cardGrowDuration: Duration.zero,
              settleDuration: Duration.zero,
            )
            ..catchUp(12)
            ..jumpTo(1);
      final moved = glide.tick(0.5);
      expect(glide.scrollOffset(leading: 0, stride: 100), closeTo(100.0, 0.01));
      expect(moved, isFalse);
    });
  });
}
