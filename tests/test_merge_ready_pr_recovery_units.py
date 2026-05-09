"""Unit tests for scripts/merge-ready-pr-recovery.py (TDD — tests first).

These tests define the contract for the merge-ready PR recovery script.
They will FAIL until the implementation is created at:
    scripts/merge-ready-pr-recovery.py

Reference specification:
    docs/reference/merge-ready-pr-recovery.md
"""

import importlib.util
import json
import os
import subprocess
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "merge-ready-pr-recovery.py"
SCENARIO_DIR = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios"

_SKIP_REASON = (
    "TDD: scripts/merge-ready-pr-recovery.py not yet implemented. "
    "See docs/reference/merge-ready-pr-recovery.md"
)

if not SCRIPT_PATH.exists():
    raise unittest.SkipTest(_SKIP_REASON)

# CLI contract constants from docs/reference/merge-ready-pr-recovery.md
REQUIRED_ARGS = ["--pr", "--scenario"]
OPTIONAL_ARGS = ["--evidence-dir", "--audit-cycles", "--dry-run", "--skip-push"]
DEFAULT_AUDIT_CYCLES = 3
MIN_AUDIT_CYCLES = 3

# Evidence template markers
EVIDENCE_HEADING = "## Merge-Ready Evidence"
EVIDENCE_TABLE_HEADERS = ["Check", "Result"]
EVIDENCE_QA_ROW = "QA scenario"
EVIDENCE_AUDIT_ROW = "Quality audit"
EVIDENCE_CODE_ROW = "Code changes"
EVIDENCE_TIMESTAMP_PREFIX = "**Timestamp:**"
EVIDENCE_NOOP_PREFIX = "No-op justification:"


def _load_recovery_module():
    """Load merge-ready-pr-recovery.py as a module for unit testing."""
    spec = importlib.util.spec_from_file_location(
        "merge_ready_pr_recovery", SCRIPT_PATH
    )
    if spec is None or spec.loader is None:
        raise ImportError(f"Cannot load {SCRIPT_PATH}")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


class ScriptExistsTest(unittest.TestCase):
    """The recovery script must exist at the documented path."""

    def test_script_file_exists(self) -> None:
        self.assertTrue(
            SCRIPT_PATH.exists(),
            f"Recovery script not found at {SCRIPT_PATH.relative_to(REPO_ROOT)}. "
            "Create the implementation to satisfy the spec in "
            "docs/reference/merge-ready-pr-recovery.md.",
        )

    def test_script_is_importable(self) -> None:
        mod = _load_recovery_module()
        self.assertIsNotNone(mod)


class CliArgumentParsingTest(unittest.TestCase):
    """CLI contract from docs/reference/merge-ready-pr-recovery.md § CLI contract."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = _load_recovery_module()

    def _parse(self, argv: list[str]) -> object:
        """Parse argv through the module's argument parser."""
        return self.mod.parse_args(argv)

    def test_pr_and_scenario_are_required(self) -> None:
        with self.assertRaises(SystemExit):
            self._parse([])

    def test_pr_only_is_insufficient(self) -> None:
        with self.assertRaises(SystemExit):
            self._parse(["--pr", "425"])

    def test_scenario_only_is_insufficient(self) -> None:
        with self.assertRaises(SystemExit):
            self._parse(["--scenario", "model-export-boundary-smoke"])

    def test_minimal_valid_args(self) -> None:
        args = self._parse(["--pr", "425", "--scenario", "model-export-boundary-smoke"])
        self.assertEqual(425, args.pr)
        self.assertEqual("model-export-boundary-smoke", args.scenario)

    def test_pr_is_parsed_as_int(self) -> None:
        args = self._parse(["--pr", "42", "--scenario", "test-smoke"])
        self.assertIsInstance(args.pr, int)
        self.assertEqual(42, args.pr)

    def test_evidence_dir_default_includes_scenario_name(self) -> None:
        args = self._parse(["--pr", "1", "--scenario", "my-scenario"])
        self.assertIn("my-scenario", str(args.evidence_dir))

    def test_evidence_dir_custom(self) -> None:
        args = self._parse([
            "--pr", "1", "--scenario", "s",
            "--evidence-dir", "/custom/evidence",
        ])
        self.assertEqual(Path("/custom/evidence"), Path(args.evidence_dir))

    def test_audit_cycles_defaults_to_three(self) -> None:
        args = self._parse(["--pr", "1", "--scenario", "s"])
        self.assertEqual(DEFAULT_AUDIT_CYCLES, args.audit_cycles)

    def test_audit_cycles_custom(self) -> None:
        args = self._parse(["--pr", "1", "--scenario", "s", "--audit-cycles", "5"])
        self.assertEqual(5, args.audit_cycles)

    def test_audit_cycles_below_minimum_rejected(self) -> None:
        with self.assertRaises(SystemExit):
            self._parse(["--pr", "1", "--scenario", "s", "--audit-cycles", "2"])

    def test_audit_cycles_zero_rejected(self) -> None:
        with self.assertRaises(SystemExit):
            self._parse(["--pr", "1", "--scenario", "s", "--audit-cycles", "0"])

    def test_dry_run_defaults_false(self) -> None:
        args = self._parse(["--pr", "1", "--scenario", "s"])
        self.assertFalse(args.dry_run)

    def test_dry_run_flag(self) -> None:
        args = self._parse(["--pr", "1", "--scenario", "s", "--dry-run"])
        self.assertTrue(args.dry_run)

    def test_skip_push_defaults_false(self) -> None:
        args = self._parse(["--pr", "1", "--scenario", "s"])
        self.assertFalse(args.skip_push)

    def test_skip_push_flag(self) -> None:
        args = self._parse(["--pr", "1", "--scenario", "s", "--skip-push"])
        self.assertTrue(args.skip_push)


class ScenarioResolutionTest(unittest.TestCase):
    """Scenario YAML path resolution and existence checks."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = _load_recovery_module()

    def test_resolve_scenario_path_uses_standard_directory(self) -> None:
        path = self.mod.resolve_scenario_path("model-export-boundary-smoke")
        expected = SCENARIO_DIR / "model-export-boundary-smoke.yaml"
        self.assertEqual(expected, path)

    def test_resolve_scenario_path_respects_env_override(self) -> None:
        with patch.dict(os.environ, {"ALICE_QA_SCENARIO_DIR": "/custom/scenarios"}):
            path = self.mod.resolve_scenario_path("test-scenario")
        self.assertEqual(Path("/custom/scenarios/test-scenario.yaml"), path)

    def test_validate_scenario_exists_passes_for_real_scenario(self) -> None:
        existing = SCENARIO_DIR / "model-export-boundary-smoke.yaml"
        if not existing.exists():
            self.skipTest("Scenario YAML not on disk")
        self.mod.validate_scenario_exists(existing)

    def test_validate_scenario_exists_raises_for_missing_file(self) -> None:
        missing = SCENARIO_DIR / "nonexistent-scenario.yaml"
        with self.assertRaises(FileNotFoundError):
            self.mod.validate_scenario_exists(missing)


class EvidenceTemplateTest(unittest.TestCase):
    """Evidence template generation matches the documented structure."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = _load_recovery_module()

    def _build_evidence(self, **overrides) -> str:
        defaults = {
            "scenario_id": "alice-desktop-model-export-boundary-smoke",
            "test_count": 23,
            "audit_cycles": 3,
            "audit_clean": True,
            "cycle_summaries": [
                {"cycle": 1, "findings": 0, "in_diff": 0, "pre_existing": 0},
                {"cycle": 2, "findings": 0, "in_diff": 0, "pre_existing": 0},
                {"cycle": 3, "findings": 0, "in_diff": 0, "pre_existing": 0},
            ],
            "code_changes": "No-op",
            "noop_justification": (
                "All production code changes were already merged. This commit "
                "adds only QA scenario YAML and allowlist synchronization for "
                "the model-export-boundary-smoke lane."
            ),
            "evidence_dir": "/tmp/qa-evidence-model-export",
        }
        defaults.update(overrides)
        return self.mod.build_evidence_template(**defaults)

    def test_evidence_starts_with_heading(self) -> None:
        text = self._build_evidence()
        self.assertTrue(text.strip().startswith(EVIDENCE_HEADING))

    def test_evidence_contains_results_table(self) -> None:
        text = self._build_evidence()
        self.assertIn(EVIDENCE_QA_ROW, text)
        self.assertIn(EVIDENCE_AUDIT_ROW, text)
        self.assertIn(EVIDENCE_CODE_ROW, text)

    def test_evidence_qa_row_shows_scenario_and_count(self) -> None:
        text = self._build_evidence(
            scenario_id="alice-desktop-test-smoke", test_count=42
        )
        self.assertIn("alice-desktop-test-smoke", text)
        self.assertIn("42", text)

    def test_evidence_audit_row_shows_cycle_count(self) -> None:
        text = self._build_evidence(audit_cycles=5)
        self.assertIn("5 cycles", text)

    def test_evidence_audit_row_shows_clean_status(self) -> None:
        text = self._build_evidence(audit_clean=True)
        self.assertIn("clean", text.lower())

    def test_evidence_includes_validation_commands(self) -> None:
        text = self._build_evidence()
        self.assertIn("validate-scenarios.sh", text)
        self.assertIn("test-schema-contract.sh", text)
        self.assertIn("run-scenario.sh", text)

    def test_evidence_includes_timestamp(self) -> None:
        text = self._build_evidence()
        self.assertIn(EVIDENCE_TIMESTAMP_PREFIX, text)

    def test_evidence_noop_justification_included_when_noop(self) -> None:
        text = self._build_evidence(
            code_changes="No-op",
            noop_justification="No production changes needed.",
        )
        self.assertIn(EVIDENCE_NOOP_PREFIX, text)
        self.assertIn("No production changes needed.", text)

    def test_evidence_noop_justification_absent_for_real_changes(self) -> None:
        text = self._build_evidence(
            code_changes="abc1234 Fix null safety in exporter",
            noop_justification=None,
        )
        self.assertNotIn(EVIDENCE_NOOP_PREFIX, text)

    def test_evidence_audit_summary_lists_each_cycle(self) -> None:
        summaries = [
            {"cycle": 1, "findings": 2, "in_diff": 1, "pre_existing": 1},
            {"cycle": 2, "findings": 1, "in_diff": 0, "pre_existing": 1},
            {"cycle": 3, "findings": 0, "in_diff": 0, "pre_existing": 0},
        ]
        text = self._build_evidence(cycle_summaries=summaries)
        self.assertIn("Cycle 1:", text)
        self.assertIn("Cycle 2:", text)
        self.assertIn("Cycle 3:", text)
        self.assertIn("CLEAN", text)

    def test_evidence_audit_summary_includes_diff_vs_preexisting(self) -> None:
        summaries = [
            {"cycle": 1, "findings": 3, "in_diff": 1, "pre_existing": 2},
            {"cycle": 2, "findings": 0, "in_diff": 0, "pre_existing": 0},
            {"cycle": 3, "findings": 0, "in_diff": 0, "pre_existing": 0},
        ]
        text = self._build_evidence(cycle_summaries=summaries)
        self.assertIn("1 in diff", text)
        self.assertIn("2 pre-existing", text)


class QaValidationStepTest(unittest.TestCase):
    """Step 1: QA scenario validation logic."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = _load_recovery_module()

    def test_read_evidence_outcome_parses_passed(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            status_file = Path(td) / "status.txt"
            status_file.write_text(
                "outcome=passed\ntest_count=23\nscenario=alice-desktop-model-export-boundary-smoke\n"
            )
            result = self.mod.read_evidence_outcome(status_file)
        self.assertEqual("passed", result["outcome"])
        self.assertEqual("23", result["test_count"])

    def test_read_evidence_outcome_parses_failed(self) -> None:
        with tempfile.TemporaryDirectory() as td:
            status_file = Path(td) / "status.txt"
            status_file.write_text("outcome=failed\ntest_count=0\n")
            result = self.mod.read_evidence_outcome(status_file)
        self.assertEqual("failed", result["outcome"])

    def test_read_evidence_outcome_raises_for_missing_file(self) -> None:
        with self.assertRaises(FileNotFoundError):
            self.mod.read_evidence_outcome(Path("/nonexistent/status.txt"))

    def test_check_outcome_passed_returns_true(self) -> None:
        self.assertTrue(self.mod.check_outcome_passed({"outcome": "passed"}))

    def test_check_outcome_failed_returns_false(self) -> None:
        self.assertFalse(self.mod.check_outcome_passed({"outcome": "failed"}))

    def test_check_outcome_gated_not_run_returns_false(self) -> None:
        self.assertFalse(
            self.mod.check_outcome_passed({"outcome": "gated-not-run"})
        )


class QualityAuditCycleTest(unittest.TestCase):
    """Step 2: Quality audit cycle validation logic."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = _load_recovery_module()

    def test_is_final_cycle_clean_all_zero(self) -> None:
        summary = {"findings": 0, "in_diff": 0, "pre_existing": 0}
        self.assertTrue(self.mod.is_final_cycle_clean(summary))

    def test_is_final_cycle_clean_preexisting_only_counts_as_clean(self) -> None:
        summary = {"findings": 2, "in_diff": 0, "pre_existing": 2}
        self.assertTrue(self.mod.is_final_cycle_clean(summary))

    def test_is_final_cycle_clean_in_diff_findings_not_clean(self) -> None:
        summary = {"findings": 1, "in_diff": 1, "pre_existing": 0}
        self.assertFalse(self.mod.is_final_cycle_clean(summary))

    def test_validate_audit_results_passes_when_final_clean(self) -> None:
        cycles = [
            {"cycle": 1, "findings": 2, "in_diff": 1, "pre_existing": 1},
            {"cycle": 2, "findings": 1, "in_diff": 0, "pre_existing": 1},
            {"cycle": 3, "findings": 0, "in_diff": 0, "pre_existing": 0},
        ]
        self.mod.validate_audit_results(cycles)

    def test_validate_audit_results_raises_when_final_not_clean(self) -> None:
        cycles = [
            {"cycle": 1, "findings": 3, "in_diff": 2, "pre_existing": 1},
            {"cycle": 2, "findings": 2, "in_diff": 1, "pre_existing": 1},
            {"cycle": 3, "findings": 1, "in_diff": 1, "pre_existing": 0},
        ]
        with self.assertRaises(RuntimeError):
            self.mod.validate_audit_results(cycles)

    def test_validate_audit_results_requires_minimum_cycles(self) -> None:
        too_few = [
            {"cycle": 1, "findings": 0, "in_diff": 0, "pre_existing": 0},
            {"cycle": 2, "findings": 0, "in_diff": 0, "pre_existing": 0},
        ]
        with self.assertRaises(ValueError):
            self.mod.validate_audit_results(too_few, min_cycles=3)


class PrDescriptionUpdateTest(unittest.TestCase):
    """Step 3: PR description update logic."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = _load_recovery_module()

    def test_append_evidence_to_body_adds_section(self) -> None:
        original = "## Summary\n\nSome PR description.\n"
        evidence = "## Merge-Ready Evidence\n\n| Check | Result |\n"
        result = self.mod.append_evidence_to_body(original, evidence)
        self.assertIn(original.strip(), result)
        self.assertIn(EVIDENCE_HEADING, result)

    def test_append_evidence_replaces_existing_section(self) -> None:
        original = (
            "## Summary\n\nSome PR description.\n\n"
            "## Merge-Ready Evidence\n\nOld evidence.\n"
        )
        new_evidence = "## Merge-Ready Evidence\n\n| Check | Result |\n| New |\n"
        result = self.mod.append_evidence_to_body(original, new_evidence)
        self.assertEqual(1, result.count(EVIDENCE_HEADING))
        self.assertIn("New", result)
        self.assertNotIn("Old evidence.", result)

    def test_append_evidence_preserves_summary(self) -> None:
        original = "## Summary\n\nImportant context about the PR.\n"
        evidence = "## Merge-Ready Evidence\n\n...\n"
        result = self.mod.append_evidence_to_body(original, evidence)
        self.assertIn("Important context about the PR.", result)


class DryRunModeTest(unittest.TestCase):
    """Dry-run mode contract from docs/reference/merge-ready-pr-recovery.md."""

    def test_dry_run_subprocess_prints_plan_and_exits_zero(self) -> None:
        result = subprocess.run(
            [
                sys.executable,
                str(SCRIPT_PATH),
                "--pr", "425",
                "--scenario", "model-export-boundary-smoke",
                "--dry-run",
            ],
            cwd=REPO_ROOT,
            check=False,
            capture_output=True,
            text=True,
        )
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("model-export-boundary-smoke", result.stdout)
        self.assertNotIn("Running gated command smoke", result.stdout)
        self.assertNotIn("gh pr edit", result.stdout)


class CompatibilityRulesTest(unittest.TestCase):
    """Compatibility rules from docs/reference/merge-ready-pr-recovery.md § Compatibility rules."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.mod = _load_recovery_module()

    def test_module_does_not_import_external_packages(self) -> None:
        source = SCRIPT_PATH.read_text(encoding="utf-8")
        forbidden_imports = ["requests", "httpx", "aiohttp", "boto3", "azure"]
        for pkg in forbidden_imports:
            with self.subTest(pkg=pkg):
                self.assertNotIn(f"import {pkg}", source)

    def test_module_uses_only_stdlib_and_pathlib(self) -> None:
        source = SCRIPT_PATH.read_text(encoding="utf-8")
        import_lines = [
            line.strip()
            for line in source.splitlines()
            if line.strip().startswith("import ") or line.strip().startswith("from ")
        ]
        allowed_prefixes = [
            "import argparse", "import json", "import os", "import re",
            "import subprocess", "import sys", "import tempfile", "import textwrap",
            "import datetime", "import shutil", "import shlex",
            "from pathlib", "from datetime", "from typing",
        ]
        for line in import_lines:
            with self.subTest(line=line):
                self.assertTrue(
                    any(line.startswith(p) for p in allowed_prefixes),
                    f"Unexpected import: {line}. The script must use only stdlib.",
                )


if __name__ == "__main__":
    unittest.main()
