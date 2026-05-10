import re
import unittest
from functools import lru_cache
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
WHITESPACE_RE = re.compile(r"\s+")
STALE_BRANCH_EXAMPLE_RE = re.compile(r"uvx --from git\+[^ \n]+@<branch>(?:\s|$)")

CLI_DOCS = [
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "README.md",
    REPO_ROOT / "docs" / "howto" / "alice-desktop-outside-in-qa.md",
    REPO_ROOT / "docs" / "reference" / "alice-desktop-outside-in-qa.md",
    REPO_ROOT / "docs" / "howto" / "run-save-menu-dialog-negative-artifact-contract.md",
    REPO_ROOT / "docs" / "reference" / "save-menu-dialog-negative-artifact-contract.md",
]

NEGATIVE_CONTRACT_DOCS = [
    REPO_ROOT / "docs" / "howto" / "run-save-menu-dialog-negative-artifact-contract.md",
    REPO_ROOT / "docs" / "reference" / "save-menu-dialog-negative-artifact-contract.md",
]

POSITIVE_SAVE_PROOF_DOCS = [
    REPO_ROOT / "docs" / "howto" / "run-save-menu-dialog-write-proof.md",
    REPO_ROOT / "docs" / "reference" / "save-menu-dialog-write-proof.md",
]


@lru_cache(maxsize=None)
def read_doc(path: Path) -> str:
    return path.read_text(encoding="utf-8")


@lru_cache(maxsize=None)
def normalized_doc(path: Path) -> str:
    return WHITESPACE_RE.sub(" ", read_doc(path))


def stale_branch_examples(relative_path: Path, text: str) -> list[str]:
    if "@<branch>" not in text:
        return []

    return [
        f"{relative_path}:{line_number}"
        for line_number, line in enumerate(text.splitlines(), start=1)
        if STALE_BRANCH_EXAMPLE_RE.search(line)
    ]


class AliceQaAmplihackDocsContractTest(unittest.TestCase):
    def test_branch_installable_examples_accept_branch_or_commit_tokens(self) -> None:
        stale_examples = []

        for path in CLI_DOCS:
            text = read_doc(path)
            relative_path = path.relative_to(REPO_ROOT)
            with self.subTest(path=relative_path):
                if "amplihack alice-qa" in text:
                    self.assertIn("<branch-or-commit>", text)

            stale_examples.extend(stale_branch_examples(relative_path, text))

        self.assertEqual(
            [],
            stale_examples,
            "Branch-installable Amplihack QA examples should name <branch-or-commit>, not <branch>.",
        )

    def test_negative_save_contract_docs_keep_cli_wrapper_bounded_to_artifact_validation(self) -> None:
        for path in NEGATIVE_CONTRACT_DOCS:
            text = read_doc(path)
            normalized = normalized_doc(path)

            with self.subTest(path=path.relative_to(REPO_ROOT)):
                self.assertIn("amplihack alice-qa save-negative-contract", text)
                self.assertIn(
                    "qa/outside-in/alice-desktop/tests/test-save-menu-dialog-negative-artifact-contract.sh",
                    text,
                )
                self.assertIn("accepts no extra arguments", normalized)
                self.assertIn(
                    "success proves only that invalid Save proof artifacts fail closed",
                    normalized,
                )

    def test_shared_qa_docs_do_not_claim_cli_wrapper_proves_desktop_save_completion(self) -> None:
        reference = REPO_ROOT / "docs" / "reference" / "alice-desktop-outside-in-qa.md"
        normalized = normalized_doc(reference)

        self.assertIn(
            "Proves invalid Save proof artifacts fail closed with explicit diagnostics; "
            "it is not desktop Save completion evidence.",
            normalized,
        )

    def test_positive_save_docs_distinguish_wrapper_evidence_from_direct_maven_default(self) -> None:
        for path in POSITIVE_SAVE_PROOF_DOCS:
            text = read_doc(path)
            normalized = normalized_doc(path)

            with self.subTest(path=path.relative_to(REPO_ROOT)):
                self.assertIn("robot-save-menu-dialog-write-readback-proof.json", text)
                self.assertIn("scenario run directory", normalized)
                self.assertIn("save-proof-validation.log", text)
                self.assertIn("-Dorg.alice.eatme.saveProof.evidencePath", text)
                self.assertIn(
                    "core/ide/target/save-menu-proofs/robot-save-menu-dialog-write-readback-proof.json",
                    text,
                )

    def test_positive_save_docs_keep_silver_thread_claim_boundary(self) -> None:
        for path in POSITIVE_SAVE_PROOF_DOCS:
            normalized = normalized_doc(path)

            with self.subTest(path=path.relative_to(REPO_ROOT)):
                self.assertRegex(
                    normalized,
                    r"(one rendered path|proves one path|single rendered Save path)",
                )
                self.assertIn(
                    "does not prove Save As, overwrite prompts, cancellation, retry, native file dialogs, "
                    "every Save variant, lesson completion, grading, or broad desktop automation",
                    normalized,
                )


if __name__ == "__main__":
    unittest.main()
