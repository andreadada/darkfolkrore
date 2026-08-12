#!/usr/bin/env python3
"""Analyze a Dark Folklore Atlas scan as a cross-mod recipe/canonicalization graph.

The tool is intentionally advisory: it never rewrites recipes and never treats name similarity as equivalence.
It produces deterministic JSON + Markdown reports for review before datapack/code changes.
"""
from __future__ import annotations

import argparse
import json
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any


def load(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def namespace(identifier: str) -> str:
    return identifier.split(":", 1)[0] if ":" in identifier else "minecraft"


def audit(scan: Path) -> dict[str, Any]:
    consumers: dict[str, list[str]] = load(scan / "recipes" / "consumers_by_item.json", {})
    producers: dict[str, list[str]] = load(scan / "recipes" / "producers_by_item.json", {})
    matrix: list[dict[str, Any]] = load(scan / "canonicalization" / "matrix.json", [])
    mods: list[dict[str, Any]] = load(scan / "mods.json", [])

    edges: Counter[tuple[str, str]] = Counter()
    incoming: defaultdict[str, int] = defaultdict(int)
    outgoing: defaultdict[str, int] = defaultdict(int)
    for item, recipes in consumers.items():
        item_ns = namespace(item)
        for recipe in recipes:
            recipe_ns = namespace(recipe)
            if item_ns == recipe_ns:
                continue
            edges[(item_ns, recipe_ns)] += 1
            outgoing[item_ns] += 1
            incoming[recipe_ns] += 1

    duplicate_asymmetry: list[dict[str, Any]] = []
    high_risk: list[dict[str, Any]] = []
    undecided: list[dict[str, Any]] = []
    producerless: list[dict[str, Any]] = []
    for row in matrix:
        if row.get("kind") != "item":
            continue
        left = row.get("left", "")
        right = row.get("right", "")
        lc = len(consumers.get(left, []))
        rc = len(consumers.get(right, []))
        lp = len(producers.get(left, []))
        rp = len(producers.get(right, []))
        compact = {
            "concept": row.get("concept", ""),
            "relation": row.get("relation", ""),
            "left": left,
            "right": right,
            "score": row.get("score", 0),
            "replacement_risk": row.get("replacement_risk", "UNKNOWN"),
            "decision": row.get("decision", "UNDECIDED"),
            "left_consumers": lc,
            "right_consumers": rc,
            "left_producers": lp,
            "right_producers": rp,
        }
        if lc != rc and max(lc, rc) > 0:
            duplicate_asymmetry.append(compact)
        if row.get("replacement_risk") in {"HIGH", "CRITICAL"}:
            high_risk.append(compact)
        if row.get("decision", "UNDECIDED") == "UNDECIDED":
            undecided.append(compact)
        if (lp == 0) != (rp == 0):
            producerless.append(compact)

    all_namespaces = {m.get("namespace_guess") or m.get("mod_id") for m in mods}
    connected = set(incoming) | set(outgoing)
    isolated = sorted(value for value in all_namespaces if value and value not in connected)

    top_edges = [
        {"ingredient_namespace": a, "recipe_namespace": b, "uses": count}
        for (a, b), count in edges.most_common()
    ]
    opportunities = sorted(
        duplicate_asymmetry,
        key=lambda value: (-abs(value["left_consumers"] - value["right_consumers"]),
                           -int(value.get("score") or 0), value["left"], value["right"]),
    )

    return {
        "scan": scan.name,
        "mod_count": len(mods),
        "consumer_item_count": len(consumers),
        "producer_item_count": len(producers),
        "cross_mod_edge_count": sum(edges.values()),
        "cross_mod_pairs": len(edges),
        "top_cross_mod_edges": top_edges,
        "duplicate_consumer_asymmetry": opportunities,
        "producerless_duplicate_candidates": producerless,
        "high_risk_duplicate_candidates": high_risk,
        "undecided_duplicate_candidates": undecided,
        "recipe_isolated_namespaces": isolated,
        "policy": {
            "same_name_is_not_equivalence": True,
            "automatic_rewrite": False,
            "recommended_classes": ["EQUIVALENT", "INTEROPERABILITY_ONLY", "KEEP_DISTINCT"],
        },
    }


def markdown(report: dict[str, Any]) -> str:
    lines = [
        f"# Atlas recipe graph audit — {report['scan']}", "",
        f"- Mods: **{report['mod_count']}**",
        f"- Cross-mod ingredient uses: **{report['cross_mod_edge_count']}**",
        f"- Distinct namespace bridges: **{report['cross_mod_pairs']}**",
        f"- Duplicate consumer asymmetries: **{len(report['duplicate_consumer_asymmetry'])}**",
        f"- Producerless duplicate candidates: **{len(report['producerless_duplicate_candidates'])}**",
        f"- High-risk duplicate candidates: **{len(report['high_risk_duplicate_candidates'])}**",
        "", "## Strongest existing cross-mod recipe bridges", "",
        "| Ingredient mod | Recipe mod | Uses |", "|---|---|---:|",
    ]
    for edge in report["top_cross_mod_edges"][:30]:
        lines.append(f"| `{edge['ingredient_namespace']}` | `{edge['recipe_namespace']}` | {edge['uses']} |")
    lines.extend(["", "## Highest-value interoperability candidates", "",
                  "These are audit candidates only; semantic/source review is mandatory before a tag or override.", "",
                  "| Left | Right | Consumers L/R | Producers L/R | Risk |", "|---|---|---:|---:|---|"])
    for row in report["duplicate_consumer_asymmetry"][:50]:
        lines.append(f"| `{row['left']}` | `{row['right']}` | {row['left_consumers']}/{row['right_consumers']} | "
                     f"{row['left_producers']}/{row['right_producers']} | {row['replacement_risk']} |")
    lines.extend(["", "## Rules", "",
                  "1. Name similarity never authorizes substitution.",
                  "2. Prefer common/NeoForge tags before Dark Folklore tags.",
                  "3. Recipe-safe tags are narrower than semantic lore tags.",
                  "4. Canonical outputs/loot/worldgen require a separately reviewed FULL/EQUIVALENT decision.",
                  "5. KEEP_DISTINCT and provider-owned mechanics are never bulk rewritten.", ""])
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("scan", type=Path, help="Atlas scan directory")
    parser.add_argument("--out", type=Path, default=Path("build/atlas-audit"))
    args = parser.parse_args()
    report = audit(args.scan)
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / "recipe_graph_audit.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
    (args.out / "recipe_graph_audit.md").write_text(markdown(report), encoding="utf-8")
    print(json.dumps({k: report[k] for k in ("scan", "mod_count", "cross_mod_edge_count", "cross_mod_pairs")}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
