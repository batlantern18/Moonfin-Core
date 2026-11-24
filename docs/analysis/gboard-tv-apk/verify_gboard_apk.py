#!/usr/bin/env python3
"""Local, REA-free re-verification of the Gboard TV APK analysis.

Parses every classes*.dex in the APK for its string_id table and class_defs,
then checks the identities and literals reported by the REA/JADX session.
Uses only the Python standard library; the APK is never executed.
"""
import hashlib
import json
import os
import re
import struct
import sys
import zipfile

APK = sys.argv[1] if len(sys.argv) > 1 else "/home/prana/gboardtv.apk"
OUT_DIR = sys.argv[2] if len(sys.argv) > 2 else None

CHECKS = [
    "OnScreen Remote invoked",
    "OnScreen Remote not available for package. Showing a Toast.",
    "Invalid keycode: %d",
    "Talkback enabled for blocklist apps, hiding keyboard",
    "requestOnScreenRemote",
    "fireKeyDataWithAction",
    "moveFocus",
    "DPAD_BUTTON_PRESSED",
    "com/android/inputmethod/latin/LatinIME",
    "com/google/android/apps/inputmethod/tv/keyboard/handler/TvOnScreenRemoteEventHandler",
    "com/google/android/apps/inputmethod/libs/tv/keyboard/handler/TVMotionEventHandler",
]

CLASSES = [
    "Lcom/android/inputmethod/latin/LatinIME;",
    "Lcom/google/android/apps/inputmethod/tv/LatinTVIme;",
    "Lcom/google/android/apps/inputmethod/libs/tv/keyboard/handler/TVMotionEventHandler;",
    "Lcom/google/android/apps/inputmethod/tv/keyboard/handler/TvOnScreenRemoteEventHandler;",
    "Lcom/google/android/apps/inputmethod/libs/tv/keyboard/TVKeyboard;",
]


def uleb128(data, off):
    result = shift = 0
    while True:
        b = data[off]
        off += 1
        result |= (b & 0x7F) << shift
        if not b & 0x80:
            return result, off
        shift += 7


def parse_dex(data):
    if data[:3] != b"dex":
        raise ValueError("not a DEX")
    magic = data[4:8].decode("ascii", "replace")
    tag = data[40:44]
    endian = "<" if tag == b"\x78\x56\x34\x12" else ">" if tag == b"\x12\x34\x56\x78" else None
    if endian is None:
        raise ValueError("unknown endian tag")

    def u32(off):
        return struct.unpack_from(endian + "I", data, off)[0]

    string_count, string_off = u32(56), u32(60)
    type_count, type_off = u32(64), u32(68)
    class_count, class_off = u32(96), u32(100)

    offsets = struct.unpack_from(endian + str(string_count) + "I", data, string_off)
    strings = []
    for so in offsets:
        if so >= len(data):
            continue
        try:
            _, pos = uleb128(data, so)
            end = data.index(b"\x00", pos)
            strings.append(data[pos:end].decode("utf-8", "replace"))
        except (ValueError, IndexError):
            pass

    types = struct.unpack_from(endian + str(type_count) + "I", data, type_off) if type_count else ()
    classes = []
    for i in range(class_count):
        idx = struct.unpack_from(endian + "I", data, class_off + i * 32)[0]
        if idx < len(types) and types[idx] < len(strings):
            classes.append(strings[types[idx]])
    return magic, strings, classes


def main():
    digest = hashlib.sha256(open(APK, "rb").read()).hexdigest()
    all_strings, all_classes, per_dex = [], [], {}
    with zipfile.ZipFile(APK) as z:
        for name in sorted(n for n in z.namelist() if re.fullmatch(r"classes\d*\.dex", n)):
            magic, strings, classes = parse_dex(z.read(name))
            per_dex[name] = (magic, len(strings), len(classes))
            all_strings.extend(strings)
            all_classes.extend(classes)

    printable = sorted({s for s in all_strings if 4 <= len(s) <= 160 and re.fullmatch(r"[\x20-\x7e]+", s)})
    unique = sorted(set(all_strings))
    unique_classes = set(all_classes)

    if OUT_DIR:
        os.makedirs(OUT_DIR, exist_ok=True)
        # JSON Lines keeps embedded newlines and control characters lossless.
        with open(os.path.join(OUT_DIR, "dex-strings.jsonl"), "w", encoding="utf-8") as f:
            for s in unique:
                f.write(json.dumps(s, ensure_ascii=False) + "\n")
        with open(os.path.join(OUT_DIR, "dex-strings-printable.txt"), "w", encoding="utf-8") as f:
            f.write("\n".join(printable) + "\n")

    print(f"APK SHA-256 : {digest}")
    print("DEX files   :")
    for name, (magic, s, c) in per_dex.items():
        print(f"  {name:14} version={magic} string_ids={s} class_defs={c}")
    print(f"string_ids  : {len(all_strings)} raw, {len(unique)} unique, {len(printable)} printable")
    print(f"class_defs  : {len(all_classes)} raw, {len(unique_classes)} unique")

    print("\nREA-reported claims:")
    print(f"  8,422 top-level classes  -> {'MATCH' if len(all_classes) == 8422 else 'DIFFERS'} ({len(all_classes)} DEX class_defs)")
    print(f"  41,411 raw string_ids    -> {'MATCH' if len(all_strings) == 41411 else 'DIFFERS'} ({len(all_strings)})")
    print(f"  38,984 unique strings    -> {'MATCH' if len(unique) == 38984 else 'DIFFERS'} ({len(unique)})")

    print("\nClass identities:")
    for name in CLASSES:
        print(f"  {'FOUND  ' if name in unique_classes else 'MISSING'} {name}")

    print("\nInput-handling literals:")
    for text in CHECKS:
        print(f"  {'FOUND  ' if text in unique else 'MISSING'} {text!r}")
    return 0 if all(c in unique_classes for c in CLASSES) else 1


if __name__ == "__main__":
    raise SystemExit(main())
