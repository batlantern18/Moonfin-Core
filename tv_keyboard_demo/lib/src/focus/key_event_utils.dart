import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

import 'dpad_keys.dart';

/// Maps raw key events onto the D-pad vocabulary of this app. Every surface
/// (rows, tiles, dialogs) answers select, back and the arrows through these
/// helpers so an Enter key and a remote's select card behave identically.
///
/// Behavior replicated from Moonfin-Core's `lib/util/focus/key_event_utils.dart`
/// with the Moonfin-specific additions removed.

/// Handles a one-shot select: the down triggers [onSelect] exactly once and
/// the matching key up is swallowed, so a surface that listens to key ups
/// (a text field trying to commit, say) never double-fires.
KeyEventResult handleOneShotSelect(KeyEvent event, VoidCallback onSelect) {
  if (event is KeyDownEvent && event.logicalKey.isSelectKey) {
    _SelectKeyUpSuppressor.markPressed();
    onSelect();
    return KeyEventResult.handled;
  }
  if (_SelectKeyUpSuppressor.consumeIfSuppressed(event)) {
    return KeyEventResult.handled;
  }
  return KeyEventResult.ignored;
}

class _SelectKeyUpSuppressor {
  static int _suppressCount = 0;

  static void markPressed() => _suppressCount++;

  static bool consumeIfSuppressed(KeyEvent event) {
    if (event is! KeyUpEvent) return false;
    if (!event.logicalKey.isSelectKey) return false;
    if (_suppressCount == 0) return false;
    _suppressCount--;
    return true;
  }
}

/// Handles Back's down and up as a pair: the down fires [onBack] once and the
/// up is consumed so nothing below sees a second back. Non-back keys pass
/// through untouched.
KeyEventResult handleBackKeyAction(KeyEvent event, VoidCallback onBack) {
  if (!event.logicalKey.isBackKey) return KeyEventResult.ignored;
  if (event is KeyDownEvent) {
    onBack();
    return KeyEventResult.handled;
  }
  if (event is KeyUpEvent) return KeyEventResult.handled;
  return KeyEventResult.ignored;
}

/// One focus handler for the four directions plus select. A direction with no
/// callback stays ignored so the event bubbles to the framework's traversal.
FocusOnKeyEventCallback dpadKeyHandler({
  VoidCallback? onUp,
  VoidCallback? onDown,
  VoidCallback? onLeft,
  VoidCallback? onRight,
  VoidCallback? onSelect,
}) {
  return (FocusNode node, KeyEvent event) {
    if (onSelect != null) {
      final r = handleOneShotSelect(event, onSelect);
      if (r != KeyEventResult.ignored) return r;
    }
    if (!event.isActionable) return KeyEventResult.ignored;
    final k = event.logicalKey;
    if (k.isUpKey && onUp != null) {
      onUp();
      return KeyEventResult.handled;
    }
    if (k.isDownKey && onDown != null) {
      onDown();
      return KeyEventResult.handled;
    }
    if (k.isLeftKey && onLeft != null) {
      onLeft();
      return KeyEventResult.handled;
    }
    if (k.isRightKey && onRight != null) {
      onRight();
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  };
}

/// A Focus handler that scrolls [controller] by [step] on the up and down
/// keys, for dialogs and panels whose rows take no focus of their own.
FocusOnKeyEventCallback arrowScrollHandler(
  ScrollController controller, {
  double step = 120,
}) {
  void scrollBy(double delta) {
    if (!controller.hasClients) return;
    final position = controller.position;
    final target = (position.pixels + delta).clamp(
      position.minScrollExtent,
      position.maxScrollExtent,
    );
    unawaited(
      controller.animateTo(
        target,
        duration: const Duration(milliseconds: 120),
        curve: Curves.easeOut,
      ),
    );
  }

  return dpadKeyHandler(
    onUp: () => scrollBy(-step),
    onDown: () => scrollBy(step),
  );
}

/// Blocks the edge keys of a bounded surface so a press past its end doesn't
/// leak movement to whatever sits behind it.
KeyEventResult consumeIfEdge(
  KeyEvent event, {
  bool atLeftEdge = false,
  bool atRightEdge = false,
  bool atTopEdge = false,
  bool atBottomEdge = false,
}) {
  if (!event.isActionable) return KeyEventResult.ignored;
  final k = event.logicalKey;
  if (atLeftEdge && k.isLeftKey) return KeyEventResult.handled;
  if (atRightEdge && k.isRightKey) return KeyEventResult.handled;
  if (atTopEdge && k.isUpKey) return KeyEventResult.handled;
  if (atBottomEdge && k.isDownKey) return KeyEventResult.handled;
  return KeyEventResult.ignored;
}

/// Turns a select press into tap or long-press.
///
/// The down starts a [longPressDuration] timer; letting go early fires
/// [onTap], holding past it fires [onLongPress] immediately and the up then
/// does nothing. The remote's context-menu key fires [onLongPress] directly.
/// Repeats are held so a keyup without a matching down stays ignored.
class LongPressSelectKeyHandler {
  LongPressSelectKeyHandler({this.longPressDuration = _kLongPressDuration});

  final Duration longPressDuration;
  static const Duration _kLongPressDuration = Duration(milliseconds: 500);

  bool _selectDownSeen = false;
  bool _longPressFired = false;
  Timer? _longPressTimer;

  void dispose() {
    _longPressTimer?.cancel();
  }

  KeyEventResult handleKeyEvent(
    KeyEvent event, {
    required VoidCallback onTap,
    required VoidCallback onLongPress,
  }) {
    final key = event.logicalKey;

    if (key.isSelectKey) {
      if (event is KeyDownEvent) {
        _selectDownSeen = true;
        _longPressFired = false;
        _longPressTimer?.cancel();
        _longPressTimer = Timer(longPressDuration, () {
          _longPressFired = true;
          onLongPress();
        });
        return KeyEventResult.handled;
      }
      if (event is KeyRepeatEvent) {
        return _selectDownSeen
            ? KeyEventResult.handled
            : KeyEventResult.ignored;
      }
      if (event is KeyUpEvent) {
        if (!_selectDownSeen) return KeyEventResult.ignored;
        _selectDownSeen = false;
        _longPressTimer?.cancel();
        _longPressTimer = null;
        if (!_longPressFired) {
          onTap();
        }
        _longPressFired = false;
        return KeyEventResult.handled;
      }
    }

    if (key.isContextMenuKey && event is KeyDownEvent) {
      onLongPress();
      return KeyEventResult.handled;
    }

    return KeyEventResult.ignored;
  }
}
