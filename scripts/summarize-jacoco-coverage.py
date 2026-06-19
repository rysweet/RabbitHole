#!/usr/bin/env python3
"""Summarize JaCoCo CSV line coverage for CI logs and artifacts."""

from __future__ import annotations

import argparse
import csv
import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any, List, Optional, Tuple


AGGREGATE_CSV = Path("coverage-report/target/site/jacoco-aggregate/jacoco.csv")
COVERAGE_MODEL = "open-assets-default"
JACOCO_SOURCE = "jacoco"
MAVEN_COVERAGE_COMMAND = "mvn -Dinstall4j.skip -Pcoverage verify"
VCS_DIR_NAMES = {".git", ".hg", ".svn"}


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


@dataclass(frozen=True)
class ModuleThreshold:
    module_name: str
    minimum_percent: float


def parse_module_threshold(value: str) -> ModuleThreshold:
    if "=" not in value:
        raise argparse.ArgumentTypeError("module threshold must use MODULE=PERCENT")

    module_name, minimum_text = value.rsplit("=", 1)
    module_name = module_name.strip()
    if not module_name:
        raise argparse.ArgumentTypeError("module threshold must include a module name")

    try:
        minimum = float(minimum_text)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("module threshold percent must be a number") from exc

    if minimum < 0.0 or minimum > 100.0:
        raise argparse.ArgumentTypeError("module threshold percent must be between 0 and 100")

    return ModuleThreshold(module_name=module_name, minimum_percent=minimum)


def parse_aggregate_threshold(value: str) -> float:
    try:
        minimum = float(value)
    except ValueError as exc:
        raise argparse.ArgumentTypeError("aggregate threshold percent must be a number") from exc

    if minimum < 0.0 or minimum > 100.0:
        raise argparse.ArgumentTypeError("aggregate threshold percent must be between 0 and 100")

    return minimum


def read_line_coverage(path: Path, name: str) -> Optional[Coverage]:
    covered = 0
    missed = 0
    with path.open(newline="", encoding="utf-8") as handle:
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
    aggregate_path = root / AGGREGATE_CSV
    aggregate = read_line_coverage(aggregate_path, "open-asset reactor") if aggregate_path.exists() else None

    module_reports: List[Coverage] = []
    for path in sorted(root.glob("**/target/site/jacoco/jacoco.csv")):
        if "jacoco-aggregate" in path.parts:
            continue
        coverage = read_line_coverage(path, module_name(path, root))
        if coverage is not None:
            module_reports.append(coverage)
    return aggregate, module_reports


def relative_path(path: Path, root: Path) -> str:
    return path.resolve().relative_to(root.resolve()).as_posix()


def coverage_metrics(coverage: Coverage) -> dict[str, float | int]:
    return {
        "lineCoveragePercent": round(coverage.percent, 2),
        "covered": coverage.covered,
        "missed": coverage.missed,
        "total": coverage.total,
    }


def aggregate_manifest(root: Path, aggregate: Optional[Coverage]) -> dict[str, Any]:
    aggregate_path = root / AGGREGATE_CSV
    entry: dict[str, Any] = {
        "expectedCsv": AGGREGATE_CSV.as_posix(),
        "state": "missing",
    }
    if aggregate_path.exists():
        entry["state"] = "present" if aggregate is not None else "empty"
    if aggregate is not None:
        entry.update(coverage_metrics(aggregate))
    return entry


def module_manifest_entries(root: Path) -> list[dict[str, Any]]:
    entries: list[dict[str, Any]] = []
    for path in sorted(root.glob("**/target/site/jacoco/jacoco.csv")):
        if "jacoco-aggregate" in path.parts:
            continue
        module = module_name(path, root)
        coverage = read_line_coverage(path, module)
        entry: dict[str, Any] = {
            "module": module,
            "csv": relative_path(path, root),
            "state": "present" if coverage is not None else "empty",
        }
        if coverage is not None:
            entry.update(coverage_metrics(coverage))
        entries.append(entry)
    return sorted(entries, key=lambda item: item["module"])


def iter_files_under(path: Path) -> list[Path]:
    if path.is_file():
        return [path]
    if not path.is_dir():
        return []

    files: list[Path] = []
    for current, dirnames, filenames in os.walk(path):
        dirnames[:] = sorted(dirname for dirname in dirnames if dirname not in VCS_DIR_NAMES)
        for filename in sorted(filenames):
            files.append(Path(current) / filename)
    return files


def coverage_artifact_entries(root: Path) -> list[dict[str, str]]:
    entries: list[dict[str, str]] = []
    aggregate_dir = root / "coverage-report" / "target" / "site" / "jacoco-aggregate"
    for path in iter_files_under(aggregate_dir):
        entries.append({"kind": "aggregate-report", "path": relative_path(path, root)})

    for path in sorted(root.glob("**/target/site/jacoco/**")):
        if path.is_file() and "jacoco-aggregate" not in path.parts:
            entries.append({"kind": "module-report", "path": relative_path(path, root)})

    for path in sorted(root.glob("**/target/jacoco.exec")):
        if path.is_file():
            entries.append({"kind": "exec-data", "path": relative_path(path, root)})

    for path in sorted(root.glob("**/target/surefire-reports/**")):
        if path.is_file():
            entries.append({"kind": "surefire-report", "path": relative_path(path, root)})

    return sorted(entries, key=lambda item: (item["kind"], item["path"]))


def gate_state(coverage: Optional[Coverage], minimum_percent: float) -> dict[str, Any]:
    entry: dict[str, Any] = {
        "minimumPercent": minimum_percent,
        "state": "fail",
    }
    if coverage is not None:
        entry["lineCoveragePercent"] = round(coverage.percent, 2)
        if coverage.percent >= minimum_percent:
            entry["state"] = "pass"
    return entry


def gate_manifest(
    aggregate: Optional[Coverage],
    module_reports: List[Coverage],
    aggregate_threshold: Optional[float],
    module_thresholds: List[ModuleThreshold],
) -> dict[str, Any]:
    aggregate_entry = (
        gate_state(aggregate, aggregate_threshold)
        if aggregate_threshold is not None
        else {"state": "not-configured"}
    )
    reports_by_name = {coverage.name: coverage for coverage in module_reports}
    module_entries: list[dict[str, Any]] = []
    for threshold in sorted(module_thresholds, key=lambda item: item.module_name):
        entry = {
            "module": threshold.module_name,
            **gate_state(reports_by_name.get(threshold.module_name), threshold.minimum_percent),
        }
        module_entries.append(entry)
    return {"aggregate": aggregate_entry, "modules": module_entries}


def aggregate_target_manifest(
    root: Path,
    aggregate: Optional[Coverage],
    target_percent: Optional[float],
) -> dict[str, Any]:
    if target_percent is None:
        return {"state": "not-configured"}

    entry: dict[str, Any] = {
        "minimumPercent": target_percent,
        "state": "not-claimable",
    }
    if aggregate is None:
        aggregate_path = root / AGGREGATE_CSV
        if aggregate_path.exists():
            entry["reason"] = "aggregate JaCoCo CSV has no measured line data"
        else:
            entry["reason"] = "aggregate JaCoCo CSV is missing"
        return entry

    entry["lineCoveragePercent"] = round(aggregate.percent, 2)
    entry["state"] = "met" if aggregate.percent >= target_percent else "not-met"
    return entry


def render_manifest(
    root: Path,
    aggregate: Optional[Coverage],
    module_reports: List[Coverage],
    aggregate_threshold: Optional[float],
    module_thresholds: List[ModuleThreshold],
    *,
    aggregate_target_percent: Optional[float] = None,
) -> dict[str, Any]:
    return {
        "schemaVersion": 1,
        "coverageModel": COVERAGE_MODEL,
        "source": JACOCO_SOURCE,
        "mavenCommand": MAVEN_COVERAGE_COMMAND,
        "aggregate": aggregate_manifest(root, aggregate),
        "modules": module_manifest_entries(root),
        "artifacts": coverage_artifact_entries(root),
        "gates": gate_manifest(
            aggregate,
            module_reports,
            aggregate_threshold,
            module_thresholds,
        ),
        "coverageTarget": {
            "aggregate": aggregate_target_manifest(root, aggregate, aggregate_target_percent),
        },
    }


def row(coverage: Coverage) -> str:
    return f"| {coverage.name} | {coverage.percent:.2f}% | {coverage.covered} | {coverage.missed} | {coverage.total} |"


def render_markdown(aggregate: Optional[Coverage], module_reports: List[Coverage]) -> str:
    lines = ["# JaCoCo line coverage", ""]
    if aggregate is not None:
        lines.extend([
            "## Aggregate open-asset coverage",
            "",
            "| Scope | Line coverage | Covered | Missed | Total |",
            "| --- | ---: | ---: | ---: | ---: |",
            row(aggregate),
            "",
        ])
    else:
        lines.extend([
            "## Aggregate open-asset coverage",
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
    lines.extend([
        "## Evidence inventory",
        "",
        "Run with `--evidence-manifest coverage-evidence-manifest.json` to write a deterministic JSON inventory of JaCoCo reports, diagnostic artifacts, gate results, and target status.",
        "",
    ])
    lines.append("Coverage gate details appear below when a threshold is requested.")
    lines.append("")
    return "\n".join(lines)


def append_gate(markdown: str, aggregate: Optional[Coverage], minimum: float) -> Tuple[str, bool]:
    lines = [markdown.rstrip(), "", "## Aggregate coverage gate", ""]
    if aggregate is None:
        lines.extend([
            f"Required aggregate line coverage: {minimum:.2f}%",
            "",
            "Result: FAIL - aggregate report was not found.",
            "",
        ])
        return "\n".join(lines), False

    passed = aggregate.percent >= minimum
    result = "PASS" if passed else "FAIL"
    lines.extend([
        f"Required aggregate line coverage: {minimum:.2f}%",
        f"Actual aggregate line coverage: {aggregate.percent:.2f}%",
        "",
        f"Result: {result}",
        "",
    ])
    return "\n".join(lines), passed


def append_aggregate_target(
    markdown: str,
    aggregate: Optional[Coverage],
    target_percent: float,
) -> str:
    lines = [markdown.rstrip(), "", "## Long-term aggregate coverage target", ""]
    lines.append(f"Required aggregate line coverage: {target_percent:.2f}%")

    if aggregate is None:
        lines.extend([
            "",
            "Result: NOT CLAIMABLE - aggregate report was not found.",
            "Module-level JaCoCo reports cannot prove aggregate coverage without the aggregate CSV.",
            "",
        ])
        return "\n".join(lines)

    result = "MET" if aggregate.percent >= target_percent else "NOT MET"
    lines.extend([
        f"Actual aggregate line coverage: {aggregate.percent:.2f}%",
        "",
        f"Result: {result}",
        "",
    ])
    return "\n".join(lines)


def append_module_gates(
    markdown: str,
    module_reports: List[Coverage],
    thresholds: List[ModuleThreshold],
) -> Tuple[str, bool]:
    reports_by_name = {coverage.name: coverage for coverage in module_reports}
    passed_all = True
    lines = [
        markdown.rstrip(),
        "",
        "## Module coverage gates",
        "",
        "| Module | Required line coverage | Actual line coverage | Result |",
        "| --- | ---: | ---: | --- |",
    ]
    for threshold in thresholds:
        coverage = reports_by_name.get(threshold.module_name)
        if coverage is None:
            passed_all = False
            lines.append(
                f"| {threshold.module_name} | {threshold.minimum_percent:.2f}% | missing | FAIL |"
            )
            continue

        passed = coverage.percent >= threshold.minimum_percent
        passed_all = passed_all and passed
        result = "PASS" if passed else "FAIL"
        lines.append(
            f"| {coverage.name} | {threshold.minimum_percent:.2f}% | {coverage.percent:.2f}% | {result} |"
        )
    lines.append("")
    return "\n".join(lines), passed_all


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="repository root")
    parser.add_argument("--output", type=Path, help="write Markdown summary to this path")
    parser.add_argument(
        "--evidence-manifest",
        type=Path,
        help="write deterministic JSON coverage evidence inventory to this path",
    )
    parser.add_argument(
        "--min-aggregate-line-percent",
        type=parse_aggregate_threshold,
        help="fail when aggregate line coverage is missing or below this percentage",
    )
    parser.add_argument(
        "--target-aggregate-line-percent",
        type=parse_aggregate_threshold,
        help="record the long-term aggregate line coverage target without failing the CI gate",
    )
    parser.add_argument(
        "--min-module-line-percent",
        action="append",
        default=[],
        metavar="MODULE=PERCENT",
        type=parse_module_threshold,
        help="fail when a module line coverage report is missing or below this percentage",
    )
    args = parser.parse_args()
    seen_modules = set()
    for threshold in args.min_module_line_percent:
        if threshold.module_name in seen_modules:
            parser.error(f"duplicate module threshold for {threshold.module_name}")
        seen_modules.add(threshold.module_name)

    root = args.root.resolve()
    aggregate, module_reports = collect_reports(root)
    markdown = render_markdown(aggregate, module_reports)
    passed = True
    if args.min_aggregate_line_percent is not None:
        markdown, passed = append_gate(markdown, aggregate, args.min_aggregate_line_percent)
    if args.target_aggregate_line_percent is not None:
        markdown = append_aggregate_target(markdown, aggregate, args.target_aggregate_line_percent)
    if args.min_module_line_percent:
        markdown, module_passed = append_module_gates(
            markdown,
            module_reports,
            args.min_module_line_percent,
        )
        passed = passed and module_passed
    print(markdown)

    if args.output:
        args.output.write_text(markdown, encoding="utf-8")
    if args.evidence_manifest:
        manifest = render_manifest(
            root,
            aggregate,
            module_reports,
            args.min_aggregate_line_percent,
            args.min_module_line_percent,
            aggregate_target_percent=args.target_aggregate_line_percent,
        )
        args.evidence_manifest.write_text(
            json.dumps(manifest, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )

    github_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if github_summary:
        with Path(github_summary).open("a", encoding="utf-8") as handle:
            handle.write(markdown)
            handle.write("\n")
    return 0 if passed else 2


if __name__ == "__main__":
    raise SystemExit(main())
