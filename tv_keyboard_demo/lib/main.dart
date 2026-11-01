import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'src/theme/theme_model.dart';
import 'src/theme/theme_registry.dart';

void main() {
  runApp(const TvKeyboardDemoApp());
}

/// Root widget of the standalone TV keyboard demo app.
///
/// The app intentionally starts on a placeholder screen so the project
/// structure, analysis setup and build pipeline can be verified end to end
/// before any of the D-pad focus infrastructure from the spec lands.
///
/// [registry] defaults to [ThemeRegistry.instance]; passing a custom one
/// (with its own themes) is supported for tests and embedding.
class TvKeyboardDemoApp extends StatelessWidget {
  const TvKeyboardDemoApp({super.key, this.registry});

  final ThemeRegistry? registry;

  ThemeRegistry get _themeRegistry => registry ?? ThemeRegistry.instance;

  @override
  Widget build(BuildContext context) {
    return ThemeScope(
      registry: _themeRegistry,
      child: ListenableBuilder(
        listenable: _themeRegistry,
        builder: (BuildContext context, Widget? child) => MaterialApp(
          title: 'TV Keyboard Demo',
          debugShowCheckedModeBanner: false,
          theme: _themeRegistry.current.toMaterialTheme(),
          home: child,
        ),
        child: const HomeScreen(),
      ),
    );
  }
}

/// Placeholder home screen shown on first run.
///
/// Deliberately simple, but D-pad friendly: a 3-column grid of focusable
/// tiles that rely on Flutter's default focus traversal, so up/down/left/
/// right and select already work with an Android TV remote.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  static const int _tileCount = 9;

  String _focusedLabel = '—';

  void _handleFocusChange(String label, bool focused) {
    if (focused) {
      setState(() => _focusedLabel = label);
    }
  }

  Future<void> _handleSelect(String label) async {
    await showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(label),
        content: const Text(
          'Select handled. This placeholder will be replaced by the demo '
          'screens from the TV keyboard spec.',
        ),
        actions: [
          TextButton(
            autofocus: true,
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    return Scaffold(
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text('TV Keyboard Demo', style: theme.textTheme.displaySmall),
                  const SizedBox(height: 8),
                  Text(
                    'Placeholder home screen. Use the D-pad to move focus '
                    'across the tiles, select to open the demo dialog.',
                    style: theme.textTheme.bodyLarge?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Focused: $_focusedLabel',
                    style: theme.textTheme.titleMedium?.copyWith(
                      color: theme.colorScheme.primary,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: GridView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 3,
                  mainAxisSpacing: 12,
                  crossAxisSpacing: 12,
                ),
                itemCount: _tileCount,
                itemBuilder: (BuildContext context, int index) {
                  final String label = 'Demo tile ${index + 1}';
                  return DemoTile(
                    label: label,
                    autofocus: index == 0,
                    onFocusChange: (bool focused) =>
                        _handleFocusChange(label, focused),
                    onSelect: () => _handleSelect(label),
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// A single D-pad-selectable tile used on the placeholder home screen.
class DemoTile extends StatefulWidget {
  const DemoTile({
    super.key,
    required this.label,
    required this.autofocus,
    required this.onFocusChange,
    required this.onSelect,
  });

  final String label;
  final bool autofocus;
  final ValueChanged<bool> onFocusChange;
  final VoidCallback onSelect;

  @override
  State<DemoTile> createState() => _DemoTileState();
}

class _DemoTileState extends State<DemoTile> {
  final FocusNode _focusNode = FocusNode(debugLabel: 'demo-tile');
  bool _focused = false;

  @override
  void dispose() {
    _focusNode.dispose();
    super.dispose();
  }

  KeyEventResult _handleKeyEvent(FocusNode node, KeyEvent event) {
    if (event is! KeyDownEvent && event is! KeyRepeatEvent) {
      return KeyEventResult.ignored;
    }
    if (event.logicalKey == LogicalKeyboardKey.select ||
        event.logicalKey == LogicalKeyboardKey.enter ||
        event.logicalKey == LogicalKeyboardKey.space) {
      widget.onSelect();
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);

    return Focus(
      focusNode: _focusNode,
      autofocus: widget.autofocus,
      onFocusChange: (bool focused) {
        setState(() => _focused = focused);
        widget.onFocusChange(focused);
      },
      onKeyEvent: _handleKeyEvent,
      child: Builder(
        builder: (BuildContext context) {
          final ThemeModel themeModel = ThemeModel.of(context);
          return AnimatedScale(
            scale: _focused ? themeModel.focusScale : 1.0,
            duration: themeModel.focusAnimationDuration,
            child: AnimatedContainer(
              duration: themeModel.focusAnimationDuration,
              alignment: Alignment.center,
              decoration: themeModel.focusDecoration(focused: _focused),
              child: Text(
                widget.label,
                style: theme.textTheme.titleMedium?.copyWith(
                  color: themeModel.resolveForegroundColor(focused: _focused),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
