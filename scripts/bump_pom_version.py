#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path


SEMVER_RE = re.compile(r"^(\d+)\.(\d+)\.(\d+)$")


def compute_next_snapshot(tag_name: str) -> str | None:
    base = tag_name[1:] if tag_name.startswith("v") else tag_name
    match = SEMVER_RE.fullmatch(base)
    if not match:
        return None

    major, minor, patch = (int(part) for part in match.groups())
    return f"{major}.{minor}.{patch + 1}-SNAPSHOT"


def update_pom_version(file_path: Path, next_snapshot: str) -> bool:
    text = file_path.read_text(encoding="utf-8")
    updated, count = re.subn(
        r"^POM_VERSION=.*$",
        f"POM_VERSION={next_snapshot}",
        text,
        count=1,
        flags=re.MULTILINE,
    )
    if count == 0:
        raise RuntimeError("POM_VERSION line not found in gradle.properties")

    if updated == text:
        return False

    file_path.write_text(updated, encoding="utf-8")
    return True


def main(argv: list[str]) -> int:
    if len(argv) not in (2, 3):
        print("Usage: bump_pom_version.py <tag> [gradle.properties path]", file=sys.stderr)
        return 2

    tag_name = argv[1]
    if len(argv) == 3:
        gradle_properties = Path(argv[2]).resolve()
    else:
        repo_root = Path(__file__).resolve().parents[1]
        gradle_properties = repo_root / "gradle.properties"

    next_snapshot = compute_next_snapshot(tag_name)
    if next_snapshot is None:
        print("SKIPPED")
        return 0

    update_pom_version(gradle_properties, next_snapshot)
    print(next_snapshot)
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
