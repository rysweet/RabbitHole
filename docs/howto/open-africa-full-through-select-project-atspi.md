# Open Africa Full through Select Project with AT-SPI

Use this focused Select Project lane to prove progress toward selecting or opening the committed `Africa Full` starter through Alice's Select Project dialog. The lane accepts exactly one target:

```yaml
targetStarter:
  displayName: Africa Full
  repositoryPath: core/resources/src/application/resources/starter-projects/AfricaFull.a3p
```

The evidence is intentionally narrow. It proves starter identification, target-specific selection/opening progress, or the exact blocker that stopped progress. It does not prove visible rendering correctness, full lesson execution, grading, Save behavior, full UI automation, or world interaction.

## Contents

- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [PR #437 recovery note](#pr-437-recovery-note)
- [Target evidence vocabulary](#target-evidence-vocabulary)
- [Validate the contract](#validate-the-contract)
- [What the runner validates before launch](#what-the-runner-validates-before-launch)
- [Required probe order](#required-probe-order)
- [Review opened evidence](#review-opened-evidence)
- [Review blocked evidence](#review-blocked-evidence)
- [Runner summary fields](#runner-summary-fields)
- [Post-open probe boundary](#post-open-probe-boundary)
- [Publish the result](#publish-the-result)
- [PR #437 publication boundary](#pr-437-publication-boundary)

## Prerequisites

Run commands from the repository root.

```bash
export NODE_OPTIONS=--max-old-space-size=32768

java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
test -f core/resources/src/application/resources/starter-projects/AfricaFull.a3p
```

The live AT-SPI path requires the same accessibility stack as the existing Select Project probes:

```bash
sudo apt-get install -y python3-pyatspi
test -f /usr/share/java/java-atk-wrapper.jar
```

Use isolated first-run license state for controlled QA launches:

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

Generated evidence under `qa/outside-in/alice-desktop/evidence/` is transient and ignored by Git. Promote only intentionally reviewed evidence or documentation; do not treat ignored run output as durable by default.

## Configuration

Use the checked-in runner and probe contracts directly. Do not merge manually. Do not use timeout wrappers. The runner owns readiness waits and writes an explicit blocker when the desktop or accessibility environment is not usable.

| Setting | Use |
| --- | --- |
| `NODE_OPTIONS=--max-old-space-size=32768` | Required shell preference for focused validation and QA commands in this lane. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` | Opts into isolated test license acceptance for controlled Alice launches. |
| `--evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full` | Keeps generated Select Project evidence in a predictable ignored path for review. |
| `GH_EXTERNAL_ATTEMPTS` | Optional retry count for GitHub metadata/fetch calls in the PR recovery contract. |
| `GH_EXTERNAL_RETRY_SECONDS` | Optional delay between GitHub metadata/fetch retries. |

The scenario itself supplies the only accepted target starter metadata. Do not override `Africa Full` from the shell or reuse artifacts from a different starter as proof for this lane.

## PR #437 recovery note

Use this how-to for the focused Select Project evidence run. If the run is part of PR #437 recovery or finalization, follow the [PR #437 recovery contract](../reference/select-project-africa-full-atspi-evidence.md#pr-437-recovery-contract) after the local prerequisites above are satisfied. If PR #437 is `DIRTY`, use the [Recover PR #437 after DIRTY merge state](./recover-pr437-dirty-select-project.md) flow; dirty repair is an edit-and-push path and cannot use no-op mode. These contracts require exact PR head SHA verification, current GitHub merge/check/review metadata, and a finalization report that keeps merge state, owner-free review metadata, evidence, and blockers separate.

GitHub PR metadata is an external dependency for that recovery path, not Alice runtime proof. If `gh pr view`, `gh pr checkout`, or `git fetch` cannot complete because of GitHub CLI authentication, network connectivity, or rate limiting, report `environment dependency` and do not replace the missing PR metadata with cached or hand-entered values.

Passing recovery gates makes the PR evidence-ready only. It does not authorize an agent to approve, merge, close, rebase, push unrelated changes, or otherwise mutate PR #437.

The PR #437 recovery report can use a workflow-accepted no-op justification only when a current-head run proves that no repository change is needed and no repository files are modified. At the verified head, `mergeStateStatus=CLEAN` plus required checks with `SUCCESS` conclusions is merge-ready evidence and does not require a disposable local merge check. Run the conditional local merge check only when GitHub reports `DIRTY` or mergeability metadata is unavailable/ambiguous after the head and base are verified. Empty `reviewDecision` is owner-free/unset metadata; report it honestly and do not describe it as approval. The no-op justification must cite the exact PR metadata command, local head SHA, worktree cleanliness, merge-ready GitHub evidence or conditional merge-check evidence, focused validation commands, and reviewed artifacts or the explicit reason no live artifact was required. No live artifact is acceptable only for a no-op documentation recovery that makes no live Select Project success claim and explicitly says the run verified existing documentation/contracts instead of producing a new AT-SPI evidence directory. If the recovery edits docs, tests, contracts, or other repository files, use the focused pushed-change summary path from the reference contract instead of `No-op justification:`.

## Target evidence vocabulary

The artifact contract uses the committed target-scoped field names:

| Field | Meaning |
| --- | --- |
| `targetStarter` | Validated Africa Full starter metadata. |
| `targetStarterObserved` | Safe AT-SPI observation for the Africa Full starter node. |
| `targetSelectionObserved` | Target-specific Africa Full selection was observed. |
| `openAttempted` | OK/Open was attempted after target-specific selection evidence. |
| `openedStarter` | Starter metadata recorded only after the guarded Select Project dismissal. |

## Validate the contract

Run the focused documentation-backed checks before reviewing a live run.

### Contract validation

```bash
export NODE_OPTIONS=--max-old-space-size=32768

qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-scenario-validation.sh
bash qa/outside-in/alice-desktop/tests/test-select-project-completion-contract.sh
bash qa/outside-in/alice-desktop/tests/test-select-project-proof.sh
bash qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh
bash qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh
python3 -m unittest tests/test_pr437_select_project_recovery_contract.py
python3 -m unittest tests/test_pr437_noop_recovery_report_contract.py
```

These checks should cover the scenario target metadata, validator allowlists, runner promotion fields, target-specific tab-click evidence, blocked evidence shape, post-open gating, PR #437 recovery/finalization wording, and no-overclaim wording. They are not rendering, grading, lesson, Save, or full UI automation tests.

### Live Select Project evidence run

Run the scenario only when the current task requires fresh AT-SPI evidence:

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

### Artifact review

Review `status.txt`, `tab-click-observation.json`, `post-project-open-observation.json`, `x-window-inventory.json`, and `select-project-window.json` from the same run directory when a live evidence run was produced. Contract validation alone does not prove live opening artifacts.

## What the runner validates before launch

`validate-scenarios.sh` rejects the Select Project tab-click scenario unless:

1. `workflow` is `select-project-tab-click-smoke`.
2. `targetStarter.displayName` is exactly `Africa Full`.
3. `targetStarter.repositoryPath` is exactly `core/resources/src/application/resources/starter-projects/AfricaFull.a3p`.
4. The repository path is relative, normalized, inside the repository, and not a traversal path.
5. The automation argv is one of the approved Alice desktop launch argv lists.

`run-scenario.sh` passes the validated target metadata to `tab-click-probe.py` with quoted arguments or safe environment values. The runner must not use `eval`, executable interpolation, or scenario YAML as shell code.

## Required probe order

The target `tab-click-probe.py` behavior follows the existing Select Project QA/probe flow and stops rather than overclaiming:

1. Wait for the real Select Project dialog and record its Java/window context.
2. Identify and activate the `Starters` tab.
3. Record `startersTabSafety` showing that the Starters tab was activated before target search and that the active search scope is `active-starters-tab`.
4. Search only that active context for `Africa Full`.
5. Record safe target observation data: role, state names, available actions, tree path, parent selection-interface availability, and bounded active-list context.
6. Set `targetSelectionObserved=true` only when target-specific evidence shows `Africa Full` was selected.
7. Set `openAttempted=true` only after target-specific selection evidence exists.
8. Set `projectOpenObserved=true` only when the Select Project frame is no longer present after the guarded open attempt.

A generic Select Project dismissal, unrelated main-window state, or generic starter candidate must not become Africa Full proof.

## Review opened evidence

Open `status.txt`, then review `tab-click-observation.json` in the same run directory. An opened result uses this narrow shape:

```json
{
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "evidenceStatus": "opened",
  "startersTabSafety": {
    "tabName": "Starters",
    "activationAttempted": true,
    "activatedBeforeTargetSearch": true,
    "activationDetail": "Starters tab activated before target search",
    "targetSearchScope": "active-starters-tab"
  },
  "targetStarterObserved": {
    "name": "Africa Full",
    "role": "panel",
    "states": ["enabled", "visible", "showing"],
    "availableActions": ["click"],
    "treePath": [0, 3, 1, 0],
    "parentSelectionAvailable": false
  },
  "targetSelectionObserved": true,
  "targetSelectionAttempt": {
    "actionAttempts": [
      {
        "action": "click",
        "success": true,
        "detail": "click action succeeded"
      }
    ],
    "parentSelectionAttempted": false,
    "parentSelectionSuccess": false,
    "selected": true,
    "detail": "target starter selected via click action"
  },
  "openAttempted": true,
  "projectOpenObserved": true,
  "projectOpenDetail": "Select Project frame no longer present after guarded Africa Full open attempt"
}
```

Treat `evidenceStatus=opened` as proof only when the artifact echoes the validated `targetStarter`, records safe `startersTabSafety`, records `targetSelectionObserved=true`, records `openAttempted=true`, and records `projectOpenObserved=true` from the same guarded run. It means only that AT-SPI evidence supports selecting/opening the committed Africa Full starter through Select Project.

The same run must also include `x-window-inventory.json` and `select-project-window.json` for the Alice Java/window context. These artifacts provide PID/window context only; they are not project rendering or lesson execution proof.

## Review blocked evidence

If the probe cannot complete target-specific selection/opening, it records the exact next blocker instead of success:

```json
{
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "evidenceStatus": "blocked",
  "startersTabSafety": {
    "tabName": "Starters",
    "activationAttempted": true,
    "activatedBeforeTargetSearch": true,
    "activationDetail": "Starters tab activated before target search",
    "targetSearchScope": "active-starters-tab"
  },
  "targetStarterObserved": {
    "name": "Africa Full",
    "role": "panel",
    "states": ["enabled", "visible", "showing"],
    "availableActions": [],
    "treePath": [0, 3, 1, 0],
    "parentSelectionAvailable": false
  },
  "targetSelectionObserved": false,
  "openAttempted": false,
  "projectOpenObserved": false,
  "blocker": "target-starter-selection-unavailable",
  "blockerDetail": "Africa Full is visible in the active Starters context but exposes no click/activate action and no usable parent selection interface.",
  "nextBlocker": {
    "observedAtspiState": "Africa Full panel is visible in the active Starters context but exposes no click/activate action and no usable parent selection interface.",
    "actionAttempted": "Activated Starters tab, located Africa Full, inspected actions and parent selection support.",
    "expectedNextAction": "Provide a supported AT-SPI selection/click path for the Africa Full starter before clicking OK/Open.",
    "reasonProgressStopped": "Opening without target-specific selection evidence would be a generic Select Project dismissal, not Africa Full proof."
  }
}
```

A blocked result is acceptable evidence when it names exactly one next blocker and includes the observed AT-SPI state, action attempted, expected next action, and reason progress stopped. Report that blocker as the result; do not replace it with generic main-window evidence.

## Runner summary fields

When `tab-click-observation.json` echoes the validated target metadata, the runner promotes only narrow Select Project summary fields into `status.txt`:

| Field | Meaning |
| --- | --- |
| `selectProjectTargetDisplayName` | Validated target display name: `Africa Full`. |
| `selectProjectTargetRepositoryPath` | Validated committed starter path. |
| `selectProjectEvidenceStatus` | `opened`, `selected`, `blocked`, or `failed`. |
| `selectProjectStartersTabSafety` | Compact status for the active Starters search scope. |
| `selectProjectTargetSelectionObserved` | `true` only when target-specific Africa Full selection evidence exists. |
| `selectProjectOpenAttempted` | `true` only when OK/Open was attempted after target-specific selection evidence. |
| `selectProjectProjectOpenObserved` | `true` only when the Select Project frame disappeared after that guarded attempt. |
| `selectProjectNextBlocker` | Structured blocker detail for non-opened results. |

The runner may also publish `selectProjectOpenedStarterDisplayName` and `selectProjectOpenedStarterRepositoryPath` as diagnostics. Opened proof still requires the target selection, open-attempt, and project-open flags above to be present and true. If the probe output omits or drifts from the validated `targetStarter`, the runner must not promote success-shaped Select Project fields.

## Post-open probe boundary

The `post-project-open-probe.py` gate may report narrow post-open progress only after the prior tab-click artifact proves the Africa Full path with matching target metadata, `targetSelectionObserved=true`, `openAttempted=true`, and `projectOpenObserved=true`.

The probe preserves legacy diagnostic fields, but the gate is the canonical target-specific contract above.

If that gate is missing or inconsistent, `post-project-open-observation.json` records a blocked result. If the gate passes, the post-open artifact may report accessible main-window presence and PID/window continuity. It must not claim visible rendering correctness, grading, full lesson execution, Save completion, or full UI automation.

## Publish the result

Publish only one of these outcomes:

| Outcome | Publish |
| --- | --- |
| Opened | Exact `Africa Full` `targetStarter` metadata, `evidenceStatus=opened`, `startersTabSafety`, `targetSelectionObserved=true`, `openAttempted=true`, `projectOpenObserved=true`, and Alice Java/window context. |
| Blocked | One blocker code/detail, Alice Java/window context, Select Project window context, Starters-tab safety, target observation state, target selection state, open-attempt state, project-open state, and one structured `nextBlocker`. |

Do not publish downstream claims from this lane. It does not prove visible rendering correctness, full UI automation, Save completion, grading, creative assessment, full lesson execution, model export, unrelated launcher behavior, archive fixture behavior, procedure/edit behavior, unrelated decoder behavior, or coverage.

## PR #437 publication boundary

If this run supports PR #437 recovery or finalization, use the [verified evidence report shape](../reference/select-project-africa-full-atspi-evidence.md#verified-evidence-report-shape) from the reference contract. The report must include these headings:

```markdown
## Verified evidence

- PR state: open state, `headRefName`, `headRefOid`, `baseRefName`, `isDraft`, `mergeStateStatus`, owner-free/unset review decision when empty, and check summary from a successful `gh pr view`.
- Local PR head: `git rev-parse HEAD` value and confirmation that it matches `headRefOid`.
- Merge-ready evidence: `mergeStateStatus=CLEAN`, required checks completed with `SUCCESS`, and branch refs pointing at the verified head; if GitHub reports `DIRTY` or mergeability metadata is unavailable/ambiguous after head/base verification, include the local merge check command, conflict files if any, and final merge-check result.
- Focused Select Project validation: commands run and exit status.
- Artifacts: exact run directory and files reviewed, such as `status.txt`, `tab-click-observation.json`, `post-project-open-observation.json`, `x-window-inventory.json`, and `select-project-window.json`.
- Readiness evidence: current branch/head guard, PR metadata command result, focused validation status, worktree cleanliness, and merge-ready GitHub evidence or conditional merge-check evidence.
- Review evidence: reviewed docs, contracts, and artifacts, or the exact no-live-artifact exception used for a documentation-only recovery.
- Finalization evidence: final blocker state and confirmation that the result is evidence-ready only, not a PR mutation.
- Files modified: repository paths changed by the recovery; use `None` only when the no-op justification below is valid and `git status --short --branch` shows no repository changes.

## Unverified assumptions

- Expected behavior or code-path reasoning not executed in this recovery run.
- Use `None recorded` only when every claim is backed by a command or artifact from this run.

## Current blocker

- One of `merge dirtiness`, `missing evidence`, `failing validation`, or `environment dependency`.
- Use `Current blocker: None` only when every PR finalization gate passes.
```

For PR #437 at the verified current head, use the canonical [`No-op justification:` shape](../reference/select-project-africa-full-atspi-evidence.md#workflow-accepted-no-op-justification) only for a documentation-only recovery that makes no live Select Project success claim and leaves repository files unchanged. If the recovery edits and pushes docs/contracts, use the [`EDIT_AND_PUSH` focused pushed-change summary](../reference/select-project-africa-full-atspi-evidence.md#focused-pushed-change-summary).

If GitHub metadata is unavailable, report `Current blocker: environment dependency`. If branch/head drift is observed, the report must not publish `No-op justification:`; it must first re-establish the exact PR head or stop with the blocker.

Treat PR #437 as merge-ready, not approved, only when the local `HEAD` equals the verified current head, `mergeStateStatus=CLEAN`, required checks are `SUCCESS`, focused validation passes, review metadata is reported as owner-free/unset when empty, the required evidence artifacts exist for this run or the report gives the no-op documentation exception above, and the report does not claim full UI automation, rendering, Save, grading, lesson completion, or other downstream behavior. Passing gates are evidence-ready only; they do not authorize an agent to approve, merge, close, rebase, push unrelated changes, or otherwise mutate PR #437.
