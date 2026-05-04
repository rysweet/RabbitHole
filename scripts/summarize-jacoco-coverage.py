#!/usr/bin/env python3
"""Summarize JaCoCo CSV line coverage for CI logs and artifacts."""

from __future__ import annotations

import argparse
import csv
import os
from dataclasses import dataclass
from pathlib import Path
from typing import List, Optional, Tuple


@dataclass(frozen=True)
class Coverage:
    name: str
    covered: int
    missed: int
    source: Path

    @property
    def total(self) -> int:
        return self.covered + self.missed

    @property
    def percent(self) -> float:
        return 100.0 * self.covered / self.total if self.total else 0.0


def read_line_coverage(path: Path, name: str) -> Optional[Coverage]:
    covered = 0
    missed = 0
    with path.open(newline="") as handle:
        for row in csv.DictReader(handle):
            covered += int(row["LINE_COVERED"])
            missed += int(row["LINE_MISSED"])
    if covered + missed == 0:
        return None
    return Coverage(name=name, covered=covered, missed=missed, source=path)


def module_name(csv_path: Path, root: Path) -> str:
    relative = csv_path.relative_to(root)
    target_index = relative.parts.index("target")
    module_parts = relative.parts[:target_index]
    return "/".join(module_parts) if module_parts else "."


def collect_reports(root: Path) -> Tuple[Optional[Coverage], List[Coverage]]:
    aggregate_path = root / "coverage-report" / "target" / "site" / "jacoco-aggregate" / "jacoco.csv"
    aggregate = read_line_coverage(aggregate_path, "no-Sims reactor") if aggregate_path.exists() else None

    module_reports: List[Coverage] = []
    for path in sorted(root.glob("**/target/site/jacoco/jacoco.csv")):
        if "jacoco-aggregate" in path.parts:
            continue
        coverage = read_line_coverage(path, module_name(path, root))
        if coverage is not None:
            module_reports.append(coverage)
    return aggregate, module_reports


def row(coverage: Coverage) -> str:
    return f"| {coverage.name} | {coverage.percent:.2f}% | {coverage.covered} | {coverage.missed} | {coverage.total} |"


def render_markdown(aggregate: Optional[Coverage], module_reports: List[Coverage]) -> str:
    lines = ["# JaCoCo line coverage", ""]
    if aggregate is not None:
        lines.extend([
            "## Aggregate no-Sims coverage",
            "",
            "| Scope | Line coverage | Covered | Missed | Total |",
            "| --- | ---: | ---: | ---: | ---: |",
            row(aggregate),
            "",
        ])
    else:
        lines.extend([
            "## Aggregate no-Sims coverage",
            "",
            "Aggregate report not found at `coverage-report/target/site/jacoco-aggregate/jacoco.csv`.",
            "",
        ])

    lines.extend([
        "## Per-module reports with JaCoCo CSV output",
        "",
        "| Module | Line coverage | Covered | Missed | Total |",
        "| --- | ---: | ---: | ---: | ---: |",
    ])
    for coverage in sorted(module_reports, key=lambda item: item.name):
        lines.append(row(coverage))
    if not module_reports:
        lines.append("| _none_ | n/a | 0 | 0 | 0 |")
    lines.append("")
    lines.append("No coverage threshold is enforced by this report.")
    lines.append("")
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="repository root")
    parser.add_argument("--output", type=Path, help="write Markdown summary to this path")
    args = parser.parse_args()

    root = args.root.resolve()
    aggregate, module_reports = collect_reports(root)
    markdown = render_markdown(aggregate, module_reports)
    print(markdown)

    if args.output:
        args.output.write_text(markdown, encoding="utf-8")

    github_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if github_summary:
        with Path(github_summary).open("a", encoding="utf-8") as handle:
            handle.write(markdown)
            handle.write("\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
