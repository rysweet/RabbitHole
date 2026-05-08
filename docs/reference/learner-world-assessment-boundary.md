# Learner-world assessment boundary

This reference describes the current learner-world assessment boundary. The
boundary keeps instructor/student setup/open/save evidence review separate from
assessment features that RabbitHole does not implement.

## Contents

- [Current behavior](#current-behavior)
- [Generated run artifacts](#generated-run-artifacts)
- [Current boundary artifact](#current-boundary-artifact)
- [Reviewed assessment boundary fields](#reviewed-assessment-boundary-fields)
- [Configuration](#configuration)
- [Scenario configuration](#scenario-configuration)
- [Manual review workflow](#manual-review-workflow)
- [Examples](#examples)
- [Extension rules](#extension-rules)

## Current behavior

RabbitHole learner-world QA currently supports setup/open/save evidence review
only. The selected scenario is `alice-desktop-instructor-student-setup`, and its
workflow is `instructor-student-setup`.

The scenario is intentionally `manual-evidence-required`. Running it prepares a
timestamped evidence directory and a checklist for human review. Checklist
generation is not a pass result, and the generated evidence does not perform
learner-world grading, rubric scoring, correctness assessment, creative
assessment, or any automated equivalent of those activities.

Supported evidence is limited to:

| Evidence area | What the reviewer checks |
| --- | --- |
| Instructor setup | Alice can open or prepare the starter project for learner use. |
| Student open | Alice can open the learner copy from the expected location. |
| Student save | Alice can save a separate student copy for later review. |

The runner treats learner-world state extraction for assessment as blocked until
the `define-reviewed-assessment-contract` boundary is resolved. It does not
synthesize rubric inputs, infer learner intent, judge world correctness, or
assess creativity from setup/open/save artifacts.

## Generated run artifacts

Running the selected scenario creates:

```text
<evidence-dir>/alice-desktop-instructor-student-setup/<timestamp>/
  environment.txt
  status.txt
  manual-evidence-checklist.txt
```

`status.txt` records `automationMode=manual-evidence-required`, points to the
generated checklist, and repeats the assessment boundary summary for the
selected scenario:

```text
assessmentBoundary=define-reviewed-assessment-contract
assessmentBoundaryMode=manual/unsupported
assessmentBoundaryScope=instructor-student learner-world setup/open/save evidence
assessmentLimitation=Learner-world grading, rubric scoring, correctness assessment, and creative assessment remain manual/unsupported until a reviewed assessment contract exists.
assessmentLimits=no automated grading; no rubric scoring; no correctness assessment; no creative assessment
assessmentUnsupportedUntilReviewedContract=learner-world grading; rubric scoring; correctness assessment; creative assessment
assessmentBlocker=define-reviewed-assessment-contract
assessmentBlockerDescription=learner-world state extraction for grading or creative assessment is blocked until a reviewed assessment contract and evidence mapping define safe rubric inputs and limits.
```

It does not record a learner grade, rubric score, correctness result,
creative-assessment result, or assessment pass.

`manual-evidence-checklist.txt` includes the standard manual sections:
preconditions, user actions, expected outcomes, required evidence, fallback
notes, and completion status. For `alice-desktop-instructor-student-setup`, both
`status.txt` and the generated `Assessment boundary` checklist section are
rendered from the checked-in boundary contract:

```text
qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json
```

## Current boundary artifact

The current boundary record is declarative documentation, not an assessment
engine. It is stored at:

```text
qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json
```

The current artifact exposes these fields:

| Field | Meaning |
| --- | --- |
| `id` | Stable identifier: `learner-world-assessment-boundary`. |
| `selectedScenario` | Scenario covered by the boundary: `alice-desktop-instructor-student-setup`. |
| `automationMode` | Always `manual-evidence-required` for this boundary. |
| `scope` | Instructor/student learner-world setup/open/save evidence. |
| `currentCapability` | Collects manual evidence for setup, open, and save workflow review. |
| `supportedEvidence` | Explicit list of supported evidence areas: setup/open/save evidence review only, instructor starter artifact review, student open artifact review, student save artifact review, and manual review-notes acceptance decision. |
| `assessmentLimits` | Explicit limits: no automated grading, no rubric scoring, no correctness assessment, and no creative assessment. |
| `nonCapabilities` | Unsupported learner-world grading, rubric scoring, correctness assessment, and creative assessment claims. |
| `nextBoundary` | The next required boundary before assessment work can be claimed: `define-reviewed-assessment-contract`. |
| `manualLimitationSummary` | User-facing summary rendered in generated evidence and documentation. It states that learner-world grading, rubric scoring, correctness assessment, and creative assessment remain manual/unsupported. |
| `requiresReviewedAssessmentContractBefore` | Required contract topics before any future assessment implementation can be claimed: learner-world grading, rubric scoring, correctness assessment, and creative assessment. |
| `nextBlocker.id` | Back-compatible blocker identifier for review tooling: `define-reviewed-assessment-contract`. |
| `nextBlocker.description` | User-facing explanation that a reviewed assessment contract and evidence mapping are required before grading or creative assessment work can be claimed. |
| `blocker` | User-facing blocker shown in generated evidence: learner-world state extraction for grading or creative assessment is blocked until the reviewed assessment contract and evidence mapping define safe rubric inputs and limits. |

Consumers may display these fields in documentation, generated evidence, or
review tooling. They must not treat this file as executable assessment behavior
without a separate reviewed implementation change.

The current contract intentionally does not expose executable assessment fields.
The current docs-owned contract test rejects these behavior fields because they
would imply behavior that does not exist:

```text
gradingAlgorithm
assessmentAlgorithm
scoreSchema
rubricSchema
runnerIntegration
```

## Reviewed assessment boundary fields

This feature is a stricter documentation-and-evidence boundary, not an
assessment engine. The JSON contract, generated checklist, documentation, and
contract tests use the same wording and the same fields.

The current contract includes these assessment-boundary fields:

| Field | Meaning |
| --- | --- |
| `nextBoundary` | Blocking requirement for future assessment work: `define-reviewed-assessment-contract`. |
| `manualLimitationSummary` | User-facing summary rendered in generated evidence and documentation. It states that grading, rubric scoring, correctness assessment, and creative assessment remain manual/unsupported. |
| `requiresReviewedAssessmentContractBefore` | Required contract topics before any future assessment implementation can be claimed: learner-world grading, rubric scoring, correctness assessment, and creative assessment. |

The contract uses these assessment terms consistently:

| Topic | Wording |
| --- | --- |
| Grading | `learner-world grading` |
| Rubric evaluation | `rubric scoring` |
| Correctness | `correctness assessment` |
| Creativity | `creative assessment` |

The contract tests reject every executable assessment field reserved by the
boundary, including:

```text
gradingAlgorithm
assessmentAlgorithm
scoreSchema
rubricSchema
creativeAssessmentEngine
runnerIntegration
```

## Configuration

The boundary is enabled by the checked-in contract at:

```text
qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json
```

No environment variable enables automated learner-world grading or creative
assessment. `NODE_OPTIONS=--max-old-space-size=32768` may be used for the
repository QA commands, but it changes process memory only; it does not change
the assessment boundary.

The runner reads the boundary contract only to render plain-text limitation
wording into generated evidence. It does not execute, interpret, or transform
contract values into grading logic.

## Generated checklist section

The current `manual-evidence-checklist.txt` for
`alice-desktop-instructor-student-setup` includes a generated section with this
shape:

```text
Assessment boundary
-------------------
1. Manual evidence required.
2. Scope: instructor-student learner-world setup/open/save evidence.
3. Supported evidence: setup/open/save evidence review only.
4. Supported evidence: instructor starter project artifact review.
5. Supported evidence: student open artifact review.
6. Supported evidence: student save artifact review.
7. Supported evidence: manual review-notes.txt acceptance decision.
8. Learner-world grading, rubric scoring, correctness assessment, and creative assessment remain manual/unsupported until a reviewed assessment contract exists.
9. Assessment limit: no automated grading.
10. Assessment limit: no rubric scoring.
11. Assessment limit: no correctness assessment.
12. Assessment limit: no creative assessment.
13. Next boundary: define-reviewed-assessment-contract.
14. Manual/unsupported until reviewed contract: learner-world grading.
15. Manual/unsupported until reviewed contract: rubric scoring.
16. Manual/unsupported until reviewed contract: correctness assessment.
17. Manual/unsupported until reviewed contract: creative assessment.
18. Blocker: define-reviewed-assessment-contract.
19. learner-world state extraction for grading or creative assessment is blocked until a reviewed assessment contract and evidence mapping define safe rubric inputs and limits.
```

Future changes must update the JSON contract, generated status/checklist
wording, documentation, and docs-owned contract tests together so unsupported
assessment claims stay visible.

## Scenario configuration

The scenario remains a normal Alice desktop QA scenario:

```yaml
id: alice-desktop-instructor-student-setup
title: Instructor/student learner-world setup evidence
workflow: instructor-student-setup
automationMode: manual-evidence-required
```

The current scenario schema does not accept an `assessmentBoundary` field. A
future schema extension can reference the boundary with user-facing wording such
as:

```yaml
# Example future schema extension; not accepted by the current validator.
assessmentBoundary:
  mode: manual-evidence-required
  supportedEvidence:
    - instructor starter setup
    - student project open
    - student project save
  nextBoundary: define-reviewed-assessment-contract
  manualLimitationSummary: >
    Learner-world grading, rubric scoring, correctness assessment, and creative
    assessment remain manual/unsupported until a reviewed assessment contract
    exists.
  requiresReviewedAssessmentContractBefore:
    - learner-world grading
    - rubric scoring
    - correctness assessment
    - creative assessment
```

The scenario YAML does not configure assessment behavior. If a future scenario
metadata field references the boundary, the JSON Schema, dependency-free
validator, runner checklist generation, and shell workflow contract test must be
updated together so schema and validator behavior do not drift.

## Manual review workflow

Run the selected scenario from the repository root:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-instructor-student-setup \
  --evidence-dir qa/outside-in/alice-desktop/evidence/manual-runs
```

The generated checklist is preparation only. A reviewer completes the workflow
in Alice and adds these artifacts to the same timestamped run directory:

```text
instructor-launch.log
starter-project-open.png
starter-project.a3p
student-open.log
student-project-open.png
student-copy.a3p
review-notes.txt
```

`review-notes.txt` records the setup/open/save decision only. It must not claim
automated grading, learner-world grading, rubric scoring, correctness
assessment, creative assessment, or learner-world semantic evaluation.

## Examples

Example review notes for an accepted setup/open/save evidence run:

```text
scenario: alice-desktop-instructor-student-setup
runDirectory: qa/outside-in/alice-desktop/evidence/manual-runs/alice-desktop-instructor-student-setup/<timestamp>
reviewedEvidence:
  - manual-evidence-checklist.txt
  - instructor-launch.log
  - starter-project-open.png
  - starter-project.a3p
  - student-open.log
  - student-project-open.png
  - student-copy.a3p
observedResult: The starter project was prepared, opened by the student, and saved as a separate student copy.
assessmentBoundary: setup/open/save evidence review only; learner-world grading, rubric scoring, correctness assessment, and creative assessment remain manual/unsupported until define-reviewed-assessment-contract is resolved.
deviations: None.
decision: accept
```

Example blocker notes when learner-world state extraction is requested:

```text
scenario: alice-desktop-instructor-student-setup
requestedAssessment: rubric scoring from learner-world state
blocker: define-reviewed-assessment-contract
blockerDetail: Learner-world state extraction for grading or creative assessment is blocked until a reviewed assessment contract and evidence mapping exist.
decision: reject assessment claim; keep setup/open/save evidence only.
```

## Extension rules

Future work may extend the boundary only after executable evidence proves the new
claim. Before any learner-world grading, rubric scoring, correctness assessment,
or creative assessment is claimed, the change must add a reviewed assessment
contract, define rubric inputs, define state-extraction safety rules, update
generated evidence artifacts, and add behavior tests that reject unsupported
positive assessment claims.
