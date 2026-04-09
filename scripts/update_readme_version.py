#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path


def update_readme_version(tag_name: str, readme_path: Path) -> int:
    text = readme_path.read_text(encoding="utf-8")
    updated = text

    # Prefer updating the explicit example bullet line when present.
    example_pattern = re.compile(
        r'(^\s*-\s*`?implementation\("com\.github\.logickoder:retrostash:)[^"`\)]+("\)`?\s*$)',
        flags=re.MULTILINE,
    )
    updated, example_count = example_pattern.subn(
        rf"\1{tag_name}\2",
        updated,
        count=1,
    )

    # Fallback: update the first non-placeholder dependency occurrence.
    if example_count == 0:
        generic_pattern = re.compile(
            r'implementation\("com\.github\.logickoder:retrostash:([^"\)]+)"\)'
        )

        def replace_first(match: re.Match[str]) -> str:
            current = match.group(1)
            if current == "<tag>":
                return match.group(0)
            return f'implementation("com.github.logickoder:retrostash:{tag_name}")'

        updated, _ = generic_pattern.subn(replace_first, updated, count=1)

    if updated == text:
        print("README version line not updated (pattern not found or already current).")
        return 0

    readme_path.write_text(updated, encoding="utf-8")
    print(f"README version line updated to {tag_name}.")
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
