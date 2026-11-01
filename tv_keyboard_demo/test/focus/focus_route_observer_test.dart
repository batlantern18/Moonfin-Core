import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tv_keyboard_demo/src/focus/focus_route_observer.dart';
import 'package:tv_keyboard_demo/src/focus/platform_detection.dart';

void main() {
  setUp(TvPlatformDetection.reset);
  tearDown(TvPlatformDetection.reset);

  testWidgets('on TV, a pushed route lands on its first focusable child', (
    tester,
  ) async {
    TvPlatformDetection.setTvMode(true);
    final observer = FocusRouteObserver();
    await tester.pumpWidget(
      MaterialApp(
        navigatorObservers: [observer],
        home: Scaffold(
          body: Column(
            children: <Widget>[
              const Focus(
                debugLabel: 'home-row',
                child: ListTile(title: Text('home')),
              ),
              TextButton(
                child: const Text('next'),
                onPressed: () => Navigator.of(_homeContext(tester)).push(
                  MaterialPageRoute<void>(
                    builder: (_) => Scaffold(
                      body: Focus(
                        debugLabel: 'screenB-row',
                        child: ListTile(title: Text('screen b')),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('next'));
    await tester.pumpAndSettle();

    // Nothing on screen b autofocused; the observer granted the first
    // focusable descendant of the new route.
    expect(FocusManager.instance.primaryFocus?.debugLabel, 'screenB-row');
  });

  testWidgets('on TV, popping a route hands focus back to the page below', (
    tester,
  ) async {
    TvPlatformDetection.setTvMode(true);
    final observer = FocusRouteObserver();
    late BuildContext originalContext;

    await tester.pumpWidget(
      MaterialApp(
        navigatorObservers: [observer],
        home: Scaffold(
          body: Column(
            children: <Widget>[
              const Focus(
                debugLabel: 'home-row',
                child: ListTile(title: Text('home')),
              ),
              TextButton(
                child: const Text('next'),
                onPressed: () => Navigator.of(originalContext).push(
                  MaterialPageRoute<void>(
                    builder: (dialogContext) => Scaffold(
                      body: Focus(
                        debugLabel: 'screenB-row',
                        child: Center(
                          child: TextButton(
                            child: const Text('pop'),
                            onPressed: () => Navigator.of(dialogContext).pop(),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    originalContext = _homeContext(tester);

    await tester.tap(find.text('next'));
    await tester.pumpAndSettle();
    // The first content stop can be the button inside the route's wrapper;
    // assert focus entered the route, without requiring the wrapper's label.
    expect(FocusManager.instance.primaryFocus, isNotNull);
    expect(FocusManager.instance.primaryFocus?.debugLabel, isNot('home-row'));

    await tester.tap(find.text('pop'));
    await tester.pumpAndSettle();
    expect(FocusManager.instance.primaryFocus?.debugLabel, 'home-row');
  });

  testWidgets('away from TV, pushed routes are left alone', (tester) async {
    final observer = FocusRouteObserver();
    late BuildContext originalContext;

    await tester.pumpWidget(
      MaterialApp(
        navigatorObservers: [observer],
        home: Scaffold(
          body: Column(
            children: <Widget>[
              const Focus(
                debugLabel: 'home-row',
                child: ListTile(title: Text('home')),
              ),
              TextButton(
                child: const Text('next'),
                onPressed: () => Navigator.of(originalContext).push(
                  MaterialPageRoute<void>(
                    builder: (_) => Scaffold(
                      body: Focus(
                        debugLabel: 'screenB-row',
                        child: ListTile(title: Text('screen b')),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    originalContext = _homeContext(tester);

    await tester.tap(find.text('next'));
    await tester.pumpAndSettle();
    // On a phone nothing grabs focus, so keyboard never pops open.
    expect(
      FocusManager.instance.primaryFocus?.debugLabel,
      isNot('screenB-row'),
    );
  });

  testWidgets('canClaimInitialFocus is false under a modal', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: Builder(
            key: const ValueKey('landing'),
            builder: (context) {
              return TextButton(
                onPressed: () => showDialog<void>(
                  context: context,
                  builder: (_) => const AlertDialog(title: Text('modal')),
                ),
                child: const Text('open'),
              );
            },
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    expect(
      canClaimInitialFocus(
        tester.element(find.byKey(const ValueKey('landing'))),
      ),
      isFalse,
    );
  });
}

BuildContext _homeContext(WidgetTester tester) =>
    tester.element(find.text('home').first);
