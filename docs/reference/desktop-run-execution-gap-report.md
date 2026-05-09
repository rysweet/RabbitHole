# Desktop Run execution gap report

This reference documents the implemented fail-closed report emitted by the
desktop Run evidence hook:

```text
desktop-run-execution-gap-report.json
```

The report is written after the existing bounded Run-window evidence artifacts
are produced. It states what is executable today and names the exact blocker
that prevents any full world-execution claim. It is a reporting and validation
artifact only; it does not add runtime probing, UI automation, rendering
inference, grading inference, Save validation, or world-advance proof.

## Contents

- [Scope](#scope)
- [Usage](#usage)
- [Configuration](#configuration)
- [Report API](#report-api)
- [Fail-closed behavior](#fail-closed-behavior)
- [Examples](#examples)
- [Review rules](#review-rules)
- [Validation commands](#validation-commands)
- [Troubleshooting](#troubleshooting)

## Scope

The report belongs to the opt-in desktop Run evidence flow implemented in:

```text
core/ide/src/main/java/org/alice/tools/EatmeDesktopRunExecutionEvidence.java
core/ide/src/test/java/org/alice/tools/EatmeDesktopRunExecutionEvidenceTest.java
```

When `recordRenderTargetAttached(...)` reaches the existing Run-window evidence
path, it writes the established artifacts first:

```text
desktop-run-render-affordance.json
desktop-run-pixel-boundary.json
desktop-run-pixel-observation.json
desktop-first-lesson-next-action.json
desktop-save-menu-action-target.json
desktop-run-status-summary.json
```

After those artifacts are written and validated as present, the flow writes:

```text
desktop-run-execution-gap-report.json
```

The report summarizes those bounded artifacts as the current executable
Run-window evidence. It then records the blocker to a stronger claim: missing
deterministic proof that the Alice world advances through full runtime
execution, not merely that Run-window evidence artifacts exist.

The report may reference `desktop-run-execution.json` and
`desktop-run-runtime.log` when VM-listener execution evidence is enabled in the
same evidence directory. Those files are still bounded listener artifacts. They
do not by themselves prove complete world advancement, visible rendering
correctness, grading, Save completion, or complete UI automation.

## Usage

Enable desktop Run evidence with the dedicated JVM system property and run a
focused Run-window path:

```bash
java \
  -Dorg.alice.eatme.desktopRunExecutionEvidenceDir=target/desktop-run-evidence \
  ...
```

The report appears in the selected evidence directory after the existing
Run-window artifacts:

```text
target/desktop-run-evidence/
  desktop-run-render-affordance.json
  desktop-run-pixel-boundary.json
  desktop-run-pixel-observation.json
  desktop-first-lesson-next-action.json
  desktop-save-menu-action-target.json
  desktop-run-status-summary.json
  desktop-run-execution-gap-report.json
```

Review the gap report after reviewing `desktop-run-status-summary.json`:

```bash
python3 -m json.tool \
  target/desktop-run-evidence/desktop-run-execution-gap-report.json
```

Use the report to answer only these questions:

| Question | Source |
| --- | --- |
| What Run-window evidence is executable today? | `executableToday.evidenceArtifacts` |
| What prevents a stronger world-execution claim? | `blockerToFullWorldExecution.reason` |
| Which claims are explicitly unsupported? | `doesNotClaim` |
| Did the report validate the required artifact list and blocker text? | `failClosedRequirements` and focused tests |

Do not use the report as proof that a world completed execution, pixels rendered
correctly, learner work was graded, Save finished, or the full Alice UI was
automated.

## Configuration

| Property | Purpose |
| --- | --- |
| `org.alice.eatme.desktopRunExecutionEvidenceDir` | Enables the desktop Run evidence flow and selects the output directory. When unset or blank, the desktop Run evidence flow remains disabled. |
| `org.alice.eatme.runWindowEvidenceDir` | Legacy Run-window evidence directory. The desktop Run evidence flow uses it only when the dedicated desktop property is unset or blank. |

The report does not introduce new environment variables, network calls,
credentials, authentication behavior, or persistent user preferences.

For QA wrappers that invoke Node-based orchestration, use the saved memory
preference:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Report API

The artifact name is stable:

```text
desktop-run-execution-gap-report.json
```

The schema token is:

```text
eatme.alice-desktop-run-execution-gap-report/v1
```

Field order is not part of the contract. The report has this shape:

```json
{
  "schema_version": "eatme.alice-desktop-run-execution-gap-report/v1",
  "report_kind": "desktop_run_execution_gap",
  "status": "blocked",
  "source": "desktop_run_render_target_attachment",
  "emitted_after": "desktop-run-status-summary.json",
  "executableToday": {
    "summary": "Existing tooling produces bounded Run-window evidence artifacts.",
    "evidenceArtifacts": [
      {
        "artifact": "desktop-run-render-affordance.json",
        "evidence": "Run view attachment signal",
        "claimLimit": "Run view attachment evidence only"
      },
      {
        "artifact": "desktop-run-pixel-boundary.json",
        "evidence": "Pixel validation boundary",
        "claimLimit": "pixel validation is not part of Run view attachment evidence"
      },
      {
        "artifact": "desktop-run-pixel-observation.json",
        "evidence": "desktop pixel sample status or exact blocker",
        "claimLimit": "pixel sampling status is not visible rendering correctness"
      },
      {
        "artifact": "desktop-first-lesson-next-action.json",
        "evidence": "next desktop action no-go contract",
        "claimLimit": "desktop action evidence remains missing"
      },
      {
        "artifact": "desktop-save-menu-action-target.json",
        "evidence": "Save menu target no-go contract",
        "claimLimit": "Save menu readiness or invocation is not observed here"
      },
      {
        "artifact": "desktop-run-status-summary.json",
        "evidence": "summary of bounded Run-window artifact statuses",
        "claimLimit": "summary of partial evidence, not a completion proof"
      }
    ]
  },
  "blockerToFullWorldExecution": {
    "reason": "Missing deterministic proof that the Alice world actually advances through full runtime execution, not merely that Run-window evidence artifacts exist.",
    "missingProof": "deterministic_world_advance_through_full_runtime_execution",
    "requiredNextEvidence": [
      "stable runtime advancement oracle tied to the launched world",
      "deterministic evidence that expected world state changes occurred during Run",
      "reviewed criteria that distinguish artifact presence from actual world advancement"
    ]
  },
  "failClosedRequirements": [
    "required Run-window evidence artifact names must be present",
    "executableToday evidence list must be non-empty",
    "blockerToFullWorldExecution.reason must be non-empty",
    "doesNotClaim must include every prohibited claim category"
  ],
  "doesNotClaim": [
    "full world execution",
    "visible rendering correctness",
    "grading",
    "Save completion",
    "full UI automation"
  ]
}
```

### Required fields

| Field | Required value |
| --- | --- |
| `schema_version` | `eatme.alice-desktop-run-execution-gap-report/v1` |
| `report_kind` | `desktop_run_execution_gap` |
| `status` | `blocked` |
| `source` | `desktop_run_render_target_attachment` |
| `emitted_after` | `desktop-run-status-summary.json` |
| `executableToday.summary` | Conservative statement that existing tooling produces bounded Run-window evidence artifacts. |
| `executableToday.evidenceArtifacts` | Non-empty list containing every required Run-window evidence artifact name. |
| `blockerToFullWorldExecution.reason` | Non-empty text naming the missing deterministic proof that the world advances through full runtime execution rather than artifact presence alone. |
| `blockerToFullWorldExecution.missingProof` | Stable token `deterministic_world_advance_through_full_runtime_execution`. |
| `failClosedRequirements` | Non-empty list naming the validation rules. |
| `doesNotClaim` | Contains `full world execution`, `visible rendering correctness`, `grading`, `Save completion`, and `full UI automation`. |

### Required Run-window artifact references

The report must list these artifacts in `executableToday.evidenceArtifacts`:

```text
desktop-run-render-affordance.json
desktop-run-pixel-boundary.json
desktop-run-pixel-observation.json
desktop-first-lesson-next-action.json
desktop-save-menu-action-target.json
desktop-run-status-summary.json
```

The report may also mention these VM-listener artifacts when they are enabled in
the same evidence directory:

```text
desktop-run-execution.json
desktop-run-runtime.log
```

Optional VM-listener references do not change the blocker. They remain bounded
execution-listener evidence until a separate deterministic world-advance proof
exists.

## Fail-closed behavior

The writer validates the report before the atomic JSON write. Validation fails
closed when any required condition is missing:

| Missing or invalid condition | Required behavior |
| --- | --- |
| Required evidence artifact list is empty | Throw validation error; do not accept a success-shaped report. |
| Any required Run-window artifact name is omitted | Throw validation error; do not write or accept the report. |
| `blockerToFullWorldExecution.reason` is blank | Throw validation error; do not write or accept the report. |
| `doesNotClaim` omits a prohibited claim category | Throw validation error; do not write or accept the report. |
| Artifact path is absolute, nested, parent-relative, or escapes the evidence directory | Reject through the shared artifact path guard. |
| Atomic write fails or the final artifact is empty | Surface the write failure through the existing logging/error path; do not treat the report as present. |

The fail-closed checks prevent the report from becoming a misleading pass
artifact. The expected steady-state status is `blocked` because the report names
an execution proof gap.

## Examples

### Review bounded Run-window evidence

```bash
run_dir=target/desktop-run-evidence

python3 -m json.tool "$run_dir/desktop-run-status-summary.json"
python3 -m json.tool "$run_dir/desktop-run-execution-gap-report.json"
```

Accepted review wording:

```text
The run produced bounded Run-window evidence and a desktop Run execution gap
report. The report identifies the existing executable artifacts and keeps full
world execution blocked until deterministic world-advance proof exists.
```

Rejected review wording:

```text
The run proves the world executed correctly and rendered correctly.
```

### Use the report in QA notes

```text
reviewedEvidence:
  - desktop-run-render-affordance.json
  - desktop-run-pixel-observation.json
  - desktop-run-status-summary.json
  - desktop-run-execution-gap-report.json
decision: accept bounded Run-window evidence only
blocker: deterministic world-advance proof is still missing
unsupportedClaims:
  - full world execution
  - visible rendering correctness
  - grading
  - Save completion
  - full UI automation
```

### Validate the focused implementation contract

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest \
  test
```

## Review rules

1. Treat `desktop-run-execution-gap-report.json` as a bounded evidence report,
   not as a pass artifact for world behavior.
2. Require `desktop-run-status-summary.json` to exist before the gap report.
3. Require every Run-window artifact listed in this reference to appear in
   `executableToday.evidenceArtifacts`.
4. Require the blocker reason to state that deterministic world-advance proof is
   missing.
5. Require `doesNotClaim` to include full world execution, visible rendering
   correctness, grading, Save completion, and full UI automation.
6. Keep PR and review text conservative: "bounded Run-window evidence" and
   "execution gap report" are acceptable; completion, correctness, grading, Save,
   or full-automation claims are not.

## Validation commands

Run the focused `core/ide` validation from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.tools.EatmeDesktopRunExecutionEvidenceTest \
  test
```

If QA scenario or Gadugi wiring is changed to enumerate the new artifact, also
run the touched QA contract:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  qa/outside-in/alice-desktop/tests/run-tests.sh
```

For broad Maven validation from a fresh checkout or worktree, initialize the
Tweedle grammar submodule first:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Troubleshooting

| Symptom | Meaning | Next check |
| --- | --- | --- |
| `desktop-run-execution-gap-report.json` is missing | The desktop Run evidence hook did not reach the post-summary report step, evidence was not enabled, or validation failed closed. | Check the evidence directory property, existing Run-window artifacts, and test logs. |
| Report exists but omits a required artifact name | The report is invalid. | Run `EatmeDesktopRunExecutionEvidenceTest`; fix the required artifact list before review. |
| Report has an empty blocker reason | The report is invalid. | Restore the deterministic world-advance blocker text. |
| Report omits a prohibited claim category | The report is invalid. | Restore all required `doesNotClaim` entries. |
| Review text says the world executed fully | The review overclaims the evidence. | Replace with bounded Run-window evidence wording and cite the blocker. |
