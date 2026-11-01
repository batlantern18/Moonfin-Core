import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/dpad_keys.dart';

/// One key down event for [key], the event shape a real remote sends.
KeyDownEvent _down(LogicalKeyboardKey key) => KeyDownEvent(
  physicalKey: PhysicalKeyboardKey.space,
  logicalKey: key,
  timeStamp: Duration.zero,
);

KeyUpEvent _up(LogicalKeyboardKey key) => KeyUpEvent(
  physicalKey: PhysicalKeyboardKey.space,
  logicalKey: key,
  timeStamp: Duration.zero,
);

void main() {
  group('isActionable', () {
    test('down and repeat are actionable, up is not', () {
      expect(_down(LogicalKeyboardKey.arrowDown).isActionable, isTrue);
      final repeat = KeyRepeatEvent(
        physicalKey: PhysicalKeyboardKey.arrowDown,
        logicalKey: LogicalKeyboardKey.arrowDown,
        timeStamp: Duration.zero,
      );
      expect(repeat.isActionable, isTrue);
      expect(_up(LogicalKeyboardKey.arrowDown).isActionable, isFalse);
    });
  });

  group('select keys', () {
    for (final key in <LogicalKeyboardKey>[
      LogicalKeyboardKey.select,
      LogicalKeyboardKey.enter,
      LogicalKeyboardKey.numpadEnter,
      LogicalKeyboardKey.gameButtonA,
      LogicalKeyboardKey.accept,
      LogicalKeyboardKey.space,
    ]) {
      test('${key.keyLabel} counts as select', () {
        expect(key.isSelectKey, isTrue, reason: key.debugName!);
        expect(isActivateKey(_down(key)), isTrue);
      });
    }

    test('arrow keys are not select', () {
      expect(LogicalKeyboardKey.arrowDown.isSelectKey, isFalse);
      expect(isActivateKey(_down(LogicalKeyboardKey.arrowLeft)), isFalse);
    });
  });

  group('back keys', () {
    for (final key in <LogicalKeyboardKey>[
      LogicalKeyboardKey.escape,
      LogicalKeyboardKey.goBack,
      LogicalKeyboardKey.browserBack,
      LogicalKeyboardKey.gameButtonB,
      LogicalKeyboardKey.backspace,
    ]) {
      test('${key.keyLabel} counts as back', () {
        expect(key.isBackKey, isTrue, reason: key.debugName!);
      });
    }

    test('select is not back', () {
      expect(LogicalKeyboardKey.enter.isBackKey, isFalse);
    });
  });

  group('context menu keys', () {
    test('menu and gameButtonX counts as context', () {
      expect(LogicalKeyboardKey.contextMenu.isContextMenuKey, isTrue);
      expect(LogicalKeyboardKey.gameButtonX.isContextMenuKey, isTrue);
      expect(LogicalKeyboardKey.gameButtonY.isContextMenuKey, isFalse);
    });
  });

  group('directional keys', () {
    test('the four arrows are directional, nothing else is', () {
      expect(LogicalKeyboardKey.arrowUp.isUpKey, isTrue);
      expect(LogicalKeyboardKey.arrowDown.isDownKey, isTrue);
      expect(LogicalKeyboardKey.arrowLeft.isLeftKey, isTrue);
      expect(LogicalKeyboardKey.arrowRight.isRightKey, isTrue);
      for (final key in <LogicalKeyboardKey>[
        LogicalKeyboardKey.arrowUp,
        LogicalKeyboardKey.arrowDown,
        LogicalKeyboardKey.arrowLeft,
        LogicalKeyboardKey.arrowRight,
      ]) {
        expect(key.isDirectional, isTrue, reason: key.debugName!);
      }
      expect(LogicalKeyboardKey.enter.isDirectional, isFalse);
      expect(LogicalKeyboardKey.arrowUp.isLeftKey, isFalse);
      expect(LogicalKeyboardKey.arrowDown.isUpKey, isFalse);
    });
  });

  group('page keys', () {
    test('transport forward keys page ahead', () {
      expect(LogicalKeyboardKey.mediaFastForward.pageDirection, 1);
      expect(LogicalKeyboardKey.mediaTrackNext.pageDirection, 1);
      expect(LogicalKeyboardKey.pageDown.pageDirection, 1);
    });

    test('transport back keys page backward', () {
      expect(LogicalKeyboardKey.mediaRewind.pageDirection, -1);
      expect(LogicalKeyboardKey.mediaTrackPrevious.pageDirection, -1);
      expect(LogicalKeyboardKey.pageUp.pageDirection, -1);
    });

    test('ordinary keys do not page', () {
      expect(LogicalKeyboardKey.enter.pageDirection, 0);
      expect(LogicalKeyboardKey.arrowDown.pageDirection, 0);
    });
  });
}
