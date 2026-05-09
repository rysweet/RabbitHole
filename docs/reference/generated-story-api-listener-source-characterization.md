# Headless Runtime Dispatch and Generated Story API Listener Source Characterization

This reference defines the bounded headless characterization lane for runtime
event dispatch and generated Story API listener source. It covers two evidence
targets, implemented by three required executable test methods:

1. `core/ast` proves that a small static story method dispatches virtual-machine
   statement events to registered listeners without desktop startup.
2. `netbeans` proves that generated Story API listener registration source is
   emitted, compiles, registers a generated listener, and observes one scene
   activation callback through the existing headless runtime dispatch seam.

**Non-goals:** this lane does not launch Alice desktop, execute full world
playback, assert visible correctness, grade learner work, complete Save
workflows, or provide full UI automation evidence.

## Contents

- [Feature intent](#feature-intent)
- [Runtime Event Dispatch PR Recovery](#runtime-event-dispatch-pr-recovery)
- [Core AST virtual-machine event contract](#core-ast-virtual-machine-event-contract)
- [Generated listener source contract](#generated-listener-source-contract)
- [Headless scene activation dispatch contract](#headless-scene-activation-dispatch-contract)
- [Executable characterization](#executable-characterization)
- [QA scenario metadata](#qa-scenario-metadata)
- [Evidence links](#evidence-links)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Examples](#examples)
- [Tutorial: review the characterization lane](#tutorial-review-the-characterization-lane)
- [Compatibility rules](#compatibility-rules)
- [Limits](#limits)

## Runtime Event Dispatch PR Recovery

PR #403 is recovered on branch `wave6-runtime-event-dispatch-1778302300`.

The recovery scope is intentionally narrow: it validates the
runtime-event-dispatch silver thread using executable current-head evidence. It
does not expand the feature area, does not manually merge the PR, and does not
claim full UI automation, rendering correctness, grading behavior,
creative assessment, or lesson completion.

Resolve the live PR head, then verify that the checked-out branch and HEAD match
that review target before relying on this evidence:

```bash
EXPECTED_PR_HEAD="$(gh pr view 403 --json headRefOid --jq .headRefOid)"
test "$(git rev-parse --abbrev-ref HEAD)" = "wave6-runtime-event-dispatch-1778302300"
test "$(git rev-parse HEAD)" = "$EXPECTED_PR_HEAD"
```

Initialize the required Tweedle grammar submodule:

```bash
git submodule update --init tweedle-lang
```

Run readiness evidence:

```bash
NODE_OPTIONS=--max-old-space-size=32768 qa/outside-in/alice-desktop/runners/validate-scenarios.sh
NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

Run runtime-event-dispatch and generated-listener evidence:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=org.lgna.project.virtualmachine.VirtualMachineHeadlessRuntimeEventTest test -q
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl netbeans -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest test -q
```

Run documentation-boundary evidence:

```bash
python3 -m unittest tests.test_runtime_event_dispatch_docs_contract
EXPECTED_PR_HEAD="$(gh pr view 403 --json headRefOid --jq .headRefOid)"
python3 tests/test_runtime_event_dispatch_docs_contract.py --guard-check --worktree . --expected-branch wave6-runtime-event-dispatch-1778302300 --expected-head "$EXPECTED_PR_HEAD"
```

Do not manually merge PR #403. Finalization must stay tied to executable
current-head evidence from the named readiness, runtime-event-dispatch,
generated listener, and documentation-boundary checks.

No-op justification: when the checked-out HEAD matches the live PR head on
branch `wave6-runtime-event-dispatch-1778302300` and no pending repository
changes remain, no additional repository changes are required for PR #403
recovery when the named current-head checks pass.

## Feature intent

The feature protects current Alice behavior with deterministic, no-Sims tests.
It is intentionally narrower than desktop playback.

The `core/ast` target builds a static `UserMethod` containing a `BlockStatement`
and `Comment`, invokes it through `ReleaseVirtualMachine`, and observes
`VirtualMachineListener` callbacks in execution order. It also proves that
removing the listener stops future event delivery.

The `netbeans` target builds a synthetic Alice project in memory, writes it to a
temporary `.a3p`, generates Java source with `ProjectCodeGenerator`, compiles the
generated files, loads the generated scene, registers the generated scene
activation listener, fires `EventManager.sceneActivated()`, and observes the
generated listener callback with a bounded latch.

Evidence for this lane must come from existing headless runtime seams and
generated source. It must not depend on JavaFX startup, Swing windows, NetBeans
UI, exported launcher execution, full playback, visual rendering, grading, Save
completion, or real project payloads.

## Core AST virtual-machine event contract

The `core/ast` characterization target is:

```text
core/ast/src/test/java/org/lgna/project/virtualmachine/VirtualMachineHeadlessRuntimeEventTest.java
```

It protects the headless virtual-machine listener dispatch contract for a small
story method body.

| Runtime surface | Contract |
| --- | --- |
| `ReleaseVirtualMachine` | Invokes a static story method without gallery assets, JavaFX, Swing, or scene rendering. |
| `VirtualMachineListener` | Receives statement execution callbacks for the invoked AST body while registered. |
| `BlockStatement` | Emits `statementExecuting` before contained statements and `statementExecuted` after them. |
| `Comment` | Emits statement execution callbacks as a simple deterministic child statement. |
| `removeVirtualMachineListener(...)` | Stops subsequent listener delivery for the removed listener. |

The accepted event sequence is:

```text
executing:BlockStatement
executing:Comment
executed:Comment
executed:BlockStatement
```

After listener removal, invoking the same static story method again must not add
new entries to the recording listener. This proves listener registration and
unregistration behavior without broadening the lane into world playback.

## Generated listener source contract

The generated-source characterization target is:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

The source-generation contract protects listener registration emitted for a
synthetic `Scene` type assignable to `org.lgna.story.SScene`.

| Synthetic AST call | Required generated source |
| --- | --- |
| `SScene.addTimeListener(TimeListener, Number, AddTimeListener.Detail...)` with the source-only fixture argument and interval `2` | `Scene.java` contains `this.addTimeListener(null,2);`. |
| `SScene.addSceneActivationListener(SceneActivationListener)` with the source-only fixture argument | `Scene.java` contains `this.addSceneActivationListener(null);`. |
| `SScene.addSceneActivationListener(SceneActivationListener)` with the runtime-dispatch fixture listener | `Scene.java` contains `this.addSceneActivationListener((SceneActivationEvent p0) ->` and the generated lambda calls `ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordSceneActivationRuntimeDispatch(p0);`. |

The synthetic project must be built in memory and written only to temporary test
files. `ProjectCodeGenerator.generateCode(..., false)` emits the Java source.
The characterization then verifies the expected listener registration source and
compiles every generated `.java` file with the JDK compiler.

Generated listener source must remain textually recognizable, compiler-valid,
and usable by the headless dispatch seam. A failure means generated Story API
listener source changed, stopped emitting the expected registration path, no
longer compiles against the current Story API classpath, or no longer connects to
the expected runtime callback path.

## Headless scene activation dispatch contract

Scene activation is the runtime dispatch seam for generated Story API listener
participation.

| Runtime surface | Contract |
| --- | --- |
| `EventManager.sceneActivated()` | Fires the existing headless scene activation dispatch path used by Story API runtime code. |
| `SceneActivationHandler` | Adapts runtime scene activation events to registered Story API scene activation listeners. |
| `AbstractEventHandler` | Provides event handler base behavior for the dispatch path. |
| `ComponentExecutor` | May deliver callbacks asynchronously, so validation waits with a bounded timeout. |
| `CountDownLatch` | Observes callback delivery without unbounded waits or timing-only assertions. |

The generated listener registration path must be invoked before dispatch. The
test must assert that registration alone has not already recorded a payload or
released the latch. After `EventManager.sceneActivated()` fires, the generated
listener must record exactly one callback and a `SceneActivationEvent` payload
from that dispatch.

A direct `SceneActivationHandler.handleEventFire(...)` call may characterize a
lower-level helper, but it does not satisfy this feature. The count, latch, and
payload evidence for this lane must be observed from the generated listener
after `EventManager.sceneActivated()` fires.

## Executable characterization

The feature has two evidence targets: the `core/ast` runtime listener dispatch
target and the `netbeans` generated-source/runtime-dispatch target. Those
targets are represented by these three required executable test methods:

```text
VirtualMachineHeadlessRuntimeEventTest.headlessStaticStoryMethodNotifiesListenerAroundBlockAndCommentStatements
ProjectCodeGeneratorStoryApiGeneratedSourceTest.generatedSyntheticSceneListenerRegistrationSourceCompiles
ProjectCodeGeneratorStoryApiGeneratedSourceTest.generatedSceneActivationListenerParticipatesInHeadlessRuntimeDispatch
```

`headlessStaticStoryMethodNotifiesListenerAroundBlockAndCommentStatements`
succeeds when:

1. A static `UserMethod` containing a `BlockStatement` and `Comment` runs through
   `ReleaseVirtualMachine`.
2. The registered `VirtualMachineListener` records the expected before/after
   statement event sequence.
3. Removing the listener prevents additional event recording on the next
   invocation.

`generatedSyntheticSceneListenerRegistrationSourceCompiles` succeeds when:

1. A deterministic synthetic project generates `Scene.java`.
2. `Scene.java` contains the expected listener registration calls.
3. Every generated `.java` file compiles.

`generatedSceneActivationListenerParticipatesInHeadlessRuntimeDispatch` succeeds
when:

1. Generated source is compiled and loaded by the test.
2. The generated scene and generated listener registration path are invoked
   through explicit reflection targets.
3. Pre-dispatch assertions confirm registration has not synthesized a runtime
   payload or released the latch.
4. `EventManager.sceneActivated()` fires for the generated headless scene.
5. The generated listener callback captures one `SceneActivationEvent` payload
   and releases a `CountDownLatch` before the bounded timeout.

The executable tests create only temporary files. Generated project archives,
source directories, and class directories are managed by JUnit temporary
locations and are not persisted in the repository.

## QA scenario metadata

The Alice desktop QA scenario system exposes this lane through the
`runtime-event-dispatch-smoke` and `generated-listener-runtime-dispatch-smoke`
gated command-smoke workflows.

Workflow values and supported argv allowlists must be updated together with
their contract tests. Keep these files synchronized when changing the scenario
metadata, workflow names, or Maven argv values:

- `qa/outside-in/alice-desktop/runners/validate-scenarios.sh`
- `qa/outside-in/alice-desktop/runners/run-scenario.sh`
- `qa/outside-in/alice-desktop/schema/scenario.schema.json`
- `qa/outside-in/alice-desktop/tests/test-schema-contract.sh`
- `qa/outside-in/alice-desktop/tests/test-workflow-contract.sh`

Scenario metadata must not introduce arbitrary commands, paths, or shell
fragments. The runner accepts only checked-in workflow values and explicit argv
allowlist entries.

## Evidence links

Evidence links for this lane must stay focused on PR #403/runtime-dispatch
characterization evidence: this reference, the focused `core/ast` and
`netbeans` tests, the QA scenario contract tests, and the runtime-dispatch docs
contract. When `docs/index.md` is updated during develop integration, preserve
only focused PR #403/runtime-dispatch references for this lane; do not link this
feature to desktop UI, rendering, playback, Save, grading, or full automation
evidence unless a separate evidence lane documents and tests that broader
behavior.

## API reference

This feature adds no public Java API. It characterizes existing runtime and
generator behavior.

| Surface | Contract |
| --- | --- |
| `ReleaseVirtualMachine.ENTRY_POINT_invoke(...)` | Executes the static story method used by the headless `core/ast` characterization. |
| `ReleaseVirtualMachine.addVirtualMachineListener(...)` | Registers the listener that observes statement execution events. |
| `ReleaseVirtualMachine.removeVirtualMachineListener(...)` | Removes the listener and prevents additional callback recording. |
| `VirtualMachineListener.statementExecuting(...)` | Receives before-execution statement events for the synthetic method body. |
| `VirtualMachineListener.statementExecuted(...)` | Receives after-execution statement events for the synthetic method body. |
| `IoUtilities.writeProject(File, Project)` | Writes the deterministic synthetic project to a temporary `.a3p` input for generated-source tests. |
| `ProjectCodeGenerator.generateCode(File, File, ..., false)` | Generates Java source for the temporary synthetic Alice project while skipping NetBeans formatting in the test helper. |
| `AstUtilities.lookupMethod(...)` | Resolves Story API methods used by the fixture instead of hard-coding generated Java as input. |
| `SScene.addTimeListener(...)` | Remains source-generatable as a scene instance listener registration call. |
| `SScene.addSceneActivationListener(...)` | Remains source-generatable and participates in the headless scene activation seam after registration. |
| `EventManager.sceneActivated()` | Provides the generated-listener runtime dispatch seam without desktop startup or full playback. |
| `CountDownLatch.await(timeout, unit)` | Bounds asynchronous callback validation so the test fails instead of hanging. |

Production generator or runtime behavior should not change for this evidence
lane unless implementation exposes a directly related defect. Any behavior
change must be covered by focused characterization tests and documented as a
separate compatibility decision.

## Configuration

Run commands from the repository root resolved by Git:

```bash
WORKTREE_ROOT="$(git rev-parse --show-toplevel)"
git -C "$WORKTREE_ROOT" branch --show-current
```

The Python TDD/no-op guard is
`tests/test_runtime_event_dispatch_docs_contract.py`. It fails closed when the
checked path is not inside a Git worktree, uses
`git rev-parse --show-toplevel` to resolve the actual linked-worktree root,
verifies the expected branch and PR head, and runs status or no-op checks
with `git -C "$WORKTREE_ROOT" ...`. It must not silently fall back to a parent
directory, a non-git path, or an unlinked workspace.

Initialize the Tweedle grammar submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Use the saved Node memory setting when running automation wrappers around Maven:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

No Sims payloads, Git LFS assets, GUI display, Xvfb, desktop runtime, exported
launcher execution, external Alice project payloads, or Save workflow automation
are required for this characterization. The characterization tests perform no
network I/O; Maven still follows the normal repository dependency
resolution/cache behavior for the checkout. The optional PR-head lookup above
uses GitHub CLI metadata only to bind recovery evidence to the live review head.

## Validation commands

Run the focused `core/ast` headless runtime dispatch characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.virtualmachine.VirtualMachineHeadlessRuntimeEventTest \
  test
```

Run the focused NetBeans generated listener source and dispatch
characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

When Maven reports missing generated Tweedle parser classes, check the submodule
before changing the characterization:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

## Examples

### Virtual-machine event sequence

The `core/ast` test records this sequence for a static story method whose body is
a block containing one comment:

```text
executing:BlockStatement
executing:Comment
executed:Comment
executed:BlockStatement
```

This proves statement listener dispatch for the small method body only. It is
not evidence that a rendered world played correctly.

### Expected generated `Scene.java` listener registration shape

The source-only generated-listener characterization accepts listener
registration shaped exactly like the current executable assertions:

```java
void handleActiveChanged(Boolean isActive,Integer activationCount) {
  this.addTimeListener(null,2);
  this.addSceneActivationListener(null);
}
```

The source-only fixture intentionally uses `null` listener arguments so the
assertion stays narrow: it proves the generator emits the scene instance
registration calls and that the generated Java still compiles. It does not prove
runtime listener dispatch.

The runtime-dispatch fixture uses a generated lambda instead:

```java
this.addSceneActivationListener((SceneActivationEvent p0) ->
    ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordSceneActivationRuntimeDispatch(p0));
```

That lambda is the bridge from generated source to the bounded headless runtime
dispatch assertion. The listener values are fixture-controlled generated-source
inputs. They are not external scripts, user-supplied class names, production API
extensions, or desktop automation hooks.

### Expected headless scene activation observation

The generated-listener runtime characterization loads the generated scene, invokes
the generated registration path, fires `EventManager.sceneActivated()`, then
asserts one callback and a `SceneActivationEvent` payload through the exact
existing runtime seam. This example describes the contract, not a new public API.

### Review checklist

| Question | Accepted answer |
| --- | --- |
| Does `core/ast` run without desktop startup? | Yes, it invokes a static AST method through `ReleaseVirtualMachine`. |
| Does listener removal stop subsequent event recording? | Yes, the second invocation leaves the recorded event list unchanged. |
| Does the generated-source fixture use synthetic AST input? | Yes, the project is built in memory and written to a temporary `.a3p`. |
| Does generated source include listener registration on `this`? | Yes, `Scene.java` contains the expected listener registration calls. |
| Does generated Java compile? | Yes, all generated `.java` files compile with the JDK compiler. |
| Does scene activation evidence use `EventManager.sceneActivated()`? | Yes, dispatch flows through the existing headless runtime seam. |
| Are count and payload observed from the same runtime dispatch? | Yes, the generated listener callback captures both after `sceneActivated()` fires. |
| Is callback validation async-safe? | Yes, a bounded `CountDownLatch` observes the callback and fails on timeout. |
| Does the lane launch Alice or a GUI toolkit? | No, it never starts desktop runtime, JavaFX, Swing, NetBeans UI, or a display loop. |
| Does the lane require Sims, LFS, exported projects, or real project payloads? | No, all inputs are deterministic synthetic fixtures. |

## Tutorial: review the characterization lane

Use this flow when changing virtual-machine listener dispatch, generated Story
API listener source, or scene activation dispatch.

### Step 1: Resolve the linked worktree

From the checkout you intend to validate:

```bash
WORKTREE_ROOT="$(git rev-parse --show-toplevel)"
git -C "$WORKTREE_ROOT" status --short --branch
```

If Git cannot resolve a worktree root, stop and fix the path. Do not treat a
non-git directory as clean or no-op.

### Step 2: Initialize required generated grammar inputs

```bash
git -C "$WORKTREE_ROOT" submodule update --init tweedle-lang
test -d "$WORKTREE_ROOT/tweedle-lang/Grammar"
```

Do not fetch Sims or Git LFS assets for this lane.

### Step 3: Run the focused `core/ast` dispatch proof

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.virtualmachine.VirtualMachineHeadlessRuntimeEventTest \
  test
```

If this fails, inspect listener registration, statement event order, and listener
removal before changing broader runtime code.

### Step 4: Run the focused generated-listener proof

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

If generated-source assertions fail, inspect `Scene.java` first. If compilation
succeeds but the latch is not released, verify that the generated registration
method was invoked and that dispatch still flows through
`EventManager.sceneActivated()`. Do not replace the dispatch proof with direct
listener invocation.

### Step 5: Keep claims bounded

Review notes may claim headless virtual-machine listener dispatch,
compiler-valid generated listener registration source, and one generated scene
activation listener callback through the headless runtime seam. They must not
claim desktop runtime execution, full world playback, visible correctness,
grading, Save completion, exported launcher behavior, or full UI automation.

## Compatibility rules

Changes in this area must preserve these rules:

| Rule | Reason |
| --- | --- |
| Keep the `core/ast` fixture small and static. | The test protects listener dispatch without needing scene state, gallery assets, or playback. |
| Assert listener removal behavior. | The lane protects both registration and unregistration boundaries. |
| Keep generated-source fixtures synthetic and deterministic. | The tests must run in a normal no-Sims checkout without LFS payloads. |
| Keep source assertions narrow. | The characterization protects listener registration source, not wholesale formatting of generated files. |
| Compile generated Java after text assertions. | Text presence alone does not prove generated Story API source remains type-correct. |
| Use the existing scene activation dispatch seam. | The lane proves runtime participation without test-only dispatch bypasses. |
| Validate asynchronous callbacks with a bounded latch. | Dispatch may use executor-backed delivery and must not race or hang CI. |
| Resolve guard paths through Git. | No-op or TDD guards must fail closed outside the real linked worktree. |
| Avoid production rewrites for characterization-only changes. | The lane preserves current Alice behavior unless a directly related defect is exposed. |
| Do not launch desktop, playback, grading, Save, or export paths. | Those claims require separate evidence lanes. |

## Limits

This characterization does not prove that Alice desktop runtime execution works,
that a full world plays back correctly, that a scene renders visibly, that
learner work is graded, that Save completes, that exported launchers run, or that
full UI automation is correct.

It proves only that:

1. a small static AST method dispatches virtual-machine statement events to a
   registered listener in headless execution and stops dispatching to that
   listener after removal;
2. deterministic generated Story API listener registration source compiles; and
3. the generated scene activation listener can participate in one bounded
   headless scene activation dispatch through the existing runtime seam.
