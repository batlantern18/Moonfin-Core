import 'package:fake_async/fake_async.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/key_event_utils.dart';

KeyEvent _down(LogicalKeyboardKey key) => KeyDownEvent(
  physicalKey: PhysicalKeyboardKey.space,
  logicalKey: key,
  timeStamp: Duration.zero,
);

KeyEvent _up(LogicalKeyboardKey key) => KeyUpEvent(
  physicalKey: PhysicalKeyboardKey.space,
  logicalKey: key,
  timeStamp: const Duration(milliseconds: 10),
);

KeyEvent _repeat(LogicalKeyboardKey key) => KeyRepeatEvent(
  physicalKey: PhysicalKeyboardKey.space,
  logicalKey: key,
  timeStamp: const Duration(milliseconds: 200),
);

void main() {
  group('handleOneShotSelect', () {
    test('select down fires the callback once and is handled', () {
      var fired = 0;
      expect(
        handleOneShotSelect(_down(LogicalKeyboardKey.select), () {
          fired++;
        }),
        KeyEventResult.handled,
      );
      expect(fired, 1);
    });

    test('the matching key up is swallowed', () {
      handleOneShotSelect(_down(LogicalKeyboardKey.select), () {});
      expect(
        handleOneShotSelect(_up(LogicalKeyboardKey.select), () {}),
        KeyEventResult.handled,
      );
      expect(
        // A second stray up with no pending press is not consumed.
        handleOneShotSelect(_up(LogicalKeyboardKey.select), () {}),
        KeyEventResult.ignored,
      );
    });

    test('a lost release does not suppress a different select key', () {
      // Enter is pressed, but its release is lost when focus changes. A later
      // Space press/release must still pair with Space rather than consuming
      // the stale Enter press.
      handleOneShotSelect(_down(LogicalKeyboardKey.enter), () {});
      handleOneShotSelect(_down(LogicalKeyboardKey.space), () {});
      expect(
        handleOneShotSelect(_up(LogicalKeyboardKey.space), () {}),
        KeyEventResult.handled,
      );
      expect(
        handleOneShotSelect(_up(LogicalKeyboardKey.enter), () {}),
        KeyEventResult.handled,
      );
      expect(
        handleOneShotSelect(_up(LogicalKeyboardKey.enter), () {}),
        KeyEventResult.ignored,
      );
    });

    test('arrow keys do not fire select', () {
      var fired = 0;
      expect(
        handleOneShotSelect(_down(LogicalKeyboardKey.arrowRight), () {
          fired++;
        }),
        KeyEventResult.ignored,
      );
      expect(fired, 0);
    });

    test('every select alias produces one press and one swallowed up', () {
      for (final key in <LogicalKeyboardKey>[
        LogicalKeyboardKey.enter,
        LogicalKeyboardKey.numpadEnter,
        LogicalKeyboardKey.gameButtonA,
        LogicalKeyboardKey.accept,
        LogicalKeyboardKey.space,
      ]) {
        var fired = 0;
        expect(
          handleOneShotSelect(_down(key), () {
            fired++;
          }),
          KeyEventResult.handled,
        );
        expect(fired, 1, reason: key.debugName!);
        expect(
          handleOneShotSelect(_up(key), () {
            fired++;
          }),
          KeyEventResult.handled,
        );
        expect(fired, 1, reason: key.debugName!);
      }
    });
  });

  group('handleBackKeyAction', () {
    test('back down fires the callback and is handled', () {
      var backed = 0;
      expect(
        handleBackKeyAction(_down(LogicalKeyboardKey.escape), () {
          backed++;
        }),
        KeyEventResult.handled,
      );
      expect(backed, 1);
    });

    test('back up is consumed without firing the callback', () {
      var backed = 0;
      expect(
        handleBackKeyAction(_up(LogicalKeyboardKey.goBack), () {
          backed++;
        }),
        KeyEventResult.handled,
      );
      expect(backed, 0);
    });

    test('delete is a back key too', () {
      var backed = 0;
      handleBackKeyAction(_down(LogicalKeyboardKey.backspace), () {
        backed++;
      });
      expect(backed, 1);
    });

    test('direction keys pass through untouched', () {
      expect(
        handleBackKeyAction(_down(LogicalKeyboardKey.arrowLeft), () {}),
        KeyEventResult.ignored,
      );
    });
  });

  group('dpadKeyHandler', () {
    test('each direction fires its own callback on down', () {
      String? moved;
      final handler = dpadKeyHandler(
        onUp: () => moved = 'up',
        onDown: () => moved = 'down',
        onLeft: () => moved = 'left',
        onRight: () => moved = 'right',
      );
      expect(
        handler(FocusNode(), _down(LogicalKeyboardKey.arrowUp)),
        KeyEventResult.handled,
      );
      expect(moved, 'up');
      expect(
        handler(FocusNode(), _down(LogicalKeyboardKey.arrowDown)),
        KeyEventResult.handled,
      );
      expect(moved, 'down');
      expect(
        handler(FocusNode(), _down(LogicalKeyboardKey.arrowLeft)),
        KeyEventResult.handled,
      );
      expect(moved, 'left');
      expect(
        handler(FocusNode(), _down(LogicalKeyboardKey.arrowRight)),
        KeyEventResult.handled,
      );
      expect(moved, 'right');
    });

    test('a direction with no callback stays ignored', () {
      final handler = dpadKeyHandler(onDown: () {});
      expect(
        handler(FocusNode(), _down(LogicalKeyboardKey.arrowRight)),
        KeyEventResult.ignored,
      );
      expect(
        handler(FocusNode(), _down(LogicalKeyboardKey.arrowLeft)),
        KeyEventResult.ignored,
      );
      expect(
        handler(FocusNode(), _down(LogicalKeyboardKey.arrowUp)),
        KeyEventResult.ignored,
      );
    });

    test('key ups let the event bubble for traversal to take over', () {
      var moved = 0;
      final handler = dpadKeyHandler(onDown: () => moved++);
      expect(
        handler(FocusNode(), _up(LogicalKeyboardKey.arrowDown)),
        KeyEventResult.ignored,
      );
      expect(moved, 0);
    });

    test('repeat events move focus like a down', () {
      var moved = 0;
      final handler = dpadKeyHandler(onDown: () => moved++);
      expect(
        handler(FocusNode(), _repeat(LogicalKeyboardKey.arrowDown)),
        KeyEventResult.handled,
      );
      expect(moved, 1);
    });

    test('select goes through the one-shot path', () {
      var selected = 0;
      final handler = dpadKeyHandler(onSelect: () => selected++);
      final node = FocusNode();
      expect(
        handler(node, _down(LogicalKeyboardKey.select)),
        KeyEventResult.handled,
      );
      expect(
        handler(node, _up(LogicalKeyboardKey.select)),
        KeyEventResult.handled,
      );
      expect(selected, 1);
    });
  });

  group('consumeIfEdge', () {
    test('the key at an edge is consumed, the others are not', () {
      expect(
        consumeIfEdge(_down(LogicalKeyboardKey.arrowLeft), atLeftEdge: true),
        KeyEventResult.handled,
      );
      expect(
        consumeIfEdge(_down(LogicalKeyboardKey.arrowRight), atLeftEdge: true),
        KeyEventResult.ignored,
      );
      expect(
        consumeIfEdge(_down(LogicalKeyboardKey.arrowRight), atRightEdge: true),
        KeyEventResult.handled,
      );
      expect(
        consumeIfEdge(_down(LogicalKeyboardKey.arrowUp), atTopEdge: true),
        KeyEventResult.handled,
      );
      expect(
        consumeIfEdge(_down(LogicalKeyboardKey.arrowDown), atBottomEdge: true),
        KeyEventResult.handled,
      );
      expect(
        consumeIfEdge(_down(LogicalKeyboardKey.arrowUp), atBottomEdge: true),
        KeyEventResult.ignored,
      );
    });

    test('key ups never count as edge presses', () {
      expect(
        consumeIfEdge(_up(LogicalKeyboardKey.arrowLeft), atLeftEdge: true),
        KeyEventResult.ignored,
      );
    });
  });

  group('LongPressSelectKeyHandler', () {
    late LongPressSelectKeyHandler handler;
    int taps = 0;
    int longPresses = 0;

    setUp(() {
      handler = LongPressSelectKeyHandler();
      taps = 0;
      longPresses = 0;
    });

    tearDown(() {
      handler.dispose();
    });

    KeyEventResult handle(KeyEvent event) => handler.handleKeyEvent(
      event,
      onTap: () => taps++,
      onLongPress: () => longPresses++,
    );

    test('a quick select taps on key up', () {
      fakeAsync((async) {
        expect(
          handle(_down(LogicalKeyboardKey.select)),
          KeyEventResult.handled,
        );
        expect(taps, 0);
        async.elapse(const Duration(milliseconds: 100));
        expect(handle(_up(LogicalKeyboardKey.select)), KeyEventResult.handled);
        expect(taps, 1);
        expect(longPresses, 0);
      });
    });

    test('holding select past 500ms long-presses and the up does nothing', () {
      fakeAsync((async) {
        expect(
          handle(_down(LogicalKeyboardKey.select)),
          KeyEventResult.handled,
        );
        async.elapse(const Duration(milliseconds: 550));
        expect(longPresses, 1);
        expect(handle(_up(LogicalKeyboardKey.select)), KeyEventResult.handled);
        expect(taps, 0);
        expect(longPresses, 1);
      });
    });

    test('repeats are held while the press is down', () {
      fakeAsync((async) {
        handle(_down(LogicalKeyboardKey.select));
        async.elapse(const Duration(milliseconds: 100));
        expect(
          handle(_repeat(LogicalKeyboardKey.select)),
          KeyEventResult.handled,
        );
        expect(taps, 0);
      });
    });

    test('the context menu key long-presses immediately', () {
      fakeAsync((async) {
        expect(
          handle(_down(LogicalKeyboardKey.contextMenu)),
          KeyEventResult.handled,
        );
        expect(longPresses, 1);
        expect(taps, 0);
      });
    });

    test('a select up with no seen down is ignored', () {
      expect(handle(_up(LogicalKeyboardKey.select)), KeyEventResult.ignored);
      expect(taps, 0);
    });
  });
}
