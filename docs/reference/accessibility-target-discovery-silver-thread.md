# Accessibility Target Discovery Silver-Thread Contract

This reference documents the repository-owned accessibility target discovery
silver-thread contract:

```text
qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh
```

The contract ties together existing launch, run/runtime, and Select Project
evidence paths. It proves that each lane exposes target discovery signals,
structured blockers, and bounded review artifacts. It does not introduce a new
scenario workflow and does not claim full UI automation, visual correctness,
rendering correctness, world execution correctness, full world execution, or
general accessibility compliance.

## Contents

- [Usage](#usage)
- [Evidence lanes](#evidence-lanes)
- [Readiness evidence record](#readiness-evidence-record)
- [Current-head readiness gate](#current-head-readiness-gate)
- [Artifact API](#artifact-api)
- [Configuration](#configuration)
- [Examples](#examples)
- [Tutorial: review the silver thread](#tutorial-review-the-silver-thread)
- [Merge-ready decision model](#merge-ready-decision-model)
- [Claim boundaries](#claim-boundaries)
- [Extension rules](#extension-rules)

## Usage

Run the focused contract from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh
```

Run the Alice desktop QA contract suite when reviewing the lane in context:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/tests/run-tests.sh
```

Run the PR419 current-head readiness gate after the final commit is pushed and
the PR body has been updated with that final head SHA:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-pr419-current-head-readiness-gate-contract.sh
```

Use the focused contract when a pull request needs to prove that the checked-in
launch, run/runtime, and Select Project evidence paths still expose the
accessibility target discovery surface expected by reviewers. Use the lower-level
scenario runners when a review needs fresh local evidence artifacts.

The focused contract exits `0` only when all required files, scenario metadata,
runner wiring, probe fields, structured blocker fields, and scope wording remain
present. A missing marker or broadened claim is a failure, not a warning.

## Evidence lanes

The contract covers these checked-in evidence paths.

| Lane | Checked input | What the contract proves |
| --- | --- | --- |
| Launch | `qa/outside-in/alice-desktop/scenarios/launch.yaml` | The launch path remains executable through `xvfb-real-alice`, records launch evidence such as root-directory prep, launch logs, window inventory, application-root errors, license-dialog blockers, controlled-display pixel observation, and preserves exact failure logs when launch cannot proceed. |
| Run/manual context | `qa/outside-in/alice-desktop/scenarios/run-debug.yaml` | The run/debug scenario stays manual by default and names scoped run evidence artifacts without pretending to automate live Run UI behavior. |
| Runtime target discovery | `qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml` and `qa/outside-in/alice-desktop/runners/post-open-runtime-display-probe.py` | The post-open runtime/display lane exposes candidate counts, candidate summaries, geometry status, screen extents, target-ready status, and precise fail-closed blockers for missing runtime display targets. |
| Select Project target discovery | `qa/outside-in/alice-desktop/scenarios/select-project-tab-click-exec.yaml` and `qa/outside-in/alice-desktop/runners/tab-click-probe.py` | The Select Project lane stays bound to the committed `Africa Full` starter and records target observation, selection, open-attempt, opened-starter, project-open, and structured next-blocker evidence. |

The launch and run/manual context lanes keep the silver thread honest: launch
must distinguish ready launch evidence from splash-only, crashed, blocked, or
manual fallback states; run/debug must remain scoped manual evidence unless a
separate automation contract exists. The runtime and Select Project lanes provide
the target-specific accessibility discovery records.

## Readiness evidence record

PR readiness evidence for this lane lives outside generated QA artifacts:

```text
.copilot-evidence/default-workflow-attempt.log
```

The record is a bounded review handoff, not product behavior, not generated
desktop evidence, and not live-head proof. It records a
point-in-time PR419 readiness handoff with the PR, branch, base, checked branch
HEAD SHA, `origin/develop` SHA, merge-base, commands, outcomes, and claim
boundaries for the accessibility target discovery lane under review. Do not
treat it as live-head readiness evidence for later commits.

Every readiness record for this lane must include:

| Field | Required content |
| --- | --- |
| PR identity | Existing PR number and URL. Do not create a new PR or issue for this lane. |
| Branch identity | Existing branch name and point-in-time checked `HEAD` SHA. |
| Base reconciliation | `origin/develop` SHA plus merge status. Use merge, not rebase, when the PR branch must be reconciled with `develop`. |
| Validation commands | Focused scenario/schema, docs, probe, sampler, and silver-thread commands run with `NODE_OPTIONS=--max-old-space-size=32768`. |
| Outcomes | Pass or blocked result for each focused command. Blocked results name the missing dependency, target, or next unblocker. |
| Scope | Discovered accessibility/runtime display target readiness and silver-thread review readiness only. |
| Non-claims | Explicit exclusions for full UI automation, visible rendering correctness, full world execution, grading, Save completion, Sims validation, installer/deployment success, and broad accessibility compliance. |

Do not use a readiness record to preserve screenshots, broad environment dumps,
access tokens, unrelated desktop state, generated project data, or private user
state. Generated run artifacts remain local QA output and stay uncommitted.

## Current-head readiness gate

For PR419, `test-pr419-current-head-readiness-gate-contract.sh` is the
current-head merge-ready gate. It is run only after the final branch commit is
pushed and the PR body names that live head SHA plus the focused gate evidence.
It generates transient current-head proof at runtime from `git rev-parse HEAD`
and one cached `gh pr view 419 --json
number,url,headRefName,headRefOid,mergeStateStatus,mergeable,statusCheckRollup,body`
response. Tracked evidence must not store or predict the current PR head SHA.

The gate checks these live inputs:

| Input | Required condition |
| --- | --- |
| Local branch | The current branch is `feat/issue-416-rabbithole-wave7-accessibility-target-lane-follow`. |
| PR identity | `gh pr view 419` returns PR number `419`, URL `https://github.com/rysweet/RabbitHole/pull/419`, and the expected head branch. |
| Head alignment | `git rev-parse HEAD` exactly matches the PR `headRefOid`. |
| PR body evidence | The PR body contains the live head SHA and names `test-pr419-current-head-readiness-gate-contract.sh`. |
| Conflict state | `git diff --name-only --diff-filter=U` returns no unmerged paths, and the docs/QA conflict-marker scan finds no merge markers. |
| Mergeability | The PR metadata reports `mergeStateStatus=CLEAN` and `mergeable=MERGEABLE`. |
| Checks | The PR check rollup is readable, completed, and successful. |
| Evidence log | `.copilot-evidence/default-workflow-attempt.log` records the focused validation commands, current-head gate model, quality-audit cycles, bounded claims, no-timeout-wrapper posture, and no manual PR merge. |
| Documentation | This reference names the transient current-head proof model and both readiness outcomes. |

Final PR readiness requires live GitHub mergeability. The current-head gate
queries `mergeStateStatus` and `mergeable` directly and fails unless both prove
that PR419 is cleanly mergeable.

The gate is a readiness decision contract, not a merge command. It never runs
`gh pr merge`, never merges PR419 into `develop`, never rebases, never
force-pushes, and never treats green checks alone as sufficient.

The shell gate reports readiness through normal assertion output and exit code,
not by printing a machine-readable decision token. Reviewers may interpret a
passing gate, including the live GitHub mergeability assertion, as
`MERGE_READY`. Any failing gate, stale evidence, pending or failed check, dirty
mergeability, or claim broader than the bounded accessibility target discovery scope is
`NOT_MERGE_READY`, and the failing assertion or review note must name the
remaining blocker instead of implying partial readiness.

## Artifact API

The contract validates declared artifact API markers in checked-in scenarios,
probes, runners, and docs; it does not create new runtime evidence artifacts
itself.

### Focused contract output

The focused shell test prints normal assertion output and ends with:

```text
accessibility target discovery silver-thread contract satisfied
```

when the contract is satisfied. Any missing required file, marker, scenario
field, probe field, blocker field, or bounded-scope wording makes the script
exit non-zero.

### Launch evidence fields

Launch evidence is scenario and runner evidence, not a target-specific widget
proof. Required launch evidence names include:

| Artifact or field | Meaning |
| --- | --- |
| `root-directory-prep.json` | The runner recorded the Alice exec root-directory preparation result. |
| `Launch log` | The runner preserved process output for review. |
| `x-window-inventory.json` | The runner recorded X window discovery evidence when available. |
| `application-root-error.json` | Application-root failures are structured instead of being collapsed into a generic launch failure. |
| `license-dialog.json` | License-dialog blockers are explicit launch evidence. |
| `controlled-display-pixel-observation.json` | Controlled-display observation is separated from target-specific rendering claims. |
| `Exit, status, or timeout record` | The runner records how the launch attempt ended. |

### Runtime target discovery fields

`post-open-runtime-display-accessibility-evidence.json` is the runtime/display
decision artifact. The contract requires these target discovery signals:

| Field or value | Meaning |
| --- | --- |
| `runtimeDisplayCandidateCount` | Number of candidate runtime/display accessibility nodes considered by the probe. |
| `runtimeDisplayCandidates` | Safe summaries of candidate nodes inspected during discovery. |
| `geometryStatus` | Geometry readiness state for target-scoped sampling. |
| `screenExtents` | Screen-coordinate geometry associated with a runtime/display candidate. |
| `worldCanvasPixelTarget.status=target-ready` | Exactly one visible/showing target with usable screen extents was identified. |
| `visible-rendering-pixel-target-blocker.json` | Fail-closed artifact when a valid target cannot be identified. |
| `missingTarget=run-window-world-canvas-screen-extents` | Structured blocker name for the missing sampling target. |
| `exactNextUnblocker=reliable-run-window-world-canvas-pixel-sampling-target` | The next required unblocker before target-scoped sampling can be claimed. |
| `visibleRenderingCorrectnessEstablished=false` | Pixel sampling, when present, remains an observation and does not establish rendering correctness. |
| `runtime-display-accessible-candidate-not-found` | Structured blocker when no acceptable runtime/display candidate is exposed. |

An observed runtime target means only that the accessibility target discovery
path found a bounded candidate for review. A blocked runtime target means the
artifact names the exact blocker and next unblocker.

### Select Project target discovery fields

`tab-click-observation.json` is the Select Project decision artifact. The
contract requires these target discovery signals:

| Field or value | Meaning |
| --- | --- |
| `targetStarter.displayName=Africa Full` | The lane targets the committed `Africa Full` starter, not an arbitrary project. |
| `targetStarter.repositoryPath=core/resources/src/application/resources/starter-projects/AfricaFull.a3p` | The target starter path stays stable and reviewable. |
| `targetStarterObserved` | The probe observed a target starter candidate. |
| `targetStarterSelected=true` | The target starter was selected before any open claim is made. |
| `targetStarterOpenAttempted=true` | The probe attempted the target-specific open action. |
| `openedStarter` | Opened starter evidence is tied back to the selected target. |
| `projectOpenObserved=true` | The probe observed post-open state for the target starter. |
| `nextBlocker.observedAtspiState` | Blocked evidence records the observed accessibility state. |
| `nextBlocker.actionAttempted` | Blocked evidence records the action attempted before progress stopped. |
| `nextBlocker.expectedNextAction` | Blocked evidence records the next action that would unblock progress. |
| `nextBlocker.reasonProgressStopped` | Blocked evidence records why the lane stopped. |
| `target role/name/states/actions/tree path` | Target summaries remain inspectable without relying on hidden local state. |
| `parent selection-interface availability` | Selection blockers name whether parent selection support was available. |

Do not claim Africa Full opening unless the target observation, selection,
open-attempt, opened-starter, and project-open fields agree.

## Configuration

The focused contract has no feature flags and takes no arguments.

| Setting | Value |
| --- | --- |
| Working directory | Repository root, or any child directory from which the script can resolve the repository root. |
| `NODE_OPTIONS` | `--max-old-space-size=32768` for consistency with the Alice desktop QA lane. |
| Arguments | None. Any argument is unsupported. |
| Network | Not used. |
| Generated artifacts | Scratch files only; the focused contract removes them before exit. |

Fresh scenario evidence uses the existing Alice desktop QA runner configuration.
Set `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` only when intentionally accepting
Alice license dialogs in an isolated QA run. Set `ALICE_QA_SCENARIO_DIR` only
when validating a custom local scenario catalog before moving it into
`qa/outside-in/alice-desktop/scenarios/`.

The PR419 current-head readiness gate has a separate review configuration:

| Setting | Value |
| --- | --- |
| Working directory | Repository root on `feat/issue-416-rabbithole-wave7-accessibility-target-lane-follow`. |
| `NODE_OPTIONS` | `--max-old-space-size=32768`. |
| GitHub access | Existing authenticated `gh` access capable of reading PR419 metadata and checks. |
| Arguments | None. |
| Timeout wrappers | Not used. Run the gate directly. |
| Merge behavior | The gate reports readiness only; it does not merge the PR. |

## Examples

### Validate only the target discovery contract

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh
```

Use this command for a fast review of checked-in launch, run/runtime, and Select
Project target discovery contracts. It does not launch Alice or generate fresh
desktop evidence.

### Validate the Alice desktop QA contract suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/tests/run-tests.sh
```

Use this command before merging changes that touch scenarios, runners, probe
contracts, QA documentation, or claim-boundary wording.

### Collect fresh Select Project target evidence

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

Review `tab-click-observation.json`. An opened result is target-specific only
when `targetStarterObserved`, `targetStarterSelected=true`,
`targetStarterOpenAttempted=true`, `openedStarter`, and
`projectOpenObserved=true` all match the configured `Africa Full` starter.

### Collect fresh runtime target discovery evidence

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-open-runtime-display \
  --timeout-seconds 300
```

`--timeout-seconds` is runner configuration for collecting fresh scenario
evidence. It is not a timeout wrapper around the focused PR419 validation or
readiness gate, which must be run directly.

Review `post-open-runtime-display-accessibility-evidence.json` first. If the
runtime target is blocked, review `visible-rendering-pixel-target-blocker.json`
for the exact missing target and next unblocker. If the target is ready, review
the target-scoped observation as accessibility discovery evidence only.

### Decide PR419 current-head readiness

Run the current-head gate after the final branch commit is pushed and the PR body
references that exact live head SHA:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-pr419-current-head-readiness-gate-contract.sh
```

Interpret the result strictly:

| Result | Meaning |
| --- | --- |
| `MERGE_READY` | Reviewer interpretation of a passing current-head gate, including live GitHub mergeability. The local final head, PR head, PR body evidence, focused evidence log, conflict checks, documentation contract, completed green GitHub Actions, and mergeability all agree for PR419. |
| `NOT_MERGE_READY` | Reviewer interpretation of any failing gate, missing mergeability confirmation, stale evidence, blocked check, conflict, or claim outside the bounded target-discovery scope. The named blocker must be resolved before reviewers treat the branch as merge-ready. |

## Tutorial: review the silver thread

1. Run the focused contract:

   ```bash
   NODE_OPTIONS=--max-old-space-size=32768 \
   bash qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh
   ```

2. Confirm the contract reports success and ends with
   `accessibility target discovery silver-thread contract satisfied`.

3. Open the scenario JSON through the existing validator when reviewing a
   specific lane:

   ```bash
   qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json \
     alice-desktop-post-open-runtime-display-accessibility-evidence
   ```

4. Check that blocked evidence is structured. Runtime blockers must name the
   missing target and next unblocker. Select Project blockers must name observed
   AT-SPI state, the attempted action, the expected next action, and why progress
   stopped.

5. Keep the review language bounded. A passing contract supports only this
   statement: the repository contains executable validation that the launch,
   run/runtime, and Select Project lanes expose the expected accessibility target
   discovery evidence and structured blockers.

6. For PR419 finalization, push the final branch head, update the PR body with
   that exact head SHA and the current-head gate evidence, then run
   `test-pr419-current-head-readiness-gate-contract.sh`. Treat a passing gate
   plus confirmed live GitHub mergeability as `MERGE_READY`; treat any failure,
   stale evidence, or missing mergeability confirmation as `NOT_MERGE_READY` and
   document the exact failing condition.

## Merge-ready decision model

The accessibility target discovery lane uses two different proof levels:

| Proof level | Source | Valid claim |
| --- | --- | --- |
| Static silver-thread contract | `test-accessibility-target-discovery-silver-thread.sh` | Checked-in launch, run/runtime, and Select Project artifacts expose the expected target discovery fields, structured blockers, and bounded wording. |
| PR419 current-head gate | `test-pr419-current-head-readiness-gate-contract.sh` | The final live PR419 head is aligned with the local worktree, PR body evidence, completed green checks, focused evidence log, bounded documentation, and GitHub mergeability. |

`MERGE_READY` is available only as a reviewer decision after the current-head
gate passes on the final pushed PR419 head, including the live GitHub
mergeability assertion. `NOT_MERGE_READY` is the correct result for stale PR body evidence,
different local and remote heads, pending or failed checks, unresolved conflicts,
missing focused validation evidence, missing audit cycles, missing mergeability
confirmation, or any claim that expands beyond accessibility target discovery
and structured blockers.

## Claim boundaries

The implemented contract may claim only:

- Launch evidence and launch fallback wiring remain structured and reviewable.
- Run/debug evidence remains manual and scoped unless a separate automation
  contract is added.
- Runtime/display target discovery exposes candidate, geometry, target-ready, and
  blocker fields.
- Select Project target discovery is tied to the committed `Africa Full` starter
  and exposes target observation, selection, open-attempt, opened-starter,
  project-open, and blocker fields.
- Documentation and scenarios keep the scope bounded to target discovery
  evidence.
- PR readiness evidence in `.copilot-evidence/default-workflow-attempt.log`,
  when present, records only point-in-time silver-thread target discovery
  readiness for the existing PR branch.
- Current-head PR419 readiness may be claimed only from the transient
  current-head proof produced by `test-pr419-current-head-readiness-gate-contract.sh`
  after the final commit is pushed and the PR body names that live head SHA. A
  failed gate is a `NOT_MERGE_READY` blocker, not an implied pass.

The implemented contract must not claim:

- Full UI automation.
- Visual correctness.
- Rendering correctness or visible rendering correctness.
- World execution correctness or full world execution.
- General accessibility compliance.
- Learner grading, scoring, lesson completion, or creative assessment.
- Save behavior.
- Decoder behavior.
- Current local generated evidence from a previous run.

## Extension rules

When changing the launch, run/runtime, or Select Project evidence paths:

1. Prefer preserving the existing scenario IDs, workflow names, and artifact
   names unless the evidence contract is intentionally replaced.
2. Keep blockers structured. Do not replace named blocker fields with generic log
   text.
3. Add new target discovery fields next to the existing artifact API instead of
   overloading rendering, execution, grading, Save, or decoder language.
4. Update this reference when artifact fields, scenario IDs, or supported review
   statements change.
5. Run the focused contract before using the changed lane as silver-thread
   target discovery evidence.
