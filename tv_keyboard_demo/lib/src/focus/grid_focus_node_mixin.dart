import 'package:flutter/widgets.dart';

/// Position memory survives pushing a new screen instance after returning to
/// the app's home route. Content versions remain local to each screen state.
abstract final class GridFocusMemory {
  static final Map<String, int> _indices = <String, int>{};

  static int? indexFor(String key) => _indices[key];

  static void remember(String key, int index) => _indices[key] = index;

  static void clear() => _indices.clear();
}

/// State mixin for a grid screen whose tiles each own one FocusNode.
///
/// Keeps the nodes in a map by position, remembers the last focused index
/// together with a content version the screen advances whenever its item
/// list changes, and restores focus onto the remembered tile after the list
/// changes — position-clamped, since the grid may have shrunk. Replicated
/// from Moonfin-Core's grid_focus_node_mixin.dart.
mixin GridFocusNodeMixin<T extends StatefulWidget> on State<T> {
  final Map<int, FocusNode> gridItemFocusNodes = {};
  final Map<int, VoidCallback> _gridItemListeners = {};
  int? lastFocusedGridIndex;
  int gridContentVersion = 0;
  int lastFocusedGridContentVersion = 0;

  String get _gridMemoryKey => T.toString();

  FocusNode getGridItemFocusNode(int index, {String prefix = 'grid_item'}) {
    final existing = gridItemFocusNodes[index];
    if (existing != null) return existing;
    final node = FocusNode(debugLabel: '$prefix:$index');
    void listener() => trackGridItemFocus(index, node.hasFocus);
    node.addListener(listener);
    gridItemFocusNodes[index] = node;
    _gridItemListeners[index] = listener;
    return node;
  }

  void trackGridItemFocus(int index, bool hasFocus) {
    if (!hasFocus) return;
    lastFocusedGridIndex = index;
    lastFocusedGridContentVersion = gridContentVersion;
    GridFocusMemory.remember(_gridMemoryKey, index);
  }

  void cleanupGridFocusNodes(int currentCount) {
    final toRemove = gridItemFocusNodes.keys
        .where((i) => i >= currentCount)
        .toList(growable: false);
    for (final i in toRemove) {
      final node = gridItemFocusNodes.remove(i);
      final listener = _gridItemListeners.remove(i);
      if (node != null) {
        if (listener != null) node.removeListener(listener);
        node.dispose();
      }
    }
  }

  void disposeGridFocusNodes() {
    for (final entry in gridItemFocusNodes.entries) {
      final listener = _gridItemListeners[entry.key];
      if (listener != null) entry.value.removeListener(listener);
      entry.value.dispose();
    }
    gridItemFocusNodes.clear();
    _gridItemListeners.clear();
  }

  void restoreGridFocusIfNeeded() {
    if (ModalRoute.of(context)?.isCurrent != true) return;
    if (lastFocusedGridContentVersion == gridContentVersion) return;
    final idx =
        lastFocusedGridIndex ?? GridFocusMemory.indexFor(_gridMemoryKey);
    if (idx == null) return;
    // The grid may have shrunk, so fall back to the last available row when
    // the remembered index no longer exists.
    var node = gridItemFocusNodes[idx];
    if (node == null && gridItemFocusNodes.isNotEmpty) {
      final maxIndex = gridItemFocusNodes.keys.reduce((a, b) => a > b ? a : b);
      node = gridItemFocusNodes[maxIndex];
    }
    if (node != null && node.canRequestFocus) {
      final restoredIndex = gridItemFocusNodes.entries
          .firstWhere((entry) => identical(entry.value, node))
          .key;
      lastFocusedGridIndex = restoredIndex;
      GridFocusMemory.remember(_gridMemoryKey, restoredIndex);
      node.requestFocus();
    }
    lastFocusedGridContentVersion = gridContentVersion;
  }
}
