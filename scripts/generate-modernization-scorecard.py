#!/usr/bin/env python3
"""Generate the Alice modernization scorecard from deterministic repo inputs."""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import subprocess
import sys
from collections import Counter
from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path
from typing import Iterable


AGGREGATE_CSV = Path("coverage-report/target/site/jacoco-aggregate/jacoco.csv")
CORPUS_MANIFEST = Path("docs/reference/modernization-corpus-manifest.json")
CORPUS_FIXTURE_ROOT = Path("generated-fixtures")
CORPUS_FIXTURE_EXTENSIONS = {".a3p", ".a3w", ".a3c"}
QA_VALIDATOR = Path("qa/outside-in/alice-desktop/runners/validate-scenarios.sh")
WORKFLOW_PATH = Path(".github/workflows/alice-coverage-ci.yml")
TARGET_LINE_PERCENT = 70.0
HOTSPOT_LINE_LIMIT = 500
LINE_COUNT_CHUNK_SIZE = 1024 * 1024
VCS_DIR_NAMES = {".git", ".hg", ".svn"}
AGGREGATE_RATCHET_RE = re.compile(
    r"--min-aggregate-line-percent\s+([0-9]+(?:\.[0-9]+)?)"
)
MODULE_RATCHET_RE = re.compile(
    r"--min-module-line-percent\s+([^\s\\=]+)=([0-9]+(?:\.[0-9]+)?)"
)
CORPUS_ENTRY_ID_RE = re.compile(r"[a-z0-9][a-z0-9-]*")
MODE_CATEGORIES = {
    "xvfb-real-alice": "Automated real Alice journey",
    "gated-command-smoke": "Gated command smoke coverage",
    "manual-evidence-required": "Manual evidence gap",
}


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
class CoverageRatchets:
    aggregate_minimum_percent: float | None
    module_minimum_percent_by_module: dict[str, float]


@dataclass(frozen=True)
class CoverageTargetStatus:
    state: str
    message: str


@dataclass(frozen=True)
class Hotspot:
    path: str
    line_count: int


@dataclass(frozen=True)
class Scenario:
    id: str
    workflow: str
    automation_mode: str


@dataclass(frozen=True)
class JourneySummary:
    count_by_mode: dict[str, int]
    manual_evidence_gaps: list[Scenario]
    gated_smoke_gaps: list[Scenario]


@dataclass(frozen=True)
class CorpusManifestStatus:
    state: str
    message: str


def format_floor(value: float | None) -> str:
    return "n/a" if value is None else f"{value:.1f}%"


def display_state(state: str) -> str:
    return state[:1].upper() + state[1:]


def markdown_cell(value: object) -> str:
    text = str(value).replace("\r\n", "\n").replace("\r", "\n")
    text = " ".join(part.strip() for part in text.split("\n"))
    return text.replace("|", r"\|")


def inline_code(value: object) -> str:
    text = markdown_cell(value)
    delimiter = "`"
    while delimiter in text:
        delimiter += "`"
    if delimiter != "`":
        return f"{delimiter} {text} {delimiter}"
    return f"`{text}`"


def parse_percent(value: str, context: str) -> float:
    percent = float(value)
    if percent < 0.0 or percent > 100.0:
        raise ValueError(f"{context} must be between 0.0 and 100.0")
    return percent


def parse_coverage_ratchets(workflow_text: str) -> CoverageRatchets:
    aggregate_match = AGGREGATE_RATCHET_RE.search(workflow_text)
    aggregate = (
        parse_percent(aggregate_match.group(1), "aggregate coverage ratchet")
        if aggregate_match
        else None
    )

    modules: dict[str, float] = {}
    for module, percent in MODULE_RATCHET_RE.findall(workflow_text):
        if module in modules:
            raise ValueError(f"duplicate coverage ratchet for module {module}")
        modules[module] = parse_percent(percent, f"coverage ratchet for module {module}")

    return CoverageRatchets(
        aggregate_minimum_percent=aggregate,
        module_minimum_percent_by_module=dict(sorted(modules.items())),
    )


def parse_jacoco_line_count(row: dict[str, str], field: str, report_name: str) -> int:
    try:
        value = int(row[field])
    except KeyError as exc:
        raise ValueError(f"JaCoCo CSV for {report_name} is missing required column {field}") from exc
    except ValueError as exc:
        raise ValueError(f"JaCoCo CSV for {report_name} column {field} must be an integer") from exc
    if value < 0:
        raise ValueError(f"JaCoCo CSV for {report_name} column {field} must be non-negative")
    return value


def read_jacoco_line_coverage(path: Path, name: str) -> Coverage | None:
    covered = 0
    missed = 0
    with path.open(newline="", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            covered += parse_jacoco_line_count(row, "LINE_COVERED", name)
            missed += parse_jacoco_line_count(row, "LINE_MISSED", name)
    if covered + missed == 0:
        return None
    return Coverage(name=name, covered=covered, missed=missed, source=path)


@lru_cache(maxsize=None)
def tracked_repo_files(root: Path) -> tuple[str, ...] | None:
    result = subprocess.run(
        ["git", "ls-files"],
        cwd=root,
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode == 0:
        return tuple(line for line in result.stdout.splitlines() if line)
    if result.returncode == 128 and "not a git repository" in result.stderr.lower():
        return None
    raise ValueError(f"git ls-files failed while collecting tracked files (exit {result.returncode})")


@lru_cache(maxsize=None)
def tracked_repo_file_set(root: Path) -> frozenset[str] | None:
    files = tracked_repo_files(root)
    return None if files is None else frozenset(files)


def module_name(csv_path: Path, root: Path) -> str:
    relative = csv_path.relative_to(root)
    target_index = relative.parts.index("target")
    module_parts = relative.parts[:target_index]
    return "/".join(module_parts) if module_parts else "."


def tracked_maven_module_dirs(root: Path) -> list[Path] | None:
    tracked = tracked_repo_files(root)
    if tracked is None:
        return None
    module_dirs = {
        Path(path).parent
        for path in tracked
        if path == "pom.xml" or path.endswith("/pom.xml")
    }
    return sorted(module_dirs, key=lambda path: path.as_posix())


def iter_module_jacoco_csvs_by_walk(root: Path) -> Iterable[Path]:
    for current, dirnames, _filenames in os.walk(root):
        dirnames[:] = sorted(dirname for dirname in dirnames if dirname not in VCS_DIR_NAMES)
        if "target" not in dirnames:
            continue
        candidate = Path(current) / "target" / "site" / "jacoco" / "jacoco.csv"
        if candidate.exists():
            yield candidate
        dirnames.remove("target")


def iter_module_jacoco_csvs(root: Path) -> Iterable[Path]:
    module_dirs = tracked_maven_module_dirs(root)
    if module_dirs:
        for module_dir in module_dirs:
            candidate = root / module_dir / "target" / "site" / "jacoco" / "jacoco.csv"
            if candidate.exists():
                yield candidate
        return
    yield from iter_module_jacoco_csvs_by_walk(root)


def collect_coverage_reports(root: Path) -> tuple[Coverage | None, list[Coverage]]:
    aggregate_path = root / AGGREGATE_CSV
    aggregate = (
        read_jacoco_line_coverage(aggregate_path, "open-asset reactor")
        if aggregate_path.exists()
        else None
    )

    module_reports: list[Coverage] = []
    for path in iter_module_jacoco_csvs(root):
        if "jacoco-aggregate" in path.parts:
            continue
        coverage = read_jacoco_line_coverage(path, module_name(path, root))
        if coverage is not None:
            module_reports.append(coverage)
    return aggregate, module_reports


def evaluate_coverage_target(
    aggregate: Coverage | None,
    target_percent: float = TARGET_LINE_PERCENT,
) -> CoverageTargetStatus:
    if aggregate is None:
        return CoverageTargetStatus(
            state="not claimable",
            message=(
                "Aggregate JaCoCo CSV is missing, so the scorecard cannot claim "
                "the 70% target from current measured data."
            ),
        )
    if aggregate.percent >= target_percent:
        return CoverageTargetStatus(
            state="met",
            message=(
                f"Measured aggregate line coverage is {aggregate.percent:.2f}%, "
                f"at or above the {target_percent:.1f}% target."
            ),
        )
    return CoverageTargetStatus(
        state="not met",
        message=(
            f"Measured aggregate line coverage is {aggregate.percent:.2f}%, "
            f"below the {target_percent:.1f}% target."
        ),
    )


def is_production_java_path(path: str) -> bool:
    parts = path.split("/")
    if not path.endswith(".java"):
        return False
    if not any(parts[index : index + 3] == ["src", "main", "java"] for index in range(len(parts) - 2)):
        return False
    if any(part in {"target", "build", "generated", "generated-sources", "drinkme"} for part in parts):
        return False
    return "/src/main/java/test/" not in f"/{path}"


def tracked_java_files(root: Path) -> list[str]:
    tracked = tracked_repo_files(root)
    if tracked is None:
        raise ValueError(
            "git ls-files failed while collecting Java hotspots "
            "(not a git repository)"
        )
    return [line for line in tracked if line.endswith(".java")]


def count_physical_lines(path: Path) -> int:
    line_count = 0
    saw_data = False
    last_byte = b""
    with path.open("rb") as handle:
        while True:
            chunk = handle.read(LINE_COUNT_CHUNK_SIZE)
            if not chunk:
                break
            saw_data = True
            line_count += chunk.count(b"\n")
            last_byte = chunk[-1:]
    return line_count + int(saw_data and last_byte != b"\n")


def find_production_hotspots(root: Path, line_limit: int = HOTSPOT_LINE_LIMIT) -> list[Hotspot]:
    hotspots: list[Hotspot] = []
    for relative_path in tracked_java_files(root):
        if not is_production_java_path(relative_path):
            continue
        path = root / relative_path
        try:
            line_count = count_physical_lines(path)
        except FileNotFoundError as exc:
            raise ValueError(f"tracked Java file is missing: {relative_path}") from exc
        if line_count > line_limit:
            hotspots.append(Hotspot(path=relative_path, line_count=line_count))
    return sorted(hotspots, key=lambda hotspot: (-hotspot.line_count, hotspot.path))


def load_qa_scenarios(root: Path) -> list[dict[str, object]]:
    validator = root / QA_VALIDATOR
    if not validator.exists():
        raise ValueError(f"QA scenario validator is missing: {QA_VALIDATOR}")

    result = subprocess.run(
        ["bash", str(validator), "--dump-json"],
        cwd=root,
        check=False,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise ValueError(
            "QA scenario validator failed while dumping JSON "
            f"(exit {result.returncode})"
        )
    try:
        scenarios = json.loads(result.stdout)
    except json.JSONDecodeError as exc:
        raise ValueError(f"QA scenario validator JSON is invalid: {exc.msg}") from exc
    if not isinstance(scenarios, list):
        raise ValueError("QA scenario validator JSON must be a list of scenarios")
    return scenarios


def normalize_scenario(raw: dict[str, object]) -> Scenario:
    if not isinstance(raw, dict):
        raise ValueError("QA scenario validator JSON contains a non-object scenario")
    scenario_id = raw.get("id")
    workflow = raw.get("workflow")
    mode = raw.get("automationMode")
    title = raw.get("title")
    if not all(isinstance(value, str) and value for value in (scenario_id, title, workflow, mode)):
        raise ValueError("QA scenario validator JSON contains a scenario with missing string fields")
    return Scenario(
        id=scenario_id,
        workflow=workflow,
        automation_mode=mode,
    )


def classify_journeys(scenarios: Iterable[dict[str, object]]) -> JourneySummary:
    counts: Counter[str] = Counter()
    manual_evidence_gaps: list[Scenario] = []
    gated_smoke_gaps: list[Scenario] = []
    for raw in scenarios:
        scenario = normalize_scenario(raw)
        counts[scenario.automation_mode] += 1
        if scenario.automation_mode == "manual-evidence-required":
            manual_evidence_gaps.append(scenario)
        elif scenario.automation_mode == "gated-command-smoke":
            gated_smoke_gaps.append(scenario)
    return JourneySummary(
        count_by_mode=dict(sorted(counts.items())),
        manual_evidence_gaps=sorted(manual_evidence_gaps, key=lambda scenario: scenario.id),
        gated_smoke_gaps=sorted(gated_smoke_gaps, key=lambda scenario: scenario.id),
    )


def is_git_tracked(root: Path, relative_path: Path) -> bool:
    tracked = tracked_repo_file_set(root)
    if tracked is None:
        return False
    return relative_path.as_posix() in tracked


def validate_corpus_entry(entry: object, index: int, root: Path) -> None:
    if not isinstance(entry, dict):
        raise ValueError(f"corpus manifest entry {index} must be an object")
    allowed = {
        "id",
        "path",
        "description",
        "generatedFixtureExpectations",
        "journeys",
        "notes",
    }
    required = {"id", "path", "description", "generatedFixtureExpectations"}
    unknown = sorted(set(entry) - allowed)
    if unknown:
        raise ValueError(f"corpus manifest entry {index} has unknown field(s): {', '.join(unknown)}")
    missing = sorted(required - set(entry))
    if missing:
        raise ValueError(f"corpus manifest entry {index} is missing field(s): {', '.join(missing)}")
    entry_id = entry["id"]
    if not isinstance(entry_id, str) or not CORPUS_ENTRY_ID_RE.fullmatch(entry_id):
        raise ValueError(f"corpus manifest entry {index} has invalid id")
    for field in ("path", "description"):
        if not isinstance(entry[field], str) or not entry[field].strip():
            raise ValueError(f"corpus manifest entry {index} field {field} must be a non-empty string")
    path = Path(entry["path"])
    if not path.parts or path.is_absolute() or any(part in {"", ".."} for part in path.parts):
        raise ValueError(f"corpus manifest entry {index} path must be repository-relative")
    if path.parts[0] != CORPUS_FIXTURE_ROOT.as_posix():
        raise ValueError(
            f"corpus manifest entry {index} path must be under {CORPUS_FIXTURE_ROOT.as_posix()}/"
        )
    if path.suffix.lower() not in CORPUS_FIXTURE_EXTENSIONS:
        raise ValueError(
            f"corpus manifest entry {index} path must end with .a3p, .a3w, or .a3c"
        )
    if is_git_tracked(root, path):
        raise ValueError(
            f"corpus manifest entry {index} path must not point to a checked-in payload"
        )
    expectations = entry["generatedFixtureExpectations"]
    if not isinstance(expectations, list) or not expectations:
        raise ValueError(f"corpus manifest entry {index} generatedFixtureExpectations must be a non-empty list")
    if not all(isinstance(expectation, str) and expectation.strip() for expectation in expectations):
        raise ValueError(
            f"corpus manifest entry {index} generatedFixtureExpectations must contain non-empty strings"
        )
    if "journeys" in entry:
        journeys = entry["journeys"]
        if not isinstance(journeys, list) or not journeys:
            raise ValueError(f"corpus manifest entry {index} journeys must be a non-empty list")
        if not all(isinstance(journey, str) and journey.strip() for journey in journeys):
            raise ValueError(f"corpus manifest entry {index} journeys must contain non-empty strings")
    if "notes" in entry and not isinstance(entry["notes"], str):
        raise ValueError(f"corpus manifest entry {index} notes must be a string")


def inspect_corpus_manifest(root: Path) -> CorpusManifestStatus:
    manifest_path = root / CORPUS_MANIFEST
    if not manifest_path.exists():
        return CorpusManifestStatus(
            state="missing",
            message="No checked-in LFS-independent corpus manifest found.",
        )
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"corpus manifest JSON is invalid: {exc.msg}") from exc
    if not isinstance(manifest, dict):
        raise ValueError("corpus manifest must be a JSON object")
    allowed = {"schemaVersion", "coverageStatement", "entries"}
    unknown = sorted(set(manifest) - allowed)
    if unknown:
        raise ValueError(f"corpus manifest has unknown field(s): {', '.join(unknown)}")
    if manifest.get("schemaVersion") != 1:
        raise ValueError("corpus manifest schemaVersion must be 1")
    coverage_statement = manifest.get("coverageStatement")
    if not isinstance(coverage_statement, str) or not coverage_statement.strip():
        raise ValueError("corpus manifest coverageStatement must be a non-empty string")
    normalized_statement = coverage_statement.lower()
    if (
        "representative" not in normalized_statement
        or "not full historical archive coverage" not in normalized_statement
    ):
        raise ValueError(
            "corpus manifest coverageStatement must state that coverage is representative "
            "and not full historical archive coverage"
        )
    entries = manifest.get("entries")
    if not isinstance(entries, list) or not entries:
        raise ValueError("corpus manifest entries must be a non-empty list")
    for index, entry in enumerate(entries, 1):
        validate_corpus_entry(entry, index, root)
    return CorpusManifestStatus(
        state="present",
        message=(
            f"Found {len(entries)} representative checked-in corpus manifest "
            f"entr{'y' if len(entries) == 1 else 'ies'}."
        ),
    )


def ratcheted_module_csv(module: str) -> Path:
    return Path(module) / "target/site/jacoco/jacoco.csv"


def render_module_rows(
    ratchets: CoverageRatchets,
    module_reports: list[Coverage],
) -> list[str]:
    reports_by_name = {coverage.name: coverage for coverage in module_reports}
    modules = sorted(set(ratchets.module_minimum_percent_by_module) | set(reports_by_name))
    rows: list[str] = []
    for module in modules:
        floor = ratchets.module_minimum_percent_by_module.get(module)
        coverage = reports_by_name.get(module)
        state = f"{coverage.percent:.2f}%" if coverage else "Missing measurement"
        rows.append(
            f"| {inline_code(module)} | {format_floor(floor)} | {inline_code(ratcheted_module_csv(module))} | {markdown_cell(state)} |"
        )
    if not rows:
        rows.append("| _none_ | n/a | n/a | No module JaCoCo CSVs or ratchets found |")
    return rows


def render_scenario_rows(scenarios: list[Scenario]) -> list[str]:
    if not scenarios:
        return ["| _none_ | n/a |"]
    return [
        f"| {inline_code(scenario.id)} | {inline_code(scenario.workflow)} |"
        for scenario in scenarios
    ]


def category_for_mode(mode: str) -> str:
    return MODE_CATEGORIES.get(mode, "Unclassified automation mode")


def render_scorecard(root: Path) -> str:
    workflow_file = root / WORKFLOW_PATH
    if not workflow_file.exists():
        raise ValueError(f"coverage workflow is missing: {WORKFLOW_PATH}")
    ratchets = parse_coverage_ratchets(workflow_file.read_text(encoding="utf-8"))
    aggregate, module_reports = collect_coverage_reports(root)
    target_status = evaluate_coverage_target(aggregate, TARGET_LINE_PERCENT)
    hotspots = find_production_hotspots(root, HOTSPOT_LINE_LIMIT)
    journey_summary = classify_journeys(load_qa_scenarios(root))
    corpus_status = inspect_corpus_manifest(root)

    missing_ratcheted_modules = [
        module
        for module in sorted(ratchets.module_minimum_percent_by_module)
        if not (root / ratcheted_module_csv(module)).exists()
    ]

    lines = [
        "# Alice Modernization Scorecard",
        "",
        "The Alice modernization scorecard is the repo-owned reference view of current",
        "modernization evidence. It reports what is measured, what is ratcheted, and",
        "what remains blocked without treating the long-term 70% line coverage target",
        "as current reality.",
        "",
        "## Generation",
        "",
        "Refresh this checked-in scorecard from the repository root:",
        "",
        "```sh",
        "python3 scripts/generate-modernization-scorecard.py \\",
        "  --output docs/reference/modernization-scorecard.md",
        "```",
        "",
        "Reviewers can reproduce this scorecard from an Alice modernization checkout",
        "with the repository script, then compare the checked-in scorecard with the",
        "generated output.",
        "",
        "See the [Modernization scorecard generator reference](./modernization-scorecard-generator.md)",
        "for the durable CLI contract, output-path safety behavior, examples, and",
        "review workflow.",
        "",
        "## Coverage ratchets",
        "",
        "Coverage ratchets are executable CI floors, not the long-term target. They are",
        "parsed from `.github/workflows/alice-coverage-ci.yml`, which runs the open-asset",
        "coverage summary with Git LFS disabled.",
        "",
        "| Scope | Current CI floor | Source |",
        "| --- | ---: | --- |",
        f"| Aggregate reactor | {format_floor(ratchets.aggregate_minimum_percent)} | `--min-aggregate-line-percent {ratchets.aggregate_minimum_percent:.1f}` |"
        if ratchets.aggregate_minimum_percent is not None
        else "| Aggregate reactor | n/a | No aggregate ratchet found |",
    ]
    lines.extend(
        f"| {inline_code(module)} | {format_floor(percent)} | {inline_code(f'--min-module-line-percent {module}={percent:.1f}')} |"
        for module, percent in ratchets.module_minimum_percent_by_module.items()
    )
    lines.extend(
        [
            "",
            "When these workflow values change, the generated scorecard changes with them.",
            "Do not manually maintain a second ratchet table.",
            "",
            "## Aggregate coverage state",
            "",
            "Aggregate coverage is measured only when this file exists in the inspected",
            "checkout:",
            "",
            "```text",
            str(AGGREGATE_CSV),
            "```",
            "",
            "| Measurement | State | Meaning |",
            "| --- | --- | --- |",
        ]
    )
    if aggregate is None:
        lines.extend(
            [
                "| Aggregate JaCoCo CSV | Missing | The open-asset Maven coverage lane has not produced aggregate coverage data in this checkout. |",
                "| Aggregate line coverage | Not measured | The scorecard cannot report a current aggregate percent without the CSV. |",
                f"| Aggregate CI ratchet | {format_floor(ratchets.aggregate_minimum_percent)} | The CI floor is still reported because it comes from the workflow. |",
            ]
        )
    else:
        lines.extend(
            [
                f"| Aggregate JaCoCo CSV | Present | `{AGGREGATE_CSV}` |",
                f"| Aggregate line coverage | {aggregate.percent:.2f}% | {aggregate.covered} covered, {aggregate.missed} missed, {aggregate.total} total lines. |",
                f"| Aggregate CI ratchet | {format_floor(ratchets.aggregate_minimum_percent)} | The CI floor is reported separately from the long-term target. |",
            ]
        )
    lines.extend(
        [
            "",
            "Missing aggregate coverage is a blocker for claiming coverage progress, but it",
            "is not a scorecard generation failure.",
            "",
            "## Module coverage state",
            "",
            "Module coverage is measured from `target/site/jacoco/jacoco.csv` files under",
            "module directories. A module with a CI ratchet is listed even when its local",
            "CSV is missing, because losing a ratcheted module report is itself a gap.",
            "",
            "| Module | CI floor | Expected CSV | State |",
            "| --- | ---: | --- | --- |",
        ]
    )
    lines.extend(render_module_rows(ratchets, module_reports))
    lines.extend(
        [
            "",
            "Rows with no line totals are ignored rather than converted to false zero",
            "coverage. This matches the coverage summary contract.",
            "",
            "## 70% target status",
            "",
            "The 70% line coverage goal is the modernization mission target. It is not the",
            "current CI ratchet and is not considered met unless measured aggregate JaCoCo",
            "coverage is at least `70.0%`.",
            "",
            "| Target | State | Evidence |",
            "| --- | --- | --- |",
            f"| {TARGET_LINE_PERCENT:.1f}% aggregate line coverage | {display_state(target_status.state)} | {target_status.message} |",
            "",
            "## Production hotspots over 500 lines",
            "",
            "A production hotspot is a tracked Java file with more than 500 physical lines",
            "using this deterministic filter:",
            "",
            "1. Start from `git ls-files '*.java'`.",
            "2. Include paths containing `/src/main/java/`.",
            "3. Exclude paths containing `/target/`, `/build/`, `/generated/`, `/generated-sources/`, `/drinkme/`, or `/src/main/java/test/`.",
            "4. Include only files with line count greater than 500.",
            "5. Sort by descending line count, then by path.",
            "",
            f"Current scorecard state for this checkout: {len(hotspots)} production-root Java hotspots over 500 lines.",
            "",
            "| File | Lines |",
            "| --- | ---: |",
        ]
    )
    if hotspots:
        lines.extend(f"| {inline_code(hotspot.path)} | {hotspot.line_count} |" for hotspot in hotspots)
    else:
        lines.append("| _none_ | 0 |")
    lines.extend(
        [
            "",
            "Hotspot rows are not automatic refactor instructions. A hotspot is a blocker",
            "only when its size prevents safe characterization, review, or focused",
            "modernization work. Production refactors still require behavior",
            "characterization before code moves.",
            "",
            "## QA journey automation gaps",
            "",
            "Journey status comes from:",
            "",
            "```sh",
            "qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json",
            "```",
            "",
            "The validator returns the normalized scenario catalog. The scorecard groups",
            "scenarios by `automationMode` and treats manual evidence and gated command",
            "smokes as remaining evidence gaps.",
            "",
            "| Automation mode | Count | Scorecard category |",
            "| --- | ---: | --- |",
        ]
    )
    ordered_modes = ["xvfb-real-alice", "gated-command-smoke", "manual-evidence-required"]
    for mode in ordered_modes:
        if mode in journey_summary.count_by_mode:
            lines.append(
                f"| {inline_code(mode)} | {journey_summary.count_by_mode[mode]} | {markdown_cell(category_for_mode(mode))} |"
            )
    for mode in sorted(set(journey_summary.count_by_mode) - set(ordered_modes)):
        lines.append(f"| {inline_code(mode)} | {journey_summary.count_by_mode[mode]} | {markdown_cell(category_for_mode(mode))} |")
    lines.extend(
        [
            "",
            "Manual evidence gaps:",
            "",
            "| Scenario | Workflow |",
            "| --- | --- |",
        ]
    )
    lines.extend(render_scenario_rows(journey_summary.manual_evidence_gaps))
    lines.extend(
        [
            "",
            "Gated command smoke gaps:",
            "",
            "| Scenario | Workflow |",
            "| --- | --- |",
        ]
    )
    lines.extend(render_scenario_rows(journey_summary.gated_smoke_gaps))
    lines.extend(
        [
            "",
            "Gated command smokes are automation coverage, but they remain evidence gaps",
            "when `ALICE_QA_RUN_GATED_SMOKES=1` has not been used or when the required",
            "artifacts are unavailable in the local environment.",
            "",
            "## Corpus gaps",
            "",
            "The scorecard looks for a checked-in, LFS-independent corpus manifest at:",
            "",
            "```text",
            str(CORPUS_MANIFEST),
            "```",
            "",
            "| Corpus signal | State | Meaning |",
            "| --- | --- | --- |",
            f"| LFS-independent corpus manifest | {display_state(corpus_status.state)} | {corpus_status.message} |",
            "| Git LFS payloads | Not required | The scorecard does not fetch or inspect large binary project files. |",
            "",
        ]
    )
    if corpus_status.state == "missing":
        lines.extend(
            [
                "Until a manifest exists, corpus coverage cannot be scored. The accepted fix is",
                "a small text manifest that describes the corpus entries, expected metadata,",
                "and how each entry maps to modernization journeys without embedding large",
                "project payloads.",
            ]
        )
    else:
        lines.append(
            "Corpus coverage is representative manifest evidence only; it is not full historical archive coverage "
            "and does not depend on local LFS payload availability."
        )
    lines.extend(
        [
            "",
            "## Remaining blockers",
            "",
            "| Blocker | Current state | Required movement |",
            "| --- | --- | --- |",
        ]
    )
    if aggregate is None:
        lines.append("| Aggregate coverage measurement | Missing aggregate JaCoCo CSV | Run the open-asset coverage lane and regenerate the scorecard. |")
    elif aggregate.percent < (ratchets.aggregate_minimum_percent or 0.0):
        lines.append("| Aggregate coverage measurement | Measured below the CI ratchet | Restore aggregate coverage above the executable floor. |")
    else:
        lines.append("| Aggregate coverage measurement | Available | Keep regenerating the scorecard from current JaCoCo CSVs before claiming progress. |")
    if missing_ratcheted_modules:
        lines.append(
            f"| Ratcheted module measurements | Missing module JaCoCo CSVs for {len(missing_ratcheted_modules)} ratcheted modules in this checkout | Run the open-asset coverage lane and confirm each ratcheted module still emits a report. |"
        )
    else:
        lines.append("| Ratcheted module measurements | Available for all ratcheted modules | Keep module CSVs attached to the coverage workflow artifacts. |")
    lines.append(
        f"| 70% target evidence | {display_state(target_status.state)} | Produce aggregate measured coverage at or above {TARGET_LINE_PERCENT:.1f}% before marking the target met. |"
    )
    lines.extend(
        [
            f"| Production hotspots | {len(hotspots)} files over 500 lines | Characterize behavior first; refactor only protected hotspots in focused changes. |",
            f"| Manual QA journeys | {len(journey_summary.manual_evidence_gaps)} scenarios require manual evidence | Add stable automation or collect accepted manual evidence for each workflow. |",
            f"| Gated QA smokes | {len(journey_summary.gated_smoke_gaps)} smokes are gated by local prerequisites | Run with `ALICE_QA_RUN_GATED_SMOKES=1` where prerequisites exist, or attach equivalent CI evidence. |",
        ]
    )
    if corpus_status.state == "missing":
        lines.append("| Corpus manifest | Missing LFS-independent manifest | Add a small checked-in manifest before claiming corpus coverage. |")
    else:
        lines.append("| Corpus manifest | Present | Keep manifest entries mapped to representative modernization journeys. |")
    lines.extend(
        [
            "",
            "## Interpretation notes",
            "",
            "Use the scorecard as an evidence index, not as a single pass/fail badge.",
            "",
            "Coverage ratchets answer \"what regressions does CI prevent today?\" The 70%",
            "target status answers \"can the project honestly claim the mission target",
            "today?\" Those are different questions. A low aggregate ratchet can be healthy",
            "when it matches measured coverage with margin, and the 70% target must remain",
            "not met or not claimable until current aggregate data proves otherwise.",
            "",
            "Missing coverage reports are explicit blockers because they prevent",
            "measurement. They are not interpreted as zero coverage and are not hidden",
            "behind stale values.",
            "",
            "Hotspots identify review and characterization risk. They do not authorize",
            "production refactors by themselves. Follow the protected hotspot rule in",
            "[Testing](../testing.md) before moving",
            "production behavior.",
            "",
            "Journey gaps are derived from the outside-in QA scenario catalog, not from",
            "prose summaries. A manual scenario remains a gap until there is accepted",
            "evidence or a stable automation mode. A gated smoke remains partial coverage",
            "until it runs in an environment with the required prerequisites.",
            "",
            "Corpus gaps are intentionally conservative. The scorecard can run without Git",
            "LFS, so representative corpus coverage must be described by a small checked-in",
            "manifest rather than inferred from local binary payloads.",
        ]
    )
    return "\n".join(lines) + "\n"


def resolve_output_path(root: Path, output: Path, parser: argparse.ArgumentParser) -> Path:
    candidate = output if output.is_absolute() else root / output
    resolved_output = candidate.resolve()
    try:
        resolved_output.relative_to(root)
    except ValueError:
        parser.error(f"--output must resolve inside --root: {output}")
    return resolved_output


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="repository root to inspect")
    parser.add_argument("--output", type=Path, help="write Markdown scorecard to this path")
    args = parser.parse_args(argv)

    root = args.root.resolve()
    if not root.is_dir():
        parser.error(f"--root must be an existing directory: {args.root}")

    output = resolve_output_path(root, args.output, parser) if args.output else None

    try:
        markdown = render_scorecard(root)
    except ValueError as exc:
        print(exc, file=sys.stderr)
        return 1

    if output:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(markdown, encoding="utf-8")
    else:
        print(markdown, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
