#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Инкремент версии проекта HRM HuntTech в build.gradle при каждом коммите.

Формат версии: x.y или x.y.z (строка `version = '...'` в блоке cuba.artifact).
По умолчанию инкрементится ПОСЛЕДНИЙ сегмент (подверсия):
    0.1   -> 0.2
    0.9.1 -> 0.9.2
Опциональные аргументы (полный перебор по умолчанию):
    patch  — последний сегмент (по умолчанию)
    minor  — средний сегмент, младшие обнуляются
    major  — первый сегмент, остальные обнуляются
    --dry-run — только показать результат, файл не менять

Возвращает 0 при успехе, 1 если строка версии не найдена.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
BUILD_GRADLE = ROOT / "build.gradle"

# `version = '0.1'` / `version = "0.9.1"` — строка с cuba.artifact.version.
# \bversion\b не задевает cubaVersion/App-Component-Version (нет границы слова).
VERSION_LINE = re.compile(r"^(.*\bversion\b[ \t]*=[ \t]*)(['\"])(\d+)(?:\.(\d+))?(?:\.(\d+))?(['\"])(.*)$")


def bump(parts, mode):
    parts = list(parts)
    if mode == "major":
        parts[0] += 1
        parts[1:] = [0] * (len(parts) - 1)
    elif mode == "minor" and len(parts) >= 2:
        parts[1] += 1
        parts[2:] = [0] * (len(parts) - 2)
    else:  # patch (по умолчанию): последний сегмент
        parts[-1] += 1
    return parts


def main():
    mode = "patch"
    dry_run = False
    for arg in sys.argv[1:]:
        if arg == "--dry-run":
            dry_run = True
        elif arg in ("major", "minor", "patch"):
            mode = arg
        else:
            print(f"bump-version: неизвестный аргумент {arg!r}", file=sys.stderr)
            return 2

    text = BUILD_GRADLE.read_text(encoding="utf-8")
    lines = text.splitlines()

    for i, line in enumerate(lines):
        m = VERSION_LINE.match(line)
        if not m:
            continue
        prefix, quote, a, b, c, closing, tail = m.groups()
        old_version = ".".join(x for x in (a, b, c) if x is not None)
        parts = [int(x) for x in (a, b, c) if x is not None]
        new_version = ".".join(str(p) for p in bump(parts, mode))
        if dry_run:
            print(f"bump-version (dry-run): {old_version} -> {new_version} [{mode}]")
            return 0
        lines[i] = f"{prefix}{quote}{new_version}{closing}{tail}"
        BUILD_GRADLE.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"bump-version: {old_version} -> {new_version} [{mode}]")
        return 0

    print("bump-version: строка version = '...' не найдена в build.gradle", file=sys.stderr)
    return 1


if __name__ == "__main__":
    sys.exit(main())
