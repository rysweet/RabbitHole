# Open Africa Full through Select Project with AT-SPI

Use the Select Project AT-SPI scenario to prove that the committed `Africa Full` starter was selected/opened, or to capture the exact automation blocker that prevents that proof.

## Prerequisites

Run commands from the repository root.

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

The scenario requires the real Alice desktop accessibility path:

```bash
sudo apt-get install -y python3-pyatspi
test -f /usr/share/java/java-atk-wrapper.jar
```

Use an isolated first-run license state for controlled QA launches:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

## What the scenario targets

The checked-in scenario binds the proof to the starter target in `targetStarter`:

```yaml
targetStarter:
  displayName: Africa Full
  repositoryPath: core/resources/src/application/resources/starter-projects/AfricaFull.a3p
```

The validator rejects missing target metadata, absolute paths, path traversal, and any path other than the committed Africa Full starter path for this scenario. The path is evidence metadata only; the AT-SPI probe does not read or write that file.

## Validate the focused contract

Run only the focused Select Project contract checks:

```bash
export NODE_OPTIONS=--max-old-space-size=32768

qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
bash qa/outside-in/alice-desktop/tests/test-select-project-proof.sh
bash qa/outside-in/alice-desktop/tests/test-tab-click-probe.sh
```

These checks cover the scenario contract, Select Project window proof shape, and target-specific `Africa Full` opened/blocked evidence shape. They do not exercise Save, rendering, grading, lessons, model export, archive fixtures, procedure/edit, or coverage.

## Required action order

The proof must keep the action sequence conservative:

1. Activate the active `Starters` context.
2. Locate `Africa Full` in that active context and record its safe AT-SPI role, state, action, tree path, and selection evidence.
3. Try the target node's supported action first, such as `click` or `activate`.
4. If the target node has no usable action, try the parent selection interface second and record the interface used.
5. Click OK/Open only after target-specific selection evidence exists.

Do not convert a generic Select Project dismissal into Africa Full proof.

## Review successful evidence

Open `status.txt`, then open `tab-click-observation.json` in the same run directory. Treat the Select Project step as proved only when the artifact contains target-specific starter evidence:

```json
{
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
  "projectOpenObserved": true,
  "evidenceStatus": "opened"
}
```

`evidenceStatus=opened` means only that AT-SPI evidence supports selecting/opening `Africa Full` and the Select Project frame was dismissed. It does not prove visible rendering, world interaction, grading, creative assessment, or lesson completion.

The run must also include Alice Java/window context in `x-window-inventory.json` and `select-project-window.json`. The Select Project window context must be the current Java dialog from the same run, not a broad process list or unrelated window.

## Review blocked evidence

If AT-SPI can see the Select Project window but cannot complete target-specific selection/opening, `tab-click-observation.json` must preserve the existing blocker shape with a string `blocker` code and `blockerDetail`, then add structured target-specific details in `nextBlocker`:

```json
{
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterObserved": {
    "name": "Africa Full",
    "role": "panel",
    "states": ["enabled", "visible", "showing"],
    "availableActions": [],
    "parentSelectionAvailable": false,
    "treePath": [0, 3, 1, 0],
    "indexInParent": 0
  },
  "targetStarterSelected": false,
  "targetStarterOpenAttempted": false,
  "projectOpenObserved": false,
  "evidenceStatus": "blocked",
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

A blocked artifact is an acceptable next-step result when it names the observed AT-SPI state, action attempted, expected next action, and reason progress stopped. Do not replace this with generic main-window evidence.

When publishing a blocked result, report exactly one next blocker. Include the current Alice Java/window PID context, Select Project window context, Starters-tab activation state, target observation state, target selection state, and OK/Open attempt state. Do not include a general status dump.

## Evidence hygiene

Evidence and blocker payloads must stay scoped to safe AT-SPI state and scenario metadata. Do not dump unrelated environment variables, process lists, usernames, home paths, tokens, credentials, or arbitrary filesystem paths into `tab-click-observation.json` or post-open artifacts.

## Publish the narrow result

Publish only the Select Project result through default-workflow:

| Result | Publish |
| --- | --- |
| Opened | `evidenceStatus=opened`, exact `Africa Full` target metadata, `targetStarterSelected=true`, `targetStarterOpenAttempted=true`, matching `openedStarter`, `projectOpenObserved=true`, and the Alice Java/window PID context. |
| Blocked | One blocker code/detail plus Alice Java/window PID context, Select Project window context, Starters-tab activation state, target observation state, target selection state, OK/Open attempt state, and the single next exact action. |

Do not publish downstream claims from this proof. The lane does not prove Save completion, visible rendering correctness, grading, creative assessment, first-lesson completion, model export, launcher behavior, archive fixture behavior, procedure/edit behavior, or coverage.
