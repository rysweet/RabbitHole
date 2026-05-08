# Learner-world assessment boundary

This reference describes the current learner-world assessment boundary and the
planned implementation that will make that boundary explicit in generated
evidence. The boundary keeps instructor/student setup/open/save evidence review
separate from assessment features that the runner does not implement.

## Contents

- [Current behavior](#current-behavior)
- [Current generated run artifacts](#current-generated-run-artifacts)
- [Current boundary artifact fields](#current-boundary-artifact-fields)
- [Planned boundary implementation](#planned-boundary-implementation)
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
learner-work grading, rubric scoring, correctness assessment, correctness
scoring, creativity assessment, or creative assessment.

Supported evidence is limited to:

| Evidence area | What the reviewer checks |
| --- | --- |
| Instructor setup | Alice can open or prepare the starter project for learner use. |
| Student open | Alice can open the learner copy from the expected location. |
| Student save | Alice can save a separate student copy for later review. |

The runner treats any unavailable learner-world state extraction for assessment
as blocked. It does not synthesize rubric inputs, infer learner intent, score
world correctness, or assess creativity from setup/open/save artifacts.

## Current generated run artifacts

Running the selected scenario creates:

```text
<evidence-dir>/alice-desktop-instructor-student-setup/<timestamp>/
  environment.txt
  status.txt
  manual-evidence-checklist.txt
```

`status.txt` records `automationMode=manual-evidence-required` and points to the
generated checklist. It does not record an assessment pass or score.

`manual-evidence-checklist.txt` currently includes the standard manual sections:
preconditions, user actions, expected outcomes, required evidence, fallback
notes, and completion status. It does not yet include a generated `Assessment
boundary` section.

Until the planned checklist extension lands, reviewers should pair the generated
checklist with the checked-in boundary record:

```text
qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json
```

## Current boundary artifact fields

The boundary record is a declarative contract, not runner configuration and not
an assessment engine. It is stored at:

```text
qa/outside-in/alice-desktop/contracts/learner-world-assessment-boundary.json
```

The current record exposes these fields:

| Field | Meaning |
| --- | --- |
| `id` | Stable identifier: `learner-world-assessment-boundary`. |
| `scope` | Instructor/student learner-world setup/open/save evidence. |
| `currentCapability` | Collects manual evidence for setup, open, and save workflow review. |
| `nonCapabilities` | Unsupported learner-work grading, rubric scoring, correctness assessment, and creativity assessment claims. |
| `nextBlocker.id` | Blocking requirement for future assessment work: `define-reviewed-assessment-contract`. |
| `nextBlocker.description` | User-facing explanation that a reviewed assessment contract and evidence mapping are required before grading or creative assessment work can be claimed. |

Consumers may display these fields in documentation or review tooling. They must
not treat this file as executable assessment behavior without a separate
reviewed implementation change.

## Planned boundary implementation

The feature we will build expands the boundary from a passive declarative record
into explicit generated evidence while preserving the same conservative claim
limit.

### Planned generated checklist section

`manual-evidence-checklist.txt` for `alice-desktop-instructor-student-setup`
will include this generated section:

```text
Assessment boundary
- RabbitHole learner-world QA currently supports setup/open/save evidence review only.
- Manual evidence required.
- Setup, open, save evidence review only.
- No automated grading.
- No rubric scoring.
- No correctness scoring.
- No creative assessment.
- Learner-world state extraction for grading or creative assessment is blocked
  until define-reviewed-assessment-contract is resolved.
```

### Planned boundary record fields

The planned record will add fields that make the selected scenario, automation
mode, supported evidence, limits, and blocker easier for docs and review tools to
read:

| Field | Meaning |
| --- | --- |
| `id` | Stable identifier: `learner-world-assessment-boundary`. |
| `selectedScenario` | Scenario covered by the boundary: `alice-desktop-instructor-student-setup`. |
| `automationMode` | Always `manual-evidence-required` for this boundary. |
| `scope` | Instructor/student learner-world setup/open/save evidence. |
| `currentCapability` | Collects manual evidence for setup, open, and save workflow review. |
| `supportedEvidence` | Explicit list of supported evidence areas: instructor setup, student open, and student save. |
| `assessmentLimits` | Explicit list of unsupported capabilities, including no automated grading, no rubric scoring, no correctness scoring, and no creative assessment. |
| `nonCapabilities` | Compatibility field naming unsupported learner-work grading, rubric scoring, correctness assessment, and creativity assessment claims. |
| `blocker.id` | Blocking requirement for future assessment work: `define-reviewed-assessment-contract`. |
| `blocker.description` | User-facing explanation that a reviewed assessment contract and evidence mapping are required before grading or creative assessment work can be claimed. |
| `nextBlocker` | Compatibility alias for the same blocker when older readers expect that field. |

The same implementation change must update the JSON record, generated checklist,
schema or validator expectations, and tests together so the docs do not outrun
the runner again.

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
# Planned schema extension; not accepted by the current validator.
assessmentBoundary:
  mode: manual-evidence-required
  supportedEvidence:
    - instructor starter setup
    - student project open
    - student project save
  limits:
    - no automated grading
    - no rubric scoring
    - no correctness scoring
    - no creative assessment
  blocker: define-reviewed-assessment-contract
```

If this scenario metadata field is added, the JSON Schema, dependency-free
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
automated grading, rubric scoring, correctness scoring, creative assessment, or
learner-world semantic evaluation.

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
assessmentBoundary: setup/open/save evidence review only; no automated grading, no rubric scoring, no correctness scoring, no creative assessment.
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
claim. Before any learner-work grading, rubric scoring, correctness assessment,
correctness scoring, creativity assessment, or creative assessment is claimed,
the change must add a reviewed assessment contract, define rubric inputs, define
state-extraction safety rules, update generated evidence artifacts, and add
behavior tests that reject unsupported positive assessment claims.
