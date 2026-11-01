import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../focus/locked_focus_row.dart';
import '../focus/input_mode_tracker.dart';
import '../focus/row_glide.dart';
import '../focus/row_focus_coordinator.dart';
import '../theme/theme_registry.dart';
import 'demo_settings.dart';
import 'list_grid_screens.dart';
import 'search_screen.dart';
import 'utility_screens.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final RowFocusCoordinator _coordinator = RowFocusCoordinator();
  final GlobalKey<LockedFocusRowState<String>> _featuredKey =
      GlobalKey<LockedFocusRowState<String>>();
  final GlobalKey<LockedFocusRowState<String>> _continueKey =
      GlobalKey<LockedFocusRowState<String>>();
  final GlobalKey<LockedFocusRowState<String>> _popularKey =
      GlobalKey<LockedFocusRowState<String>>();
  final FocusNode _navigationFocus = FocusNode(debugLabel: 'bottom-navigation');
  final List<RowGlide> _rowGlides = List<RowGlide>.generate(
    3,
    (_) => RowGlide()..growth = 28,
  );
  final List<FocusNode> _navItems = List<FocusNode>.generate(
    5,
    (index) => FocusNode(debugLabel: 'navigation-$index'),
  );
  int _selectedNavigation = 0;
  String _focusedLabel = '—';

  static const _featured = <String>[
    'The Grand Adventure',
    'Moonlit Harbor',
    'A Very Long Feature Title That Tests Wrapping',
    'Paper Satellites',
    'Wildwood',
    'After the Rain',
  ];
  static const _continueWatching = <String>[
    'Episode 4',
    'The Last Signal',
    'Northbound',
    'Tiny Worlds',
    'Blue Hour',
  ];
  static const _popular = <String>[
    'City Lights',
    'Deep Space',
    'Kitchen Stories',
    'The Long Way Home',
    'Wildlife: Earth',
    'Hidden Rivers',
  ];

  @override
  void dispose() {
    _navigationFocus.dispose();
    for (final glide in _rowGlides) {
      glide.dispose();
    }
    for (final node in _navItems) {
      node.dispose();
    }
    super.dispose();
  }

  void _rebuildEntries() {
    _coordinator.entries = <RowFocusEntry>[
      RowFocusEntry.row(_featuredKey),
      RowFocusEntry.row(_continueKey),
      RowFocusEntry.row(_popularKey),
      for (final node in _navItems) RowFocusEntry.node(node),
    ];
  }

  void _showDetails(String title) {
    setState(() => _focusedLabel = title);
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: const Text('Selected from the home row.'),
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
              subtitle: const Text('Home row context menu'),
            ),
            ListTile(
              leading: const Icon(Icons.info_outline),
              title: const Text('Open details'),
              onTap: () {
                Navigator.of(context).pop();
                _showDetails(title);
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

  Widget _row({
    required GlobalKey<LockedFocusRowState<String>> key,
    required String title,
    required List<String> items,
    required int entryIndex,
    required RowGlide glide,
    bool autofocus = false,
  }) => Padding(
    padding: const EdgeInsets.only(bottom: 20),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 8),
          child: Text(title, style: Theme.of(context).textTheme.titleLarge),
        ),
        SizedBox(
          height: 128,
          child: LockedFocusRow<String>(
            key: key,
            hubKey: title,
            itemExtent: 210,
            itemSpacing: 16,
            height: 128,
            autofocus: autofocus,
            items: items,
            onVerticalNavigation: (isUp) =>
                _coordinator.moveVertical(fromIndex: entryIndex, isUp: isUp),
            onIndexChanged: (_, item) {
              if (mounted) setState(() => _focusedLabel = item);
            },
            onFocusChange: (focused) {
              if (focused) {
                setState(
                  () => _focusedLabel = items.isEmpty
                      ? '$title (empty)'
                      : items.first,
                );
              }
            },
            onTap: (_, item) => _showDetails(item),
            onLongPress: (_, item) => _showContextMenu(item),
            glide: DemoSettingsScope.of(context).rowGlideEnabled ? glide : null,
            itemBuilder: (context, item, index, focused) {
              if (!DemoSettingsScope.of(context).rowGlideEnabled) {
                return HomeRowCard(title: item, focused: focused);
              }
              return ListenableBuilder(
                listenable: glide,
                builder: (context, _) => HomeRowCard(
                  title: item,
                  focused: focused,
                  width: 210 + glide.growth * glide.openness(index),
                ),
              );
            },
          ),
        ),
      ],
    ),
  );

  void _onNavigation(int index) {
    setState(() => _selectedNavigation = index);
    if (index != 0) {
      final Widget screen = switch (index) {
        1 => const ListScreen(),
        2 => const GridScreen(),
        3 => const SearchScreen(),
        _ => const SettingsScreen(),
      };
      Navigator.of(
        context,
      ).push(MaterialPageRoute<void>(builder: (_) => screen));
    }
  }

  @override
  Widget build(BuildContext context) {
    _rebuildEntries();
    final navLabels = ['Home', 'Lists', 'Grid', 'Search', 'Settings'];
    final settings = DemoSettingsScope.of(context);
    List<String> sampleItems(List<String> full, String emptyLabel) =>
        switch (settings.dataMode) {
          DemoDataMode.full => full,
          DemoDataMode.boundary => <String>[
            'A Very Long Title With Emoji 🌌 and RTL אבג',
          ],
          DemoDataMode.empty => <String>['No $emptyLabel items'],
        };
    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(32, 24, 32, 8),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      'Moonfin TV Keyboard Demo',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.headlineMedium,
                    ),
                  ),
                  SizedBox(
                    width: 260,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          'Input: ${InputModeTracker.of(context).name}',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          'Focused: $_focusedLabel',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.only(top: 8),
                children: [
                  _row(
                    key: _featuredKey,
                    title: 'Featured',
                    items: sampleItems(_featured, 'Featured'),
                    entryIndex: 0,
                    autofocus: true,
                    glide: _rowGlides[0],
                  ),
                  _row(
                    key: _continueKey,
                    title: 'Continue Watching',
                    items: sampleItems(_continueWatching, 'Continue Watching'),
                    entryIndex: 1,
                    glide: _rowGlides[1],
                  ),
                  _row(
                    key: _popularKey,
                    title: 'Popular Now',
                    items: sampleItems(_popular, 'Popular Now'),
                    entryIndex: 2,
                    glide: _rowGlides[2],
                  ),
                ],
              ),
            ),
            Focus(
              focusNode: _navigationFocus,
              onFocusChange: (focused) {
                if (focused) {
                  setState(() => _focusedLabel = 'Bottom navigation');
                }
              },
              child: Padding(
                padding: const EdgeInsets.fromLTRB(24, 8, 24, 16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    for (var i = 0; i < navLabels.length; i++)
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 8),
                        child: Focus(
                          focusNode: _navItems[i],
                          onFocusChange: (focused) {
                            if (focused) {
                              setState(() {
                                _selectedNavigation = i;
                                _focusedLabel = 'Navigation: ${navLabels[i]}';
                              });
                            }
                          },
                          onKeyEvent: (_, event) {
                            if ((event is KeyDownEvent ||
                                    event is KeyRepeatEvent) &&
                                (event.logicalKey ==
                                        LogicalKeyboardKey.arrowLeft ||
                                    event.logicalKey ==
                                        LogicalKeyboardKey.arrowRight)) {
                              final isRtl =
                                  Directionality.of(context) ==
                                  TextDirection.rtl;
                              final movesForward =
                                  (event.logicalKey ==
                                      LogicalKeyboardKey.arrowRight) !=
                                  isRtl;
                              final nextIndex = i + (movesForward ? 1 : -1);
                              if (nextIndex >= 0 &&
                                  nextIndex < _navItems.length) {
                                _navItems[nextIndex].requestFocus();
                              }
                              return KeyEventResult.handled;
                            }
                            if (event is KeyDownEvent &&
                                (event.logicalKey ==
                                        LogicalKeyboardKey.select ||
                                    event.logicalKey ==
                                        LogicalKeyboardKey.enter)) {
                              _onNavigation(i);
                              return KeyEventResult.handled;
                            }
                            if (event is KeyDownEvent &&
                                event.logicalKey ==
                                    LogicalKeyboardKey.arrowUp) {
                              _coordinator.focusFirst();
                              return KeyEventResult.handled;
                            }
                            return KeyEventResult.ignored;
                          },
                          child: OutlinedButton(
                            onPressed: () => _onNavigation(i),
                            style: OutlinedButton.styleFrom(
                              backgroundColor: _selectedNavigation == i
                                  ? Theme.of(
                                      context,
                                    ).colorScheme.primaryContainer
                                  : null,
                            ),
                            child: Text(navLabels[i]),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// A theme-aware card used in the horizontal home rows.
class HomeRowCard extends StatelessWidget {
  const HomeRowCard({
    super.key,
    required this.title,
    required this.focused,
    this.width = 210,
  });

  final String title;
  final bool focused;
  final double width;

  @override
  Widget build(BuildContext context) {
    final theme = ThemeScope.of(context);
    final settings = DemoSettingsScope.of(context);
    final duration = Duration(
      milliseconds:
          (theme.focusAnimationDuration.inMilliseconds *
                  settings.animationFactor)
              .round(),
    );
    return AnimatedScale(
      scale: focused ? theme.focusScale : 1,
      duration: duration,
      child: SizedBox(
        width: width,
        child: AnimatedContainer(
          duration: duration,
          alignment: Alignment.centerLeft,
          padding: const EdgeInsets.all(16),
          decoration: theme.focusDecoration(
            focused: focused,
            styleOverride: settings.focusStyleOverride,
          ),
          child: Text(
            title,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              color: theme.resolveForegroundColor(focused: focused),
            ),
          ),
        ),
      ),
    );
  }
}
