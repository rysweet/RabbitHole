import importlib.util
import json
import subprocess
import sys
import tempfile
import textwrap
import unittest
from functools import lru_cache
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "generate-modernization-scorecard.py"


WORKFLOW_TEXT = textwrap.dedent(
    """\
    name: Alice coverage CI
    jobs:
      coverage:
        steps:
          - name: Summarize and gate line coverage
            run: |
              python3 scripts/summarize-jacoco-coverage.py \\
                --output coverage-summary.md \\
                --min-aggregate-line-percent 8.0 \\
                --min-module-line-percent core/ast=18.0 \\
                --min-module-line-percent core/model-loading=10.0 \\
                --min-module-line-percent core/story-api-migration=75.0 \\
                --min-module-line-percent core/tweedle=50.0 \\
                --min-module-line-percent netbeans=25.0
    """
)


SECTION_ORDER = [
    "# Alice Modernization Scorecard",
    "## Generation",
    "## Coverage ratchets",
    "## Aggregate coverage state",
    "## Module coverage state",
    "## 70% target status",
    "## Production hotspots over 500 lines",
    "## QA journey automation gaps",
    "## Corpus gaps",
    "## Remaining blockers",
    "## Interpretation notes",
]


FORBIDDEN_REVIEWER_INSTRUCTION_PHRASES = (
    "ampli" + "hack alice-scorecard",
    "branch-installable wrapper",
)

GENERATED_SCORECARD_REFERENCE_PHRASES = (
    "### CLI reference",
    "### Examples",
    "Preview the scorecard without modifying files",
    "Compare the current branch with another worktree",
    "Generate a review artifact under the inspected checkout",
    "export NODE_OPTIONS",
)


@lru_cache(maxsize=1)
def load_generator():
    if not SCRIPT_PATH.exists():
        raise AssertionError(f"Expected scorecard generator at {SCRIPT_PATH}")
    spec = importlib.util.spec_from_file_location("generate_modernization_scorecard", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def assert_scorecard_uses_plain_reviewer_instructions(
    test_case: unittest.TestCase,
    markdown: str,
) -> None:
    test_case.assertIn("python3 scripts/generate-modernization-scorecard.py", markdown)
    test_case.assertIn(
        "Reviewers can reproduce this scorecard from an Alice modernization checkout",
        markdown,
    )
    test_case.assertIn(
        "Modernization scorecard generator reference",
        markdown,
    )
    test_case.assertIn(
        "./modernization-scorecard-generator.md",
        markdown,
    )
    for phrase in GENERATED_SCORECARD_REFERENCE_PHRASES:
        test_case.assertNotIn(phrase, markdown)
    lower_markdown = markdown.lower()
    for phrase in FORBIDDEN_REVIEWER_INSTRUCTION_PHRASES:
        test_case.assertNotIn(phrase, lower_markdown)


def write_jacoco_csv(root: Path, relative_path: str, rows: list[tuple[int, int]]) -> Path:
    csv_path = root / relative_path
    csv_path.parent.mkdir(parents=True, exist_ok=True)
    lines = ["LINE_MISSED,LINE_COVERED"]
    lines.extend(f"{missed},{covered}" for missed, covered in rows)
    csv_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return csv_path


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")


def write_java_file(root: Path, relative_path: str, line_count: int) -> None:
    body = "\n".join(f"// line {index}" for index in range(line_count))
    write_file(root / relative_path, body + "\n")


def initialize_git_repo(root: Path) -> None:
    subprocess.run(["git", "init", "-q"], cwd=root, check=True)
    subprocess.run(["git", "config", "user.email", "tests@example.invalid"], cwd=root, check=True)
    subprocess.run(["git", "config", "user.name", "Scorecard Tests"], cwd=root, check=True)
    subprocess.run(["git", "add", "."], cwd=root, check=True)


def write_workflow(root: Path, workflow_text: str = WORKFLOW_TEXT) -> None:
    write_file(root / ".github" / "workflows" / "alice-coverage-ci.yml", workflow_text)


def write_validator(root: Path, scenarios: list[dict[str, object]] | str) -> None:
    script = root / "qa" / "outside-in" / "alice-desktop" / "runners" / "validate-scenarios.sh"
    script.parent.mkdir(parents=True, exist_ok=True)
    if isinstance(scenarios, str):
        payload = scenarios
    else:
        payload = json.dumps(scenarios, sort_keys=True)
    script.write_text(
        textwrap.dedent(
            f"""\
            #!/usr/bin/env bash
            set -euo pipefail
            if [[ "${{1:-}}" == "--dump-json" ]]; then
              printf '%s\\n' {json.dumps(payload)}
              exit 0
            fi
            echo "expected --dump-json" >&2
            exit 2
            """
        ),
        encoding="utf-8",
    )
    script.chmod(0o755)


def write_corpus_manifest(root: Path, entries: list[dict[str, object]] | None = None) -> Path:
    manifest_path = root / "docs" / "reference" / "modernization-corpus-manifest.json"
    write_file(
        manifest_path,
        json.dumps(
            {
                "schemaVersion": 1,
                "coverageStatement": (
                    "Representative corpus evidence only; this is not full historical archive coverage."
                ),
                "entries": entries
                if entries is not None
                else [
                    {
                        "id": "generated-project-xml-fallback",
                        "path": "generated-fixtures/project-io/generated-project-xml-fallback.a3p",
                        "description": "Representative generated project archive behavior.",
                        "generatedFixtureExpectations": [
                            "Generated fixture includes version.txt and programType.xml entries."
                        ],
                        "journeys": ["project-io-corpus"],
                    }
                ],
            },
            sort_keys=True,
        ),
    )
    return manifest_path


def standard_scenarios() -> list[dict[str, object]]:
    return [
        {
            "id": "alice-desktop-launch",
            "title": "Launch Alice",
            "workflow": "launch",
            "automationMode": "xvfb-real-alice",
        },
        {
            "id": "alice-desktop-save-command",
            "title": "Save command smoke",
            "workflow": "save-load",
            "automationMode": "gated-command-smoke",
        },
        {
            "id": "alice-desktop-export",
            "title": "Export Alice project output",
            "workflow": "export",
            "automationMode": "manual-evidence-required",
        },
    ]


class ModernizationScorecardComponentTest(unittest.TestCase):
    def test_parse_coverage_ratchets_extracts_ci_thresholds(self) -> None:
        generator = load_generator()

        ratchets = generator.parse_coverage_ratchets(WORKFLOW_TEXT)

        self.assertEqual(8.0, ratchets.aggregate_minimum_percent)
        self.assertEqual(
            {
                "core/ast": 18.0,
                "core/model-loading": 10.0,
                "core/story-api-migration": 75.0,
                "core/tweedle": 50.0,
                "netbeans": 25.0,
            },
            ratchets.module_minimum_percent_by_module,
        )

    def test_parse_coverage_ratchets_rejects_out_of_range_thresholds(self) -> None:
        generator = load_generator()

        with self.assertRaisesRegex(ValueError, "between 0.0 and 100.0"):
            generator.parse_coverage_ratchets("--min-aggregate-line-percent 101.0")

        with self.assertRaisesRegex(ValueError, "between 0.0 and 100.0"):
            generator.parse_coverage_ratchets("--min-module-line-percent core/ast=101.0")

    def test_jacoco_reader_matches_summary_semantics_and_ignores_empty_totals(self) -> None:
        generator = load_generator()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            measured_csv = write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(10, 30), (5, 55)])
            empty_csv = write_jacoco_csv(root, "core/empty/target/site/jacoco/jacoco.csv", [(0, 0)])

            coverage = generator.read_jacoco_line_coverage(measured_csv, "core/ast")
            empty = generator.read_jacoco_line_coverage(empty_csv, "core/empty")

        self.assertIsNotNone(coverage)
        assert coverage is not None
        self.assertEqual("core/ast", coverage.name)
        self.assertEqual(85, coverage.covered)
        self.assertEqual(15, coverage.missed)
        self.assertAlmostEqual(85.0, coverage.percent)
        self.assertIsNone(empty)

    def test_jacoco_reader_reports_malformed_csv_without_traceback(self) -> None:
        generator = load_generator()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            missing_column = root / "missing-column.csv"
            non_integer = root / "non-integer.csv"
            negative = root / "negative.csv"
            write_file(missing_column, "LINE_MISSED\n1\n")
            write_file(non_integer, "LINE_MISSED,LINE_COVERED\n1,nope\n")
            write_file(negative, "LINE_MISSED,LINE_COVERED\n1,-1\n")

            with self.assertRaisesRegex(ValueError, "missing required column LINE_COVERED"):
                generator.read_jacoco_line_coverage(missing_column, "core/ast")
            with self.assertRaisesRegex(ValueError, "column LINE_COVERED must be an integer"):
                generator.read_jacoco_line_coverage(non_integer, "core/ast")
            with self.assertRaisesRegex(ValueError, "column LINE_COVERED must be non-negative"):
                generator.read_jacoco_line_coverage(negative, "core/ast")

    def test_coverage_report_collection_prunes_target_contents_after_jacoco_probe(self) -> None:
        generator = load_generator()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(10, 90)])
            nested_target_csv = (
                root
                / "core"
                / "ast"
                / "target"
                / "generated"
                / "nested"
                / "target"
                / "site"
                / "jacoco"
                / "jacoco.csv"
            )
            write_file(
                nested_target_csv,
                "LINE_MISSED,LINE_COVERED\n1,99\n",
            )
            write_jacoco_csv(root, "coverage-report/target/site/jacoco-aggregate/jacoco.csv", [(35, 65)])

            aggregate, reports = generator.collect_coverage_reports(root)

        self.assertIsNotNone(aggregate)
        self.assertEqual(["core/ast"], [coverage.name for coverage in reports])

    def test_coverage_report_collection_uses_tracked_maven_modules_when_available(self) -> None:
        generator = load_generator()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_file(root / "core/ast/pom.xml", "<project />\n")
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(10, 90)])
            write_jacoco_csv(root, "scratch/target/site/jacoco/jacoco.csv", [(1, 999)])
            initialize_git_repo(root)

            _aggregate, reports = generator.collect_coverage_reports(root)

        self.assertEqual(["core/ast"], [coverage.name for coverage in reports])

    def test_coverage_target_status_is_conservative_until_aggregate_is_measured(self) -> None:
        generator = load_generator()

        missing = generator.evaluate_coverage_target(None, target_percent=70.0)
        low = generator.evaluate_coverage_target(
            generator.Coverage(
                name="open-asset reactor",
                covered=699,
                missed=301,
                source=Path("coverage-report/target/site/jacoco-aggregate/jacoco.csv"),
            ),
            target_percent=70.0,
        )
        met = generator.evaluate_coverage_target(
            generator.Coverage(
                name="open-asset reactor",
                covered=700,
                missed=300,
                source=Path("coverage-report/target/site/jacoco-aggregate/jacoco.csv"),
            ),
            target_percent=70.0,
        )

        self.assertEqual("not claimable", missing.state)
        self.assertIn("cannot claim", missing.message.lower())
        self.assertEqual("not met", low.state)
        self.assertIn("69.90%", low.message)
        self.assertEqual("met", met.state)
        self.assertIn("70.00%", met.message)

    def test_hotspot_detector_filters_tracked_production_java_over_500_lines(self) -> None:
        generator = load_generator()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_java_file(root, "core/app/src/main/java/org/example/Alpha.java", 600)
            write_java_file(root, "core/app/src/main/java/org/example/Beta.java", 600)
            write_java_file(root, "core/app/src/main/java/org/example/Gamma.java", 501)
            write_file(
                root / "core/app/src/main/java/org/example/NoTrailingNewline.java",
                "\n".join("// line" for _ in range(501)),
            )
            write_java_file(root, "core/app/src/main/java/org/example/ExactLimit.java", 500)
            write_java_file(root, "core/app/src/test/java/org/example/LargeTest.java", 900)
            write_java_file(root, "core/app/generated/src/main/java/org/example/Generated.java", 900)
            write_java_file(root, "core/app/src/main/java/test/Fixture.java", 900)
            write_java_file(root, "drinkme/src/main/java/org/example/Investigation.java", 900)
            initialize_git_repo(root)

            hotspots = generator.find_production_hotspots(root, line_limit=500)

        self.assertEqual(
            [
                ("core/app/src/main/java/org/example/Alpha.java", 600),
                ("core/app/src/main/java/org/example/Beta.java", 600),
                ("core/app/src/main/java/org/example/Gamma.java", 501),
                ("core/app/src/main/java/org/example/NoTrailingNewline.java", 501),
            ],
            [(hotspot.path, hotspot.line_count) for hotspot in hotspots],
        )

    def test_journey_classifier_reports_manual_and_gated_gaps(self) -> None:
        generator = load_generator()

        summary = generator.classify_journeys(standard_scenarios())

        self.assertEqual(
            {
                "gated-command-smoke": 1,
                "manual-evidence-required": 1,
                "xvfb-real-alice": 1,
            },
            summary.count_by_mode,
        )
        self.assertEqual(
            ["alice-desktop-export"],
            [scenario.id for scenario in summary.manual_evidence_gaps],
        )
        self.assertEqual(
            ["alice-desktop-save-command"],
            [scenario.id for scenario in summary.gated_smoke_gaps],
        )

    def test_journey_classifier_requires_title_field_from_validator_contract(self) -> None:
        generator = load_generator()
        scenario = {
            "id": "alice-desktop-export",
            "workflow": "export",
            "automationMode": "manual-evidence-required",
        }

        with self.assertRaisesRegex(ValueError, "missing string fields"):
            generator.classify_journeys([scenario])

    def test_markdown_rendering_escapes_table_cells_from_repo_metadata(self) -> None:
        generator = load_generator()
        ratchets = generator.CoverageRatchets(
            aggregate_minimum_percent=None,
            module_minimum_percent_by_module={"core/weird|`module`": 10.0},
        )
        rows = generator.render_module_rows(ratchets, [])
        scenarios = [
            generator.Scenario(
                id="manual|scenario",
                workflow="save\nload`workflow`",
                automation_mode="manual-evidence-required",
            )
        ]

        self.assertEqual(
            "| `` core/weird\\|`module` `` | 10.0% | `` core/weird\\|`module`/target/site/jacoco/jacoco.csv `` | Missing measurement |",
            rows[0],
        )
        self.assertEqual(
            ["| `manual\\|scenario` | `` save load`workflow` `` |"],
            generator.render_scenario_rows(scenarios),
        )

    def test_corpus_manifest_missing_is_a_gap_and_invalid_metadata_is_rejected(self) -> None:
        generator = load_generator()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)

            missing = generator.inspect_corpus_manifest(root)

            self.assertEqual("missing", missing.state)
            self.assertIn("No checked-in LFS-independent corpus manifest found", missing.message)

            invalid_entries = (
                (
                    r"missing field\(s\): description",
                    {
                        "id": "starter-scene",
                        "path": "generated-fixtures/starter-scene.a3p",
                        "generatedFixtureExpectations": ["Generated fixture exists in tests."],
                    },
                ),
                (
                    "field path must be a non-empty string",
                    {
                        "id": "starter-scene",
                        "path": " ",
                        "description": "Starter scene generated archive.",
                        "generatedFixtureExpectations": ["Generated fixture exists in tests."],
                    },
                ),
                (
                    "path must be repository-relative",
                    {
                        "id": "starter-scene",
                        "path": "../outside/starter-scene.a3p",
                        "description": "Starter scene generated archive.",
                        "generatedFixtureExpectations": ["Generated fixture exists in tests."],
                    },
                ),
                (
                    "path must be repository-relative",
                    {
                        "id": "starter-scene",
                        "path": ".",
                        "description": "Starter scene generated archive.",
                        "generatedFixtureExpectations": ["Generated fixture exists in tests."],
                    },
                ),
                (
                    "field description must be a non-empty string",
                    {
                        "id": "starter-scene",
                        "path": "generated-fixtures/starter-scene.a3p",
                        "description": "",
                        "generatedFixtureExpectations": ["Generated fixture exists in tests."],
                    },
                ),
                (
                    "generatedFixtureExpectations must be a non-empty list",
                    {
                        "id": "starter-scene",
                        "path": "generated-fixtures/starter-scene.a3p",
                        "description": "Starter scene generated archive.",
                        "generatedFixtureExpectations": [],
                    },
                ),
                (
                    "generatedFixtureExpectations must contain non-empty strings",
                    {
                        "id": "starter-scene",
                        "path": "generated-fixtures/starter-scene.a3p",
                        "description": "Starter scene generated archive.",
                        "generatedFixtureExpectations": [" "],
                    },
                ),
                (
                    "unknown field",
                    {
                        "id": "starter-scene",
                        "path": "generated-fixtures/starter-scene.a3p",
                        "description": "Starter scene generated archive.",
                        "generatedFixtureExpectations": ["Generated fixture exists in tests."],
                        "unexpected": "must fail",
                    },
                ),
            )

            for expected_error, entry in invalid_entries:
                with self.subTest(expected_error=expected_error):
                    write_corpus_manifest(root, [entry])
                    with self.assertRaisesRegex(ValueError, expected_error):
                        generator.inspect_corpus_manifest(root)

    def test_corpus_manifest_present_requires_representative_scope_statement(self) -> None:
        generator = load_generator()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)

            write_corpus_manifest(root)
            present = generator.inspect_corpus_manifest(root)

            self.assertEqual("present", present.state)
            self.assertIn("representative checked-in corpus manifest", present.message)

            manifest_path = root / "docs" / "reference" / "modernization-corpus-manifest.json"
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
            manifest["coverageStatement"] = "Complete corpus coverage."
            write_file(manifest_path, json.dumps(manifest))

            with self.assertRaisesRegex(ValueError, "representative"):
                generator.inspect_corpus_manifest(root)

    def test_corpus_manifest_paths_are_generated_alice_archive_fixture_shapes(self) -> None:
        generator = load_generator()
        base_entry = {
            "id": "starter-scene",
            "path": "generated-fixtures/project-io/starter-scene.a3p",
            "description": "Starter scene generated archive shape.",
            "generatedFixtureExpectations": ["Generated fixture exists only as deterministic test output."],
        }
        invalid_paths = (
            ("docs/reference/starter-scene.a3p", "must be under generated-fixtures/"),
            ("generated-fixtures/project-io/starter-scene.txt", r"must end with \.a3p, \.a3w, or \.a3c"),
            ("generated-fixtures/project-io/starter-scene.png", r"must end with \.a3p, \.a3w, or \.a3c"),
        )

        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)

            for path, expected_error in invalid_paths:
                with self.subTest(path=path):
                    entry = dict(base_entry, path=path)
                    write_corpus_manifest(root, [entry])

                    with self.assertRaisesRegex(ValueError, expected_error):
                        generator.inspect_corpus_manifest(root)

    def test_corpus_manifest_rejects_checked_in_payload_at_manifest_path(self) -> None:
        generator = load_generator()
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            payload_path = root / "generated-fixtures" / "project-io" / "starter-scene.a3p"
            write_file(payload_path, "not a real Alice archive payload\n")
            write_corpus_manifest(
                root,
                [
                    {
                        "id": "starter-scene",
                        "path": "generated-fixtures/project-io/starter-scene.a3p",
                        "description": "Starter scene generated archive shape.",
                        "generatedFixtureExpectations": [
                            "Generated fixture exists only as deterministic test output."
                        ],
                    }
                ],
            )
            initialize_git_repo(root)

            with self.assertRaisesRegex(ValueError, "must not point to a checked-in payload"):
                generator.inspect_corpus_manifest(root)


class ModernizationScorecardCliTest(unittest.TestCase):
    def test_cli_generates_deterministic_scorecard_without_lfs_or_jacoco_payloads(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_workflow(root)
            write_validator(root, standard_scenarios())
            write_corpus_manifest(root)
            write_java_file(root, "core/app/src/main/java/org/example/Alpha.java", 501)
            write_file(root / "docs" / "index.md", "- [Alice modernization scorecard](./reference/modernization-scorecard.md)\n")
            initialize_git_repo(root)
            output = root / "docs" / "reference" / "modernization-scorecard.md"

            command = [
                sys.executable,
                str(SCRIPT_PATH),
                "--root",
                str(root),
                "--output",
                "docs/reference/modernization-scorecard.md",
            ]
            first = subprocess.run(command, check=False, capture_output=True, text=True)
            first_markdown = output.read_text(encoding="utf-8") if output.exists() else ""
            second = subprocess.run(command, check=False, capture_output=True, text=True)
            second_markdown = output.read_text(encoding="utf-8") if output.exists() else ""

        self.assertEqual(0, first.returncode, first.stderr)
        self.assertEqual(0, second.returncode, second.stderr)
        self.assertEqual("", first.stdout)
        self.assertEqual("", second.stdout)
        self.assertEqual(first_markdown, second_markdown)
        self.assertTrue(first_markdown.endswith("\n"))
        self.assertFalse(first_markdown.endswith("\n\n"))

        previous_index = -1
        for heading in SECTION_ORDER:
            with self.subTest(heading=heading):
                current_index = first_markdown.index(heading)
                self.assertGreater(current_index, previous_index)
                previous_index = current_index

        self.assertIn("| Aggregate JaCoCo CSV | Missing |", first_markdown)
        self.assertIn("| `core/ast` | 18.0% | `core/ast/target/site/jacoco/jacoco.csv` | Missing measurement |", first_markdown)
        self.assertIn("| 70.0% aggregate line coverage | Not claimable |", first_markdown)
        self.assertIn("| `core/app/src/main/java/org/example/Alpha.java` | 501 |", first_markdown)
        self.assertIn("| `manual-evidence-required` | 1 | Manual evidence gap |", first_markdown)
        self.assertIn("| `alice-desktop-export` | `export` |", first_markdown)
        self.assertIn("| LFS-independent corpus manifest | Present |", first_markdown)
        self.assertIn("representative checked-in corpus manifest", first_markdown)
        self.assertIn("not full historical archive coverage", first_markdown)
        self.assertIn("Coverage ratchets are executable CI floors, not the long-term target.", first_markdown)
        assert_scorecard_uses_plain_reviewer_instructions(self, first_markdown)
        self.assertNotIn(str(Path(tempfile.gettempdir())), first_markdown)

    def test_cli_rejects_output_traversal_outside_root_before_writing(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir) / "one" / "two" / "repo"
            root.mkdir(parents=True)
            unsafe_output = "../../../tmp/out.md"
            escaped_output = (root / unsafe_output).resolve()

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--output", unsafe_output],
                check=False,
                capture_output=True,
                text=True,
            )

            self.assertEqual(2, result.returncode)
            self.assertEqual("", result.stdout)
            self.assertIn("error: --output must resolve inside --root", result.stderr)
            self.assertNotIn("Traceback", result.stderr)
            self.assertFalse(escaped_output.exists())

    def test_cli_accepts_relative_and_absolute_outputs_inside_root(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_workflow(root)
            write_validator(root, standard_scenarios())
            initialize_git_repo(root)
            relative_output = Path("review-artifacts/relative-scorecard.md")
            absolute_output = root / "review-artifacts" / "absolute-scorecard.md"

            cases = (
                (relative_output, root / relative_output),
                (absolute_output, absolute_output),
            )
            results = [
                (
                    expected_output,
                    subprocess.run(
                        [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--output", str(output_arg)],
                        check=False,
                        capture_output=True,
                        text=True,
                    ),
                )
                for output_arg, expected_output in cases
            ]

            for output, result in results:
                with self.subTest(output=output):
                    self.assertEqual(0, result.returncode, result.stderr)
                    self.assertEqual("", result.stdout)
                    self.assertTrue(output.exists())
                    self.assertIn("# Alice Modernization Scorecard", output.read_text(encoding="utf-8"))

    def test_cli_reports_measured_aggregate_coverage_without_pretending_low_coverage_meets_70_percent(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_workflow(root)
            write_validator(root, standard_scenarios())
            write_jacoco_csv(root, "coverage-report/target/site/jacoco-aggregate/jacoco.csv", [(35, 65)])
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(10, 90)])
            initialize_git_repo(root)
            output = root / "scorecard.md"

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--output", str(output)],
                check=False,
                capture_output=True,
                text=True,
            )
            markdown = output.read_text(encoding="utf-8") if output.exists() else ""

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("| Aggregate line coverage | 65.00% |", markdown)
        self.assertIn("| `core/ast` | 18.0% | `core/ast/target/site/jacoco/jacoco.csv` | 90.00% |", markdown)
        self.assertIn("| 70.0% aggregate line coverage | Not met |", markdown)
        self.assertNotIn("| 70.0% aggregate line coverage | Met |", markdown)

    def test_cli_fails_for_invalid_qa_validator_json_contract(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_workflow(root)
            write_validator(root, "not json")
            initialize_git_repo(root)
            output = root / "scorecard.md"

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--output", str(output)],
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("QA scenario validator JSON", result.stderr)
        self.assertFalse(output.exists())

    def test_cli_fails_cleanly_for_non_object_qa_scenario(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_workflow(root)
            write_validator(root, ["not an object"])
            initialize_git_repo(root)
            output = root / "scorecard.md"

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--output", str(output)],
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("non-object scenario", result.stderr)
        self.assertNotIn("Traceback", result.stderr)
        self.assertFalse(output.exists())

    def test_cli_validator_failure_does_not_echo_raw_stdout_or_stderr(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_workflow(root)
            script = root / "qa" / "outside-in" / "alice-desktop" / "runners" / "validate-scenarios.sh"
            script.parent.mkdir(parents=True, exist_ok=True)
            script.write_text(
                textwrap.dedent(
                    """\
                    #!/usr/bin/env bash
                    echo "stdout secret token" >&1
                    echo "stderr secret token" >&2
                    exit 7
                    """
                ),
                encoding="utf-8",
            )
            script.chmod(0o755)
            initialize_git_repo(root)
            output = root / "scorecard.md"

            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "--root", str(root), "--output", str(output)],
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertNotEqual(0, result.returncode)
        self.assertIn("QA scenario validator failed while dumping JSON (exit 7)", result.stderr)
        self.assertNotIn("secret token", result.stderr)
        self.assertNotIn("Traceback", result.stderr)
        self.assertFalse(output.exists())


class ModernizationScorecardDocumentationContractTest(unittest.TestCase):
    def test_docs_index_links_the_generator_reference_only(self) -> None:
        index = (REPO_ROOT / "docs" / "index.md").read_text(encoding="utf-8")

        self.assertIn("./reference/modernization-scorecard-generator.md", index)
        self.assertNotIn("./reference/modernization-scorecard.md", index)

    def test_generated_scorecard_snapshot_is_not_checked_in(self) -> None:
        snapshot = REPO_ROOT / "docs" / "reference" / "modernization-scorecard.md"

        self.assertFalse(snapshot.exists())

    def test_generator_reference_owns_cli_safety_and_review_workflow_docs(self) -> None:
        markdown = (REPO_ROOT / "docs" / "reference" / "modernization-scorecard-generator.md").read_text(encoding="utf-8")

        self.assertIn("# Modernization scorecard generator reference", markdown)
        self.assertIn("## CLI contract", markdown)
        self.assertIn("## Output-path safety", markdown)
        self.assertIn("## Review workflow", markdown)
        self.assertIn("python3 scripts/generate-modernization-scorecard.py", markdown)
        self.assertIn("`--output` is always constrained to the resolved `--root`.", markdown)

    def test_checked_in_corpus_manifest_is_representative_text_evidence(self) -> None:
        generator = load_generator()
        manifest_path = REPO_ROOT / "docs" / "reference" / "modernization-corpus-manifest.json"
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

        status = generator.inspect_corpus_manifest(REPO_ROOT)

        self.assertEqual("present", status.state)
        self.assertIn("representative", manifest["coverageStatement"].lower())
        self.assertIn("not full historical archive coverage", manifest["coverageStatement"].lower())
        for entry in manifest["entries"]:
            with self.subTest(entry=entry["id"]):
                self.assertFalse((REPO_ROOT / entry["path"]).exists())
                self.assertTrue(entry["description"].strip())
                self.assertTrue(entry["generatedFixtureExpectations"])
                for expectation in entry["generatedFixtureExpectations"]:
                    self.assertTrue(expectation.strip())

    def test_docs_index_links_corpus_manifest_reference(self) -> None:
        index = (REPO_ROOT / "docs" / "index.md").read_text(encoding="utf-8")

        self.assertIn("./reference/modernization-corpus-manifest.md", index)


if __name__ == "__main__":
    unittest.main()
