import 'package:flutter/material.dart';

import '../focus/dpad_list_tile.dart';
import '../focus/focusable_wrapper.dart';
import '../focus/grid_focus_node_mixin.dart';
import '../focus/scroll_utils.dart';
import '../theme/theme_registry.dart';
import 'demo_settings.dart';

const demoTitles = <String>[
  'The Grand Adventure',
  'A Very Long Feature Title That Exercises Text Wrapping',
  'Moonlit Harbor',
  'Paper Satellites',
  'Wildwood 🌲',
  'After the Rain',
  'City Lights',
  'Deep Space',
  'Kitchen Stories',
  'The Long Way Home',
  'Hidden Rivers',
  'Étoile du matin',
];

class ListScreen extends StatefulWidget {
  const ListScreen({super.key});
  @override
  State<ListScreen> createState() => _ListScreenState();
}

class _ListScreenState extends State<ListScreen> {
  final ScrollController _scrollController = ScrollController();
  final Map<int, FocusNode> _focusNodes = <int, FocusNode>{};
  String _selected = 'Nothing selected';
  int _buildVersion = 0;
  int _lastFocusedIndex = 0;

  @override
  void dispose() {
    _scrollController.dispose();
    for (final node in _focusNodes.values) {
      node.dispose();
    }
    super.dispose();
  }

  FocusNode _focusNodeFor(int index) => _focusNodes.putIfAbsent(
    index,
    () => FocusNode(debugLabel: 'list-item-$index'),
  );

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || ModalRoute.isCurrentOf(context) == false) return;
      final items = _itemsFor(DemoSettingsScope.of(context).dataMode);
      if (items.isNotEmpty) _focusNodeFor(0).requestFocus();
    });
  }

  void _open(String title) {
    setState(() => _selected = title);
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text('Details for $title'),
        actions: [
          TextButton(
            autofocus: true,
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  void _scrollToFocusedItem(int index) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final context = FocusManager.instance.primaryFocus?.context;
      if (context == null) return;
      final down = index >= _lastFocusedIndex;
      _lastFocusedIndex = index;
      if (!stepScrollWithinContextBounds(context, down: down, step: 72)) {
        scrollFocusIntoView(context);
      }
    });
  }

  List<String> _itemsFor(DemoDataMode mode) => switch (mode) {
    DemoDataMode.full => demoTitles,
    DemoDataMode.boundary => <String>[
      'A Very Long List Item Title With Emoji 🚀 and accents é',
    ],
    DemoDataMode.empty => const <String>[],
  };

  @override
  Widget build(BuildContext context) {
    final items = _itemsFor(DemoSettingsScope.of(context).dataMode);
    return Scaffold(
      appBar: AppBar(
        title: const Text('List navigation'),
        actions: [
          Center(child: Text(_selected)),
          IconButton(
            tooltip: 'Refresh list',
            onPressed: () => setState(() => _buildVersion++),
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: items.isEmpty
          ? const Center(child: Text('No list items in this demo data mode.'))
          : ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(24),
              itemCount: items.length,
              itemBuilder: (context, index) => DpadListTile(
                key: ValueKey('list-$_buildVersion-$index'),
                focusNode: _focusNodeFor(index),
                autofocus: index == 0,
                onFocusChange: (focused) {
                  if (focused) _scrollToFocusedItem(index);
                },
                title: Text(items[index]),
                subtitle: Text('Demo item ${index + 1}'),
                onTap: () => _open(items[index]),
                onLongPress: () => showModalBottomSheet<void>(
                  context: context,
                  builder: (context) => SafeArea(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        ListTile(
                          autofocus: true,
                          leading: const Icon(Icons.info_outline),
                          title: Text(items[index]),
                          subtitle: const Text('Context menu actions'),
                        ),
                        ListTile(
                          leading: const Icon(Icons.bookmark_add_outlined),
                          title: const Text('Add to watchlist'),
                          onTap: () => Navigator.of(context).pop(),
                        ),
                        ListTile(
                          leading: const Icon(Icons.play_circle_outline),
                          title: const Text('Open details'),
                          onTap: () {
                            Navigator.of(context).pop();
                            _open(items[index]);
                          },
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
    );
  }
}

class GridScreen extends StatefulWidget {
  const GridScreen({super.key});
  @override
  State<GridScreen> createState() => _GridScreenState();
}

class _GridScreenState extends State<GridScreen>
    with GridFocusNodeMixin<GridScreen> {
  String? _selected;
  DemoDataMode? _lastDataMode;

  void _open(String title) {
    setState(() => _selected = title);
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text('Details for $title'),
        actions: [
          TextButton(
            autofocus: true,
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Close'),
          ),
        ],
      ),
    );
  }

  void _showContextMenu(String title) {
    showModalBottomSheet<void>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              autofocus: true,
              title: Text(title),
              subtitle: const Text('Grid context menu'),
            ),
            ListTile(
              leading: const Icon(Icons.info_outline),
              title: const Text('Open details'),
              onTap: () {
                Navigator.of(context).pop();
                _open(title);
              },
            ),
            ListTile(
              leading: const Icon(Icons.close),
              title: const Text('Close menu'),
              onTap: () => Navigator.of(context).pop(),
            ),
          ],
        ),
      ),
    );
  }

  List<String> _itemsFor(DemoDataMode mode) => switch (mode) {
    DemoDataMode.full => demoTitles,
    DemoDataMode.boundary => <String>[
      'A Very Long Grid Tile Title With Emoji 🌠 and accents é',
    ],
    DemoDataMode.empty => const <String>[],
  };

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || ModalRoute.isCurrentOf(context) == false) return;
      if (lastFocusedGridIndex == null &&
          GridFocusMemory.indexFor('GridScreen') == null) {
        gridItemFocusNodes[0]?.requestFocus();
      } else {
        restoreGridFocusIfNeeded();
      }
    });
  }

  @override
  void dispose() {
    disposeGridFocusNodes();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final settings = DemoSettingsScope.of(context);
    final items = _itemsFor(settings.dataMode);
    if (_lastDataMode != settings.dataMode) {
      _lastDataMode = settings.dataMode;
      gridContentVersion++;
      cleanupGridFocusNodes(items.length);
    }
    final theme = ThemeScope.of(context);
    final duration = Duration(
      milliseconds: (140 * settings.animationFactor).round(),
    );
    return Scaffold(
      appBar: AppBar(
        title: const Text('Grid navigation'),
        actions: [
          if (_selected != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Center(child: Text('Selected: $_selected')),
            ),
        ],
      ),
      body: items.isEmpty
          ? const Center(child: Text('No grid items in this demo data mode.'))
          : GridView.builder(
              padding: const EdgeInsets.all(24),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: 4,
                mainAxisSpacing: 16,
                crossAxisSpacing: 16,
                childAspectRatio: 1.35,
              ),
              itemCount: items.length,
              itemBuilder: (context, index) {
                final node = getGridItemFocusNode(index, prefix: 'library');
                return FocusableWrapper(
                  key: ValueKey('grid-$index'),
                  focusNode: node,
                  autofocus: index == 0,
                  autoScroll: true,
                  enableLongPress: true,
                  onSelect: () => _open(items[index]),
                  onLongPress: () => _showContextMenu(items[index]),
                  child: Builder(
                    builder: (context) => AnimatedContainer(
                      duration: duration,
                      padding: const EdgeInsets.all(16),
                      alignment: Alignment.centerLeft,
                      decoration: theme.focusDecoration(
                        focused: node.hasFocus,
                        styleOverride: settings.focusStyleOverride,
                      ),
                      child: Text(
                        items[index],
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ),
                );
              },
            ),
    );
  }
}
