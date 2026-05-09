import re
import unittest
from functools import lru_cache
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]

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


@lru_cache(maxsize=None)
def read_doc(path: Path) -> str:
    return path.read_text(encoding="utf-8")


@lru_cache(maxsize=None)
def normalized_doc(path: Path) -> str:
    return re.sub(r"\s+", " ", read_doc(path))


class AliceQaAmplihackDocsContractTest(unittest.TestCase):
    def test_branch_installable_examples_accept_branch_or_commit_tokens(self) -> None:
        stale_examples = []

        for path in CLI_DOCS:
            text = read_doc(path)
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                if "amplihack alice-qa" in text:
                    self.assertIn("<branch-or-commit>", text)

            for match in re.finditer(r"uvx --from git\+[^ \n]+@<branch>(?:\s|$)", text):
                stale_examples.append(
                    f"{path.relative_to(REPO_ROOT)}:{text.count(chr(10), 0, match.start()) + 1}"
                )

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


if __name__ == "__main__":
    unittest.main()
