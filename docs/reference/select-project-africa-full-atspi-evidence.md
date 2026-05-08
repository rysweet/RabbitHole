# Select Project Africa Full AT-SPI evidence reference

This reference defines the scenario metadata, runner contract, JSON evidence, and publishing boundary for selecting/opening the committed `Africa Full` starter through the Alice Select Project dialog.

## Target starter metadata

`qa/outside-in/alice-desktop/scenarios/select-project-tab-click-exec.yaml` binds the proof to one committed starter:

| Field | Value |
| --- | --- |
| `targetStarter.displayName` | `Africa Full` |
| `targetStarter.repositoryPath` | `core/resources/src/application/resources/starter-projects/AfricaFull.a3p` |

The validator rejects missing target metadata, absolute repository paths, path traversal, and any repository path other than the committed Africa Full starter path for `alice-desktop-select-project-tab-click-exec`.

## Runner interface

`qa/outside-in/alice-desktop/runners/run-scenario.sh` extracts the target starter metadata from the validated scenario and passes it to `qa/outside-in/alice-desktop/runners/tab-click-probe.py` as runner-managed environment variables:

| Variable | Value for this scenario | Notes |
| --- | --- | --- |
| `TARGET_STARTER_DISPLAY_NAME` | `Africa Full` | Non-empty display label used for AT-SPI target discovery. |
| `TARGET_STARTER_REPO_PATH` | `core/resources/src/application/resources/starter-projects/AfricaFull.a3p` | Relative repository path recorded as evidence metadata. |

The runner quotes both values and executes the existing argv-backed Alice launch path directly. It does not construct shell commands from scenario YAML.

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

Do not use this proof run to start Save, rendering, grading, lesson, model export, archive fixture, procedure/edit, or coverage work.

## `tab-click-observation.json`

The Select Project probe writes `tab-click-observation.json`. Existing tab inventory fields remain present, and target starter fields provide the project-specific proof boundary.

| Field | Type | Meaning |
| --- | --- | --- |
| `targetStarter.displayName` | string | Scenario target display name. |
| `targetStarter.repositoryPath` | string | Scenario target repository path. |
| `javaPid` | integer or null | Alice Java process ID selected from the current Alice window inventory, or null when no safe PID is available. |
| `targetStarterObserved` | object or null | AT-SPI discovery result for `Africa Full` in the active Starters context. This must be non-null and identify `Africa Full` for `evidenceStatus=opened`. |
| `targetStarterSelected` | boolean | `true` only when the probe has target-specific evidence that `Africa Full` was selected. |
| `targetStarterOpenAttempted` | boolean | `true` only when OK/Open was attempted after target-specific selection evidence. |
| `openedStarter` | object or null | Filled with the `Africa Full` target metadata only when the target-specific open path succeeds. |
| `projectOpenObserved` | boolean | `true` when the Select Project frame is no longer present after the open attempt. |
| `evidenceStatus` | enum | `opened`, `selected`, `blocked`, or `failed`. |
| `blocker` | string or null | Existing machine-readable blocker code. Preserve this field for compatibility with current artifacts and tests. |
| `blockerDetail` | string or object or null | Existing human-readable blocker detail. Preserve this field for compatibility with current artifacts and tests. |
| `nextBlocker` | object or null | Structured target-specific blocker details for `selected`, `blocked`, and `failed` statuses. |

### `targetStarterObserved`

When the target is found, the observation records the AT-SPI node shape:

| Field | Type | Meaning |
| --- | --- | --- |
| `name` | string | Accessible name. |
| `role` | string | Accessible role name. |
| `description` | string | Accessible description when AT-SPI exposes one. |
| `states` | string list | Safe state names reported by AT-SPI. |
| `availableActions` | string list | Available AT-SPI action names, such as `click` or `activate`. |
| `treePath` | integer list | Child indexes from the Select Project frame to the target node. |
| `depth` | integer | Target node depth in the captured AT-SPI tree. |
| `indexInParent` | integer or null | Target index in its immediate parent when available. |
| `listName` | string | Accessible name of the containing starter list. |
| `listRole` | string | Accessible role name of the containing starter list. |
| `listStates` | string list | Safe state names reported for the containing starter list. |
| `listChildCount` | integer | Number of children in the containing starter list. |
| `listChildIndex` | integer | Target index within the containing starter list. |
| `parentSelectionAvailable` | boolean | Whether the containing starter list exposes a usable selection interface. |
| `activeListCandidates` | object list | Safe summaries of the active Starters list candidates inspected during target discovery. |

If the target is not found, `targetStarterObserved` is null; `blocker`, `blockerDetail`, and `nextBlocker` explain the active Starters context and discovered candidate names.

### Select Project and PID context

The focused run also writes `x-window-inventory.json`, `select-project-window.json`, and `status.txt`. These files provide the window/PID context for the proof:

| Artifact | Required context |
| --- | --- |
| `x-window-inventory.json` | A Java window candidate for Alice, including title, class, process name, process ID, and geometry. |
| `select-project-window.json` | The exact `Select Project` Java dialog context, including title, class, process ID, geometry, and `interactionProof=select-project-window-visible`. |
| `status.txt` | `scenario=alice-desktop-select-project-tab-click-exec`, `selectProjectWaitStatus=select-project-window-found`, `tabClickObservation=tab-click-observation.json`, `tabClickStatus`, and `tabClickBlocker`. |

For an `opened` result, the Alice Java PID and Select Project window PID must refer to the same Java process observed during the run. For a blocker result, publish the current Alice Java/window PID context from these artifacts instead of replacing it with a broad process dump.

### Required action order

The probe must collect evidence in this order:

1. Activate the active `Starters` context.
2. Locate `Africa Full` inside that context and record its safe AT-SPI role, states, actions, tree path, and selection-interface availability.
3. Try the target node's supported action first, such as `click` or `activate`.
4. If the target node has no usable action, try the parent selection interface second and record the interface used.
5. Click OK/Open only after target-specific selection evidence exists.

If any step cannot provide target-specific evidence, the result must stop at `selected`, `blocked`, or `failed`; it must not infer Africa Full success from generic Select Project dismissal.

### Evidence statuses

| `evidenceStatus` | Required meaning |
| --- | --- |
| `opened` | `Africa Full` was observed in the active Starters context, `targetStarterObserved.name` identifies `Africa Full`, target-specific selection/open was attempted, `openedStarter` records matching target metadata, `projectOpenObserved=true`, and the run has Alice Java/window PID context. |
| `selected` | `Africa Full` selection is supported by evidence, but opening did not complete. The blocker names the remaining open step. |
| `blocked` | AT-SPI automation could not prove target-specific selection/opening. The blocker records the observed state and `nextBlocker` action. |
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
| `observedAtspiState` | Final relevant AT-SPI state, including target visibility, role, states, actions, selection interface availability, and active context. |
| `actionAttempted` | The action sequence attempted by the probe. |
| `expectedNextAction` | The next implementation or automation capability needed to continue. |
| `reasonProgressStopped` | Why continuing would overclaim evidence or become unsafe. |

The blocker report must name exactly one next blocker. Include the current Alice Java/window PID context, Select Project window context, Starters-tab activation state, target observation state, target selection state, and OK/Open attempt state. Do not publish a general status dump.

## Evidence hygiene

Evidence and blocker payloads must stay scoped to safe AT-SPI state and scenario metadata. Do not dump unrelated environment variables, process lists, usernames, home paths, tokens, credentials, or arbitrary filesystem paths into `tab-click-observation.json` or post-open artifacts.

## Post-open gating

`qa/outside-in/alice-desktop/runners/post-project-open-probe.py` consumes target metadata when present. It treats post-open main-window observation as eligible only when the prior tab-click artifact proves the `Africa Full` target-specific path:

```json
{
  "evidenceStatus": "opened",
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterObserved": {
    "name": "Africa Full"
  },
  "targetStarterSelected": true,
  "targetStarterOpenAttempted": true,
  "openedStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "projectOpenObserved": true
}
```

If any required target-starter field is missing, null, false, or inconsistent, `post-project-open-observation.json` must record a blocked result rather than converting generic main-window state into Africa Full proof.

The Select Project completion proof does not require a separate downstream workflow. The tab-click proof is sufficient only when `tab-click-observation.json` records `evidenceStatus=opened`, matching `targetStarter` and `openedStarter` metadata for the committed `Africa Full` starter, `targetStarterObserved.name=Africa Full`, `targetStarterSelected=true`, `targetStarterOpenAttempted=true`, and `projectOpenObserved=true`.

## Contract test coverage

Use the existing QA contract test structure for this feature:

| Test | Coverage |
| --- | --- |
| `qa/outside-in/alice-desktop/tests/test-schema-contract.sh` | JSON schema accepts the target metadata and evidence contract fields. |
| `qa/outside-in/alice-desktop/tests/test-scenario-validation.sh` | Scenario catalog normalization preserves the committed Africa Full target metadata and rejects missing, absolute, or drifted target paths. |
| `qa/outside-in/alice-desktop/tests/test-select-project-completion-contract.sh` | Select Project completion spec keeps the focused scenario ID/workflow and runner-published target/opened/blocker status fields. |
| `qa/outside-in/alice-desktop/tests/test-select-project-proof.sh` | Select Project window proof preserves exact Java dialog/window context and does not claim project/world interaction. |
| `qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh` | Tab-click probe emits target-specific `Africa Full` opened/blocked evidence and does not click OK/Open without target-specific selection evidence. |
| `qa/outside-in/alice-desktop/tests/test-validator-contract.sh` | Validator enforces `targetStarter`, path safety, workflow allowlists, and scenario-specific Africa Full path constraints. |
| `qa/outside-in/alice-desktop/tests/test-runner-contract.sh` | Runner passes target metadata to the probe without shell construction and preserves argv-backed execution. |
| `qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh` | Post-open gating requires prior target-specific opened evidence and blocks generic main-window proof. |

## Publishing boundary

Publish only one of these outcomes:

| Outcome | Required published content |
| --- | --- |
| Opened | `evidenceStatus=opened`, `targetStarter.displayName=Africa Full`, `targetStarter.repositoryPath=core/resources/src/application/resources/starter-projects/AfricaFull.a3p`, `targetStarterObserved.name=Africa Full`, `targetStarterSelected=true`, `targetStarterOpenAttempted=true`, `openedStarter` matching the same metadata, `projectOpenObserved=true`, and the Alice Java/window PID context. |
| Blocked | One blocker code and detail, current Alice Java/window PID context, Select Project window context, Starters-tab activation state, target observation state, target selection state, OK/Open attempt state, and one structured `nextBlocker`. |

Do not publish full Alice UI automation, Save proof, visible rendering correctness, grading, creative assessment, first-lesson completion, model exporter behavior, unrelated launcher behavior, archive fixture behavior, procedure/edit behavior, unrelated decoder behavior, or coverage measurements from this lane.

## Claim boundaries

This evidence lane proves only what its JSON artifacts state:

- `opened` proves target-specific AT-SPI selection/opening evidence for the committed Africa Full starter and Select Project dismissal.
- `selected` proves target-specific selection evidence but not opening.
- `blocked` proves a reproducible automation gap.
- Main-window AT-SPI state proves accessible frame presence only.

The lane does not prove full Alice UI automation, visible rendering, full project interaction, grading, creative assessment, Save completion, first-lesson completion, unrelated launcher behavior, unrelated decoder behavior, or lesson completion.
