# Select Project Africa Full AT-SPI evidence reference

This reference defines the intended scenario metadata, runner contract, and JSON evidence for selecting/opening the committed `Africa Full` starter through the Alice Select Project dialog.

It is a feature contract, not a claim that the current runner already emits every field below. The scenario YAML, JSON schema, validator, runner, tab-click probe, post-open probe, and tests must land together before this contract is treated as implemented.

## Target starter metadata

The target-specific feature must add exact starter metadata to `qa/outside-in/alice-desktop/scenarios/select-project-tab-click-exec.yaml`:

| Field | Value |
| --- | --- |
| `targetStarter.displayName` | `Africa Full` |
| `targetStarter.repositoryPath` | `core/resources/src/application/resources/starter-projects/AfricaFull.a3p` |

The validator must reject missing target metadata, absolute repository paths, path traversal, and any repository path other than the committed Africa Full starter path for `alice-desktop-select-project-tab-click-exec`.

## Runner interface

`run-scenario.sh` must extract the target starter metadata from the validated scenario and pass it to `tab-click-probe.py` as runner-managed environment variables:

| Variable | Value for this scenario | Notes |
| --- | --- | --- |
| `TARGET_STARTER_DISPLAY_NAME` | `Africa Full` | Non-empty display label used for AT-SPI target discovery. |
| `TARGET_STARTER_REPO_PATH` | `core/resources/src/application/resources/starter-projects/AfricaFull.a3p` | Relative repository path recorded as evidence metadata. |

The runner must quote both values and execute the existing argv-backed Alice launch path directly. It must not construct shell commands from scenario YAML.

## `tab-click-observation.json`

The Select Project probe must write `tab-click-observation.json`. Existing tab inventory fields remain present, and target starter fields provide the project-specific proof boundary.

| Field | Type | Meaning |
| --- | --- | --- |
| `targetStarter.displayName` | string | Scenario target display name. |
| `targetStarter.repositoryPath` | string | Scenario target repository path. |
| `targetStarterObserved` | object | AT-SPI discovery result for `Africa Full` in the active Starters context. |
| `targetStarterSelected` | boolean | `true` only when the probe has target-specific evidence that `Africa Full` was selected. |
| `targetStarterOpenAttempted` | boolean | `true` only when OK/Open was attempted after target-specific selection evidence. |
| `openedStarter` | object or null | Filled with the `Africa Full` target metadata only when the target-specific open path succeeds. |
| `projectOpenObserved` | boolean | `true` when the Select Project frame is no longer present after the open attempt. |
| `evidenceStatus` | enum | `opened`, `selected`, `blocked`, or `failed`. |
| `blocker` | string or null | Existing machine-readable blocker code. Preserve this field for compatibility with current artifacts and tests. |
| `blockerDetail` | string or object or null | Existing human-readable blocker detail. Preserve this field for compatibility with current artifacts and tests. |
| `targetStarterBlocker` | object or null | Structured target-specific blocker details for `selected`, `blocked`, and `failed` statuses. |

### `targetStarterObserved`

When the target is found, the observation records the AT-SPI node shape:

| Field | Type | Meaning |
| --- | --- | --- |
| `observed` | boolean | Whether an accessible matching `Africa Full` was found. |
| `name` | string | Accessible name. |
| `role` | string | Accessible role name. |
| `states` | string list | Safe state names reported by AT-SPI. |
| `actions` | string list | Available AT-SPI action names, such as `click` or `activate`. |
| `treePath` | integer list | Child indexes from the Select Project frame to the target node. |
| `indexInParent` | integer or null | Target index in its immediate parent when available. |
| `selectionInterfaceAvailable` | boolean | Whether the target or parent exposes a usable selection interface. |

If the target is not found, `targetStarterObserved.observed=false`; `blocker`, `blockerDetail`, and `targetStarterBlocker` explain the active Starters context and discovered candidate names.

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
| `opened` | `Africa Full` was observed, target-specific selection/open was attempted, `openedStarter` records the target metadata, and `projectOpenObserved=true`. |
| `selected` | `Africa Full` selection is supported by evidence, but opening did not complete. The blocker names the remaining open step. |
| `blocked` | AT-SPI automation could not prove target-specific selection/opening. The blocker records the observed state and next action. |
| `failed` | The probe or runtime failed before producing a normal AT-SPI capability result. The blocker records the failure boundary. |

`status=observed` can coexist with `evidenceStatus=blocked` when the probe successfully collected AT-SPI state but could not complete a safe target-specific action.

## Blocker object

Every non-`opened` terminal result must preserve existing blocker compatibility and add target-specific structure:

| Field | Meaning |
| --- | --- |
| `blocker` | Stable machine-readable blocker code, such as `target-starter-selection-blocked`. |
| `blockerDetail` | Concise existing-style explanation for humans and current tests. |
| `targetStarterBlocker` | Structured target-specific blocker evidence. |

`targetStarterBlocker` contains:

| Field | Meaning |
| --- | --- |
| `observedAtspiState` | Final relevant AT-SPI state, including target visibility, role, states, actions, selection interface availability, and active context. |
| `actionAttempted` | The action sequence attempted by the probe. |
| `expectedNextAction` | The next implementation or automation capability needed to continue. |
| `reasonProgressStopped` | Why continuing would overclaim evidence or become unsafe. |

## Evidence hygiene

Evidence and blocker payloads must stay scoped to safe AT-SPI state and scenario metadata. Do not dump unrelated environment variables, process lists, usernames, home paths, tokens, credentials, or arbitrary filesystem paths into `tab-click-observation.json` or post-open artifacts.

## Post-open gating

`post-project-open-probe.py` must consume richer target metadata when present. It treats post-open main-window observation as eligible only when the prior tab-click artifact proves the `Africa Full` target-specific path:

```json
{
  "evidenceStatus": "opened",
  "targetStarterSelected": true,
  "targetStarterOpenAttempted": true,
  "openedStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "projectOpenObserved": true
}
```

If those fields are missing or inconsistent, `post-project-open-observation.json` must record a blocked result rather than converting generic main-window state into Africa Full proof.

## Contract test coverage

Use the existing QA contract test structure for this feature unless the implementation plan explicitly adds new files:

| Test | Coverage |
| --- | --- |
| `qa/outside-in/alice-desktop/tests/test-schema-contract.sh` | JSON schema accepts the target metadata and evidence contract fields. |
| `qa/outside-in/alice-desktop/tests/test-validator-contract.sh` | Validator enforces `targetStarter`, path safety, workflow allowlists, and scenario-specific Africa Full path constraints. |
| `qa/outside-in/alice-desktop/tests/test-runner-contract.sh` | Runner passes target metadata to the probe without shell construction and preserves argv-backed execution. |
| `qa/outside-in/alice-desktop/tests/test-post-project-open-probe.sh` | Post-open gating requires prior target-specific opened evidence and blocks generic main-window proof. |

## Claim boundaries

This evidence lane proves only what its JSON artifacts state:

- `opened` proves target-specific AT-SPI selection/opening evidence for the committed Africa Full starter and Select Project dismissal.
- `selected` proves target-specific selection evidence but not opening.
- `blocked` proves a reproducible automation gap.
- Main-window AT-SPI state proves accessible frame presence only.

The lane does not prove visible rendering, full project interaction, grading, creative assessment, or lesson completion.
