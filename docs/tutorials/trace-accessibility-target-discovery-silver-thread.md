# Tutorial: Trace the Accessibility Target Discovery Silver Thread

This tutorial walks through the accessibility target discovery silver-thread
contract: a focused executable check that validates bounded launch, run/runtime,
and Select Project target discovery evidence and structured blockers across the
Alice desktop outside-in QA lane.

## What you will do

You will:

1. Run the focused silver-thread contract.
2. Inspect the launch evidence lane.
3. Inspect the runtime target discovery lane.
4. Inspect the Select Project target discovery lane.
5. Review structured blockers.
6. Check claim boundaries.
7. Collect fresh runtime and Select Project evidence (optional).

## Before you start

Open a terminal at the repository root and confirm prerequisites:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Set the standard memory option:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

The silver-thread contract is a static executable check over checked-in
artifacts. It does not require Xvfb, AT-SPI, or a running Alice instance. Steps
1–6 work in any environment with Bash and the repository checkout.

## Step 1: Run the focused contract

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/tests/test-accessibility-target-discovery-silver-thread.sh
```

The contract checks scenario metadata, runner wiring, probe field names,
structured blocker fields, and bounded scope wording across all three lanes. It
exits `0` and prints:

```text
accessibility target discovery silver-thread contract satisfied
```

A missing required file, marker, scenario field, probe field, blocker field, or
overclaimed wording makes the script exit non-zero.

## Step 2: Inspect the launch evidence lane

The contract validates the launch lane through `launch.yaml` and launch runner
expectations. Open the scenario:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json \
  alice-desktop-launch
```

Confirm these launch evidence markers are present in the output:

| Marker | What it proves |
| --- | --- |
| `root-directory-prep.json` | Runner records exec root-directory preparation. |
| Launch log | Runner preserves process output for review. |
| `x-window-inventory.json` | X window discovery is structured. |
| `application-root-error.json` | Application-root failures are named, not collapsed. |
| `license-dialog.json` | License-dialog blockers are explicit. |
| `controlled-display-pixel-observation.json` | Display observation is separated from rendering claims. |
| Exit or timeout record | Launch termination reason is recorded. |

The launch lane keeps the silver thread honest: launch must distinguish ready,
splash-only, crashed, blocked, or manual fallback states.

## Step 3: Inspect the runtime target discovery lane

The runtime lane lives in:

```text
qa/outside-in/alice-desktop/scenarios/post-open-runtime-display-accessibility-evidence.yaml
qa/outside-in/alice-desktop/runners/post-open-runtime-display-probe.py
```

Dump the scenario:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json \
  alice-desktop-post-open-runtime-display-accessibility-evidence
```

The contract checks that the probe and runner expose these target discovery
fields:

| Field | Meaning |
| --- | --- |
| `runtimeDisplayCandidateCount` | Number of AT-SPI candidates inspected. |
| `runtimeDisplayCandidates` | Safe summaries with name, role, path, states. |
| `geometryStatus` | Geometry readiness for target-scoped sampling. |
| `screenExtents` | Screen-coordinate geometry of the candidate. |
| `worldCanvasPixelTarget.status=target-ready` | Exactly one valid target identified. |

When no target is available, the runner writes a structured blocker:

```json
{
  "status": "blocked",
  "missingTarget": "run-window-world-canvas-screen-extents",
  "exactNextUnblocker": "reliable-run-window-world-canvas-pixel-sampling-target"
}
```

Open the probe source to see how candidates are collected:

```bash
head -40 qa/outside-in/alice-desktop/runners/post-open-runtime-display-probe.py
```

The probe traverses the AT-SPI tree for visible runtime/display candidates and
emits bounded candidate summaries. It does not click, save, execute worlds, or
assert visual correctness.

## Step 4: Inspect the Select Project target discovery lane

The Select Project lane lives in:

```text
qa/outside-in/alice-desktop/scenarios/select-project-tab-click-exec.yaml
qa/outside-in/alice-desktop/runners/tab-click-probe.py
```

Dump the scenario:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh --dump-json \
  alice-desktop-select-project-tab-click-exec
```

Confirm the lane is bound to the committed `Africa Full` starter:

| Field | Expected value |
| --- | --- |
| `targetStarter.displayName` | `Africa Full` |
| `targetStarter.repositoryPath` | `core/resources/src/application/resources/starter-projects/AfricaFull.a3p` |

The contract checks the full target discovery chain:

```text
targetStarterObserved
  → targetStarterSelected=true
  → targetStarterOpenAttempted=true
  → openedStarter
  → projectOpenObserved=true
```

When progress stops, the blocker artifact names:

| Blocker field | What it records |
| --- | --- |
| `nextBlocker.observedAtspiState` | The AT-SPI state at the point of failure. |
| `nextBlocker.actionAttempted` | The last action tried. |
| `nextBlocker.expectedNextAction` | The action needed to unblock. |
| `nextBlocker.reasonProgressStopped` | Why the lane stopped. |

## Step 5: Review structured blockers

The silver thread enforces fail-closed semantics: every missing prerequisite
becomes a named blocker artifact.

Check that the contract rejects generic error messages. For each lane, blocked
evidence must:

1. Name the exact missing target or dependency.
2. Name the exact next unblocker.
3. Use structured JSON fields, not freeform log text.
4. Set `visibleRenderingCorrectnessEstablished=false` when pixel sampling context
   is present.

The nonclaim boundary is documented in
[Visible rendering evidence nonclaim contract](../reference/visible-rendering-evidence-nonclaim-contract.md).

## Step 6: Check claim boundaries

The contract enforces narrow scope wording. Confirm the contract rejects any of
these overclaims:

- Full UI automation
- Visual correctness or visible rendering correctness
- Rendering correctness
- World execution correctness or full world execution
- General accessibility compliance
- Learner grading, scoring, or lesson completion
- Save behavior
- Decoder behavior

A passing contract supports only this statement: the repository contains
executable validation that launch, run/runtime, and Select Project lanes expose
the expected accessibility target discovery evidence and structured blockers.

See [Accessibility Target Discovery Silver-Thread Contract](../reference/accessibility-target-discovery-silver-thread.md)
for the complete claim boundary list and extension rules.

## Step 7: Collect fresh evidence (optional)

If you need fresh desktop evidence beyond the static contract, collect runtime
and Select Project evidence under Xvfb with AT-SPI.

### Fresh runtime evidence

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-post-open-runtime-display-accessibility-evidence \
  --evidence-dir qa/outside-in/alice-desktop/evidence/post-open-runtime-display \
  --timeout-seconds 300
```

Review `post-open-runtime-display-accessibility-evidence.json`. If the target is
blocked, review `visible-rendering-pixel-target-blocker.json` for the exact
blocker.

### Fresh Select Project evidence

```bash
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-select-project-tab-click-exec \
  --evidence-dir qa/outside-in/alice-desktop/evidence/select-project-africa-full
```

Review `tab-click-observation.json`. A valid opened result requires all five
fields in the target discovery chain from Step 4.

## What you proved

After completing Steps 1–6, you confirmed:

1. The silver-thread contract passes on checked-in artifacts.
2. Launch evidence separates ready, blocked, and fallback states.
3. Runtime target discovery exposes candidate, geometry, target-ready, and blocker
   fields.
4. Select Project target discovery is bound to the committed `Africa Full` starter
   and exposes the full observation-to-open chain.
5. Blocked evidence uses structured named blockers, not generic errors.
6. Claim wording stays within the bounded target discovery scope.

These results do not prove full UI automation, visual correctness, rendering
correctness, world execution, accessibility compliance, grading, Save behavior,
or decoder behavior.
