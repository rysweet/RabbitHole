# Open Africa Full through Select Project with AT-SPI

Use the Select Project AT-SPI scenario to advance beyond the main-window proof and either prove that the committed `Africa Full` starter was selected/opened or capture the exact automation blocker.

This page describes the target-specific contract for the feature to build. Until the matching scenario, schema, validator, runner, and probe changes land together, treat the `targetStarter` and `evidenceStatus` fields below as the intended review contract rather than current runner output.

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

After the target-specific implementation lands, use an isolated first-run license state for controlled QA launches:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

## What the scenario targets

The feature will extend the checked-in scenario with the starter target in `targetStarter`:

```yaml
targetStarter:
  displayName: Africa Full
  repositoryPath: core/resources/src/application/resources/starter-projects/AfricaFull.a3p
```

The validator must reject missing target metadata, absolute paths, path traversal, and any path other than the committed Africa Full starter path for this scenario. The path is evidence metadata only; the AT-SPI probe must not read or write that file.

## Required action order

The proof must keep the action sequence conservative:

1. Activate the active `Starters` context.
2. Locate `Africa Full` in that active context and record its safe AT-SPI role, state, action, tree path, and selection evidence.
3. Try the target node's supported action first, such as `click` or `activate`.
4. If the target node has no usable action, try the parent selection interface second and record the interface used.
5. Click OK/Open only after target-specific selection evidence exists.

Do not convert a generic Select Project dismissal into Africa Full proof.

## Review successful evidence

Open `tab-click-observation.json` in the run directory. Treat the Select Project step as proved only when the implemented artifact contains target-specific starter evidence:

```json
{
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterObserved": {
    "observed": true,
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

## Review blocked evidence

If AT-SPI can see the Select Project window but cannot complete target-specific selection/opening, `tab-click-observation.json` must preserve the existing blocker shape with a string `blocker` code and `blockerDetail`, then add structured target-specific details in `targetStarterBlocker`:

```json
{
  "targetStarter": {
    "displayName": "Africa Full",
    "repositoryPath": "core/resources/src/application/resources/starter-projects/AfricaFull.a3p"
  },
  "targetStarterObserved": {
    "observed": true,
    "name": "Africa Full",
    "role": "panel",
    "states": ["enabled", "visible", "showing"],
    "actions": [],
    "selectionInterfaceAvailable": false,
    "treePath": [0, 3, 1, 0],
    "indexInParent": 0
  },
  "targetStarterSelected": false,
  "targetStarterOpenAttempted": false,
  "projectOpenObserved": false,
  "evidenceStatus": "blocked",
  "blocker": "target-starter-selection-blocked",
  "blockerDetail": "Africa Full is visible in the active Starters context but exposes no click/activate action and no usable parent selection interface.",
  "targetStarterBlocker": {
    "observedAtspiState": "Africa Full panel is visible in the active Starters context but exposes no click/activate action and no usable parent selection interface.",
    "actionAttempted": "Activated Starters tab, located Africa Full, inspected actions and parent selection support.",
    "expectedNextAction": "Provide a supported AT-SPI selection/click path for the Africa Full starter before clicking OK/Open.",
    "reasonProgressStopped": "Opening without target-specific selection evidence would be a generic Select Project dismissal, not Africa Full proof."
  }
}
```

A blocked artifact is an acceptable next-step result when it names the observed AT-SPI state, action attempted, expected next action, and reason progress stopped. Do not replace this with generic main-window evidence.

## Evidence hygiene

Evidence and blocker payloads must stay scoped to safe AT-SPI state and scenario metadata. Do not dump unrelated environment variables, process lists, usernames, home paths, tokens, credentials, or arbitrary filesystem paths into `tab-click-observation.json` or post-open artifacts.

## Continue to the post-open proof

Run the post-open window-state scenario only after `tab-click-observation.json` records target-specific `Africa Full` evidence with `evidenceStatus=opened` and `projectOpenObserved=true`:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-project-open-window-state \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-project-open
```

`post-project-open-observation.json` characterizes AT-SPI main-window state after the Select Project frame is dismissed. It does not upgrade a generic dismissal into Africa Full proof; the target-specific evidence must already exist in `tab-click-observation.json`.
