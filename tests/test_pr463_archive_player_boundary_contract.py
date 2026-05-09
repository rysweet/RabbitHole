from functools import lru_cache
import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]

REFERENCE_DOC = REPO_ROOT / "docs" / "reference" / "player-archive-unsupported-tweedle-diagnostics.md"
HOWTO_DOC = REPO_ROOT / "docs" / "howto" / "characterize-player-archive-unsupported-tweedle-diagnostics.md"
TUTORIAL_DOC = REPO_ROOT / "docs" / "tutorials" / "player-archive-unsupported-this-call-diagnostic.md"
ARCHIVE_FIXTURE_SCENARIO = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "archive-fixture-smoke.yaml"
)
TWEEDLE_BOUNDARY_SCENARIO = (
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "scenarios" / "tweedle-decoder-boundary-smoke.yaml"
)
HISTORICAL_ARCHIVE_TEST = (
    REPO_ROOT
    / "core"
    / "story-api-migration"
    / "src"
    / "test"
    / "java"
    / "org"
    / "lgna"
    / "project"
    / "io"
    / "HistoricalArchiveRoundTripCharacterizationTest.java"
)
DECODER_TEST = (
    REPO_ROOT
    / "core"
    / "ast"
    / "src"
    / "test"
    / "java"
    / "org"
    / "alice"
    / "serialization"
    / "tweedle"
    / "TweedleEncoderDecoderTest.java"
)

BOUNDARY_SURFACES = [
    REFERENCE_DOC,
    HOWTO_DOC,
    TUTORIAL_DOC,
    ARCHIVE_FIXTURE_SCENARIO,
    TWEEDLE_BOUNDARY_SCENARIO,
]

NONCLAIMS = [
    "full tweedle/player decode",
    "migration completeness",
    "ui automation",
    "rendering",
    "grading",
    "save/open",
    "lesson completion",
]

FORBIDDEN_CLAIMS = [
    "proves full tweedle/player decode",
    "proves historical archive migration completeness",
    "proves full ui automation",
    "proves visible rendering correctness",
    "proves grading",
    "proves save/open",
    "proves lesson completion",
    "guarantees save/open",
    "guarantees lesson completion",
]

WHITESPACE_PATTERN = re.compile(r"\s+")
REFERENCE_DECODER_COMMAND_PATTERN = re.compile(
    r"-Dtest=org\.alice\.serialization\.tweedle\.TweedleEncoderDecoderTest#([^\\\s]+)"
)
DIRECT_RUNNER_EXAMPLE_PATTERN = re.compile(
    r"(?m)^\s*qa/outside-in/alice-desktop/runners/[a-z-]+\.sh\b"
)

REFERENCE_DECODER_SELECTORS = [
    "zeroArgumentThisMethodCallDecodeCreatesMethodInvocation",
    "zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall",
    "zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod",
    "zeroArgumentThisMethodCallDecodeRejectsUnknownMethod",
    "zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName",
    "zeroArgumentThisMethodCallDecodeRejectsNonThisTarget",
    "zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod",
    "zeroArgumentThisMethodCallDecodeRejectsChainedCall",
]
UNSUPPORTED_DECODER_SELECTORS = REFERENCE_DECODER_SELECTORS[1:]


@lru_cache(maxsize=None)
def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def normalized(text: str) -> str:
    return WHITESPACE_PATTERN.sub(" ", text.lower())


class Pr463ArchivePlayerBoundaryContractTest(unittest.TestCase):
    def test_boundary_claim_routing_is_durable_behavior_wording(self) -> None:
        combined = "\n".join(read_text(path) for path in BOUNDARY_SURFACES)
        lower = normalized(combined)

        required_fragments = (
            "boundary claim routing",
            "durable home",
            "generated fixture evidence",
            "missing-entry diagnostics",
            "gated command-smoke semantics",
            "explicit nonclaims",
            "archive-fixture-smoke.yaml",
            "archive-io",
            "superseded at the behavior level",
        )
        forbidden_fragments = (
            "pr #463 recovery disposition",
            "active archive/player boundary overlap status",
            "partially recover pr #463",
            "real overlap/conflict",
        )

        missing = [
            fragment
            for fragment in required_fragments
            if fragment not in lower
        ]
        self.assertEqual([], missing)
        for fragment in forbidden_fragments:
            with self.subTest(fragment=fragment):
                self.assertNotIn(fragment, lower)

    def test_archive_fixture_smoke_preserves_archive_io_wording_and_missing_entry_evidence(self) -> None:
        scenario = read_text(ARCHIVE_FIXTURE_SCENARIO)
        lower = scenario.lower()

        self.assertIn("workflow: archive-fixture-smoke", scenario)
        self.assertIn("automationMode: gated-command-smoke", scenario)
        self.assertIn("archive-io", lower)
        self.assertIn("archive i/o", lower)
        self.assertIn("missing-entry", lower)
        self.assertIn("unsupported tweedle diagnostics", lower)
        self.assertNotIn("project io", lower)
        self.assertNotIn("project-io", lower)
        self.assertNotIn("alice-desktop-project-io-smoke", lower)

    def test_tweedle_decoder_boundary_smoke_stays_gated_core_ast_decoder_evidence_only(self) -> None:
        scenario = read_text(TWEEDLE_BOUNDARY_SCENARIO)

        self.assertIn("workflow: tweedle-decoder-boundary-smoke", scenario)
        self.assertIn("automationMode: gated-command-smoke", scenario)
        self.assertIn("- core/ast", scenario)
        for selector in UNSUPPORTED_DECODER_SELECTORS:
            with self.subTest(selector=selector):
                self.assertIn(selector, scenario)
        self.assertIn("gated decoder-boundary evidence only", scenario)
        self.assertNotIn("IoUtilities.readProject", scenario)

    def test_java_characterization_suite_declares_unit_edge_and_error_contracts(self) -> None:
        source = read_text(HISTORICAL_ARCHIVE_TEST)

        expected_tests = [
            "generatedJsonPlayerArchiveDecodesProgramLiteralArithmeticFieldInitializer",
            "generatedJsonPlayerArchiveWithArgumentBearingExplicitThisMethodCallReportsUnsupportedDecodeBoundary",
            "generatedJsonPlayerArchiveMissingManifestDeclaredProgramEntryFailsClearly",
            "generatedJsonPlayerArchiveMissingManifestDeclaredSiblingEntryFailsClearly",
            "generatedJsonPlayerArchiveWithMixedIdentifierProgramInitializerIsRejectedWithoutPartialProgramDecode",
            "generatedJsonPlayerArchiveWithResourceFieldInitializerProgramTypeIsRejectedWithoutPartialProgramDecode",
        ]
        for test_name in expected_tests:
            with self.subTest(test_name=test_name):
                self.assertRegex(source, rf"\bvoid\s+{re.escape(test_name)}\s*\(")

    def test_boundary_surfaces_keep_nonclaims_explicit_and_avoid_positive_overclaims(self) -> None:
        for path in BOUNDARY_SURFACES:
            text = read_text(path)
            lower = normalized(text)
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                for term in NONCLAIMS:
                    self.assertIn(term, lower)
                self.assertNotIn("project io", lower)
                self.assertNotIn("project i/o", lower)
                for claim in FORBIDDEN_CLAIMS:
                    self.assertNotIn(claim, lower)

    def test_documentation_names_temporary_generated_fixture_location_only(self) -> None:
        expectations = {
            REFERENCE_DOC: (
                "Approved fixtures for this boundary are generated inside that JUnit test's temporary folder",
                "There is no stable checked-in archive fixture directory",
                "do not commit generated `.a3w` archives or binary corpus payloads",
            ),
            HOWTO_DOC: (
                "Generate archive fixtures in that test's JUnit temporary folder",
                "There is no stable checked-in fixture directory",
                "Do not commit generated `.a3w` archives or add binary corpus payloads",
            ),
            TUTORIAL_DOC: (
                "Generate this archive in the characterization test's temporary folder",
                "Do not add a checked-in `.a3w` fixture",
            ),
        }

        for path, required_fragments in expectations.items():
            text = read_text(path)
            lower = normalized(text)
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                for fragment in required_fragments:
                    self.assertIn(normalized(fragment), lower)
                self.assertNotIn("src/test/resources", text)
                self.assertNotIn("approved test fixture location", text.lower())

    def test_documentation_separates_stable_diagnostic_substrings_from_optional_context(self) -> None:
        docs_to_required_fragments = {
            REFERENCE_DOC: (
                "contractually stable decoder reason substring",
                "contractually stable call-site context substring",
                "argument-bearing explicit this method calls",
                "caller.this.helper",
                "Additional context such as archive path, manifest entry, decoder phase, source location, or wrapper exception wording is optional",
            ),
            HOWTO_DOC: (
                "Assert stable message substrings",
                "argument-bearing explicit this method calls",
                "caller.this.helper",
                "optional archive path, manifest entry, decoder phase, source location, or wrapper exception context",
            ),
            TUTORIAL_DOC: (
                "contractually stable decoder reason substring",
                "contractually stable call-site context substring",
                "argument-bearing explicit this method calls",
                "caller.this.helper",
                "archive path, manifest entry, decoder phase, source location, and wrapper exception text are optional context",
            ),
        }

        for path, required_fragments in docs_to_required_fragments.items():
            text = read_text(path)
            lower = normalized(text)
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                for fragment in required_fragments:
                    self.assertIn(normalized(fragment), lower)

    def test_reference_doc_limits_historical_archive_language_to_characterization_checks(self) -> None:
        reference = read_text(REFERENCE_DOC)
        normalized_reference = normalized(reference)
        combined = "\n".join(read_text(path) for path in BOUNDARY_SURFACES)
        lower = normalized(combined)

        self.assertIn(
            normalized("Historical Alice archives are loaded only by focused characterization and migration checks"),
            normalized_reference,
        )
        self.assertIn(
            normalized("This page does not claim general desktop Open support for historical archives"),
            normalized_reference,
        )
        self.assertNotIn("historical alice archives can still be opened", lower)
        self.assertNotIn("opened for characterization", lower)
        self.assertNotIn("general save/open support", lower)

    def test_reference_doc_decoder_command_covers_supported_and_unsupported_boundary_selectors(self) -> None:
        text = read_text(REFERENCE_DOC)
        match = REFERENCE_DECODER_COMMAND_PATTERN.search(text)

        self.assertIsNotNone(match)
        self.assertEqual(REFERENCE_DECODER_SELECTORS, match.group(1).split("+"))

    def test_documented_decoder_selectors_exist_in_java_characterization_suite(self) -> None:
        source = read_text(DECODER_TEST)

        for selector in REFERENCE_DECODER_SELECTORS:
            with self.subTest(selector=selector):
                self.assertRegex(source, rf"\bvoid\s+{re.escape(selector)}\s*\(")

    def test_reference_doc_literal_arithmetic_claim_stays_manifest_declared_type_scoped(self) -> None:
        text = read_text(REFERENCE_DOC)

        self.assertIn(
            "manifest-declared JSON `.a3w` program or sibling type",
            text,
        )
        self.assertIn(
            "Supported JSON `.a3w` manifest-declared type shard",
            text,
        )
        self.assertNotIn(
            "Supported JSON `.a3w` player shard | Literal arithmetic field initializers decode",
            text,
        )
        self.assertNotIn("manifest-declared JSON `.a3w` player type", text)

    def test_documented_qa_runner_examples_use_bash_wrappers(self) -> None:
        docs_to_expected_commands = {
            REFERENCE_DOC: (
                "bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
                "bash qa/outside-in/alice-desktop/runners/run-scenario.sh run",
            ),
            HOWTO_DOC: (
                "bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
            ),
        }

        for path, expected_commands in docs_to_expected_commands.items():
            text = read_text(path)
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                for command in expected_commands:
                    self.assertIn(command, text)
                self.assertNotRegex(
                    text,
                    DIRECT_RUNNER_EXAMPLE_PATTERN,
                )

    def test_recovery_evidence_docs_require_real_final_sha_status_and_validation_records(self) -> None:
        reference = read_text(REFERENCE_DOC)
        lower = normalized(reference)

        required_fragments = (
            "developbasesha",
            "headsha",
            "mergestatestatus",
            "recoverymode",
            "focused-archive-player-repair",
            "manualmergeperformed",
            "replacementpullrequestcreated",
            "noopmodeused",
            "archiveplayerevidencesurfaces",
            "repairdifffiles",
            "python-pr463-contracts",
            "alice-desktop-scenario-catalog",
            "story-api-migration-characterization",
            "core-ast-decoder-boundary",
        )
        forbidden_fragments = (
            "<origin/develop sha used for reconciliation>",
            "<final repaired branch sha>",
            "<all files changed by the focused repair",
        )

        for fragment in required_fragments:
            with self.subTest(fragment=fragment):
                self.assertIn(fragment, lower)
        for fragment in forbidden_fragments:
            with self.subTest(fragment=fragment):
                self.assertNotIn(fragment, lower)


if __name__ == "__main__":
    unittest.main()
