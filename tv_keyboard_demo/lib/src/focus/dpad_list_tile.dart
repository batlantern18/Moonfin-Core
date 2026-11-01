import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'dpad_keys.dart';
import 'glass_focus_halo.dart';
import 'platform_detection.dart';

/// A list tile with a visible keyboard/D-pad focus state and reliable select
/// activation on TV remotes.
///
/// On TV devices the row routes select through a Focus handler, because a
/// remote's select card does not map every platform to an InkWell tap. Away
/// from TV the plain [ListTile] is returned so pointer platforms keep their
/// native behavior. Replicated from Moonfin-Core's DpadListTile for the demo.
class DpadListTile extends StatelessWidget {
  const DpadListTile({
    super.key,
    required this.title,
    this.focusNode,
    this.autofocus = false,
    this.enabled = true,
    this.leading,
    this.subtitle,
    this.trailing,
    this.contentPadding,
    this.selected,
    this.focusColor,
    this.onFocusChange,
    this.onTap,
    this.onLongPress,
  });

  final FocusNode? focusNode;
  final bool autofocus;
  final bool enabled;
  final Widget? leading;
  final Widget title;
  final Widget? subtitle;
  final Widget? trailing;
  final EdgeInsetsGeometry? contentPadding;

  /// Marks the row visually selected (radio tiles show their check this way).
  final bool? selected;

  /// The highlight color used while the row is focused on TV.
  final Color? focusColor;
  final ValueChanged<bool>? onFocusChange;

  final VoidCallback? onTap;
  final VoidCallback? onLongPress;

  Widget _tvTile(BuildContext context) => _TvFocusHighlight(
    enabled: enabled,
    focusNode: focusNode,
    focusColor: focusColor,
    onFocusChange: onFocusChange,
    builder: (context, focused, node) => ListTile(
      focusNode: node,
      autofocus: autofocus,
      enabled: enabled,
      selected: selected ?? false,
      focusColor: Colors.transparent,
      hoverColor: Colors.transparent,
      leading: leading,
      title: title,
      subtitle: subtitle,
      trailing: trailing,
      contentPadding: contentPadding,
      onTap: enabled ? onTap : null,
      onLongPress: enabled ? onLongPress : null,
    ),
  );

  @override
  Widget build(BuildContext context) {
    if (TvPlatformDetection.isTV) {
      return _DpadListTileKeyHandler(
        enabled: enabled,
        onTap: onTap,
        onLongPress: onLongPress,
        child: _tvTile(context),
      );
    }

    return Focus(
      skipTraversal: true,
      onFocusChange: onFocusChange,
      child: ListTile(
        enabled: enabled,
        selected: selected ?? false,
        leading: leading,
        title: title,
        subtitle: subtitle,
        trailing: trailing,
        contentPadding: contentPadding,
        onTap: enabled ? onTap : null,
        onLongPress: enabled ? onLongPress : null,
      ),
    );
  }
}

class _DpadListTileKeyHandler extends StatefulWidget {
  const _DpadListTileKeyHandler({
    required this.enabled,
    required this.child,
    this.onTap,
    this.onLongPress,
  });

  final bool enabled;
  final VoidCallback? onTap;
  final VoidCallback? onLongPress;
  final Widget child;

  @override
  State<_DpadListTileKeyHandler> createState() =>
      _DpadListTileKeyHandlerState();
}

class _DpadListTileKeyHandlerState extends State<_DpadListTileKeyHandler> {
  Timer? _holdTimer;
  bool _longPressFired = false;

  @override
  void dispose() {
    _holdTimer?.cancel();
    super.dispose();
  }

  KeyEventResult _onKeyEvent(FocusNode _, KeyEvent event) {
    if (!widget.enabled) return KeyEventResult.ignored;
    if (event.logicalKey.isContextMenuKey && widget.onLongPress != null) {
      if (event is KeyDownEvent) widget.onLongPress!();
      return KeyEventResult.handled;
    }
    if (!event.logicalKey.isSelectKey || widget.onTap == null) {
      return KeyEventResult.ignored;
    }

    if (widget.onLongPress == null) {
      if (event is KeyDownEvent) widget.onTap!();
      return KeyEventResult.handled;
    }
    if (event is KeyDownEvent) {
      _holdTimer?.cancel();
      _longPressFired = false;
      _holdTimer = Timer(const Duration(milliseconds: 500), () {
        _longPressFired = true;
        widget.onLongPress?.call();
      });
      return KeyEventResult.handled;
    }
    if (event is KeyUpEvent) {
      _holdTimer?.cancel();
      _holdTimer = null;
      if (!_longPressFired) widget.onTap!();
      _longPressFired = false;
      return KeyEventResult.handled;
    }
    return KeyEventResult.handled;
  }

  @override
  Widget build(BuildContext context) =>
      Focus(skipTraversal: true, onKeyEvent: _onKeyEvent, child: widget.child);
}

/// Switch variant of [DpadListTile]. Pressing the remote select key toggles
/// the value even on platforms that do not map D-pad center to an InkWell tap.
class DpadSwitchListTile extends StatelessWidget {
  const DpadSwitchListTile({
    super.key,
    required this.title,
    required this.value,
    required this.onChanged,
    this.focusNode,
    this.autofocus = false,
    this.enabled = true,
    this.secondary,
    this.subtitle,
    this.focusColor,
  });

  final FocusNode? focusNode;
  final bool autofocus;
  final bool enabled;
  final Widget? secondary;
  final Widget title;
  final Widget? subtitle;
  final Color? focusColor;
  final bool value;
  final ValueChanged<bool>? onChanged;

  Widget _tvTile() => _TvFocusHighlight(
    enabled: enabled,
    focusNode: focusNode,
    focusColor: focusColor,
    builder: (context, focused, node) => SwitchListTile.adaptive(
      focusNode: node,
      autofocus: autofocus,
      secondary: secondary,
      title: title,
      subtitle: subtitle,
      value: value,
      onChanged: enabled ? onChanged : null,
    ),
  );

  @override
  Widget build(BuildContext context) {
    final canToggle = enabled && onChanged != null;
    if (TvPlatformDetection.isTV) {
      return Focus(
        skipTraversal: true,
        onKeyEvent: (_, event) {
          if (!event.logicalKey.isSelectKey || !canToggle) {
            return KeyEventResult.ignored;
          }
          if (event is KeyDownEvent) onChanged!(!value);
          return KeyEventResult.handled;
        },
        child: _tvTile(),
      );
    }

    return SwitchListTile.adaptive(
      secondary: secondary,
      title: title,
      subtitle: subtitle,
      value: value,
      onChanged: canToggle ? onChanged : null,
    );
  }
}

/// A radio group that retains Flutter's native arrow-to-select behavior off TV
/// and uses independently focusable settings rows on TV.
class DpadRadioGroup<T> extends StatelessWidget {
  const DpadRadioGroup({
    super.key,
    required this.groupValue,
    required this.onChanged,
    required this.child,
  });

  final T? groupValue;
  final ValueChanged<T?> onChanged;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    if (TvPlatformDetection.isTV) {
      return _DpadRadioGroupScope<T>(
        groupValue: groupValue,
        onChanged: onChanged,
        child: Semantics(container: true, child: child),
      );
    }

    return RadioGroup<T>(
      groupValue: groupValue,
      onChanged: onChanged,
      child: child,
    );
  }
}

class _DpadRadioGroupScope<T> extends InheritedWidget {
  const _DpadRadioGroupScope({
    required this.groupValue,
    required this.onChanged,
    required super.child,
  });

  final T? groupValue;
  final ValueChanged<T?> onChanged;

  @override
  bool updateShouldNotify(_DpadRadioGroupScope<T> oldWidget) =>
      groupValue != oldWidget.groupValue || onChanged != oldWidget.onChanged;
}

/// Radio tile for use inside a [DpadRadioGroup]. On TV it follows the
/// settings-picker pattern: arrows move focus and Select commits the checked
/// row. Other platforms retain the native [RadioListTile].
class DpadRadioListTile<T> extends StatelessWidget {
  const DpadRadioListTile({
    super.key,
    required this.value,
    required this.title,
    this.subtitle,
    this.focusNode,
    this.autofocus = false,
  });

  final T value;
  final Widget title;
  final Widget? subtitle;
  final FocusNode? focusNode;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    if (TvPlatformDetection.isTV) {
      final group = context
          .dependOnInheritedWidgetOfExactType<_DpadRadioGroupScope<T>>();
      assert(group != null, 'DpadRadioListTile must be inside DpadRadioGroup.');
      final selected = group?.groupValue == value;
      return Semantics(
        checked: selected,
        inMutuallyExclusiveGroup: true,
        child: DpadListTile(
          focusNode: focusNode,
          autofocus: autofocus,
          selected: selected,
          title: title,
          subtitle: subtitle,
          trailing: selected ? const Icon(Icons.check) : null,
          onTap: () => group?.onChanged(value),
        ),
      );
    }

    return RadioListTile<T>(title: title, subtitle: subtitle, value: value);
  }
}

/// Draws the focus highlight around a TV row.
///
/// Moonfin-Core's TvFocusHighlight is the equivalent; the demo keeps a
/// simpler colored container driven by the row's own focus node so the focus
/// treatment flows from the theme under test rather than a registry.
class _TvFocusHighlight extends StatefulWidget {
  final bool enabled;
  final FocusNode? focusNode;
  final Color? focusColor;
  final ValueChanged<bool>? onFocusChange;
  final Widget Function(BuildContext context, bool focused, FocusNode node)
  builder;

  const _TvFocusHighlight({
    required this.enabled,
    required this.builder,
    this.focusNode,
    this.focusColor,
    this.onFocusChange,
  });

  @override
  State<_TvFocusHighlight> createState() => _TvFocusHighlightState();
}

class _TvFocusHighlightState extends State<_TvFocusHighlight> {
  FocusNode? _tracked;

  FocusNode get _node {
    if (widget.focusNode != null) return widget.focusNode!;
    return _tracked ??= FocusNode();
  }

  @override
  void initState() {
    super.initState();
    _node.addListener(_mark);
  }

  @override
  void didUpdateWidget(covariant _TvFocusHighlight oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.focusNode != widget.focusNode) {
      final oldNode = oldWidget.focusNode ?? _tracked;
      oldNode?.removeListener(_mark);
      if (oldWidget.focusNode == null) _tracked?.dispose();
      _tracked = null;
      _node.addListener(_mark);
    }
  }

  @override
  void dispose() {
    _node.removeListener(_mark);
    _tracked?.dispose();
    super.dispose();
  }

  void _mark() {
    if (mounted) {
      setState(() {});
      widget.onFocusChange?.call(_node.hasFocus);
    }
  }

  @override
  Widget build(BuildContext context) {
    final focused = _node.hasFocus && widget.enabled;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 150),
      padding: const EdgeInsets.all(2),
      decoration: focused
          ? BoxDecoration(
              borderRadius: BorderRadius.circular(10),
              border: Border.all(
                color:
                    widget.focusColor ??
                    (GlassFocusHalo.appleStyleActive
                        ? Colors.white
                        : Theme.of(context).primaryColor),
                width: 2,
              ),
            )
          : const BoxDecoration(),
      child: widget.builder(context, focused, _node),
    );
  }
}
