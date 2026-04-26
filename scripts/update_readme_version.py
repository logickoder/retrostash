#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

MODULES = ("retrostash-core", "retrostash-annotations", "retrostash-ktor", "retrostash-okhttp")


def update_readme_version(tag_name: str, readme_path: Path) -> int:
    text = readme_path.read_text(encoding="utf-8")
    updated = text
    total = 0

    for module in MODULES:
        pattern = re.compile(
            rf'(implementation\("dev\.logickoder:{re.escape(module)}:)[^"\)]+("\))'
        )
        updated, count = pattern.subn(rf"\g<1>{tag_name}\g<2>", updated)
        total += count

    if total == 0 or updated == text:
        print("README version lines not updated (pattern not found or already current).")
        return 0

    readme_path.write_text(updated, encoding="utf-8")
    print(f"README updated {total} dependency line(s) to {tag_name}.")
    return 0


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("Usage: update_readme_version.py <tag>", file=sys.stderr)
        return 2

    tag_name = argv[1]
    repo_root = Path(__file__).resolve().parents[1]
    readme_path = repo_root / "README.md"
    return update_readme_version(tag_name, readme_path)


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
