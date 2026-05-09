"""tests/test_merge_ready_pr_recovery_units.py

Failing unit contract tests for the planned merge-ready PR recovery implementation.
"""

from __future__ import annotations

import importlib.util
import json
import sys
import unittest
from functools import lru_cache
from pathlib import Path
from types import SimpleNamespace
from typing import Any


REPO_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = REPO_ROOT / "scripts" / "merge-ready-pr-recovery.py"
MODEL_EXPORT_TEST = "core/model-loading/src/test/java/org/lgna/story/resourceutilities/ModelExportTest.java"
HEAD_SHA = "f" * 40
BASE_SHA = "e" * 40


@lru_cache(maxsize=1)
def load_recovery_module():
    if not SCRIPT_PATH.exists():
        raise AssertionError(f"Expected merge-ready recovery implementation at {SCRIPT_PATH}")
    spec = importlib.util.spec_from_file_location("merge_ready_pr_recovery", SCRIPT_PATH)
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def as_mapping(value: Any) -> dict[str, Any]:
    if isinstance(value, dict):
        return value
    if hasattr(value, "__dict__"):
        return dict(value.__dict__)
    raise AssertionError(f"Expected mapping-like result, got {type(value)!r}: {value!r}")


class MergeReadyRecoveryUnitContractTest(unittest.TestCase):
    def setUp(self) -> None:
        self.recovery = load_recovery_module()

    def test_parse_pr_metadata_preserves_first_class_state_fields(self) -> None:
        raw_metadata = json.dumps(
            {
                "number": 425,
                "state": "OPEN",
                "isDraft": False,
                "reviewDecision": None,
                "baseRefName": "develop",
                "headRefName": "feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow",
                "headRefOid": HEAD_SHA,
                "title": "Characterize model export boundary lane follow",
                "url": "https://github.com/rysweet/RabbitHole/pull/425",
            }
        )

        metadata = as_mapping(self.recovery.parse_pr_metadata(raw_metadata))

        self.assertEqual(425, metadata["number"])
        self.assertEqual("OPEN", metadata["state"])
        self.assertFalse(metadata["is_draft"])
        self.assertEqual("none", metadata["review_decision"])
        self.assertEqual("develop", metadata["base_ref_name"])
        self.assertEqual(
            "feat/issue-412-rabbithole-wave7-model-export-boundary-lane-follow",
            metadata["head_ref_name"],
        )
        self.assertEqual(HEAD_SHA, metadata["head_ref_oid"])

    def test_head_alignment_rejects_stale_local_state(self) -> None:
        metadata = SimpleNamespace(head_ref_oid=HEAD_SHA)

        with self.assertRaisesRegex(self.recovery.RecoveryEvidenceError, "headRefOid|stale|HEAD"):
            self.recovery.validate_head_alignment(BASE_SHA, metadata)

        evidence = as_mapping(self.recovery.validate_head_alignment(HEAD_SHA, metadata))
        self.assertEqual("aligned", evidence["status"])
        self.assertEqual(HEAD_SHA, evidence["head_sha"])

    def test_diff_scope_accepts_only_expected_model_export_characterization_file(self) -> None:
        focused = as_mapping(
            self.recovery.analyze_diff_scope(
                changed_files=[("M", MODEL_EXPORT_TEST)],
                allowed_paths={MODEL_EXPORT_TEST},
                design_scope="test-only",
            )
        )
        self.assertEqual("focused", focused["status"])
        self.assertEqual([], focused["blockers"])

        out_of_scope = as_mapping(
            self.recovery.analyze_diff_scope(
                changed_files=[
                    ("M", MODEL_EXPORT_TEST),
                    ("A", "docs/reference/merge-ready-pr-recovery.md"),
                    ("M", "core/model-loading/src/main/java/org/lgna/story/resourceutilities/ModelResourceExporter.java"),
                ],
                allowed_paths={MODEL_EXPORT_TEST},
                design_scope="test-only",
            )
        )
        self.assertEqual("blocker", out_of_scope["status"])
        self.assertIn("NOT_MERGE_READY", "\n".join(out_of_scope["blockers"]))
        self.assertIn("docs/reference/merge-ready-pr-recovery.md", "\n".join(out_of_scope["blockers"]))
        self.assertIn("ModelResourceExporter.java", "\n".join(out_of_scope["blockers"]))

    def test_model_export_validation_command_uses_heap_preference_without_timeout_wrapper(self) -> None:
        validation = as_mapping(self.recovery.model_export_validation_command())
        command = validation["command"]
        env = validation["env"]

        self.assertEqual("32768", env["NODE_OPTIONS"].rsplit("=", 1)[-1])
        self.assertEqual("mvn", command[0])
        self.assertIn("-pl", command)
        self.assertIn("core/model-loading", command)
        self.assertIn("-Dsurefire.failIfNoSpecifiedTests=false", command)
        self.assertIn("-Dtest=ModelExportTest", command)
        self.assertNotIn("timeout", " ".join(command).lower())
        self.assertNotIn("gtimeout", " ".join(command).lower())

    def test_command_failure_reports_exit_code_with_sanitized_bounded_excerpt(self) -> None:
        result = SimpleNamespace(
            returncode=2,
            stdout="",
            stderr=(
                "/home/azureuser/src/private/build.log token=abc123 "
                "Authorization: token auth-secret-value password: hunter2 "
                '{"token":"json-secret"} '
                + "x" * 500
            ),
        )

        failure = self.recovery.format_command_failure("focused local validation", result, ["mvn", "test"])

        self.assertIn("focused local validation failed", failure)
        self.assertIn("mvn exit 2", failure)
        self.assertIn("<path>", failure)
        self.assertIn("token=<redacted>", failure)
        self.assertIn("Authorization: token <redacted>", failure)
        self.assertIn("password: <redacted>", failure)
        self.assertIn('"token":<redacted>', failure)
        self.assertNotIn("/home/azureuser/src/private", failure)
        self.assertNotIn("abc123", failure)
        self.assertNotIn("auth-secret-value", failure)
        self.assertNotIn("hunter2", failure)
        self.assertNotIn("json-secret", failure)
        self.assertLessEqual(len(failure), 320)

    def test_checks_require_stable_sha_and_all_green_check_states(self) -> None:
        checks = [
            {"name": "build", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/build"},
            {"name": "focused-tests", "state": "SUCCESS", "bucket": "pass", "link": "https://example.invalid/tests"},
        ]

        green = as_mapping(
            self.recovery.evaluate_checks(
                checks=checks,
                head_before=HEAD_SHA,
                head_after=HEAD_SHA,
                local_head=HEAD_SHA,
            )
        )
        self.assertEqual("green", green["status"])
        self.assertEqual([], green["blockers"])

        moved = as_mapping(
            self.recovery.evaluate_checks(
                checks=checks,
                head_before=HEAD_SHA,
                head_after=BASE_SHA,
                local_head=HEAD_SHA,
            )
        )
        self.assertEqual("blocker", moved["status"])
        self.assertIn("SHA", "\n".join(moved["blockers"]))

        pending = as_mapping(
            self.recovery.evaluate_checks(
                checks=[{"name": "build", "state": "IN_PROGRESS", "bucket": "pending", "link": "https://example.invalid/build"}],
                head_before=HEAD_SHA,
                head_after=HEAD_SHA,
                local_head=HEAD_SHA,
            )
        )
        self.assertEqual("blocker", pending["status"])
        self.assertIn("build", "\n".join(pending["blockers"]))

    def test_pr_state_gate_blocks_draft_and_changes_requested(self) -> None:
        reviewable = as_mapping(
            self.recovery.evaluate_pr_state(
                {
                    "state": "OPEN",
                    "is_draft": False,
                    "review_decision": "APPROVED",
                },
                branch_policy_requires_approval=True,
            )
        )
        self.assertEqual("reviewable", reviewable["status"])
        self.assertEqual([], reviewable["blockers"])

        blocked = as_mapping(
            self.recovery.evaluate_pr_state(
                {
                    "state": "OPEN",
                    "is_draft": True,
                    "review_decision": "CHANGES_REQUESTED",
                },
                branch_policy_requires_approval=True,
            )
        )
        self.assertEqual("blocker", blocked["status"])
        blockers = "\n".join(blocked["blockers"])
        self.assertIn("NOT_MERGE_READY", blockers)
        self.assertIn("draft", blockers.lower())
        self.assertIn("CHANGES_REQUESTED", blockers)

    def test_qa_evidence_distinguishes_not_applicable_from_missing_or_prepare_only(self) -> None:
        not_applicable = as_mapping(
            self.recovery.classify_qa_evidence(
                changed_files=[MODEL_EXPORT_TEST],
                discovered_paths=[],
                executed_evidence=[],
                ci_evidence=[],
            )
        )
        self.assertEqual("not-applicable", not_applicable["classification"])
        self.assertEqual([], not_applicable["blockers"])
        self.assertIn("focused validation", not_applicable["rationale"])

        missing = as_mapping(
            self.recovery.classify_qa_evidence(
                changed_files=["qa/outside-in/alice-desktop/scenarios/export-model.yaml"],
                discovered_paths=[],
                executed_evidence=[],
                ci_evidence=[],
            )
        )
        self.assertEqual("missing", missing["classification"])
        self.assertIn("NOT_MERGE_READY", "\n".join(missing["blockers"]))

        prepare_only = as_mapping(
            self.recovery.classify_qa_evidence(
                changed_files=["qa/outside-in/alice-desktop/scenarios/export-model.yaml"],
                discovered_paths=["qa/outside-in/alice-desktop/runners/validate-scenarios.sh"],
                executed_evidence=[{"path": "qa/outside-in/alice-desktop/runners/validate-scenarios.sh", "kind": "prepare-only"}],
                ci_evidence=[],
            )
        )
        self.assertEqual("prepare-only", prepare_only["classification"])
        self.assertIn("NOT_MERGE_READY", "\n".join(prepare_only["blockers"]))

    def test_docs_impact_records_no_doc_change_only_for_unchanged_test_only_behavior(self) -> None:
        no_doc_change = as_mapping(
            self.recovery.review_docs_impact(
                changed_files=[MODEL_EXPORT_TEST],
                design_scope="test-only",
                behavior_changed=False,
            )
        )
        self.assertEqual("no-doc-change", no_doc_change["status"])
        self.assertIn("test-only", no_doc_change["rationale"])
        self.assertEqual([], no_doc_change["blockers"])

        docs_in_test_only_pr = as_mapping(
            self.recovery.review_docs_impact(
                changed_files=[MODEL_EXPORT_TEST, "docs/howto/run-merge-ready-pr-recovery.md"],
                design_scope="test-only",
                behavior_changed=False,
            )
        )
        self.assertEqual("blocker", docs_in_test_only_pr["status"])
        self.assertIn("NOT_MERGE_READY", "\n".join(docs_in_test_only_pr["blockers"]))

    def test_pr_description_evidence_requires_reviewer_visible_bounded_evidence(self) -> None:
        sufficient_body = "\n".join(
            [
                f"Head SHA: {HEAD_SHA}",
                "State: OPEN",
                "Draft: false",
                "Review state: APPROVED",
                f"Focused diff: {MODEL_EXPORT_TEST}",
                "CI/check status: green for exact SHA",
                "Local validation: NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/model-loading -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=ModelExportTest test",
                "QA/scenario evidence: not-applicable; focused validation is the applicable evidence",
                "Docs impact: no documentation change required for test-only characterization coverage",
                "Quality audit cycles: cycle 1 diff scope clean; cycle 2 test adequacy clean; cycle 3 evidence completeness clean",
                "Non-claims: no full UI automation, visible rendering correctness, grading, creative assessment, full lesson completion, or full Tweedle/player decode claimed",
            ]
        )

        sufficient = as_mapping(self.recovery.validate_pr_description(sufficient_body))
        self.assertEqual("sufficient", sufficient["status"])
        self.assertEqual([], sufficient["blockers"])

        missing = as_mapping(self.recovery.validate_pr_description("Local validation passed."))
        self.assertEqual("blocker", missing["status"])
        blockers = "\n".join(missing["blockers"])
        self.assertIn("NOT_MERGE_READY", blockers)
        self.assertIn("QA/scenario", blockers)
        self.assertIn("Non-claims", blockers)

    def test_quality_audit_requires_three_cycles_and_clean_final_cycle(self) -> None:
        evidence = {
            "diff_scope": {"status": "focused", "blockers": []},
            "test_adequacy": {"status": "sufficient", "blockers": []},
            "ci": {"status": "green", "blockers": []},
            "qa": {"classification": "not-applicable", "blockers": []},
            "docs": {"status": "no-doc-change", "blockers": []},
            "pr_metadata": {"status": "sufficient", "blockers": []},
            "pr_state": {"status": "reviewable", "blockers": []},
            "claims": {"status": "bounded", "blockers": []},
        }

        cycles = self.recovery.run_quality_audit_cycles(evidence)

        self.assertEqual(3, len(cycles))
        for cycle in cycles:
            cycle_map = as_mapping(cycle)
            self.assertIn("SEEK", cycle_map)
            self.assertIn("VALIDATE", cycle_map)
            self.assertIn("FIX", cycle_map)
        self.assertEqual("clean", as_mapping(cycles[-1])["status"])

        evidence["pr_metadata"] = {
            "status": "blocker",
            "blockers": ["NOT_MERGE_READY: PR body lacks QA/scenario evidence."],
        }
        blocked_cycles = self.recovery.run_quality_audit_cycles(evidence)
        self.assertEqual("blocker", as_mapping(blocked_cycles[-1])["status"])
        self.assertIn("NOT_MERGE_READY", "\n".join(as_mapping(blocked_cycles[-1])["blockers"]))


if __name__ == "__main__":
    unittest.main()
