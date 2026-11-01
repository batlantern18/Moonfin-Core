import 'dart:async';

import 'package:flutter/widgets.dart';

/// D-pad scrolling helpers. Replicated from Moonfin-Core's
/// lib/util/focus/scroll_utils.dart, focus_scroll.dart and step_scroll.dart
/// with the preference lookups replaced by fixed defaults.

const Duration kDefaultScrollDuration = Duration(milliseconds: 180);
const Curve _kScrollCurve = Curves.easeOut;

/// Brings a newly focused widget into view, far enough from the top edge that
/// whatever follows it is visible too. Shared so that moving a remote down a
/// page reads as one motion rather than several differently timed ones.
void scrollFocusIntoView(BuildContext context) {
  Scrollable.ensureVisible(
    context,
    duration: const Duration(milliseconds: 250),
    alignment: 0.15,
    curve: Curves.easeOutCubic,
  );
}

Future<void> scrollListToIndex(
  ScrollController controller,
  int index, {
  required double itemExtent,
  double leadingPadding = 0,
  bool animate = true,
  Duration duration = kDefaultScrollDuration,
  Curve curve = _kScrollCurve,
}) {
  if (!controller.hasClients) return Future<void>.value();
  final maxOffset = controller.position.maxScrollExtent;
  final target = (index * itemExtent + leadingPadding).clamp(0.0, maxOffset)
      .toDouble();
  if (animate) {
    return controller.animateTo(target, duration: duration, curve: curve);
  }
  controller.jumpTo(target);
  return Future<void>.value();
}

void focusItemAndEnsureVisible({
  required bool Function() isMounted,
  required List<FocusNode> focusNodes,
  required int index,
  double alignment = 0.2,
  Duration? duration,
  Curve curve = Curves.easeOut,
}) {
  if (!isMounted() || index < 0 || index >= focusNodes.length) return;
  final node = focusNodes[index];
  final effectiveDuration = duration ?? kDefaultScrollDuration;

  WidgetsBinding.instance.addPostFrameCallback((_) {
    if (!isMounted()) return;
    if (!node.hasFocus) {
      node.requestFocus();
    }
    final targetContext = node.context;
    if (targetContext == null) return;
    Scrollable.ensureVisible(
      targetContext,
      duration: effectiveDuration,
      curve: curve,
      alignment: alignment,
      alignmentPolicy: ScrollPositionAlignmentPolicy.explicit,
    );
  });
}

/// Scrolls the widget at [context] to [alignment] inside the nearest
/// scrollable, clamped to the scrollable's own bounds. Returns without doing
/// anything when the context has no scrollable ancestor or no render object.
void revealInNearestScrollable(
  BuildContext context, {
  required double alignment,
  Duration duration = const Duration(milliseconds: 200),
  Curve curve = Curves.easeOut,
}) {
  final scrollable = Scrollable.maybeOf(context);
  final renderObject = context.findRenderObject();
  if (scrollable == null || renderObject is! RenderBox) return;
  final position = scrollable.position;
  final target = RenderAbstractViewport.of(renderObject)
      .getOffsetToReveal(renderObject, alignment)
      .offset
      .clamp(position.minScrollExtent, position.maxScrollExtent)
      .toDouble();
  unawaited(
    position.animateTo(target, duration: duration, curve: curve),
  );
}

/// Steps a scrollable between the reveal extents of [context]'s widget:
/// [down] moves [step] further, never below the widget's own top edge and
/// never past its bottom edge. Returns false when there's no scrollable, no
/// render object, or the step wouldn't change the offset -- callers hand the
/// key on then.
bool stepScrollWithinContextBounds(
  BuildContext context, {
  required bool down,
  double step = 64.0,
  Duration duration = const Duration(milliseconds: 160),
}) {
  final scrollable = Scrollable.maybeOf(context);
  if (scrollable == null) return false;

  final position = scrollable.position;
  final renderObject = context.findRenderObject();
  if (renderObject is! RenderBox || !renderObject.attached) {
    return false;
  }

  final viewport = RenderAbstractViewport.of(renderObject);
  final lowerBound = viewport
      .getOffsetToReveal(renderObject, 0.0)
      .offset
      .clamp(position.minScrollExtent, position.maxScrollExtent)
      .toDouble();
  final upperBound = viewport
      .getOffsetToReveal(renderObject, 1.0)
      .offset
      .clamp(position.minScrollExtent, position.maxScrollExtent)
      .toDouble();
  if (upperBound - lowerBound < 0.5) {
    return false;
  }

  final delta = down ? step : -step;
  final target = (position.pixels + delta)
      .clamp(lowerBound, upperBound)
      .toDouble();
  if ((target - position.pixels).abs() < 0.5) {
    return false;
  }

  unawaited(
    position.animateTo(target, duration: duration, curve: Curves.easeOut),
  );
  return true;
}