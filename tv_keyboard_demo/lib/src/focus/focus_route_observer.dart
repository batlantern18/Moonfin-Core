import 'dart:async';

import 'package:flutter/widgets.dart';

import 'platform_detection.dart';

/// Grants initial focus to the first focusable content descendant of a
/// freshly pushed (or popped-back-to) route, so the d-pad always has
/// somewhere to start. Works around Flutter Navigator's default of not
/// auto-focusing any child of a new route's FocusScope.
///
/// Replicated from Moonfin-Core's FocusRouteObserver. Only TV runs it: a
/// mouse never needs focus handed to it, and on phones landing on the first
/// widget pops the keyboard open on any screen that opens with a text field.
class FocusRouteObserver extends NavigatorObserver {
  static const _maxAttempts = 8;
  static const _retryDelay = Duration(milliseconds: 50);

  Timer? _retryTimer;

  @override
  void didPush(Route<dynamic> route, Route<dynamic>? previousRoute) {
    _scheduleFocus(route);
  }

  @override
  void didPop(Route<dynamic> route, Route<dynamic>? previousRoute) {
    if (previousRoute != null) _scheduleFocus(previousRoute);
  }

  @override
  void didReplace({Route<dynamic>? newRoute, Route<dynamic>? oldRoute}) {
    if (newRoute != null) _scheduleFocus(newRoute);
  }

  @override
  void didRemove(Route<dynamic> route, Route<dynamic>? previousRoute) {}

  void _scheduleFocus(Route<dynamic> route) {
    if (!TvPlatformDetection.isTV) return;
    _retryTimer?.cancel();
    WidgetsBinding.instance.addPostFrameCallback((_) => _tryFocus(route, 0));
  }

  void _tryFocus(Route<dynamic> route, int attempt) {
    final modal = route is ModalRoute ? route : null;
    final ctx = modal?.subtreeContext;
    if (ctx == null || !ctx.mounted) return;

    final scope = FocusScope.of(ctx);
    if (_scopeHasFocusedDescendant(scope)) return;

    final target = _firstContentDescendant(scope);
    if (target != null) {
      scope.requestFocus(target);
    }

    if (_scopeHasFocusedDescendant(scope)) return;
    if (attempt + 1 >= _maxAttempts) return;
    _retryTimer = Timer(
      _retryDelay,
      () => _tryFocus(route, attempt + 1),
    );
  }

  /// First focusable in tree order. The demo has no navigation chrome, so
  /// like Moonfin's own leaf-only screens this is the plain first focusable.
  FocusNode? _firstContentDescendant(FocusScopeNode scope) {
    for (final node in scope.traversalDescendants) {
      if (node is FocusScopeNode) continue;
      if (node.context == null) continue;
      return node;
    }
    return null;
  }

  bool _scopeHasFocusedDescendant(FocusScopeNode scope) {
    if (scope.focusedChild != null) return true;
    final primary = FocusManager.instance.primaryFocus;
    if (primary == null || identical(primary, scope)) return false;
    FocusNode? node = primary;
    while (node != null) {
      if (identical(node, scope)) return true;
      node = node.parent;
    }
    return false;
  }

  /// Cancels a pending retry; call when the owning app shuts down.
  void dispose() {
    _retryTimer?.cancel();
  }
}

/// Whether a screen may take focus for itself right now.
///
/// False while something sits on top of it. A modal takes the focus with it,
/// and a screen underneath that grabs it back pulls the remote out from under
/// whatever the user is actually looking at.
///
/// A context with no route counts as claimable, so a widget pumped on its own
/// keeps the behaviour it had. Replicated from Moonfin-Core's
/// can_claim_initial_focus.dart.
bool canClaimInitialFocus(BuildContext context) =>
    ModalRoute.isCurrentOf(context) ?? true;
