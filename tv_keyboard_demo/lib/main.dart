import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'src/focus/input_mode_tracker.dart';
import 'src/focus/focus_route_observer.dart';
import 'src/screens/demo_settings.dart';
import 'src/screens/home_screen.dart';
import 'src/theme/theme_registry.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final preferences = await SharedPreferences.getInstance();
  runApp(
    TvKeyboardDemoApp(
      registry: ThemeRegistry(initialThemeId: preferences.getString('themeId')),
    ),
  );
}

/// Root widget of the standalone TV keyboard demo app.
///
/// [registry] defaults to [ThemeRegistry.instance]; passing a custom one
/// (with its own themes) is supported for tests and embedding.
class TvKeyboardDemoApp extends StatelessWidget {
  const TvKeyboardDemoApp({super.key, this.registry, this.settings});

  final ThemeRegistry? registry;
  final DemoSettings? settings;

  ThemeRegistry get _themeRegistry => registry ?? ThemeRegistry.instance;
  DemoSettings get _demoSettings => settings ?? DemoSettings.instance;

  @override
  Widget build(BuildContext context) {
    final themeRegistry = _themeRegistry;
    final demoSettings = _demoSettings;
    return InputModeTracker(
      child: DemoSettingsScope(
        settings: demoSettings,
        child: ThemeScope(
          registry: themeRegistry,
          child: ListenableBuilder(
            listenable: themeRegistry,
            builder: (BuildContext context, Widget? home) => ListenableBuilder(
              listenable: demoSettings,
              builder: (BuildContext context, Widget? child) => MaterialApp(
                title: 'TV Keyboard Demo',
                debugShowCheckedModeBanner: false,
                navigatorObservers: <NavigatorObserver>[FocusRouteObserver()],
                theme: themeRegistry.current.toMaterialTheme(),
                home: home,
                builder: (context, routedContent) => Directionality(
                  textDirection: demoSettings.rtlEnabled
                      ? TextDirection.rtl
                      : TextDirection.ltr,
                  child: routedContent ?? const SizedBox.shrink(),
                ),
              ),
              child: home,
            ),
            child: const HomeScreen(),
          ),
        ),
      ),
    );
  }
}
