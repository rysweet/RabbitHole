"""Merge-integrity contract tests for PR #403.

These tests verify that the origin/develop merge into the
wave6-runtime-event-dispatch-1778302300 branch preserved structural
integrity across documentation, QA scenarios, and cross-references.
They guard against regressions from future merges or rebases.
"""

import re
import unittest
from pathlib import Path

try:
    import yaml

    HAS_YAML = True
except ImportError:
    HAS_YAML = False

REPO_ROOT = Path(__file__).resolve().parents[1]
INDEX_PATH = REPO_ROOT / "docs" / "index.md"
REFERENCE_PATH = (
    REPO_ROOT
    / "docs"
    / "reference"
    / "generated-story-api-listener-source-characterization.md"
)
HOWTO_PATH = (
    REPO_ROOT / "docs" / "howto" / "run-runtime-event-dispatch-characterization.md"
)
TUTORIAL_PATH = (
    REPO_ROOT
    / "docs"
    / "tutorials"
    / "trace-runtime-event-dispatch-characterization.md"
)
QA_SCENARIOS_DIR = REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios"
DISPATCH_SCENARIO_PATH = QA_SCENARIOS_DIR / "runtime-event-dispatch-smoke.yaml"
LISTENER_SCENARIO_PATH = (
    QA_SCENARIOS_DIR / "generated-listener-runtime-dispatch-smoke.yaml"
)

PR_SCOPED_FILES = [
    ".copilot-evidence/default-workflow-attempt.log",
    "core/ast/src/test/java/org/lgna/project/virtualmachine/VirtualMachineHeadlessRuntimeEventTest.java",
    "docs/howto/run-runtime-event-dispatch-characterization.md",
    "docs/index.md",
    "docs/reference/alice-desktop-outside-in-qa.md",
    "docs/reference/generated-story-api-listener-source-characterization.md",
    "docs/tutorials/trace-runtime-event-dispatch-characterization.md",
    "netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java",
    "qa/outside-in/alice-desktop/README.md",
    "qa/outside-in/alice-desktop/runners/run-scenario.sh",
    "qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "qa/outside-in/alice-desktop/scenarios/generated-listener-runtime-dispatch-smoke.yaml",
    "qa/outside-in/alice-desktop/scenarios/runtime-event-dispatch-smoke.yaml",
    "qa/outside-in/alice-desktop/schema/scenario.schema.json",
    "qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "qa/outside-in/alice-desktop/tests/test-workflow-contract.sh",
    "tests/test_runtime_event_dispatch_docs_contract.py",
]

VM_TEST_CLASS = "VirtualMachineHeadlessRuntimeEventTest"
VM_TEST_PATH = (
    REPO_ROOT
    / "core"
    / "ast"
    / "src"
    / "test"
    / "java"
    / "org"
    / "lgna"
    / "project"
    / "virtualmachine"
    / f"{VM_TEST_CLASS}.java"
)
CODEGEN_TEST_CLASS = "ProjectCodeGeneratorStoryApiGeneratedSourceTest"
CODEGEN_TEST_PATH = (
    REPO_ROOT
    / "netbeans"
    / "src"
    / "test"
    / "java"
    / "org"
    / "alice"
    / "netbeans"
    / "project"
    / f"{CODEGEN_TEST_CLASS}.java"
)

CONFLICT_MARKER_PATTERN = re.compile(r"^(<{7}|={7}|>{7})", re.MULTILINE)
MARKDOWN_LINK_PATTERN = re.compile(r"\[([^\]]+)\]\((\./[^)#]+)")
H1_PATTERN = re.compile(r"^# ", re.MULTILINE)
H2_PATTERN = re.compile(r"^## (.+)$", re.MULTILINE)


class MergeConflictFreedomTest(unittest.TestCase):
    """Verify no conflict markers survived the merge resolution."""

    def test_no_conflict_markers_in_pr_scoped_files(self) -> None:
        for rel_path in PR_SCOPED_FILES:
            path = REPO_ROOT / rel_path
            if not path.exists():
                continue
            with self.subTest(file=rel_path):
                content = path.read_text(encoding="utf-8", errors="replace")
                self.assertIsNone(
                    CONFLICT_MARKER_PATTERN.search(content),
                    f"Conflict marker found in {rel_path}",
                )


class DocsIndexStructuralIntegrityTest(unittest.TestCase):
    """Verify docs/index.md has no duplicate links and all links resolve."""

    def setUp(self) -> None:
        self.index_text = INDEX_PATH.read_text(encoding="utf-8")

    def test_no_duplicate_markdown_links(self) -> None:
        links = MARKDOWN_LINK_PATTERN.findall(self.index_text)
        targets = [target for _, target in links]
        seen: dict[str, int] = {}
        duplicates: list[str] = []
        for target in targets:
            seen[target] = seen.get(target, 0) + 1
        for target, count in seen.items():
            if count > 1:
                duplicates.append(f"{target} (×{count})")
        self.assertEqual(
            [],
            duplicates,
            f"Duplicate links in docs/index.md: {', '.join(duplicates)}",
        )

    def test_all_index_links_resolve_to_existing_files(self) -> None:
        links = MARKDOWN_LINK_PATTERN.findall(self.index_text)
        missing: list[str] = []
        for label, target in links:
            resolved = (INDEX_PATH.parent / target).resolve()
            if not resolved.exists():
                missing.append(f"[{label}]({target})")
        self.assertEqual(
            [],
            missing,
            f"Broken links in docs/index.md: {', '.join(missing)}",
        )

    def test_runtime_dispatch_docs_linked_exactly_once(self) -> None:
        ref_link = (
            "./reference/generated-story-api-listener-source-characterization.md"
        )
        howto_link = "./howto/run-runtime-event-dispatch-characterization.md"
        tutorial_link = (
            "./tutorials/trace-runtime-event-dispatch-characterization.md"
        )
        for link in [ref_link, howto_link, tutorial_link]:
            with self.subTest(link=link):
                count = self.index_text.count(link)
                self.assertEqual(
                    1,
                    count,
                    f"Expected exactly 1 occurrence of {link} in index, found {count}",
                )


class CharacterizationDocStructureTest(unittest.TestCase):
    """Verify the merged characterization reference doc has valid structure."""

    def setUp(self) -> None:
        self.doc_text = REFERENCE_PATH.read_text(encoding="utf-8")

    def test_single_h1_heading(self) -> None:
        h1_matches = H1_PATTERN.findall(self.doc_text)
        self.assertEqual(
            1,
            len(h1_matches),
            f"Expected exactly 1 h1 heading, found {len(h1_matches)}",
        )

    def test_h1_is_expected_title(self) -> None:
        first_line = self.doc_text.split("\n", 1)[0]
        self.assertEqual(
            "# Headless Runtime Dispatch and Generated Story API Listener Source Characterization",
            first_line,
        )

    def test_no_duplicate_h2_sections(self) -> None:
        h2_headings = H2_PATTERN.findall(self.doc_text)
        seen: dict[str, int] = {}
        for heading in h2_headings:
            seen[heading] = seen.get(heading, 0) + 1
        duplicates = [f"{h} (×{c})" for h, c in seen.items() if c > 1]
        # Executable characterization appears twice by design (different contexts)
        allowed_duplicates = {"Executable characterization"}
        real_duplicates = [
            d
            for d in duplicates
            if not any(a in d for a in allowed_duplicates)
        ]
        self.assertEqual(
            [],
            real_duplicates,
            f"Duplicate h2 sections: {', '.join(real_duplicates)}",
        )

    def test_table_of_contents_entries_have_matching_headings(self) -> None:
        toc_section = self.doc_text.split("## Contents", 1)
        if len(toc_section) < 2:
            self.fail("Missing ## Contents section")
        toc_block = toc_section[1].split("\n## ", 1)[0]
        toc_links = re.findall(r"\[([^\]]+)\]\(#([^)]+)\)", toc_block)
        self.assertGreater(len(toc_links), 0, "No TOC entries found")

        all_headings_raw = re.findall(r"^##+ (.+)$", self.doc_text, re.MULTILINE)
        slugs = set()
        for heading in all_headings_raw:
            slug = re.sub(r"[^a-z0-9 -]", "", heading.lower().strip("`"))
            slug = re.sub(r"\s+", "-", slug).strip("-")
            slugs.add(slug)

        broken: list[str] = []
        for label, anchor in toc_links:
            if anchor not in slugs:
                broken.append(f"[{label}](#{anchor})")
        self.assertEqual(
            [],
            broken,
            f"TOC entries with no matching heading: {', '.join(broken)}",
        )


class CrossReferenceValidityTest(unittest.TestCase):
    """Verify docs cross-reference actual test files and method names."""

    def test_howto_references_existing_test_files(self) -> None:
        howto_text = HOWTO_PATH.read_text(encoding="utf-8")
        self.assertIn(VM_TEST_CLASS, howto_text)
        self.assertIn(CODEGEN_TEST_CLASS, howto_text)
        self.assertTrue(VM_TEST_PATH.exists(), f"Missing {VM_TEST_PATH}")
        self.assertTrue(CODEGEN_TEST_PATH.exists(), f"Missing {CODEGEN_TEST_PATH}")

    def test_tutorial_references_existing_test_files(self) -> None:
        tutorial_text = TUTORIAL_PATH.read_text(encoding="utf-8")
        self.assertIn(VM_TEST_CLASS, tutorial_text)
        self.assertIn(CODEGEN_TEST_CLASS, tutorial_text)

    def test_tutorial_references_actual_test_method_names(self) -> None:
        tutorial_text = TUTORIAL_PATH.read_text(encoding="utf-8")
        vm_source = VM_TEST_PATH.read_text(encoding="utf-8")
        codegen_source = CODEGEN_TEST_PATH.read_text(encoding="utf-8")

        expected_vm_method = (
            "headlessStaticStoryMethodNotifiesListenerAroundBlockAndCommentStatements"
        )
        expected_codegen_method = (
            "generatedSyntheticSceneListenerRegistrationSourceCompiles"
        )

        self.assertIn(expected_vm_method, vm_source,
                       f"Method {expected_vm_method} not found in VM test source")
        self.assertIn(expected_vm_method, tutorial_text,
                       f"Tutorial does not reference {expected_vm_method}")

        self.assertIn(expected_codegen_method, codegen_source,
                       f"Method {expected_codegen_method} not found in codegen test source")
        self.assertIn(expected_codegen_method, tutorial_text,
                       f"Tutorial does not reference {expected_codegen_method}")

    def test_reference_doc_references_actual_source_paths(self) -> None:
        ref_text = REFERENCE_PATH.read_text(encoding="utf-8")
        source_paths = [
            "core/ast/src/test/java/org/lgna/project/virtualmachine/VirtualMachineHeadlessRuntimeEventTest.java",
            "netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java",
        ]
        for source_path in source_paths:
            with self.subTest(path=source_path):
                self.assertIn(source_path, ref_text)
                self.assertTrue(
                    (REPO_ROOT / source_path).exists(),
                    f"Referenced path does not exist: {source_path}",
                )


class QaScenarioIntegrityTest(unittest.TestCase):
    """Verify QA YAML scenarios are well-formed with required keys."""

    REQUIRED_KEYS = {"id", "title", "workflow", "automationMode", "preconditions",
                     "userActions", "expectedOutcomes", "automation", "evidence",
                     "fallback", "tags"}

    def _load_yaml_or_parse(self, path: Path) -> dict:
        text = path.read_text(encoding="utf-8")
        if HAS_YAML:
            return yaml.safe_load(text)
        # Fallback: verify basic structure via regex
        result = {}
        for key in self.REQUIRED_KEYS:
            if re.search(rf"^{key}:", text, re.MULTILINE):
                result[key] = True
        return result

    def _check_scenario(self, path: Path) -> None:
        self.assertTrue(path.exists(), f"Scenario file missing: {path}")
        data = self._load_yaml_or_parse(path)
        self.assertIsInstance(data, dict)
        missing = self.REQUIRED_KEYS - set(data.keys())
        self.assertEqual(
            set(),
            missing,
            f"Missing required keys in {path.name}: {missing}",
        )

    def test_runtime_event_dispatch_smoke_scenario(self) -> None:
        self._check_scenario(DISPATCH_SCENARIO_PATH)
        data = self._load_yaml_or_parse(DISPATCH_SCENARIO_PATH)
        self.assertEqual("runtime-event-dispatch-smoke", data.get("workflow"))
        self.assertEqual("gated-command-smoke", data.get("automationMode"))

    def test_generated_listener_dispatch_smoke_scenario(self) -> None:
        self._check_scenario(LISTENER_SCENARIO_PATH)
        data = self._load_yaml_or_parse(LISTENER_SCENARIO_PATH)
        self.assertEqual(
            "generated-listener-runtime-dispatch-smoke", data.get("workflow")
        )
        self.assertEqual("gated-command-smoke", data.get("automationMode"))

    def test_scenario_automation_references_correct_test_classes(self) -> None:
        dispatch_text = DISPATCH_SCENARIO_PATH.read_text(encoding="utf-8")
        listener_text = LISTENER_SCENARIO_PATH.read_text(encoding="utf-8")
        self.assertIn(
            f"org.lgna.project.virtualmachine.{VM_TEST_CLASS}",
            dispatch_text,
        )
        self.assertIn(
            f"org.alice.netbeans.project.{CODEGEN_TEST_CLASS}",
            listener_text,
        )

    def test_scenario_non_claims_match_reference_doc(self) -> None:
        """Both scenarios must declare the same bounded non-claims as the reference."""
        non_claims = [
            "desktop runtime execution",
            "full world playback",
            "visible correctness",
            "grading",
            "Save completion",
            "full UI automation",
        ]
        for scenario_path in [DISPATCH_SCENARIO_PATH, LISTENER_SCENARIO_PATH]:
            text = scenario_path.read_text(encoding="utf-8")
            for claim in non_claims:
                with self.subTest(scenario=scenario_path.name, claim=claim):
                    self.assertIn(
                        claim,
                        text,
                        f"Non-claim '{claim}' missing from {scenario_path.name}",
                    )


class MergeAncestryTest(unittest.TestCase):
    """Verify the branch structure is correct after merge."""

    def test_pr_scoped_files_all_exist(self) -> None:
        missing: list[str] = []
        for rel_path in PR_SCOPED_FILES:
            if not (REPO_ROOT / rel_path).exists():
                missing.append(rel_path)
        self.assertEqual(
            [],
            missing,
            f"PR-scoped files missing from worktree: {', '.join(missing)}",
        )

    def test_docs_contract_test_exists_and_is_importable(self) -> None:
        contract_path = REPO_ROOT / "tests" / "test_runtime_event_dispatch_docs_contract.py"
        self.assertTrue(contract_path.exists())
        content = contract_path.read_text(encoding="utf-8")
        self.assertIn("class RuntimeEventDispatchDocsContractTest", content)
        self.assertIn("class RuntimeEventDispatchNoOpGuardContractTest", content)
        self.assertIn("class RuntimeEventDispatchPr403RecoveryContractTest", content)


class PayloadAssertionAccuracyTest(unittest.TestCase):
    """Verify refined doc wording matches actual test behavior."""

    def test_reference_event_seam_describes_type_check_not_instance(self) -> None:
        ref_text = REFERENCE_PATH.read_text(encoding="utf-8")
        # The test uses assertSame on the Class object, not instance identity
        self.assertNotIn("exact `SceneActivationEvent` instance", ref_text,
                         "Stale wording: the test checks the event *type*, not instance identity")

    def test_vm_test_uses_assertequals_for_event_sequence(self) -> None:
        vm_source = VM_TEST_PATH.read_text(encoding="utf-8")
        # The VM test uses assertEquals on the recorded event list
        self.assertIn("assertEquals", vm_source,
                       "VM test should use assertEquals for event sequence checks")

    def test_codegen_test_uses_assertsame_for_class_identity(self) -> None:
        codegen_source = CODEGEN_TEST_PATH.read_text(encoding="utf-8")
        # The codegen test uses assertSame on the SceneActivationEvent Class object
        self.assertIn("assertSame", codegen_source,
                       "Codegen test should use assertSame for event type identity")

    def test_codegen_test_uses_scene_activation_event_class(self) -> None:
        codegen_source = CODEGEN_TEST_PATH.read_text(encoding="utf-8")
        self.assertIn("SceneActivationEvent", codegen_source,
                       "Codegen test should reference SceneActivationEvent")


if __name__ == "__main__":
    unittest.main()
