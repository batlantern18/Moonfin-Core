import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/grid_focus_node_mixin.dart';

/// Host widget that exercises the grid mixin exactly as a screen would:
/// one FocusNode per tile, focus listeners tracking the last tile, and
/// restore called after content changes.
class _GridHost extends StatefulWidget {
  const _GridHost({super.key, required this.count});

  final int count;

  @override
  State<_GridHost> createState() => _GridHostState();
}

class _GridHostState extends State<_GridHost>
    with GridFocusNodeMixin<_GridHost> {
  @override
  void dispose() {
    disposeGridFocusNodes();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    cleanupGridFocusNodes(widget.count);
    return Column(
      children: <Widget>[
        for (var i = 0; i < widget.count; i++)
          Focus(
            key: ValueKey('tile-$i'),
            focusNode: getGridItemFocusNode(i, prefix: 'grid_host'),
            child: SizedBox(height: 50, child: Text('tile $i')),
          ),
      ],
    );
  }
}

void main() {
  setUp(GridFocusMemory.clear);

  testWidgets('per-position focus nodes survive rebuilds by position', (
    tester,
  ) async {
    const hostKey = ValueKey('host');
    Widget host(int count) => MaterialApp(
      home: _GridHost(key: hostKey, count: count),
    );

    await tester.pumpWidget(host(6));
    await tester.pumpAndSettle();

    final state = tester.state<_GridHostState>(find.byKey(hostKey));
    final node2 = state.getGridItemFocusNode(2);
    node2.requestFocus();
    await tester.pumpAndSettle();

    expect(FocusManager.instance.primaryFocus, node2);
    expect(state.lastFocusedGridIndex, 2);
    expect(state.lastFocusedGridContentVersion, state.gridContentVersion);

    // Same position, same node after a rebuild.
    await tester.pumpWidget(host(6));
    await tester.pumpAndSettle();
    expect(state.getGridItemFocusNode(2), same(node2));
  });

  testWidgets('restore refocuses the remembered tile after content changes', (
    tester,
  ) async {
    const hostKey = ValueKey('host');
    Widget host(int count) => MaterialApp(
      home: _GridHost(key: hostKey, count: count),
    );

    await tester.pumpWidget(host(6));
    await tester.pumpAndSettle();
    final state = tester.state<_GridHostState>(find.byKey(hostKey));

    state.getGridItemFocusNode(4).requestFocus();
    await tester.pumpAndSettle();
    expect(state.lastFocusedGridIndex, 4);

    // Content rotates: nothing is focused any more, then restore brings
    // tile 4 back.
    state.requestFocusCleared();
    await tester.pumpAndSettle();

    state.gridContentVersion = 1;
    state.restoreGridFocusIfNeeded();
    await tester.pumpAndSettle();
    expect(FocusManager.instance.primaryFocus, state.getGridItemFocusNode(4));

    // A later version with no content change restores nothing.
    state.gridContentVersion = 1;
    state.restoreGridFocusIfNeeded();
    expect(FocusManager.instance.primaryFocus, state.getGridItemFocusNode(4));
  });

  testWidgets('restore clamps onto the last tile the shrunken grid still has', (
    tester,
  ) async {
    const hostKey = ValueKey('host');
    Widget host(int count) => MaterialApp(
      home: _GridHost(key: hostKey, count: count),
    );

    await tester.pumpWidget(host(6));
    final state = tester.state<_GridHostState>(find.byKey(hostKey));

    state.getGridItemFocusNode(5).requestFocus();
    await tester.pumpAndSettle();

    // Shrink the grid: nodes 5 are disposed of by cleanup, so restore
    // falls back to the last remaining position.
    await tester.pumpWidget(host(3));
    await tester.pumpAndSettle();
    state.gridContentVersion = 1;
    state.restoreGridFocusIfNeeded();
    await tester.pumpAndSettle();

    expect(FocusManager.instance.primaryFocus, state.getGridItemFocusNode(2));
  });

  testWidgets('cleanup disposes the nodes past the new count', (tester) async {
    const hostKey = ValueKey('host');
    await tester.pumpWidget(
      MaterialApp(home: _GridHost(key: hostKey, count: 6)),
    );
    final state = tester.state<_GridHostState>(find.byKey(hostKey));
    state.getGridItemFocusNode(5);
    expect(state.gridItemFocusNodes.length, 6);

    await tester.pumpWidget(
      MaterialApp(home: _GridHost(key: hostKey, count: 3)),
    );
    // Build's cleanup pruned the tail.
    expect(state.gridItemFocusNodes.keys.toSet(), {0, 1, 2});
  });

  testWidgets('the focused position survives creating a new grid screen', (
    tester,
  ) async {
    Widget host(int count) => MaterialApp(home: _GridHost(count: count));

    await tester.pumpWidget(host(6));
    await tester.pumpAndSettle();
    final first = tester.state<_GridHostState>(find.byType(_GridHost));
    first.getGridItemFocusNode(3).requestFocus();
    await tester.pumpAndSettle();
    expect(GridFocusMemory.indexFor('_GridHost'), 3);

    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pumpWidget(host(6));
    await tester.pumpAndSettle();
    final returned = tester.state<_GridHostState>(find.byType(_GridHost));
    returned.gridContentVersion = 1;
    returned.restoreGridFocusIfNeeded();
    await tester.pumpAndSettle();

    expect(returned.lastFocusedGridIndex, 3);
    expect(
      FocusManager.instance.primaryFocus,
      returned.getGridItemFocusNode(3),
    );
  });
}

extension _GridHostTestHelper on _GridHostState {
  /// Defocuses everything the host holds, the way a modal taking over would.
  void requestFocusCleared() {
    for (final node in gridItemFocusNodes.values) {
      if (node.hasFocus) node.unfocus();
    }
  }
}
