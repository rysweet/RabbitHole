# Review the Run-Window Creation/Wiring Contract

Use this guide to review the focused Run-window evidence lane. The lane verifies
only that the Run-window creation hook can write the bounded
`run-window-created.json` artifact through the expected seam.

For the complete artifact and API contract, see the [Run-Window
Creation/Wiring Contract reference](../reference/run-window-creation-wiring-contract.md).

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

Do not add timeout wrappers around the commands in this guide.

## Validate the focused Java seam

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

Review the test as the executable contract for:

| Assertion area | What it protects |
| --- | --- |
| JSON shape | Schema version, `created` status, creation/wiring scope, evidence source, fixed artifact name, and explicit false claim booleans. |
| Escaping | Frame title and program type metadata cannot break JSON output. |
| Path safety | Parent traversal, nested artifact paths, absolute artifact paths, missing directories, symlink evidence directories, and pre-existing artifact symlinks fail closed. |
| Non-claims | The artifact names unsupported active rendering, run execution, world execution correctness, rendering correctness, Save behavior, grading, creative assessment, lesson completion, and full UI automation boundaries. |

## Validate scenario wiring

Prepare the QA scenario without executing the gated command:

```bash
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --prepare-only \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-window-contract
```

The evidence directory contains:

```text
qa/outside-in/alice-desktop/evidence/run-window-contract/alice-desktop-run-window-contract/<timestamp>/
  environment.txt
  status.txt
  manual-evidence-checklist.txt
```

`status.txt` must show:

```text
scenario=alice-desktop-run-window-contract
automationMode=gated-command-smoke
outcome=gated-not-run
skipMode=prepare-only
```

This prepare-only output verifies scenario wiring and checklist generation. It
does not show that `run-window-created.json` was written.

## Run the gated scenario

When the reviewer wants the QA runner to execute the focused seam command, run:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-run-window-contract \
  --evidence-dir qa/outside-in/alice-desktop/evidence/run-window-contract
```

The runner executes only this Maven argv:

```text
mvn -DincludeSims=false -Dinstall4j.skip -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -pl core/ide -am -Dtest=org.alice.tools.EatmeRunWindowEvidenceTest test
```

Do not broaden the selector, replace the argv with a shell string, launch a full
desktop automation workflow, or add rendering/run-execution probes to satisfy
this scenario.

## Review a success artifact

When a product-side run is configured with
`-Dorg.alice.eatme.runWindowEvidenceDir=<existing-dir>`, the seam writes:

```text
<existing-dir>/run-window-created.json
```

Check these fields:

```text
schema_version=eatme.alice-run-window-created/v1
status=created
contract_scope=run-window-creation-wiring
evidence_source=org.alice.stageide.run.RunComposite#handlePreShowWindow
artifact=run-window-created.json
```

Every capability boolean must remain false:

```text
active_rendering_claimed=false
run_program_claimed=false
run_execution_claimed=false
world_execution_claimed=false
rendering_correctness_claimed=false
save_claimed=false
grading_claimed=false
full_ui_automation_claimed=false
```

The `does_not_claim` array must include:

```text
active-rendering
run-execution
world-execution-correctness
rendering-correctness
save
grading
full-ui-automation
```

## Keep the review narrow

Accept this lane only as Run-window creation/wiring evidence. Do not cite it for
active rendering, run execution, world execution correctness, visible rendering
correctness, Save behavior, grading, creative assessment, lesson completion, or
full UI automation.
