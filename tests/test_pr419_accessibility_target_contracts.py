"""PR #419 accessibility target discovery contract tests.

Validates that every deliverable file, runner interface, documentation structure,
assertion helper, and non-claim boundary introduced by PR #419 remains correctly
wired. These tests define the contracts; implementation must satisfy them.
"""

import ast
import re
import subprocess
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
PR_NUMBER = 419
PR_BRANCH = "feat/issue-416-rabbithole-wave7-accessibility-target-lane-follow"

# ── Files introduced or significantly changed by PR #419 ──────────────────────

PR419_CHANGED_FILES = sorted(
    [
        ".copilot-evidence/default-workflow-attempt.log",
        "docs/howto/alice-desktop-outside-in-qa.md",
        "docs/index.md",
        "docs/reference/accessibility-target-discovery-silver-thread.md",
        "docs/reference/alice-desktop-outside-in-qa.md",
        "docs/reference/post-open-runtime-display-accessibility-evidence.md",
        "docs/reference/visible-rendering-evidence-nonclaim-contract.md",
        "docs/tutorials/trace-accessibility-target-discovery-silver-thread.md",
        "qa/outside-in/alice-desktop/README.md",
        "qa/outside-in/alice-desktop/runners/post-open-runtime-display-probe.py",
        "qa/outside-in/alice-desktop/runners/world-canvas-pixel-sampler.py",
        "qa/outside-in/alice-desktop/tests/lib/assertions.sh",
        "qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh",
        "qa/outside-in/alice-desktop/tests/test-current-head-evidence-doc-refinement-contract.sh",
        "qa/outside-in/alice-desktop/tests/test-post-open-runtime-display-probe.sh",
        "qa/outside-in/alice-desktop/tests/test-pr419-current-head-readiness-gate-contract.sh",
        "qa/outside-in/alice-desktop/tests/test-pr419-finalization-evidence-contract.sh",
        "qa/outside-in/alice-desktop/tests/test-pr419-readiness-evidence-contract.sh",
        "qa/outside-in/alice-desktop/tests/test-world-canvas-pixel-sampler-contract.sh",
    ]
)

# ── Forbidden overclaim phrases that must never appear in docs or runners ──────

FORBIDDEN_CLAIMS = [
    "full UI automation",
    "visible rendering correctness",
    "full world execution",
    "grading",
    "creative assessment",
    "full lesson completion",
    "full Tweedle/player decode",
    "installer deployment success",
    "broad accessibility compliance",
]

# ── Runner paths ──────────────────────────────────────────────────────────────

PROBE_RUNNER = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners"
    / "post-open-runtime-display-probe.py"
)
SAMPLER_RUNNER = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "runners"
    / "world-canvas-pixel-sampler.py"
)
ASSERTIONS_LIB = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests" / "lib"
    / "assertions.sh"
)

# ── Reference and tutorial docs ───────────────────────────────────────────────

SILVER_THREAD_REF = (
    REPO_ROOT / "docs" / "reference"
    / "accessibility-target-discovery-silver-thread.md"
)
SILVER_THREAD_TUTORIAL = (
    REPO_ROOT / "docs" / "tutorials"
    / "trace-accessibility-target-discovery-silver-thread.md"
)
NONCLAIM_CONTRACT_REF = (
    REPO_ROOT / "docs" / "reference"
    / "visible-rendering-evidence-nonclaim-contract.md"
)
POST_OPEN_REF = (
    REPO_ROOT / "docs" / "reference"
    / "post-open-runtime-display-accessibility-evidence.md"
)

# ── Shell test scripts ────────────────────────────────────────────────────────

SHELL_TESTS = [
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests"
    / "test-accessibility-target-discovery-silver-thread.sh",
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests"
    / "test-current-head-evidence-doc-refinement-contract.sh",
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests"
    / "test-post-open-runtime-display-probe.sh",
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests"
    / "test-pr419-current-head-readiness-gate-contract.sh",
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests"
    / "test-pr419-finalization-evidence-contract.sh",
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests"
    / "test-pr419-readiness-evidence-contract.sh",
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "tests"
    / "test-world-canvas-pixel-sampler-contract.sh",
]


def git_output(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        check=True,
        capture_output=True,
        text=True,
    )
    return result.stdout


def pr_branch_output(*args: str) -> str:
    result = subprocess.run(
        ["git", *args],
        cwd=REPO_ROOT,
        capture_output=True,
        text=True,
    )
    if result.returncode != 0:
        raise unittest.SkipTest(
            f"PR branch contract requires origin/develop: {result.stderr.strip()}"
        )
    return result.stdout


# ═══════════════════════════════════════════════════════════════════════════════
# 1. File existence contracts
# ═══════════════════════════════════════════════════════════════════════════════


class PR419FileExistenceTest(unittest.TestCase):
    """Every file listed in the PR scope must exist."""

    def test_all_pr419_deliverable_files_exist(self) -> None:
        missing = [
            f for f in PR419_CHANGED_FILES
            if not (REPO_ROOT / f).exists()
        ]
        self.assertEqual(
            [],
            missing,
            "PR #419 deliverable files must all be checked in.",
        )

    def test_shell_test_scripts_are_executable_or_source_assertions(self) -> None:
        for script in SHELL_TESTS:
            with self.subTest(script=script.name):
                self.assertTrue(
                    script.is_file(),
                    f"Shell test {script.name} must exist.",
                )
                text = script.read_text(encoding="utf-8")
                self.assertTrue(
                    text.startswith("#!/usr/bin/env bash")
                    or text.startswith("#!/bin/bash"),
                    f"{script.name} must have a bash shebang.",
                )


# ═══════════════════════════════════════════════════════════════════════════════
# 2. Runner interface contracts
# ═══════════════════════════════════════════════════════════════════════════════


class PR419ProbeRunnerInterfaceTest(unittest.TestCase):
    """post-open-runtime-display-probe.py CLI contract."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.source = PROBE_RUNNER.read_text(encoding="utf-8")

    def test_probe_runner_has_shebang(self) -> None:
        self.assertTrue(self.source.startswith("#!/usr/bin/env python3"))

    def test_probe_runner_declares_required_argparse_flags(self) -> None:
        required_flags = [
            "--inventory",
            "--post-open-window-observation",
            "--output",
            "--status-file",
            "--scenario-id",
            "--automation-mode",
        ]
        for flag in required_flags:
            with self.subTest(flag=flag):
                self.assertIn(
                    flag,
                    self.source,
                    f"Probe runner must accept {flag}.",
                )

    def test_probe_runner_declares_unsupported_claims(self) -> None:
        self.assertIn("UNSUPPORTED_CLAIMS", self.source)
        expected_claims = [
            "full-ui-automation",
            "visible-rendering-correctness",
            "full-world-execution",
            "grading",
            "save-completion",
        ]
        for claim in expected_claims:
            with self.subTest(claim=claim):
                self.assertIn(claim, self.source)

    def test_probe_runner_declares_claim_scope(self) -> None:
        self.assertIn(
            "post-open-runtime-display-accessibility-evidence",
            self.source,
        )

    def test_probe_runner_is_read_only(self) -> None:
        self.assertIn("read-only", self.source.lower())

    def test_probe_runner_parses_as_valid_python(self) -> None:
        ast.parse(self.source, filename=str(PROBE_RUNNER))


class PR419SamplerRunnerInterfaceTest(unittest.TestCase):
    """world-canvas-pixel-sampler.py CLI contract."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.source = SAMPLER_RUNNER.read_text(encoding="utf-8")

    def test_sampler_runner_has_shebang(self) -> None:
        self.assertTrue(self.source.startswith("#!/usr/bin/env python3"))

    def test_sampler_runner_declares_required_argparse_flags(self) -> None:
        for flag in ("--target-json", "--output"):
            with self.subTest(flag=flag):
                self.assertIn(flag, self.source)

    def test_sampler_runner_declares_unsupported_claims(self) -> None:
        self.assertIn("UNSUPPORTED_CLAIMS", self.source)
        for claim in (
            "world-canvas-pixel-correctness",
            "full-visible-rendering-correctness",
            "rendered-world-correctness",
        ):
            with self.subTest(claim=claim):
                self.assertIn(claim, self.source)

    def test_sampler_runner_declares_claim_scope(self) -> None:
        self.assertIn(
            "visible-rendering-world-canvas-pixel-sampling",
            self.source,
        )

    def test_sampler_runner_enforces_coordinate_type_screen(self) -> None:
        self.assertIn('"screen"', self.source)

    def test_sampler_runner_sets_correctness_false(self) -> None:
        self.assertIn("visibleRenderingCorrectnessEstablished", self.source)

    def test_sampler_runner_parses_as_valid_python(self) -> None:
        ast.parse(self.source, filename=str(SAMPLER_RUNNER))


# ═══════════════════════════════════════════════════════════════════════════════
# 3. Assertion library contracts
# ═══════════════════════════════════════════════════════════════════════════════


class PR419AssertionLibraryTest(unittest.TestCase):
    """assertions.sh must export all expected helper functions."""

    REQUIRED_HELPERS = [
        "create_scratch_root",
        "fail",
        "pass",
        "assert_success",
        "assert_failure",
        "assert_exit_code",
        "assert_file_exists",
        "assert_contains",
        "assert_not_contains",
        "assert_literal_in_file",
        "assert_literal_absent_from_file",
        "assert_pattern_absent_from_file",
        "assert_exact_count_in_file",
        "single_child_dir",
        "finish",
    ]

    @classmethod
    def setUpClass(cls) -> None:
        cls.source = ASSERTIONS_LIB.read_text(encoding="utf-8")

    def test_assertions_lib_exists(self) -> None:
        self.assertTrue(ASSERTIONS_LIB.is_file())

    def test_assertions_lib_declares_all_required_helpers(self) -> None:
        for helper in self.REQUIRED_HELPERS:
            with self.subTest(helper=helper):
                pattern = rf"^{re.escape(helper)}\s*\(\)"
                self.assertRegex(
                    self.source,
                    re.compile(pattern, re.MULTILINE),
                    f"assertions.sh must declare {helper}().",
                )

    def test_assertions_lib_tracks_failure_count(self) -> None:
        self.assertIn("failures=", self.source)

    def test_assertions_lib_finish_exits_nonzero_on_failure(self) -> None:
        self.assertIn("exit 1", self.source)


# ═══════════════════════════════════════════════════════════════════════════════
# 4. Documentation structure contracts
# ═══════════════════════════════════════════════════════════════════════════════


class PR419SilverThreadReferenceDocTest(unittest.TestCase):
    """Silver-thread reference doc must have required sections and markers."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.text = SILVER_THREAD_REF.read_text(encoding="utf-8")

    def test_reference_doc_exists(self) -> None:
        self.assertTrue(SILVER_THREAD_REF.is_file())

    def test_reference_doc_has_required_sections(self) -> None:
        required_sections = [
            "## Usage",
            "## Evidence lanes",
            "## Readiness evidence record",
            "## Current-head readiness gate",
            "## Artifact API",
            "## Configuration",
            "## Claim boundaries",
        ]
        for section in required_sections:
            with self.subTest(section=section):
                self.assertIn(section, self.text)

    def test_reference_doc_names_executable_contract(self) -> None:
        self.assertIn(
            "test-accessibility-target-discovery-silver-thread.sh",
            self.text,
        )

    def test_reference_doc_names_evidence_lanes(self) -> None:
        for lane in ("Launch", "Run", "Runtime target discovery", "Select Project"):
            with self.subTest(lane=lane):
                self.assertIn(lane, self.text)

    def test_reference_doc_names_pr419_readiness_gate(self) -> None:
        self.assertIn(
            "test-pr419-current-head-readiness-gate-contract.sh",
            self.text,
        )

    def test_reference_doc_includes_node_options(self) -> None:
        self.assertIn("NODE_OPTIONS=--max-old-space-size=32768", self.text)


class PR419TutorialDocTest(unittest.TestCase):
    """Tutorial must follow the expected step structure."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.text = SILVER_THREAD_TUTORIAL.read_text(encoding="utf-8")

    def test_tutorial_exists(self) -> None:
        self.assertTrue(SILVER_THREAD_TUTORIAL.is_file())

    def test_tutorial_has_title(self) -> None:
        self.assertIn(
            "# Tutorial: Trace the Accessibility Target Discovery Silver Thread",
            self.text,
        )

    def test_tutorial_has_prerequisite_section(self) -> None:
        self.assertIn("## Before you start", self.text)

    def test_tutorial_names_all_seven_steps(self) -> None:
        for step_number in range(1, 8):
            with self.subTest(step=step_number):
                self.assertIn(f"## Step {step_number}:", self.text)

    def test_tutorial_includes_contract_run_command(self) -> None:
        self.assertIn(
            "test-accessibility-target-discovery-silver-thread.sh",
            self.text,
        )

    def test_tutorial_names_tweedle_submodule_init(self) -> None:
        self.assertIn("git submodule update --init tweedle-lang", self.text)


class PR419NonclaimContractDocTest(unittest.TestCase):
    """Visible rendering nonclaim contract doc must enforce boundaries."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.text = NONCLAIM_CONTRACT_REF.read_text(encoding="utf-8")

    def test_nonclaim_doc_exists(self) -> None:
        self.assertTrue(NONCLAIM_CONTRACT_REF.is_file())

    def test_nonclaim_doc_has_required_sections(self) -> None:
        for section in (
            "## Scope",
            "## Usage",
            "## Artifact API",
            "## Allowed wording",
            "## Rejected wording",
            "## Review checklist",
        ):
            with self.subTest(section=section):
                self.assertIn(section, self.text)

    def test_nonclaim_doc_enforces_correctness_false(self) -> None:
        self.assertIn("visibleRenderingCorrectnessEstablished", self.text)

    def test_nonclaim_doc_enforces_correctness_not_performed(self) -> None:
        self.assertIn("correctnessCheck", self.text)
        self.assertIn("not-performed", self.text)

    def test_nonclaim_doc_lists_unsupported_claims(self) -> None:
        for claim in (
            "world-canvas-pixel-correctness",
            "full-visible-rendering-correctness",
            "rendered-world-correctness",
        ):
            with self.subTest(claim=claim):
                self.assertIn(claim, self.text)


class PR419PostOpenRefDocTest(unittest.TestCase):
    """Post-open reference doc must document the probe contract."""

    @classmethod
    def setUpClass(cls) -> None:
        cls.text = POST_OPEN_REF.read_text(encoding="utf-8")

    def test_post_open_ref_exists(self) -> None:
        self.assertTrue(POST_OPEN_REF.is_file())

    def test_post_open_ref_has_scope_and_usage(self) -> None:
        self.assertIn("## Scope", self.text)
        self.assertIn("## Usage", self.text)

    def test_post_open_ref_names_probe_runner(self) -> None:
        self.assertIn("post-open-runtime-display-probe.py", self.text)

    def test_post_open_ref_names_scenario(self) -> None:
        self.assertIn(
            "post-open-runtime-display-accessibility-evidence",
            self.text,
        )


# ═══════════════════════════════════════════════════════════════════════════════
# 5. Non-claim boundary contracts
# ═══════════════════════════════════════════════════════════════════════════════


class PR419NonClaimBoundaryTest(unittest.TestCase):
    """Docs and runners must not make forbidden overclaim assertions."""

    SCOPED_FILES = [
        SILVER_THREAD_REF,
        SILVER_THREAD_TUTORIAL,
        POST_OPEN_REF,
        NONCLAIM_CONTRACT_REF,
        PROBE_RUNNER,
        SAMPLER_RUNNER,
    ]

    def test_no_file_positively_claims_forbidden_capability(self) -> None:
        positive_claim_patterns = [
            re.compile(
                rf"(?:proves?|establishes?|confirms?|guarantees?|certifies?)\s+{re.escape(claim)}",
                re.IGNORECASE,
            )
            for claim in FORBIDDEN_CLAIMS
        ]
        negation_context = re.compile(
            r"\b(?:not?|never|neither|does\s+not|do\s+not|cannot|don't|doesn't)\b",
            re.IGNORECASE,
        )
        violations: list[str] = []
        for path in self.SCOPED_FILES:
            if not path.is_file():
                continue
            text = path.read_text(encoding="utf-8")
            for pattern in positive_claim_patterns:
                for match in pattern.finditer(text):
                    context_start = max(0, match.start() - 40)
                    preceding = text[context_start:match.start()]
                    if negation_context.search(preceding):
                        continue
                    violations.append(
                        f"{path.relative_to(REPO_ROOT)}:{text.count(chr(10), 0, match.start()) + 1}:"
                        f" {match.group(0)}"
                    )
        self.assertEqual(
            [],
            violations,
            "PR #419 deliverables must not positively assert forbidden capabilities.",
        )

    def test_silver_thread_ref_states_non_claims_explicitly(self) -> None:
        text = SILVER_THREAD_REF.read_text(encoding="utf-8")
        for marker in (
            "does not claim full UI automation",
            "does not claim",
        ):
            with self.subTest(marker=marker):
                self.assertIn(marker, text)


# ═══════════════════════════════════════════════════════════════════════════════
# 6. Diff scope contract
# ═══════════════════════════════════════════════════════════════════════════════


class PR419DiffScopeContractTest(unittest.TestCase):
    """PR #419 must not expand beyond its declared changed-file set."""

    def test_diff_scope_matches_declared_files(self) -> None:
        try:
            raw = pr_branch_output(
                "diff", "--name-only", "origin/develop...HEAD"
            )
        except unittest.SkipTest:
            raise
        actual_files = sorted(set(raw.strip().splitlines()))
        undeclared = [f for f in actual_files if f not in PR419_CHANGED_FILES]
        self.assertEqual(
            [],
            undeclared,
            "PR #419 changed files outside the declared scope.",
        )


# ═══════════════════════════════════════════════════════════════════════════════
# 7. Shell test sourcing contracts
# ═══════════════════════════════════════════════════════════════════════════════


class PR419ShellTestWiringTest(unittest.TestCase):
    """Shell tests that use assertions must source the assertions library."""

    def test_shell_tests_source_assertions_lib(self) -> None:
        assertions_sourcing = re.compile(
            r'source\s+.*assertions\.sh|[.]\s+.*assertions\.sh'
        )
        for script in SHELL_TESTS:
            text = script.read_text(encoding="utf-8")
            if "assert_" in text or "finish" in text:
                with self.subTest(script=script.name):
                    self.assertRegex(
                        text,
                        assertions_sourcing,
                        f"{script.name} uses assertion helpers but does not source assertions.sh.",
                    )


# ═══════════════════════════════════════════════════════════════════════════════
# 8. Cross-reference wiring contracts
# ═══════════════════════════════════════════════════════════════════════════════


class PR419CrossReferenceWiringTest(unittest.TestCase):
    """Docs must cross-reference each other and the executable contracts."""

    def test_silver_thread_ref_references_rendering_evidence_boundary(self) -> None:
        text = SILVER_THREAD_REF.read_text(encoding="utf-8")
        self.assertIn(
            "visible-rendering-pixel-target-blocker.json",
            text,
            "Silver-thread ref should reference the pixel target blocker artifact.",
        )

    def test_post_open_ref_links_to_nonclaim_contract(self) -> None:
        text = POST_OPEN_REF.read_text(encoding="utf-8")
        self.assertIn(
            "visible-rendering-evidence-nonclaim-contract",
            text,
        )

    def test_docs_index_links_to_silver_thread(self) -> None:
        index = (REPO_ROOT / "docs" / "index.md").read_text(encoding="utf-8")
        self.assertIn(
            "accessibility-target-discovery-silver-thread",
            index,
        )

    def test_howto_links_to_reference(self) -> None:
        howto = (
            REPO_ROOT / "docs" / "howto" / "alice-desktop-outside-in-qa.md"
        ).read_text(encoding="utf-8")
        self.assertIn(
            "post-open-runtime-display-accessibility-evidence",
            howto,
        )


# ═══════════════════════════════════════════════════════════════════════════════
# 9. Runner Python module invariants
# ═══════════════════════════════════════════════════════════════════════════════


class PR419RunnerModuleInvariantsTest(unittest.TestCase):
    """Runner Python modules must maintain key structural invariants."""

    def test_probe_runner_has_main_guard(self) -> None:
        source = PROBE_RUNNER.read_text(encoding="utf-8")
        self.assertIn('if __name__ == "__main__"', source)

    def test_sampler_runner_has_main_guard(self) -> None:
        source = SAMPLER_RUNNER.read_text(encoding="utf-8")
        self.assertIn('if __name__ == "__main__"', source)

    def test_probe_runner_writes_json_artifact(self) -> None:
        source = PROBE_RUNNER.read_text(encoding="utf-8")
        self.assertIn("json.dumps", source)

    def test_sampler_runner_writes_json_artifact(self) -> None:
        source = SAMPLER_RUNNER.read_text(encoding="utf-8")
        self.assertIn("json.dumps", source)

    def test_sampler_uses_xwd_sampling_method(self) -> None:
        source = SAMPLER_RUNNER.read_text(encoding="utf-8")
        self.assertIn("xwd", source)

    def test_probe_runtime_name_tokens_defined(self) -> None:
        source = PROBE_RUNNER.read_text(encoding="utf-8")
        self.assertIn("RUNTIME_NAME_TOKENS", source)
        for token in ("scene", "display", "runtime", "world", "render"):
            with self.subTest(token=token):
                self.assertIn(f'"{token}"', source)

    def test_probe_runtime_role_tokens_defined(self) -> None:
        source = PROBE_RUNNER.read_text(encoding="utf-8")
        self.assertIn("RUNTIME_ROLE_TOKENS", source)
        for token in ("canvas", "drawing", "viewport"):
            with self.subTest(token=token):
                self.assertIn(f'"{token}"', source)

    def test_sampler_validates_screen_extents_positive(self) -> None:
        source = SAMPLER_RUNNER.read_text(encoding="utf-8")
        self.assertIn("width", source)
        self.assertIn("height", source)
        self.assertRegex(
            source,
            re.compile(r"width\s*<=\s*0|height\s*<=\s*0"),
            "Sampler must reject non-positive dimensions.",
        )


# ═══════════════════════════════════════════════════════════════════════════════
# 10. Evidence log contract
# ═══════════════════════════════════════════════════════════════════════════════


class PR419EvidenceLogTest(unittest.TestCase):
    """The evidence log must exist and not contain merge conflict markers."""

    EVIDENCE_LOG = REPO_ROOT / ".copilot-evidence" / "default-workflow-attempt.log"

    def test_evidence_log_exists(self) -> None:
        self.assertTrue(
            self.EVIDENCE_LOG.is_file(),
            "Evidence log must exist.",
        )

    def test_evidence_log_has_no_conflict_markers(self) -> None:
        text = self.EVIDENCE_LOG.read_text(encoding="utf-8")
        conflict_marker = re.compile(r"^(<{7}|={7}|>{7})", re.MULTILINE)
        self.assertNotRegex(
            text,
            conflict_marker,
            "Evidence log must not contain unresolved merge conflict markers.",
        )


if __name__ == "__main__":
    unittest.main()
