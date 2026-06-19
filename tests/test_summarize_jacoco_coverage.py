import importlib.util
import inspect
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "summarize-jacoco-coverage.py"

spec = importlib.util.spec_from_file_location("summarize_jacoco_coverage", SCRIPT_PATH)
coverage_script = importlib.util.module_from_spec(spec)
assert spec.loader is not None
sys.modules[spec.name] = coverage_script
spec.loader.exec_module(coverage_script)


def write_jacoco_csv(root: Path, relative_path: str, rows: list[tuple[int, int]]) -> None:
    csv_path = root / relative_path
    csv_path.parent.mkdir(parents=True, exist_ok=True)
    lines = ["LINE_MISSED,LINE_COVERED"]
    lines.extend(f"{missed},{covered}" for missed, covered in rows)
    csv_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


class CoverageSummaryComponentTest(unittest.TestCase):
    def test_collect_reports_discovers_aggregate_and_named_module_reports(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_jacoco_csv(
                root,
                "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
                [(25, 75)],
            )
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(2, 8)])
            write_jacoco_csv(root, "core/empty/target/site/jacoco/jacoco.csv", [(0, 0)])

            aggregate, module_reports = coverage_script.collect_reports(root)

        self.assertIsNotNone(aggregate)
        assert aggregate is not None
        self.assertEqual("open-asset reactor", aggregate.name)
        self.assertEqual(75.0, aggregate.percent)
        self.assertEqual(["core/ast"], [coverage.name for coverage in module_reports])

    def test_render_manifest_inventories_reports_artifacts_and_gates_deterministically(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_jacoco_csv(
                root,
                "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
                [(25, 75)],
            )
            write_jacoco_csv(root, "netbeans/target/site/jacoco/jacoco.csv", [(60, 40)])
            write_jacoco_csv(root, "core/empty/target/site/jacoco/jacoco.csv", [(0, 0)])
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(20, 80)])
            (root / "coverage-report/target/site/jacoco-aggregate/index.html").write_text(
                "aggregate",
                encoding="utf-8",
            )
            (root / "core/ast/target/site/jacoco/index.html").write_text(
                "module",
                encoding="utf-8",
            )
            (root / "core/ast/target/jacoco.exec").write_bytes(b"exec")
            surefire_report = root / "core/ast/target/surefire-reports/TEST-core.ast.xml"
            surefire_report.parent.mkdir(parents=True, exist_ok=True)
            surefire_report.write_text("<testsuite />\n", encoding="utf-8")
            aggregate, module_reports = coverage_script.collect_reports(root)

            manifest = coverage_script.render_manifest(
                root,
                aggregate,
                module_reports,
                70.0,
                [
                    coverage_script.ModuleThreshold("netbeans", 50.0),
                    coverage_script.ModuleThreshold("core/missing", 10.0),
                    coverage_script.ModuleThreshold("core/ast", 75.0),
                ],
            )

        self.assertEqual(1, manifest["schemaVersion"])
        self.assertEqual("open-assets-default", manifest["coverageModel"])
        self.assertEqual("jacoco", manifest["source"])
        self.assertEqual(
            "mvn -Dinstall4j.skip -Pcoverage verify",
            manifest["mavenCommand"],
        )
        self.assertEqual(
            {
                "expectedCsv": "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
                "state": "present",
                "lineCoveragePercent": 75.0,
                "covered": 75,
                "missed": 25,
                "total": 100,
            },
            manifest["aggregate"],
        )
        self.assertEqual(
            ["core/ast", "core/empty", "netbeans"],
            [entry["module"] for entry in manifest["modules"]],
        )
        self.assertEqual("empty", manifest["modules"][1]["state"])
        self.assertNotIn("lineCoveragePercent", manifest["modules"][1])
        self.assertEqual(
            {
                "minimumPercent": 70.0,
                "state": "pass",
                "lineCoveragePercent": 75.0,
            },
            manifest["gates"]["aggregate"],
        )
        self.assertEqual(
            ["core/ast", "core/missing", "netbeans"],
            [entry["module"] for entry in manifest["gates"]["modules"]],
        )
        self.assertEqual("pass", manifest["gates"]["modules"][0]["state"])
        self.assertEqual("fail", manifest["gates"]["modules"][1]["state"])
        self.assertEqual("fail", manifest["gates"]["modules"][2]["state"])
        self.assertEqual(
            sorted(manifest["artifacts"], key=lambda item: (item["kind"], item["path"])),
            manifest["artifacts"],
        )
        self.assertIn(
            {"kind": "aggregate-report", "path": "coverage-report/target/site/jacoco-aggregate/index.html"},
            manifest["artifacts"],
        )
        self.assertIn(
            {"kind": "module-report", "path": "core/ast/target/site/jacoco/index.html"},
            manifest["artifacts"],
        )
        self.assertIn(
            {"kind": "exec-data", "path": "core/ast/target/jacoco.exec"},
            manifest["artifacts"],
        )
        self.assertIn(
            {"kind": "surefire-report", "path": "core/ast/target/surefire-reports/TEST-core.ast.xml"},
            manifest["artifacts"],
        )
        self.assertNotIn(str(Path(tempfile.gettempdir())), json.dumps(manifest, sort_keys=True))

    def test_render_manifest_marks_missing_and_empty_aggregate_without_false_zero_coverage(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            missing_manifest = coverage_script.render_manifest(root, None, [], None, [])

            write_jacoco_csv(
                root,
                "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
                [(0, 0)],
            )
            aggregate, module_reports = coverage_script.collect_reports(root)
            empty_manifest = coverage_script.render_manifest(root, aggregate, module_reports, None, [])

        self.assertEqual("missing", missing_manifest["aggregate"]["state"])
        self.assertNotIn("lineCoveragePercent", missing_manifest["aggregate"])
        self.assertEqual("empty", empty_manifest["aggregate"]["state"])
        self.assertNotIn("lineCoveragePercent", empty_manifest["aggregate"])
        self.assertEqual({"state": "not-configured"}, empty_manifest["gates"]["aggregate"])

    def test_render_manifest_reports_long_term_aggregate_target_separately_from_ci_gate(self) -> None:
        self.assertIn(
            "aggregate_target_percent",
            inspect.signature(coverage_script.render_manifest).parameters,
        )
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            missing_manifest = coverage_script.render_manifest(
                root,
                None,
                [],
                8.0,
                [],
                aggregate_target_percent=70.0,
            )
            low_aggregate = coverage_script.Coverage(
                name="open-asset reactor",
                covered=65,
                missed=35,
                source=root / "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
            )
            low_manifest = coverage_script.render_manifest(
                root,
                low_aggregate,
                [],
                8.0,
                [],
                aggregate_target_percent=70.0,
            )
            met_aggregate = coverage_script.Coverage(
                name="open-asset reactor",
                covered=70,
                missed=30,
                source=root / "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
            )
            met_manifest = coverage_script.render_manifest(
                root,
                met_aggregate,
                [],
                8.0,
                [],
                aggregate_target_percent=70.0,
            )

        self.assertEqual(
            {
                "minimumPercent": 70.0,
                "state": "not-claimable",
                "reason": "aggregate JaCoCo CSV is missing",
            },
            missing_manifest["coverageTarget"]["aggregate"],
        )
        self.assertEqual(
            {
                "minimumPercent": 70.0,
                "state": "not-met",
                "lineCoveragePercent": 65.0,
            },
            low_manifest["coverageTarget"]["aggregate"],
        )
        self.assertEqual("pass", low_manifest["gates"]["aggregate"]["state"])
        self.assertEqual(
            {
                "minimumPercent": 70.0,
                "state": "met",
                "lineCoveragePercent": 70.0,
            },
            met_manifest["coverageTarget"]["aggregate"],
        )

    def test_append_module_gates_renders_pass_fail_and_missing_rows(self) -> None:
        markdown, passed = coverage_script.append_module_gates(
            "# Existing summary\n",
            [
                coverage_script.Coverage(
                    name="core/ast",
                    covered=80,
                    missed=20,
                    source=Path("core/ast/target/site/jacoco/jacoco.csv"),
                ),
                coverage_script.Coverage(
                    name="netbeans",
                    covered=25,
                    missed=75,
                    source=Path("netbeans/target/site/jacoco/jacoco.csv"),
                ),
            ],
            [
                coverage_script.ModuleThreshold("core/ast", 75.0),
                coverage_script.ModuleThreshold("netbeans", 30.0),
                coverage_script.ModuleThreshold("core/tweedle", 50.0),
            ],
        )

        self.assertFalse(passed)
        self.assertIn("## Module coverage gates", markdown)
        self.assertIn("| core/ast | 75.00% | 80.00% | PASS |", markdown)
        self.assertIn("| netbeans | 30.00% | 25.00% | FAIL |", markdown)
        self.assertIn("| core/tweedle | 50.00% | missing | FAIL |", markdown)

    def test_parse_module_threshold_accepts_module_percent_pairs(self) -> None:
        threshold = coverage_script.parse_module_threshold(" core/story-api-migration = 75.5 ")

        self.assertEqual("core/story-api-migration", threshold.module_name)
        self.assertEqual(75.5, threshold.minimum_percent)

    def test_parse_module_threshold_rejects_malformed_values(self) -> None:
        invalid_values = [
            "core/ast",
            "=75.0",
            "core/ast=not-a-number",
            "core/ast=-0.1",
            "core/ast=100.1",
        ]

        for value in invalid_values:
            with self.subTest(value=value):
                with self.assertRaises(Exception):
                    coverage_script.parse_module_threshold(value)


class CoverageSummaryCliTest(unittest.TestCase):
    def test_cli_passes_and_writes_summary_when_all_gates_are_met(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            summary_path = root / "coverage-summary.md"
            manifest_path = root / "coverage-evidence-manifest.json"
            write_jacoco_csv(
                root,
                "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
                [(10, 90)],
            )
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(20, 80)])
            write_jacoco_csv(root, "netbeans/target/site/jacoco/jacoco.csv", [(70, 30)])

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--root",
                    str(root),
                    "--output",
                    str(summary_path),
                    "--evidence-manifest",
                    str(manifest_path),
                    "--min-aggregate-line-percent",
                    "85.0",
                    "--min-module-line-percent",
                    "core/ast=75.0",
                    "--min-module-line-percent",
                    "netbeans=25.0",
                ],
                check=False,
                capture_output=True,
                text=True,
            )

            self.assertEqual(0, result.returncode, result.stderr)
            summary = summary_path.read_text(encoding="utf-8")
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

        self.assertIn("Result: PASS", result.stdout)
        self.assertIn("| core/ast | 75.00% | 80.00% | PASS |", summary)
        self.assertIn("| netbeans | 25.00% | 30.00% | PASS |", summary)
        self.assertEqual("pass", manifest["gates"]["aggregate"]["state"])
        self.assertEqual("coverage-report/target/site/jacoco-aggregate/jacoco.csv", manifest["aggregate"]["expectedCsv"])

    def test_cli_fails_and_writes_summary_for_low_or_missing_module_reports(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            summary_path = root / "coverage-summary.md"
            manifest_path = root / "coverage-evidence-manifest.json"
            write_jacoco_csv(
                root,
                "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
                [(10, 90)],
            )
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(40, 60)])

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--root",
                    str(root),
                    "--output",
                    str(summary_path),
                    "--evidence-manifest",
                    str(manifest_path),
                    "--min-aggregate-line-percent",
                    "80.0",
                    "--min-module-line-percent",
                    "core/ast=75.0",
                    "--min-module-line-percent",
                    "core/tweedle=50.0",
                ],
                check=False,
                capture_output=True,
                text=True,
            )

            self.assertEqual(2, result.returncode, result.stdout)
            summary = summary_path.read_text(encoding="utf-8")
            manifest = json.loads(manifest_path.read_text(encoding="utf-8"))

        self.assertIn("| core/ast | 75.00% | 60.00% | FAIL |", summary)
        self.assertIn("| core/tweedle | 50.00% | missing | FAIL |", summary)
        self.assertEqual("pass", manifest["gates"]["aggregate"]["state"])
        self.assertEqual("fail", manifest["gates"]["modules"][0]["state"])
        self.assertEqual("fail", manifest["gates"]["modules"][1]["state"])

    def test_cli_fails_and_writes_summary_when_required_aggregate_report_is_missing(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            summary_path = root / "coverage-summary.md"
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(20, 80)])

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--root",
                    str(root),
                    "--output",
                    str(summary_path),
                    "--min-aggregate-line-percent",
                    "8.0",
                ],
                check=False,
                capture_output=True,
                text=True,
            )
            summary = summary_path.read_text(encoding="utf-8")

        self.assertEqual(2, result.returncode)
        self.assertIn("Result: FAIL - aggregate report was not found.", result.stdout)
        self.assertIn("Result: FAIL - aggregate report was not found.", summary)

    def test_cli_marks_long_term_target_not_claimable_when_only_module_reports_exist(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            summary_path = root / "coverage-summary.md"
            manifest_path = root / "coverage-evidence-manifest.json"
            write_jacoco_csv(root, "core/ast/target/site/jacoco/jacoco.csv", [(20, 80)])

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--root",
                    str(root),
                    "--output",
                    str(summary_path),
                    "--evidence-manifest",
                    str(manifest_path),
                    "--target-aggregate-line-percent",
                    "70.0",
                ],
                check=False,
                capture_output=True,
                text=True,
            )
            summary = summary_path.read_text(encoding="utf-8") if summary_path.exists() else ""
            manifest = json.loads(manifest_path.read_text(encoding="utf-8")) if manifest_path.exists() else {}

        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("## Long-term aggregate coverage target", summary)
        self.assertIn("Required aggregate line coverage: 70.00%", summary)
        self.assertIn("Result: NOT CLAIMABLE - aggregate report was not found.", summary)
        self.assertIn(
            "Module-level JaCoCo reports cannot prove aggregate coverage without the aggregate CSV.",
            summary,
        )
        self.assertEqual(
            {
                "minimumPercent": 70.0,
                "state": "not-claimable",
                "reason": "aggregate JaCoCo CSV is missing",
            },
            manifest["coverageTarget"]["aggregate"],
        )

    def test_cli_rejects_out_of_range_aggregate_thresholds_before_writing_summary(self) -> None:
        for threshold in ("-0.1", "100.1"):
            with self.subTest(threshold=threshold):
                with tempfile.TemporaryDirectory() as temp_dir:
                    root = Path(temp_dir)
                    summary_path = root / "coverage-summary.md"
                    manifest_path = root / "coverage-evidence-manifest.json"
                    write_jacoco_csv(
                        root,
                        "coverage-report/target/site/jacoco-aggregate/jacoco.csv",
                        [(0, 100)],
                    )

                    result = subprocess.run(
                        [
                            sys.executable,
                            str(SCRIPT_PATH),
                            "--root",
                            str(root),
                            "--output",
                            str(summary_path),
                            "--evidence-manifest",
                            str(manifest_path),
                            "--min-aggregate-line-percent",
                            threshold,
                        ],
                        check=False,
                        capture_output=True,
                        text=True,
                    )

                    self.assertEqual(2, result.returncode)
                    self.assertIn(
                        "aggregate threshold percent must be between 0 and 100",
                        result.stderr,
                    )
                    self.assertFalse(summary_path.exists())
                    self.assertFalse(manifest_path.exists())

    def test_cli_rejects_duplicate_module_thresholds(self) -> None:
        result = subprocess.run(
            [
                sys.executable,
                str(SCRIPT_PATH),
                "--min-module-line-percent",
                "core/ast=18.0",
                "--min-module-line-percent",
                "core/ast=19.0",
            ],
            check=False,
            capture_output=True,
            text=True,
        )

        self.assertEqual(2, result.returncode)
        self.assertIn("duplicate module threshold for core/ast", result.stderr)

    def test_cli_rejects_duplicate_module_thresholds_before_writing_summary(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            summary_path = Path(temp_dir) / "coverage-summary.md"
            manifest_path = Path(temp_dir) / "coverage-evidence-manifest.json"

            result = subprocess.run(
                [
                    sys.executable,
                    str(SCRIPT_PATH),
                    "--output",
                    str(summary_path),
                    "--evidence-manifest",
                    str(manifest_path),
                    "--min-module-line-percent",
                    "core/ast=18.0",
                    "--min-module-line-percent",
                    "core/ast=19.0",
                ],
                check=False,
                capture_output=True,
                text=True,
            )

        self.assertEqual(2, result.returncode)
        self.assertIn("duplicate module threshold for core/ast", result.stderr)
        self.assertFalse(summary_path.exists())
        self.assertFalse(manifest_path.exists())


if __name__ == "__main__":
    unittest.main()
