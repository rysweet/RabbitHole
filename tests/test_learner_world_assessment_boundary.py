import json
import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[1]
BOUNDARY_ARTIFACT = (
    REPO_ROOT
    / "qa"
    / "outside-in"
    / "alice-desktop"
    / "contracts"
    / "learner-world-assessment-boundary.json"
)
INSTRUCTOR_STUDENT_SCENARIO = (
    REPO_ROOT
    / "qa"
    / "outside-in"
    / "alice-desktop"
    / "scenarios"
    / "instructor-student-setup.yaml"
)
BOUNDARY_DOCS = [
    REPO_ROOT / "qa" / "outside-in" / "alice-desktop" / "README.md",
    REPO_ROOT / "docs" / "reference" / "alice-desktop-outside-in-qa.md",
    REPO_ROOT / "docs" / "howto" / "alice-desktop-outside-in-qa.md",
    REPO_ROOT / "docs" / "tutorials" / "alice-desktop-outside-in-qa.md",
]
BOUNDARY_PHRASE = "rabbithole learner-world qa currently supports setup/open/save evidence review"
NEXT_BLOCKER_ID = "define-reviewed-assessment-contract"
NON_CAPABILITIES = [
    "learner-work grading",
    "rubric scoring",
    "correctness assessment",
    "creativity assessment",
]
OVERCLAIM_TERMS = [
    "learner-work grading",
    "learner work grading",
    "automated grading",
    "rubric scoring",
    "correctness assessment",
    "creativity assessment",
    "creative assessment",
    "assess creativity",
]
NEGATION_MARKERS = [
    "does not",
    "do not",
    "must not",
    "not ",
    "no ",
    "noncapabilities",
    "future ",
    "requires",
    "required before",
    "only",
    "blocker",
    "cannot currently",
    "before any",
]


def normalized_text(path: Path) -> str:
    return re.sub(r"\s+", " ", path.read_text(encoding="utf-8").lower())


class LearnerWorldAssessmentBoundaryContractTest(unittest.TestCase):
    def test_boundary_artifact_is_declarative_and_names_next_blocker(self) -> None:
        boundary = json.loads(BOUNDARY_ARTIFACT.read_text(encoding="utf-8"))

        self.assertEqual("learner-world-assessment-boundary", boundary.get("id"))
        self.assertEqual(
            "instructor-student learner-world setup/open/save evidence",
            boundary.get("scope"),
        )
        self.assertEqual(
            "collects evidence for setup, open, and save workflow review",
            boundary.get("currentCapability"),
        )
        self.assertEqual(NON_CAPABILITIES, boundary.get("nonCapabilities"))
        self.assertEqual(NEXT_BLOCKER_ID, boundary.get("nextBlocker", {}).get("id"))

        blocker_description = boundary.get("nextBlocker", {}).get("description", "")
        self.assertIn("reviewed assessment contract", blocker_description)
        self.assertIn("evidence mapping", blocker_description)
        for non_capability in NON_CAPABILITIES:
            with self.subTest(non_capability=non_capability):
                self.assertIn(non_capability, blocker_description)

        behavior_fields = {
            "assessmentAlgorithm",
            "gradingAlgorithm",
            "rubricSchema",
            "scoreSchema",
            "runnerIntegration",
        }
        self.assertFalse(
            behavior_fields.intersection(boundary),
            "Boundary artifact must stay declarative and must not configure assessment behavior.",
        )

    def test_instructor_student_scenario_remains_setup_open_save_evidence_only(self) -> None:
        scenario_text = normalized_text(INSTRUCTOR_STUDENT_SCENARIO)

        self.assertIn("automationmode: manual-evidence-required", scenario_text)
        self.assertIn("setup/open/save evidence review only", scenario_text)
        for non_capability in NON_CAPABILITIES:
            with self.subTest(non_capability=non_capability):
                self.assertIn(non_capability, scenario_text)

    def test_docs_name_boundary_and_blocker_without_overclaiming_assessment(self) -> None:
        for path in BOUNDARY_DOCS:
            text = normalized_text(path)
            with self.subTest(path=path.relative_to(REPO_ROOT)):
                self.assertIn(BOUNDARY_PHRASE, text)
                self.assertIn(str(BOUNDARY_ARTIFACT.relative_to(REPO_ROOT)), text)
                self.assertIn(NEXT_BLOCKER_ID, text)

    def test_boundary_surfaces_do_not_make_unguarded_assessment_claims(self) -> None:
        scanned_paths = [BOUNDARY_ARTIFACT, INSTRUCTOR_STUDENT_SCENARIO, *BOUNDARY_DOCS]

        unguarded_claims = []
        for path in scanned_paths:
            text = path.read_text(encoding="utf-8")
            normalized = re.sub(r"\n(?=\S)", " ", text)
            for paragraph in re.split(r"\n\s*\n", normalized):
                lower = paragraph.lower()
                matches = [term for term in OVERCLAIM_TERMS if term in lower]
                if matches and not any(marker in lower for marker in NEGATION_MARKERS):
                    line_number = text.count("\n", 0, text.find(paragraph[:20])) + 1
                    unguarded_claims.append(
                        f"{path.relative_to(REPO_ROOT)}:{line_number}: {', '.join(matches)}"
                    )

        self.assertEqual(
            [],
            unguarded_claims,
            "Learner-world boundary surfaces must not imply current grading or creative assessment.",
        )


if __name__ == "__main__":
    unittest.main()
