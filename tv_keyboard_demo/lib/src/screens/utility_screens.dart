import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../focus/dpad_list_tile.dart';
import '../focus/input_mode_tracker.dart';
import '../theme/theme_model.dart';
import '../theme/theme_registry.dart';
import 'demo_settings.dart';

/// Settings for the standalone demo, including live focus/theme previews.
class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<ThemeScope>();
    final registry = scope?.notifier ?? ThemeRegistry.instance;
    final currentTheme = ThemeScope.of(context);
    final inputMode = InputModeTracker.of(context);
    final settings = DemoSettingsScope.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text('Appearance', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          DpadSwitchListTile(
            title: const Text('Force keyboard focus mode'),
            subtitle: Text('Current input mode: ${inputMode.name}'),
            value: InputModeTracker.focusModeForced,
            onChanged: InputModeTracker.setFocusModeForced,
          ),
          DpadSwitchListTile(
            title: const Text('Row glide'),
            subtitle: const Text('Spring-scroll horizontal home rows'),
            value: settings.rowGlideEnabled,
            onChanged: settings.setRowGlideEnabled,
          ),
          DpadSwitchListTile(
            title: const Text('Right-to-left layout'),
            subtitle: const Text('Preview mirrored row navigation'),
            value: settings.rtlEnabled,
            onChanged: settings.setRtlEnabled,
          ),
          const Divider(height: 32),
          Text(
            'Animation speed',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          DpadRadioGroup<DemoAnimationSpeed>(
            groupValue: settings.animationSpeed,
            onChanged: (value) {
              if (value != null) settings.setAnimationSpeed(value);
            },
            child: Column(
              children: [
                for (final speed in DemoAnimationSpeed.values)
                  DpadRadioListTile<DemoAnimationSpeed>(
                    value: speed,
                    title: Text(switch (speed) {
                      DemoAnimationSpeed.slow => 'Slow',
                      DemoAnimationSpeed.normal => 'Normal',
                      DemoAnimationSpeed.fast => 'Fast',
                    }),
                  ),
              ],
            ),
          ),
          Text('Focus style', style: Theme.of(context).textTheme.titleLarge),
          DpadRadioGroup<FocusStyle>(
            groupValue: settings.focusStyleOverride ?? currentTheme.focusStyle,
            onChanged: settings.setFocusStyleOverride,
            child: Column(
              children: [
                for (final style in FocusStyle.values)
                  DpadRadioListTile<FocusStyle>(
                    value: style,
                    title: Text(switch (style) {
                      FocusStyle.borderOnly => 'Border only',
                      FocusStyle.solidFill => 'Solid fill',
                      FocusStyle.glass => 'Glass',
                    }),
                  ),
              ],
            ),
          ),
          Text('Demo data', style: Theme.of(context).textTheme.titleLarge),
          DpadRadioGroup<DemoDataMode>(
            groupValue: settings.dataMode,
            onChanged: (value) {
              if (value != null) settings.setDataMode(value);
            },
            child: Column(
              children: [
                for (final mode in DemoDataMode.values)
                  DpadRadioListTile<DemoDataMode>(
                    value: mode,
                    title: Text(switch (mode) {
                      DemoDataMode.full => 'Full demo data',
                      DemoDataMode.boundary => 'Single long item',
                      DemoDataMode.empty => 'Empty state',
                    }),
                  ),
              ],
            ),
          ),
          const Divider(height: 32),
          Text('Theme', style: Theme.of(context).textTheme.titleLarge),
          const SizedBox(height: 8),
          for (var i = 0; i < registry.themes.length; i++)
            DpadListTile(
              selected: registry.themes[i].id == currentTheme.id,
              leading: const Icon(Icons.palette_outlined),
              title: Text(registry.themes[i].name),
              subtitle: Text(
                'Focus style: ${registry.themes[i].focusStyle.name}',
              ),
              trailing: registry.themes[i].id == currentTheme.id
                  ? const Icon(Icons.check_circle_outline)
                  : null,
              onTap: () async {
                final themeId = registry.themes[i].id;
                registry.switchTheme(themeId);
                final preferences = await SharedPreferences.getInstance();
                await preferences.setString('themeId', themeId);
              },
            ),
          const Divider(height: 40),
          ListTile(
            title: const Text('About'),
            subtitle: const Text('App information and keyboard shortcuts'),
            leading: const Icon(Icons.info_outline),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute<void>(builder: (_) => const AboutScreen()),
            ),
          ),
          ListTile(
            title: const Text('Dialog examples'),
            subtitle: const Text('Modal focus and confirmation flows'),
            leading: const Icon(Icons.chat_bubble_outline),
            trailing: const Icon(Icons.chevron_right),
            onTap: () => Navigator.of(context).push(
              MaterialPageRoute<void>(builder: (_) => const DialogScreen()),
            ),
          ),
        ],
      ),
    );
  }
}

/// Dialog examples demonstrating modal navigation and D-pad dismissal.
class DialogScreen extends StatelessWidget {
  const DialogScreen({super.key});

  Future<void> _show(BuildContext context, {required bool confirm}) async {
    if (confirm) {
      await showDialog<bool>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Confirm action'),
          content: const Text('This is a sample confirmation dialog.'),
          actions: [
            TextButton(
              autofocus: true,
              onPressed: () => Navigator.pop(context, false),
              child: const Text('Cancel'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(context, true),
              child: const Text('Continue'),
            ),
          ],
        ),
      );
    } else {
      await showDialog<void>(
        context: context,
        builder: (context) => AlertDialog(
          title: const Text('Information'),
          content: const Text('Dialogs keep focus within the active route.'),
          actions: [
            TextButton(
              autofocus: true,
              onPressed: () => Navigator.pop(context),
              child: const Text('Close'),
            ),
          ],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Dialogs')),
    body: ListView(
      padding: const EdgeInsets.all(24),
      children: [
        DpadListTile(
          autofocus: true,
          leading: const Icon(Icons.help_outline),
          title: const Text('Show information dialog'),
          onTap: () => _show(context, confirm: false),
        ),
        DpadListTile(
          leading: const Icon(Icons.warning_amber_outlined),
          title: const Text('Show confirmation dialog'),
          onTap: () => _show(context, confirm: true),
        ),
      ],
    ),
  );
}

/// Project details and a concise guide to remote controls.
class AboutScreen extends StatelessWidget {
  const AboutScreen({super.key});

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('About')),
    body: ListView(
      padding: const EdgeInsets.all(32),
      children: const [
        Icon(Icons.keyboard_alt_outlined, size: 64),
        SizedBox(height: 16),
        Text('Moonfin TV Keyboard Demo', textAlign: TextAlign.center),
        SizedBox(height: 8),
        Text(
          'A standalone Flutter app for exploring keyboard and D-pad focus on Android TV.',
          textAlign: TextAlign.center,
        ),
        SizedBox(height: 32),
        Text('Remote controls', style: TextStyle(fontWeight: FontWeight.bold)),
        Text('Use the directional pad to move focus and Select to activate.'),
        Text('Use Back or Escape to return to the previous screen.'),
        Text('Hold Select on supported rows to open their context action.'),
        SizedBox(height: 24),
        Text('Feature status', style: TextStyle(fontWeight: FontWeight.bold)),
        Text('✓ D-pad keys, gamepad keys, and input-mode tracking'),
        Text('✓ Horizontal rows, vertical lists, and two-way grids'),
        Text('✓ Long-press and context-menu actions'),
        Text('✓ Focus restoration, scroll helpers, and row glide'),
        Text('✓ Four runtime-switchable themes and focus styles'),
        Text('✓ RTL preview and empty/single-item demo data'),
      ],
    ),
  );
}
