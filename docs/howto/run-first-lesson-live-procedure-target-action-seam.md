# Run the First-Lesson Live Procedure Target Action Seam

Use this guide to collect the read-only evidence that the post-Select-Project
Alice desktop exposes the `scene.eatmeFirstLesson` procedure/code-editor target
and classifies the next desktop edit action as ready or blocked by the named
public edit-action contract gap.

For the artifact API, field definitions, accepted blocker values, examples, and
claim boundaries, see the [First-Lesson Live Procedure Target Action Seam
reference](../reference/first-lesson-live-procedure-target-observation.md).

## Before you start

Run commands from the repository root.

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

The live shard needs the controlled-display prerequisites used by the Alice
desktop QA lane:

| Requirement | Purpose |
| --- | --- |
| Java 21 and Maven | Launch Alice through the existing AT-SPI Maven path. |
| Xvfb | Provide the isolated display session. |
| `python3-pyatspi` and AT-SPI2 | Inspect the live desktop accessibility tree. |
| `/usr/share/java/java-atk-wrapper.jar` | Expose Swing widgets to AT-SPI. |
| `ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1` | Prepare isolated first-run license acceptance state for QA automation. |

## Validate the scenario catalog

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

The catalog must include:

```text
alice-desktop-first-lesson-live-procedure-target-observation
```

## Run the action-seam shard

```bash
rm -rf /tmp/alice-first-lesson-live-procedure-target
ALICE_QA_ACCEPT_LICENSES_FOR_TESTS=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-first-lesson-live-procedure-target-observation \
  --evidence-dir /tmp/alice-first-lesson-live-procedure-target \
  --timeout-seconds 300
```

The runner creates a timestamped run directory under:

```text
/tmp/alice-first-lesson-live-procedure-target/alice-desktop-first-lesson-live-procedure-target-observation/
```

## Review the evidence

Open the newest timestamped run directory and inspect:

```text
first-lesson-live-procedure-target-observation.json
status.txt
tab-click-observation.json
post-project-open-observation.json
x-window-inventory.json
launch.log
xvfb.log
```

`tab-click-observation.json` must show that Select Project opened the configured
first-lesson starter. `post-project-open-observation.json` must show that the
post-open Alice main window was observed before the procedure target probe ran.

Accept the action-seam artifact only when it has one of these two shapes:

| Outcome | Required fields |
| --- | --- |
| Edit ready | `status=edit-ready`, `observedTarget.procedureName=scene.eatmeFirstLesson`, `observedTarget.readyForDesktopEditAction=true`, `desktopEditAction.status=ready`, `desktopEditAction.blocker.kind=none`, top-level `blocker.kind=none`, and `downstreamBlockedStep=none`. |
| Named no-go | `status=blocked`, `observedTarget.procedureName=scene.eatmeFirstLesson`, `observedTarget.readyForDesktopEditAction=false`, `desktopEditAction.blocker.kind=missing-desktop-edit-action-contract`, top-level `blocker.kind=missing-desktop-edit-action-contract`, `blocker.message=missing public CodeEditor/CodeComposite edit invocation contract`, and `downstreamBlockedStep=desktop-procedure-edit-action-proof`. |

Display, AT-SPI, Select Project, post-open, and target-not-found blockers are
structured run failures. They are useful diagnostics, but they are not accepted
action-seam proof for the next desktop edit shard.

## Check the decision artifact

Use Python as an acceptance check for the two action-seam outcomes. This check
intentionally rejects structured run-failure diagnostics; those artifacts explain
why the live shard could not reach the procedure target, but they are not proof
that the action seam is edit-ready or blocked by the named edit contract gap.
A blocked artifact must still include `observedTarget`; `observedTarget=null`
means the run stopped before the action seam was reached.

```bash
artifact=/tmp/alice-first-lesson-live-procedure-target/alice-desktop-first-lesson-live-procedure-target-observation/<timestamp>/first-lesson-live-procedure-target-observation.json

python3 - "$artifact" <<'PY'
import json
import sys

artifact = json.load(open(sys.argv[1], encoding="utf-8"))
assert artifact["schemaVersion"] == "eatme.first-lesson-live-procedure-target-observation/v1"
assert artifact["scenario"] == "alice-desktop-first-lesson-live-procedure-target-observation"
assert artifact["workflow"] == "first-lesson-live-procedure-target-observation"
assert artifact["seam"] == "live-first-lesson-procedure-target-to-desktop-edit-action"
assert artifact["requiredTarget"]["procedureName"] == "scene.eatmeFirstLesson"
assert artifact["status"] in {"edit-ready", "blocked"}

blocker = artifact["blocker"]
desktop = artifact["desktopEditAction"]
target = artifact.get("observedTarget")
if artifact["status"] == "edit-ready":
    assert artifact["downstreamBlockedStep"] == "none"
    assert isinstance(target, dict)
    assert target["procedureName"] == "scene.eatmeFirstLesson"
    assert target["readyForDesktopEditAction"] is True
    assert blocker == {"kind": "none", "message": ""}
    assert desktop["status"] == "ready"
    assert desktop["blocker"] == blocker
else:
    assert artifact["downstreamBlockedStep"] == "desktop-procedure-edit-action-proof"
    assert isinstance(target, dict)
    assert target["procedureName"] == "scene.eatmeFirstLesson"
    assert target["readyForDesktopEditAction"] is False
    assert blocker["kind"] == "missing-desktop-edit-action-contract"
    assert blocker["message"] == "missing public CodeEditor/CodeComposite edit invocation contract"
    assert desktop["blocker"] == blocker

for excluded in (
    "desktop procedure edit mutation",
    "Save",
    "rendering correctness",
    "learner assessment",
    "creative assessment",
    "full first-lesson completion",
):
    assert excluded in artifact["outOfScope"]
PY
```

Replace `<timestamp>` with the actual run directory name.

## Keep the claim narrow

This shard may claim only that the configured first-lesson starter opened through
Select Project, the post-open desktop was observed, and the
`scene.eatmeFirstLesson` procedure/code-editor target is either edit-ready or
blocked by the exact missing public edit-action contract.

Do not cite this evidence as proof of desktop procedure mutation, AST editing
through the live desktop, Save or Save As behavior, rendering correctness,
learner assessment, grading, creative assessment, or full first-lesson
completion.
