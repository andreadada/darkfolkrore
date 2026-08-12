#!/usr/bin/env python3
"""Verify that an Atlas scan contains the versions Dark Folklore was audited against."""
from __future__ import annotations

import argparse
import json
from pathlib import Path

REQUIRED = {
    "vampirism": "1.10.12",
    "werewolves": "2.0.3.3",
    "mca": "7.7.32+1.21.1",
    "mcacapitals": "1.1.0",
    "mca_vamp_compat": "2.0.12",
    "fieldguide": "1.14.0",
    "enchanted": "4.2.7",
    "occultism": "1.224.2",
    "malum": "1.8.2",
    "eidolon_repraised": "0.5.0.2",
    "feywild": "5.5.5",
    "betterarcheology": "1.21.1-1.3.8",
    "quest_giver": "1.5.1",
    "almostunified": "1.21.1-1.4.2",
}

ALIASES = {
    "fieldguide": {"fieldguide", "field_guide"},
    "betterarcheology": {"betterarcheology", "better_archeology"},
}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("scan", type=Path, help="Atlas scan directory or mods.json")
    args = parser.parse_args()
    mods_path = args.scan if args.scan.name == "mods.json" else args.scan / "mods.json"
    mods = json.loads(mods_path.read_text(encoding="utf-8"))
    by_id = {str(row.get("mod_id")): str(row.get("version")) for row in mods}

    failures: list[str] = []
    resolved: dict[str, str] = {}
    for expected_id, expected_version in REQUIRED.items():
        candidates = ALIASES.get(expected_id, {expected_id})
        actual_id = next((candidate for candidate in candidates if candidate in by_id), None)
        if actual_id is None:
            failures.append(f"MISSING {expected_id} expected={expected_version}")
            continue
        actual = by_id[actual_id]
        resolved[expected_id] = actual
        if actual != expected_version:
            failures.append(f"VERSION {expected_id} expected={expected_version} actual={actual}")

    print(f"Reference pack: {len(resolved)}/{len(REQUIRED)} audited providers resolved")
    for mod_id, version in sorted(resolved.items()):
        print(f"  OK {mod_id}={version}")
    for failure in failures:
        print(f"  FAIL {failure}")
    if failures:
        print("FAIL-CLOSED: do not promote this build as an audited full-pack release candidate.")
        return 2
    print("PASS: Atlas-reported provider versions match the audited reference pack.")
    print("NOTE: version matching does not replace the separate SHA/runtime/manual provider gates.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
