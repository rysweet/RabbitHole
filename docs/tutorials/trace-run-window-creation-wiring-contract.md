# Tutorial: Trace the Run-Window Creation/Wiring Contract

This tutorial walks through the focused Run-window evidence contract from the
product seam to the generated JSON artifact.

## What you will do

1. Prepare the repository for focused `core/ide` validation.
2. Run `EatmeRunWindowEvidenceTest`.
3. Trace the evidence writer and fixed artifact shape.
4. Review path-safety and JSON-escaping checks.
5. Stop at the explicit non-claim boundary.

## Before you start

Open a terminal at the repository root:

```bash
java -version
mvn -version
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Run the characterization

Run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/ide -am \
  -Dtest=org.alice.tools.EatmeRunWindowEvidenceTest \
  test
```

This test is intentionally small. It does not drive the full UI, render a scene,
run the world, save a project, grade learner work, or check lesson completion.

## Step 2: Locate the seam

The evidence source is:

```text
org.alice.stageide.run.RunComposite#handlePreShowWindow
```

The Run-window hook calls:

```java
EatmeRunWindowEvidence.recordRunWindowCreated(frame, programType);
```

The call records evidence only when the JVM property
`org.alice.eatme.runWindowEvidenceDir` names an existing directory. If the
property is unset or blank, no artifact is written.

## Step 3: Trace the fixed artifact

The artifact name is fixed:

```text
run-window-created.json
```

The contract scope is fixed:

```text
run-window-creation-wiring
```

The schema version is fixed:

```text
eatme.alice-run-window-created/v1
```

The artifact records optional display metadata:

```json
{
  "frame_title": "Run Alice",
  "program_type": "Scene"
}
```

Those fields are evidence metadata, not rendering evidence.

## Step 4: Follow the safety checks

Read the negative tests as part of the contract:

| Check | Why it matters |
| --- | --- |
| Reject `../run-window-created.json` | Prevents evidence writes outside the configured directory. |
| Reject `nested/run-window-created.json` | Keeps the artifact API to one fixed file. |
| Reject `/tmp/run-window-created.json` | Prevents absolute-path escape. |
| Reject missing evidence directory | Avoids silently creating unreviewed evidence roots. |
| Reject pre-existing artifact symlink | Prevents writing through a symlink to another location. |
| Escape quotes, backslashes, whitespace, and control characters | Keeps display metadata from corrupting deterministic JSON. |

If any safety check weakens, the Run-window contract is not satisfied even if a
JSON file is produced.

## Step 5: Check non-claims

The artifact must keep these booleans false:

```json
{
  "active_rendering_claimed": false,
  "run_program_claimed": false,
  "run_execution_claimed": false,
  "world_execution_claimed": false,
  "rendering_correctness_claimed": false,
  "save_claimed": false,
  "grading_claimed": false,
  "full_ui_automation_claimed": false
}
```

The `does_not_claim` list must include:

```text
active-rendering
run-execution
world-execution-correctness
rendering-correctness
save
grading
full-ui-automation
```

These fields are not boilerplate. They are part of the evidence boundary and must
stay reviewable in every success artifact.

## Step 6: Connect the outside-in scenario

The QA scenario for this lane is:

```text
alice-desktop-run-window-contract
```

The default review path is prepare-only:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-window-contract
```

Prepare-only mode creates `status.txt` and `manual-evidence-checklist.txt` with
`outcome=gated-not-run`. Use that output to review scenario wiring. Use the
focused Java characterization to verify the artifact contract.

## Step 7: Stop at the seam

This tutorial covers only Run-window creation/wiring evidence. It does not cover
active rendering, run execution, world execution correctness, rendering
correctness, Save behavior, grading, creative assessment, lesson completion, or
full UI automation.

The excluded claims are:

- Active rendering.
- Run execution.
- World execution correctness.
- Visible rendering correctness.
- Save behavior.
- Grading, scoring, or creative assessment.
- Lesson completion.
- Full UI automation.

For task-oriented review commands, see [Review the Run-Window Creation/Wiring
Contract](../howto/review-run-window-creation-wiring-contract.md). For the full
artifact contract, see the [Run-Window Creation/Wiring Contract
reference](../reference/run-window-creation-wiring-contract.md).
