/// One remembered horizontal position per hub, so a screen the viewer
/// returns to lands on the card they left. Replicated from Moonfin-Core's
/// lib/ui/widgets/focus/hub_focus_memory.dart.
class HubFocusMemory {
  HubFocusMemory._();

  static final Map<String, int> _store = {};

  /// The remembered index for [key], clamped into [itemCount] items.
  static int getForHub(String key, int itemCount) {
    if (itemCount <= 0) return 0;
    final stored = _store[key] ?? 0;
    return stored.clamp(0, itemCount - 1).toInt();
  }

  /// The remembered index without clamping, null when nothing was stored.
  static int? peek(String key) => _store[key];

  static void set(String key, int index) {
    if (index < 0) return;
    _store[key] = index;
  }

  static void clear(String key) => _store.remove(key);

  static void clearAll() => _store.clear();
}
