# Select Project Africa Full AT-SPI evidence reference

This reference defines the target scenario metadata, validator rules, runner interface, probe evidence, blocker shape, and claim boundary for selecting/opening the committed `Africa Full` starter through Alice's Select Project dialog.

The lane is intentionally narrow. It records target identification, target-specific selection/opening progress, or the exact blocker that stopped progress. It does not establish visible rendering correctness, full lesson execution, grading, Save behavior, full UI automation, or world interaction.

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

Run only the focused Select Project validation and proof path:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-scenario-validation.sh
bash qa/outside-in/alice-desktop/tests/test-select-project-completion-contract.sh
bash qa/outside-in/alice-desktop/tests/test-select-project-proof.sh
bash qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh
bash qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh

ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

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
| `observedCandidates` | object list | Bounded safe summaries of candidate starter nodes inspected in the active Starters context. |
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
| `availableTabNames` | Bounded list of Select Project tab names observed by the probe. |

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

If the target is not found, `targetStarterObserved` is null and `observedCandidates`, `blocker`, `blockerDetail`, and `nextBlocker` explain the active Starters context and discovered candidates.

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

## Publishing boundary

Publish only one of these outcomes:

| Outcome | Required published content |
| --- | --- |
| Opened | `evidenceStatus=opened`, exact `Africa Full` `targetStarter` metadata, `startersTabSafety`, `targetSelectionObserved=true`, `openAttempted=true`, `projectOpenObserved=true`, and Alice Java/window context. |
| Blocked | One blocker code and detail, current Alice Java/window context, Select Project window context, Starters-tab safety, target observation state, target selection state, open-attempt state, project-open state, and one structured `nextBlocker`. |

Do not publish full Alice UI automation, Save proof, visible rendering correctness, grading, creative assessment, first-lesson completion, model exporter behavior, unrelated launcher behavior, archive fixture behavior, procedure/edit behavior, unrelated decoder behavior, or coverage measurements from this lane.

## Claim boundaries

This evidence lane proves only what its JSON artifacts state:

- `opened` proves target-specific AT-SPI selection/opening progress for the committed Africa Full starter and Select Project dismissal after the guarded attempt.
- `selected` proves target-specific selection evidence but not opening.
- `blocked` proves a reproducible automation gap and names the exact next blocker.
- Main-window AT-SPI state proves accessible frame presence only and only after the Africa Full gate passes.

The lane does not prove visible rendering correctness, full project interaction, grading, creative assessment, Save completion, first-lesson completion, unrelated launcher behavior, unrelated decoder behavior, or lesson completion.
