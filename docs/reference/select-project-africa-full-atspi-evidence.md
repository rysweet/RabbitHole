# Select Project Africa Full AT-SPI evidence reference

This reference defines the target scenario metadata, validator rules, runner interface, probe evidence, blocker shape, and claim boundary for selecting/opening the committed `Africa Full` starter through Alice's Select Project dialog.

The lane is intentionally narrow. It records target identification, target-specific selection/opening progress, or the exact blocker that stopped progress. It does not establish visible rendering correctness, full lesson execution, grading, Save behavior, full UI automation, or world interaction.

## Contents

- [PR #437 recovery contract](#pr-437-recovery-contract)
- [Artifact field names](#artifact-field-names)
- [Target starter metadata](#target-starter-metadata)
- [Validator contract](#validator-contract)
- [Runner interface](#runner-interface)
- [Focused commands](#focused-commands)
- [`tab-click-observation.json`](#tab-click-observationjson)
- [Required action order](#required-action-order)
- [Evidence statuses](#evidence-statuses)
- [Blocker object](#blocker-object)
- [Select Project and PID context](#select-project-and-pid-context)
- [Post-open gating](#post-open-gating)
- [Evidence hygiene](#evidence-hygiene)
- [Contract test coverage](#contract-test-coverage)
- [Publishing boundary](#publishing-boundary)
- [PR finalization gate](#pr-finalization-gate)
- [Claim boundaries](#claim-boundaries)

## PR #437 recovery contract

The recovery lane for PR #437 is a documentation-backed finalization gate around the Select Project evidence lane. Do not merge manually. Do not use timeout wrappers. It starts from the GitHub PR head, records current GitHub merge/check/review metadata, runs a disposable local merge check only when GitHub reports `DIRTY` or mergeability metadata is unavailable/ambiguous after the head and base are verified, resolves only confirmed PR-blocking conflicts, and publishes either focused evidence or one exact blocker. When the current task is dirty repair, follow the planned [PR #437 dirty recovery reference](./pr437-dirty-recovery.md): dirty repair is `EDIT_AND_PUSH` or `BLOCKED_WITH_REASON` only, never `NO_OP`.

The recovery lane is scoped to `rysweet/RabbitHole` PR #437. The required PR state snapshot records:

| Field | Required use |
| --- | --- |
| `state` | GitHub open state. The PR must still be open before recovery evidence can support finalization. |
| `headRefName` | Branch checked out for recovery work. Do not infer this from the current local branch. |
| `headRefOid` | Exact PR head commit. The checked-out local `git rev-parse HEAD` value must match this SHA before validation or conditional merge checks count as PR evidence. |
| `baseRefName` | Branch used for local merge reproduction. |
| `isDraft` | Current draft state metadata. `false` is required for the current owner-free merge-ready report, but draft state alone never proves evidence quality. |
| `mergeStateStatus` | GitHub mergeability signal. `CLEAN` is merge-ready evidence when the current required checks are also `SUCCESS`; `DIRTY` remains actionable until locally reproduced or disproved. |
| `reviewDecision` | Current review decision metadata. An empty value means owner-free/unset and must be reported honestly instead of being converted into approval. |
| `statusCheckRollup` | Current GitHub check context. Required checks must be `SUCCESS` for the merge-ready report; passing checks do not override missing focused evidence or overbroad claims. |

`mergeStateStatus=CLEAN` plus required checks with `SUCCESS` conclusions is the merge-ready signal for the current PR head and does not require a disposable local merge check. `mergeStateStatus=DIRTY`, or unavailable/ambiguous mergeability metadata after the exact PR head and base are known, requires a local merge check against the PR base before the report can name either no merge blocker or exact conflict files. If PR metadata cannot be verified at all after the explicit retry path, the blocker is `environment dependency`, not a guessed local merge result. The recovery lane must not mark the PR ready from a `develop` checkout, a stale local branch, a branch name match without SHA confirmation, or a GitHub metadata snapshot that is not tied to the local `HEAD`.

Passing recovery gates makes PR #437 evidence-ready only. It does not authorize an agent to approve, merge, close, rebase, push unrelated changes, or otherwise mutate the PR.

The PR-specific recovery wording is current only while PR #437 remains open at the verified `headRefOid`. For the owner-free finalization path, `reviewDecision` can be unset while merge readiness still holds when the repository policy allows merge, `mergeStateStatus=CLEAN`, and every required check succeeds.

### External service boundary

No Alice runtime API client or service adapter is required for this Select Project lane. The only external service dependency in PR #437 recovery is GitHub metadata and checkout access through the `gh` CLI and `git fetch`; treat those commands as the operator-facing service adapter.

Read-only GitHub metadata and fetch calls may be retried for transient GitHub CLI authentication, network connectivity, or rate limiting failures. Do not silently substitute cached, historical, or manually typed PR metadata after the final retry fails. Record the failure as the current `environment dependency` blocker, naming the unavailable dependency.

Do not use gh auth status --show-token, print tokens, or include authentication output in evidence. Record only the command shape, exit status, PR fields, and non-secret error category needed to explain the blocker.

### Local PR head and conditional merge check

Start from the PR's recorded head commit, then prove the local checkout matches it:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

with_external_retry() {
  attempts="${GH_EXTERNAL_ATTEMPTS:-3}"
  delay_seconds="${GH_EXTERNAL_RETRY_SECONDS:-2}"
  attempt=1

  while true; do
    "$@" && return 0
    status="$?"
    if [ "$attempt" -ge "$attempts" ]; then
      printf 'external service call failed after %s attempts: %s\n' "$attempts" "$*" >&2
      return "$status"
    fi
    printf 'external service call failed on attempt %s/%s; retrying in %ss: %s\n' \
      "$attempt" "$attempts" "$delay_seconds" "$*" >&2
    sleep "$delay_seconds"
    attempt=$((attempt + 1))
  done
}

PR_JSON="$(with_external_retry gh pr view 437 --repo rysweet/RabbitHole \
  --json number,title,state,headRefName,headRefOid,baseRefName,isDraft,mergeStateStatus,reviewDecision,statusCheckRollup,url)"
PR_HEAD_OID="$(printf '%s\n' "$PR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["headRefOid"])')"
BASE_REF="$(printf '%s\n' "$PR_JSON" | python3 -c 'import json,sys; print(json.load(sys.stdin)["baseRefName"])')"
test -n "$PR_HEAD_OID"
test -n "$BASE_REF"

gh pr checkout 437 --repo rysweet/RabbitHole
LOCAL_HEAD_SHA="$(git rev-parse HEAD)"
test "$LOCAL_HEAD_SHA" = "$PR_HEAD_OID"

with_external_retry git fetch origin "$BASE_REF"
git status --short --branch
```

When this metadata reports `mergeStateStatus=CLEAN` and the required checks are `SUCCESS`, the current-head report records that GitHub evidence and skips the disposable worktree. Run the disposable worktree block below only when GitHub reports `DIRTY` or mergeability metadata is unavailable/ambiguous after `headRefOid` and `baseRefName` have been verified.

Use a disposable worktree for merge reproduction so the PR checkout stays reviewable. Always abort the no-commit merge before removing the disposable worktree; a clean `git merge --no-commit --no-ff` still leaves staged merge results.

```bash
MERGE_WORKTREE="$(mktemp -d /tmp/pr437-merge-check.XXXXXX)"
rmdir "$MERGE_WORKTREE"

git worktree add --detach "$MERGE_WORKTREE" "$PR_HEAD_OID"
trap '(
  cd "$MERGE_WORKTREE"
  if git rev-parse -q --verify MERGE_HEAD; then
    git merge --abort
  fi
)
git worktree remove --force "$MERGE_WORKTREE"' EXIT

set +e
(
  cd "$MERGE_WORKTREE"
  git merge --no-commit --no-ff "origin/$BASE_REF"
)
MERGE_STATUS="$?"
set -e

(
  cd "$MERGE_WORKTREE"
  git diff --name-only --diff-filter=U
)
test "$MERGE_STATUS" -eq 0
```

If the merge check reports conflicts, resolve only those files on the PR branch. For each conflict, record the file, conflict reason, resolution, and next validation. If a behavior-sensitive conflict cannot be safely resolved, stop and publish `merge dirtiness` as the current blocker.

### Conflict resolution scope

Resolve only files reported by the local merge check as unmerged:

```bash
git diff --name-only --diff-filter=U
```

For each conflict file, record:

| Field | Meaning |
| --- | --- |
| Conflict file | Repository-relative path reported by Git. |
| Conflict reason | The incompatible edits or overlapping contract change that blocked the merge. |
| Resolution | The narrow behavior-preserving resolution, or `unresolved` when unsafe. |
| Next action | The exact validation, owner decision, or follow-up needed. |

If the conflict touches behavior-sensitive Alice code and no characterization or focused validation exists, the recovery lane records `merge dirtiness` as the current blocker instead of guessing a resolution.

### Workflow-accepted no-op justification

The recovery lane may publish a no-op justification only when a current-head run proves that no repository change is needed and no repository files are modified. A no-op report is still evidence, not an assumption: it records the exact PR metadata command, exact local head SHA, worktree cleanliness, merge-ready GitHub evidence or conditional merge-check evidence, focused validation commands, and reviewed artifacts or the explicit reason no live artifact was required.

Use a no-op justification only when all of these conditions are true:

| Condition | Required evidence |
| --- | --- |
| Exact PR head | `gh pr view` reports `headRefOid`, and local `git rev-parse HEAD` matches it exactly. |
| Clean worktree | `git status --short --branch` has no repository changes. The recovery did not edit, commit, or push docs, tests, contracts, or other files. |
| Merge readiness | GitHub reports `mergeStateStatus=CLEAN`, and every required status check in `statusCheckRollup` has `status=COMPLETED` and `conclusion=SUCCESS`; no disposable local merge check is required for this path. If GitHub reports `DIRTY`, or mergeability metadata is unavailable/ambiguous after the head and base are verified, the report includes the conditional local merge-check command/result or names one `merge dirtiness` blocker instead of claiming readiness. |
| Review metadata | `reviewDecision` is recorded exactly. Empty review decision is reported as owner-free/unset and is not described as approval. |
| Focused validation | The Select Project scenario/schema/probe checks in [Focused commands](#focused-commands) pass, or the report names `failing validation`. |
| Evidence boundary | The report separates verified evidence from unverified assumptions and makes no full UI automation, rendering, Save, grading, creative-assessment, or lesson-completion claim. |

If GitHub reports `mergeStateStatus=DIRTY`, the no-op report must include the disposable local merge reproduction. GitHub metadata alone is not enough to claim that no repository change is required on the dirty path. If the recovery changes docs, tests, contracts, or any other repository file, do not publish `No-op justification:`; use the focused pushed-change summary path instead.

No live artifact is acceptable only for a no-op documentation recovery that does not claim live Select Project success and explicitly says the run verified existing documentation/contracts instead of producing a new AT-SPI evidence directory. Any claim that Africa Full was selected, opened, or observed after opening requires live artifacts from the run being reported.

For PR #437, use this report shape only after the executable checks prove the repository already satisfies the focused Select Project contract at the verified PR head:

```markdown
No-op justification:
- Current branch: `feat/issue-415-rabbithole-wave7-select-project-starter-lane-follo`
- Current head: the verified `headRefOid` that matches `git rev-parse HEAD`
- PR metadata command: `gh pr view 437 --repo rysweet/RabbitHole --json number,title,state,headRefName,headRefOid,baseRefName,isDraft,mergeStateStatus,reviewDecision,statusCheckRollup,url`
- Worktree cleanliness: `git status --short --branch` showed no repository changes.
- Merge-ready evidence: `mergeStateStatus=CLEAN`, every required check in `statusCheckRollup` completed with `SUCCESS`, and branch refs point at the verified head.
- Review metadata: `reviewDecision` is empty/owner-free; report it as unset and do not claim approval.
- Focused validation: `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`, schema/scenario/proof/probe contracts, `tests/test_pr437_select_project_recovery_contract.py`, and `tests/test_pr437_noop_recovery_report_contract.py` passed at this head.
- Live artifact exception: no new AT-SPI evidence directory was required because this recovery verified existing documentation/contracts and does not claim live Select Project success.
```

If `git rev-parse --abbrev-ref HEAD`, `git rev-parse HEAD`, or the PR metadata disagree, treat it as branch/head drift and must not publish `No-op justification:`. If GitHub metadata cannot be verified after the explicit external retry path, report `Current blocker: environment dependency`; do not silently substitute cached, historical, or manually typed PR metadata.

### Focused pushed-change summary

Use the focused pushed-change summary path, not `No-op justification:`, when recovery edits and pushes documentation, tests, contracts, or other repository files. This path can still be merge-ready and owner-free, but it is an `EDIT_AND_PUSH` report because repository contents changed.

```markdown
Report path: `EDIT_AND_PUSH`
- Current branch: `feat/issue-415-rabbithole-wave7-select-project-starter-lane-follo`
- Current head: the verified pushed `headRefOid` that matches `git rev-parse HEAD`
- PR metadata command: `gh pr view 437 --repo rysweet/RabbitHole --json number,title,state,headRefName,headRefOid,baseRefName,isDraft,mergeStateStatus,reviewDecision,statusCheckRollup,url`
- Worktree cleanliness: `git status --short --branch` showed no uncommitted repository changes after the commit/push.
- Merge-ready evidence: `mergeStateStatus=CLEAN`, every required check in `statusCheckRollup` completed with `SUCCESS`, and branch refs point at the verified pushed head; include the conditional local merge-check result only when GitHub reports `DIRTY` or mergeability metadata is unavailable/ambiguous after head/base verification.
- Review metadata: `reviewDecision` is empty/owner-free; report it as unset and do not claim approval.
- Focused validation: focused Select Project documentation/schema/probe contracts passed at this head.
- Files modified: list the repository paths changed by the recovery.
```

### Recovery blocker taxonomy

The recovery lane publishes exactly one current blocker when the PR cannot advance:

| Blocker | Required meaning |
| --- | --- |
| `merge dirtiness` | PR #437 cannot be cleanly merged into its base, or conflict resolution is unsafe/incomplete. |
| `missing evidence` | Merge state is clean enough to proceed, but the focused Select Project evidence artifacts are absent or insufficient. |
| `failing validation` | Focused scenario/schema/probe contract checks fail. |
| `environment dependency` | Live AT-SPI or supporting desktop/runtime dependencies prevent execution; the docs name the missing dependency instead of claiming proof. |

For PR #437 recovery, GitHub CLI authentication, network connectivity, and rate limiting are supporting external-service dependencies under `environment dependency` because they prevent verified PR metadata retrieval.

Do not publish multiple blockers as a grab bag. The report names the first blocker that prevents responsible finalization and preserves supporting details for that blocker.

`None` is not a blocker. Use `Current blocker: None` only as the ready state after every PR finalization gate passes.

## Artifact field names

The committed artifacts use the current target-specific field names directly:

| Evidence meaning | Artifact or status field |
| --- | --- |
| Validated starter metadata | `targetStarter` |
| Safe AT-SPI observation for the starter | `targetStarterObserved` |
| Target-specific selection proof | `targetSelectionObserved` |
| OK/Open attempted after target selection | `openAttempted` |
| Matching starter opened after Select Project dismissal | `openedStarter` |
| Runner target-selection summary | `selectProjectTargetSelectionObserved` |
| Runner open-attempt summary | `selectProjectOpenAttempted` |

The older `targetStarterSelected` and `targetStarterOpenAttempted` fields may appear as diagnostics, but they do not satisfy the Africa Full opened gate by themselves.

## Target starter metadata

`qa/outside-in/alice-desktop/scenarios/select-project-tab-click-exec.yaml` binds the proof to one committed starter:

| Field | Required value |
| --- | --- |
| `targetStarter.displayName` | `Africa Full` |
| `targetStarter.repositoryPath` | `core/resources/src/application/resources/starter-projects/AfricaFull.a3p` |

The scenario path is metadata for validation and evidence review. The AT-SPI probe selects through the live Select Project dialog; it does not read or mutate the `.a3p` file.

## Validator contract

`qa/outside-in/alice-desktop/runners/validate-scenarios.sh` fails validation when the Select Project tab-click scenario has missing, mismatched, unsafe, or ambiguous target metadata.

| Rule | Required behavior |
| --- | --- |
| Workflow allowlist | `workflow` must be `select-project-tab-click-smoke` for `alice-desktop-select-project-tab-click-exec`. |
| Display name | `targetStarter.displayName` must be exactly `Africa Full`. |
| Repository path | `targetStarter.repositoryPath` must be exactly `core/resources/src/application/resources/starter-projects/AfricaFull.a3p`. |
| Path safety | The path must be relative, normalized, in-repository, and free of traversal segments. |
| Automation safety | The Alice launch argv must match an approved argv list; scenario YAML must not become shell code. |

Custom scenario catalogs can validate other scenarios, but they cannot turn arbitrary starter names or paths into trusted Africa Full success evidence.

## Runner interface

`qa/outside-in/alice-desktop/runners/run-scenario.sh` extracts the validated `targetStarter` metadata and passes it to `qa/outside-in/alice-desktop/runners/tab-click-probe.py` using quoted arguments or safe environment values:

| Input | Required value |
| --- | --- |
| `TARGET_STARTER_DISPLAY_NAME` or equivalent argv | `Africa Full` |
| `TARGET_STARTER_REPO_PATH` or equivalent argv | `core/resources/src/application/resources/starter-projects/AfricaFull.a3p` |

The runner executes the approved Alice launch argv directly. It must not use `eval`, dynamic executable construction, or string interpolation from scenario YAML.

When the probe output echoes the validated target metadata, the runner promotes only narrow `selectProject*` fields into `status.txt`:

| Status field | Meaning |
| --- | --- |
| `selectProjectTargetDisplayName` | Validated `Africa Full` display name. |
| `selectProjectTargetRepositoryPath` | Validated committed starter path. |
| `selectProjectEvidenceStatus` | Probe `evidenceStatus`: `opened`, `selected`, `blocked`, or `failed`. |
| `selectProjectStartersTabSafety` | Compact active-Starters safety status. |
| `selectProjectTargetSelectionObserved` | Probe `targetSelectionObserved` flag. |
| `selectProjectOpenAttempted` | Probe `openAttempted` flag. |
| `selectProjectProjectOpenObserved` | Probe `projectOpenObserved` flag. |
| `selectProjectNextBlocker` | Structured blocker detail for non-opened outcomes. |

Diagnostic fields such as `selectProjectOpenedStarterDisplayName` and `selectProjectOpenedStarterRepositoryPath` may appear, but opened proof still requires `selectProjectTargetSelectionObserved=true`, `selectProjectOpenAttempted=true`, and `selectProjectProjectOpenObserved=true`.

If the probe output omits `targetStarter` or the values do not match the validated scenario, the runner must fail closed and avoid success-shaped promotion.

## Focused commands

Run only the focused Select Project validation and proof path.

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

These commands validate scenario metadata, runner/schema contracts, probe behavior, blocker shape, PR #437 recovery wording, and no-overclaim boundaries. They do not produce live Select Project opening artifacts.

### Live Select Project evidence run

Run the live scenario only when the current task requires fresh AT-SPI evidence:

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

### Artifact review

When a live evidence run is produced, review `status.txt`, `tab-click-observation.json`, `post-project-open-observation.json`, `x-window-inventory.json`, and `select-project-window.json` from the same run directory. Contract validation alone does not prove live opening artifacts.

Do not use this proof run to claim Save, visible rendering, grading, lesson, model export, archive fixture, procedure/edit, or coverage behavior.

## `tab-click-observation.json`

The Select Project probe writes `tab-click-observation.json`. Existing tab inventory fields may remain present, but the fields below define the Africa Full evidence contract.

| Field | Type | Meaning |
| --- | --- | --- |
| `targetStarter.displayName` | string | Scenario target display name; must be `Africa Full`. |
| `targetStarter.repositoryPath` | string | Scenario target repository path; must be the committed Africa Full starter path. |
| `evidenceStatus` | enum | `opened`, `selected`, `blocked`, or `failed`. |
| `startersTabSafety` | object | Proof that the probe activated Starters before target search and scoped the search to `active-starters-tab`. |
| `targetStarterObserved` | object or null | Safe AT-SPI observation for the `Africa Full` node when found. |
| `targetSelectionObserved` | boolean | `true` only when target-specific evidence shows `Africa Full` was selected. |
| `targetSelectionAttempt` | object or null | Method and target metadata for the target-specific selection proof. |
| `openAttempted` | boolean | `true` only when OK/Open was attempted after target-specific selection evidence. |
| `projectOpenObserved` | boolean | `true` only when the Select Project frame is no longer present after the guarded open attempt. |
| `projectOpenDetail` | string or null | Concise open-progress detail. It must not make rendering, lesson, or grading claims. |
| `targetStarterObserved.activeListCandidates` | object list | Bounded safe summaries of active Starters list candidates when the target starter is found. |
| `blocker` | string or null | Stable machine-readable blocker code. Preserve this field for compatibility with current artifacts and tests. |
| `blockerDetail` | string or object or null | Existing human-readable blocker detail. Preserve this field for compatibility with current artifacts and tests. |
| `nextBlocker` | object or null | Structured target-specific blocker details for non-opened outcomes. |

### `startersTabSafety`

`startersTabSafety` records why the target search was safe to treat as Starters-scoped evidence.

| Field | Meaning |
| --- | --- |
| `tabName` | Expected active tab label, `Starters`. |
| `activationAttempted` | `true` when the probe attempted to activate the Starters tab before searching. |
| `activatedBeforeTargetSearch` | `true` for accepted evidence; false or missing blocks success. |
| `activationDetail` | Concise AT-SPI activation result. |
| `targetSearchScope` | `active-starters-tab` for accepted evidence. |

If this object is missing, ambiguous, or not Starters-scoped, `evidenceStatus` must not be `opened`.

### `targetStarterObserved`

When the target is found, `targetStarterObserved` records only safe AT-SPI node data:

| Field | Type | Meaning |
| --- | --- | --- |
| `name` | string | Accessible name; must identify `Africa Full` for opened evidence. |
| `role` | string | Accessible role name. |
| `description` | string | Accessible description when AT-SPI exposes one. |
| `states` | string list | Safe state names reported by AT-SPI. |
| `availableActions` | string list | Available AT-SPI action names, such as `click` or `activate`. |
| `treePath` | integer list | Child indexes from the Select Project frame to the target node. |
| `depth` | integer | Target node depth in the captured AT-SPI tree. |
| `indexInParent` | integer or null | Target index in its immediate parent when available. |
| `parentSelectionAvailable` | boolean | Whether a containing accessible object exposes a usable selection interface. |

If the target is not found, `targetStarterObserved` is null and `blocker`, `blockerDetail`, and `nextBlocker.observedAtspiState` explain the active Starters context that stopped progress.

## Required action order

The probe collects evidence in this order:

1. Activate the `Starters` tab.
2. Record `startersTabSafety`.
3. Locate `Africa Full` inside that context and record `targetStarterObserved`.
4. Try the target node's supported action first, such as `click` or `activate`.
5. If the target node has no usable action, try the parent selection interface second and record the interface used.
6. Set `targetSelectionObserved=true` only after target-specific selection evidence exists.
7. Click OK/Open only after `targetSelectionObserved=true`.
8. Set `projectOpenObserved=true` only if the Select Project frame is dismissed after that guarded open attempt.

If any step cannot provide target-specific evidence, the result must stop at `selected`, `blocked`, or `failed`. The probe must not infer Africa Full success from generic Select Project dismissal.

## Evidence statuses

| `evidenceStatus` | Required meaning |
| --- | --- |
| `opened` | The artifact echoes the validated `targetStarter`, records safe `startersTabSafety`, observes the Africa Full target, records `targetSelectionObserved=true`, records `openAttempted=true`, records `projectOpenObserved=true`, and includes Alice Java/window context from the run. |
| `selected` | Africa Full selection is supported by target-specific evidence, but opening did not complete. The blocker names the remaining open step. |
| `blocked` | AT-SPI automation could not prove target-specific selection/opening. The blocker records the observed state and one next action. |
| `failed` | The probe or runtime failed before producing a normal AT-SPI capability result. The blocker records the failure boundary. |

`status=observed` can coexist with `evidenceStatus=blocked` when the probe successfully collected AT-SPI state but could not complete a safe target-specific action.

## Blocker object

Every non-`opened` terminal result preserves existing blocker compatibility and adds target-specific structure:

| Field | Meaning |
| --- | --- |
| `blocker` | Stable machine-readable blocker code, such as `target-starter-selection-unavailable`. |
| `blockerDetail` | Concise existing-style explanation for humans and current tests. |
| `nextBlocker` | Structured target-specific blocker evidence. |

`nextBlocker` contains:

| Field | Meaning |
| --- | --- |
| `observedAtspiState` | Final relevant AT-SPI state, including active context, target visibility, role, states, actions, and selection-interface availability. |
| `actionAttempted` | The action sequence attempted by the probe. |
| `expectedNextAction` | The next implementation or automation capability needed to continue. |
| `reasonProgressStopped` | Why continuing would overclaim evidence or become unsafe. |

The blocker report must name exactly one next blocker. Include current Alice Java/window context, Select Project window context, Starters-tab safety, target observation state, target selection state, open-attempt state, and project-open state. Do not publish a general status dump.

## Select Project and PID context

The focused run also writes `x-window-inventory.json`, `select-project-window.json`, and `status.txt`.

| Artifact | Required context |
| --- | --- |
| `x-window-inventory.json` | A Java window candidate for Alice, including title, class, process name, process ID, and geometry. |
| `select-project-window.json` | The exact `Select Project` Java dialog context, including title, class, process ID, geometry, and `interactionProof=select-project-window-visible`. |
| `status.txt` | Scenario ID, Select Project wait status, tab-click artifact name, target metadata, evidence status, target-selection flag, open-attempt flag, project-open flag, and blocker summary when blocked. |

For an `opened` result, the Alice Java PID and Select Project window PID must refer to the same Java process observed during the run. For a blocker result, publish the current Alice Java/window PID context from these artifacts instead of replacing it with a broad process dump.

## Post-open gating

`qa/outside-in/alice-desktop/runners/post-project-open-probe.py` consumes target metadata when present. It treats post-open main-window observation as eligible only when the prior tab-click artifact proves the target-specific Africa Full path:

```json
{
  "evidenceStatus": "opened",
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "startersTabSafety": {
    "tabName": "Starters",
    "activationAttempted": true,
    "activatedBeforeTargetSearch": true,
    "activationDetail": "Starters tab activated before target search",
    "targetSearchScope": "active-starters-tab"
  },
  "targetSelectionObserved": true,
  "openAttempted": true,
  "projectOpenObserved": true
}
```

If any required target-starter field is missing, false, null, or inconsistent, `post-project-open-observation.json` must record a blocked result rather than converting generic main-window state into Africa Full proof. Legacy compatibility names can be preserved as diagnostics, but the accepted facts are the target contract facts above.

When the gate passes, the post-open probe may report narrow open-progress observations such as accessible main-window presence and Java/window continuity. It must not report visible rendering correctness, grading, full lesson execution, Save completion, or full UI automation.

## Evidence hygiene

Evidence and blocker payloads must stay scoped to safe AT-SPI state and scenario metadata. Do not dump unrelated environment variables, process lists, usernames, home paths, tokens, credentials, arbitrary filesystem paths, saved project contents, grading state, lesson state, or world execution traces into `tab-click-observation.json` or post-open artifacts.

## Contract test coverage

Use the existing QA contract test structure for the committed field vocabulary:

| Test | Coverage |
| --- | --- |
| `qa/outside-in/alice-desktop/tests/test-schema-contract.sh` | JSON schema accepts the target metadata and evidence contract fields. |
| `qa/outside-in/alice-desktop/tests/test-scenario-validation.sh` | Scenario catalog normalization preserves the committed Africa Full target metadata and rejects missing, absolute, traversal, or drifted target paths. |
| `qa/outside-in/alice-desktop/tests/test-select-project-completion-contract.sh` | Select Project completion spec keeps the focused scenario ID/workflow and runner-published target/opened/blocker status fields. |
| `qa/outside-in/alice-desktop/tests/test-select-project-proof.sh` | Select Project window proof preserves exact Java dialog/window context and does not claim project/world interaction. |
| `qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh` | Tab-click probe emits target-specific Africa Full opened/blocked evidence and does not click OK/Open without target-specific selection evidence. |
| `qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh` | Post-open gating requires prior target-specific opened evidence and blocks generic main-window proof. |
| `tests/test_pr437_select_project_recovery_contract.py` | PR #437 recovery docs keep exact PR-head verification, disposable merge reproduction, focused validation commands, single-blocker reporting, and no-overclaim boundaries aligned. |
| `tests/test_pr437_noop_recovery_report_contract.py` | PR #437 no-op recovery docs keep the current-head report labels, live-artifact exception, and readiness/review/finalization evidence shape aligned. |

## Publishing boundary

Publish only one of these outcomes:

| Outcome | Required published content |
| --- | --- |
| Opened | `evidenceStatus=opened`, exact `Africa Full` `targetStarter` metadata, `startersTabSafety`, `targetSelectionObserved=true`, `openAttempted=true`, `projectOpenObserved=true`, and Alice Java/window context. |
| Blocked | One blocker code and detail, current Alice Java/window context, Select Project window context, Starters-tab safety, target observation state, target selection state, open-attempt state, project-open state, and one structured `nextBlocker`. |

Do not publish full Alice UI automation, Save proof, visible rendering correctness, grading, creative assessment, first-lesson completion, model exporter behavior, unrelated launcher behavior, archive fixture behavior, procedure/edit behavior, unrelated decoder behavior, or coverage measurements from this lane.

## PR finalization gate

PR #437 is merge-ready for the owner-free finalization report when all finalization conditions are true. The earlier draft-only gate (`PR #437 remains draft unless all finalization conditions are true`) is no longer the current-head contract because `isDraft=false` is metadata, not the remaining blocker.

| Gate | Ready condition |
| --- | --- |
| PR head checked out | `gh pr checkout 437 --repo rysweet/RabbitHole` or an equivalent explicit checkout is the active branch, and local `git rev-parse HEAD` matches the recorded `headRefOid`. |
| GitHub merge state | `mergeStateStatus=CLEAN` for the verified `headRefOid`. |
| Required checks | Every required check in `statusCheckRollup` is completed with `SUCCESS`. |
| Review metadata | Empty `reviewDecision` is reported as owner-free/unset. It is not approval, and it does not block merge readiness when repository policy and checks allow merge. |
| Conflict scope | Any resolved conflicts are limited to PR-blocking files and preserve Alice/RabbitHole baseline behavior unless a tested change is documented. |
| Focused validation | Select Project scenario/schema/probe contract checks pass. |
| Evidence truthfulness | Published evidence names only commands and artifacts actually produced in the recovery run. |
| Claim boundary | The report separates verified evidence from unverified assumptions and does not imply full UI automation. |

When any gate fails, the recovery report publishes one current blocker from the blocker taxonomy. Passing GitHub checks are merge-ready evidence only when they are tied to the verified head and clean merge state; they do not make an under-evidenced or overclaiming PR ready. Even when every gate passes, the documented result is evidence-ready only; it does not authorize an agent to approve, merge, close, rebase, push unrelated changes, or otherwise mutate PR #437.

## Verified evidence report shape

Every recovery report uses these headings:

| Heading | Required content |
| --- | --- |
| `Verified evidence` | Commands run, exit status, and artifact paths produced by this recovery run. |
| `Unverified assumptions` | Expected behavior or code-path reasoning that was not executed. Use `None recorded` only when no assumptions are needed. |
| `Current blocker` | Exactly one blocker: `merge dirtiness`, `missing evidence`, `failing validation`, or `environment dependency`; use `None` only when every finalization gate is satisfied. |

The final implementation output also includes `Readiness evidence:`, `Review evidence:`, `Finalization evidence:`, and `Files modified:` labels so reviewers can distinguish current-head execution, contract review, PR finalization state, and repository changes without inferring unverified UI coverage.

The evidence report may cite `tab-click-observation.json`, `post-project-open-observation.json`, `status.txt`, `x-window-inventory.json`, and `select-project-window.json` only when those files were produced by the run being reported. It must not convert historical artifacts, expected probe behavior, or passing non-UI checks into live Select Project proof.

## Claim boundaries

This evidence lane proves only what its JSON artifacts state:

- `opened` proves target-specific AT-SPI selection/opening progress for the committed Africa Full starter and Select Project dismissal after the guarded attempt.
- `selected` proves target-specific selection evidence but not opening.
- `blocked` proves a reproducible automation gap and names the exact next blocker.
- Main-window AT-SPI state proves accessible frame presence only and only after the Africa Full gate passes.

The lane does not prove visible rendering correctness, full project interaction, grading, creative assessment, Save completion, first-lesson completion, unrelated launcher behavior, unrelated decoder behavior, or lesson completion.
