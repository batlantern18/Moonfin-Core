import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../focus/dpad_list_tile.dart';

/// Small search surface for testing D-pad focus on a text field and results.
class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  final TextEditingController _controller = TextEditingController();
  final List<FocusNode> _resultFocusNodes = List<FocusNode>.generate(
    6,
    (index) => FocusNode(debugLabel: 'search-result-$index'),
  );
  static const _titles = <String>[
    'The Grand Adventure',
    'Moonlit Harbor',
    'Paper Satellites',
    'Northbound',
    'Wildlife: Earth',
    '🌌 Beyond the Stars',
  ];

  @override
  void dispose() {
    _controller.dispose();
    for (final node in _resultFocusNodes) {
      node.dispose();
    }
    super.dispose();
  }

  void _showDetails(String title) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: const Text('Selected from search results.'),
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

  @override
  Widget build(BuildContext context) {
    final query = _controller.text.trim().toLowerCase();
    final results = _titles
        .where((title) => title.toLowerCase().contains(query))
        .toList(growable: false);

    return Scaffold(
      appBar: AppBar(title: const Text('Search')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Focus(
            skipTraversal: true,
            onKeyEvent: (_, event) {
              if ((event is KeyDownEvent || event is KeyRepeatEvent) &&
                  event.logicalKey == LogicalKeyboardKey.arrowDown &&
                  results.isNotEmpty) {
                _resultFocusNodes[_titles.indexOf(results.first)]
                    .requestFocus();
                return KeyEventResult.handled;
              }
              return KeyEventResult.ignored;
            },
            child: TextField(
              autofocus: true,
              controller: _controller,
              textInputAction: TextInputAction.search,
              decoration: const InputDecoration(
                labelText: 'Search titles',
                prefixIcon: Icon(Icons.search),
                border: OutlineInputBorder(),
              ),
              onChanged: (_) => setState(() {}),
            ),
          ),
          const SizedBox(height: 16),
          if (results.isEmpty)
            const ListTile(title: Text('No matching titles.'))
          else
            for (final title in results)
              DpadListTile(
                focusNode: _resultFocusNodes[_titles.indexOf(title)],
                leading: const Icon(Icons.movie_outlined),
                title: Text(title),
                onTap: () => _showDetails(title),
              ),
        ],
      ),
    );
  }
}
