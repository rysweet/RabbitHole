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
workflows, or provide full UI automation evidence. It does not prove broad
Tweedle/player decoding or full first-lesson completion.

## Contents

- [Feature intent](#feature-intent)
- [Runtime Event Dispatch PR Recovery](#runtime-event-dispatch-pr-recovery)
- [Core AST virtual-machine event contract](#core-ast-virtual-machine-event-contract)
- [Generated listener source contract](#generated-listener-source-contract)
- [Headless scene activation dispatch contract](#headless-scene-activation-dispatch-contract)
- [Generated-source contract](#generated-source-contract)
- [Headless event-seam contract](#headless-event-seam-contract)
- [Executable characterization](#executable-characterization)
- [QA scenario metadata](#qa-scenario-metadata)
- [Evidence links](#evidence-links)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Tutorial: review the characterization lane](#tutorial-review-the-characterization-lane)
- [Review evidence handoff](#review-evidence-handoff)
- [Generated-source specimen reference](#generated-source-specimen-reference)
- [Review checklist: source generation](#review-checklist-source-generation)
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

Refresh the live review, merge-state, and check evidence immediately before
reporting readiness:

```bash
gh pr view 403 --json number,title,headRefName,headRefOid,mergeStateStatus,reviewDecision,statusCheckRollup
gh pr checks 403
```

Classify the PR as merge-ready only when `headRefOid` still matches
`EXPECTED_PR_HEAD`, `mergeStateStatus` is not blocking, `reviewDecision` is not
blocking, and `statusCheckRollup` plus `gh pr checks 403` show that GitHub
checks are green for the live PR head.

No-op justification: when the checked-out HEAD matches the live PR head on
branch `wave6-runtime-event-dispatch-1778302300` and no pending repository
changes remain, no additional repository changes are required for PR #403
recovery when the named current-head checks pass and no scoped defect is found.
The final report must include `No-op` and tie the no-op to the live PR head,
GitHub checks, `mergeStateStatus`, `reviewDecision`, and the bounded
runtime-event-dispatch evidence above.

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
The characterization is split between the AST generator and the NetBeans project
generator:

```text
core/ast/src/test/java/org/lgna/project/ast/SourceCodeGeneratorTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorGeneratedSourceTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStandaloneProjectTest.java
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
The lane covers this bounded workflow:

1. Build deterministic AST fixtures in memory.
2. Generate Java source with `JavaCodeGenerator` or
   `ProjectCodeGenerator.generateCode(..., false)`.
3. Assert representative generated Java snippets for statements, expressions,
   members, Story API calls, listener registrations, and launcher-adjacent source.
4. Compile generated `.java` files with the JDK compiler where the owning test
   creates a complete temporary source tree.
5. Exercise only narrow headless seams for generated Story API listener lambdas
   and payload propagation.

The fixture inputs are synthetic and temporary. The tests do not launch the Alice
desktop, require Sims or Git LFS payloads, decode a broad project corpus, run a
student lesson, or validate rendered world output.

## Generated-source contract

### Core AST source snippets

`SourceCodeGeneratorTest` protects representative Java snippets emitted by the
core AST generator. The protected surface includes:

| AST fixture | Required generated-source behavior |
| --- | --- |
| `ForEachInArrayLoop` with a cached placeholder item name `COUNT__` | Repairs the item variable before header and body emission, producing `for(String itemA : new String[]{"red", "blue"})` and `final String copy=itemA;` without `COUNT__`. |
| `ForEachInArrayLoop` with explicit item name `item`, and `ForEachInIterableLoop` over a local iterable | Preserves the explicit item name in loop headers and local access bodies. |
| Local declaration, expression statement, conditional, count loop, while loop, return, `DoTogether`, and disabled statement fixtures | Emit stable representative Java snippets, including disabled code inside the existing block-comment form and the current non-lambda runnable fallback for `DoTogether`. |
| String, integer, float, double, null, type, array, field access, logical, arithmetic, relational, conditional, assignment, static method-call, and instance method-call expressions | Emit Java literals, operators, member access, and invocation snippets that remain valid source and preserve special primitive names such as `Integer.MAX_VALUE`, `Float.NaN`, and `Double.NEGATIVE_INFINITY`. |
| Named user type with constructor, method, getter, and field | Emits a compact, compiler-shaped class body with default organizer behavior. |

These are golden snippets, not a promise that every possible AST node or every
formatting style is frozen. Reviewers should treat failures as source-generation
compatibility changes that need either a focused fix or an intentional,
documented test update.

### NetBeans generated project source

`ProjectCodeGeneratorGeneratedSourceTest` protects source emitted for synthetic
Alice projects after `ProjectCodeGenerator.generateCode(...)` writes temporary
project source. The generated source must remain compiler-valid for:

| Fixture category | Required generated source |
| --- | --- |
| User methods and parameters | Method declarations, local declarations, parameter access, and method invocations compile in generated `Program.java`. |
| Control flow | Conditional statements, count loops, while loops, array `for-each`, iterable `for-each`, and repaired cached loop item names compile without leaking `COUNT__`. |
| Import folding preference | Generated import fold markers follow the current Alice option preference when the test sets it. |
| Export launcher companion | Generated project source can be compiled with the generated launcher when the test owns the temporary classpath and JavaFX stubs. |

The generated project source contract is compileability plus selected textual
shape. It is not a generated formatting contract for every whitespace choice.

### Story API generated source

`ProjectCodeGeneratorStoryApiGeneratedSourceTest` protects source for current
Story API calls that are important to exported source generation:

| Synthetic AST call | Required generated source |
| --- | --- |
| `SProgram.setSimulationSpeedFactor(Number)` with `1.5` | `this.setSimulationSpeedFactor(1.5);` in `Program.java`. |
| `SProgram.setActiveScene(SScene)` with `null` | `this.setActiveScene(null);` in `Program.java`. |
| `SProgram.setActiveScene(this.scene)` plus model calls | `this.setActiveScene(this.scene);`, `this.box.setPaint(Color.RED);`, `this.box.setOpacity(0.5);`, and `this.box.say("hello box");`. |
| `SScene.setAtmosphereColor(Color)` and `SScene.setFogDensity(Number)` | `this.setAtmosphereColor(Color.BLUE);` and `this.setFogDensity(0.25);` in `Scene.java`. |
| `SScene.addTimeListener(TimeListener, Number, AddTimeListener.Detail...)` with a `null` listener and interval `2` | `this.addTimeListener(null,2);` in `Scene.java`. |
| `SScene.addSceneActivationListener(SceneActivationListener)` with a `null` listener | `this.addSceneActivationListener(null);` in `Scene.java`. |
| Lambda-backed scene activation and time listeners | Generated lambdas use the listener event parameter shape and compile against the current Story API listener types. |

The generated `Scene` fixtures use a method named `handleActiveChanged` with
`Boolean isActive` and `Integer activationCount` parameters. The runtime-seam
tests assert the generated declaration fragment
`public void handleActiveChanged(Boolean isActive,Integer activationCount)`.
That method name and parameter shape are fixture input. They do not imply that
all Alice scene activation behavior, animation scheduling, rendering, or world
execution has been validated.

## Headless event-seam contract

The Story API test class also contains narrow headless runtime probes. These
probes compile generated source, load only the generated `Program` or `Scene`
class under test, and invoke explicit seams without launching Alice desktop UI.

| Seam | Accepted evidence |
| --- | --- |
| Generated `configureStory()` method | Direct invocation updates `SProgram.getSimulationSpeedFactor()` to `1.5` under a headless `ProgramImp` test double. |
| Generated scene activation listener lambda | After the fixture registers the listener, invoking the implementation event handler seam trips the test latch. |
| Scene activation event payload | The generated listener receives a payload whose class is exactly `SceneActivationEvent` (type check via `assertSame` on the `Class` object, not instance identity). |
| Generated time listener lambda | Activating and updating the implementation timer seam trips the test latch. |
| Time listener elapsed payload | The generated listener receives the elapsed-time value from `TimeEvent.getTimeSinceLastFire()`. |

These probes prove generated listener source can connect to specific headless
implementation seams. They do not prove full event-loop scheduling, visible UI
behavior, lesson completion, grading, Save behavior, or complete world runtime
correctness.

## Executable characterization

The focused executable tests are:

```text
SourceCodeGeneratorTest
ProjectCodeGeneratorTest
ProjectCodeGeneratorGeneratedSourceTest
ProjectCodeGeneratorStoryApiGeneratedSourceTest
```

The generated-source suite used for changes that reach generated `Program.java`,
`Scene.java`, listener payloads, or source compileability also includes
`ProjectCodeGeneratorStandaloneProjectTest`.

The tests create only temporary files. Generated `.a3p` inputs, source
directories, compiled classes, and marker files are managed by JUnit temporary
folders and are not committed.

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

This lane adds no new public Java API. It characterizes existing runtime and
generator behavior.

| Surface | Contract |
| --- | --- |
| `ReleaseVirtualMachine.ENTRY_POINT_invoke(...)` | Executes the static story method used by the headless `core/ast` characterization. |
| `ReleaseVirtualMachine.addVirtualMachineListener(...)` | Registers the listener that observes statement execution events. |
| `ReleaseVirtualMachine.removeVirtualMachineListener(...)` | Removes the listener and prevents additional callback recording. |
| `VirtualMachineListener.statementExecuting(...)` | Receives before-execution statement events for the synthetic method body. |
| `VirtualMachineListener.statementExecuted(...)` | Receives after-execution statement events for the synthetic method body. |
| `JavaCodeGenerator` | Emits Java source snippets for core AST statements, expressions, and user types. |
| `IoUtilities.writeProject(File, Project)` | Writes deterministic synthetic AST fixtures to temporary `.a3p` inputs. |
| `ProjectCodeGenerator.generateCode(File, File, ..., false)` | Generates Java source for deterministic synthetic Alice projects while the tests skip NetBeans formatting. |
| `ProjectCodeGenerator.generateLauncher(File)` | Writes the generated exported-project launcher source used by NetBeans generator characterization. |
| `AstUtilities.lookupMethod(...)` | Resolves Story API methods used by fixtures rather than hard-coding Java reflection metadata. |
| `SScene.addTimeListener(...)` | Remains source-generatable as a scene instance listener registration call. |
| `SScene.addSceneActivationListener(...)` | Remains source-generatable and participates in the headless scene activation seam after registration. |
| `SProgram`, `SScene`, `SModel`, listener event classes | Provide the existing Story API methods and event payload types used by generated source fixtures. |
| `EventManager.sceneActivated()` | Provides the generated-listener runtime dispatch seam without desktop startup or full playback. |
| `CountDownLatch.await(timeout, unit)` | Bounds asynchronous callback validation so the test fails instead of hanging. |

Production generator or runtime behavior should not change for this evidence
lane unless implementation exposes a directly related defect. Any behavior
change must be covered by focused characterization tests and documented as a
separate compatibility decision.

## Configuration

Run commands from the repository root. Initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
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

The focused characterization does not require Sims, nonfree
modules, Git LFS files, GUI display, Xvfb, network access, or external Alice
project payloads.

Run the focused core AST generator characterization:

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
  -Dtest=SourceCodeGeneratorTest \
  test
```

Run the focused NetBeans source and exported-project generator characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorTest \
  test
```

When a change touches generated `Program.java`, `Scene.java`, or Story API
listener fixtures, run the generated-source suite as well:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorTest,org.alice.netbeans.project.ProjectCodeGeneratorGeneratedSourceTest,org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest,org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

When Maven reports missing generated Tweedle parser classes, check the submodule
before changing the characterization:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

## Review evidence handoff

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
Repository documentation defines the stable source-generator contract. It is not
the place for point-in-time PR status, timestamps, commit SHAs, or copied CI
logs.

When a review workflow requires source-generator readiness evidence, record that
evidence in the workflow-owned evidence file:

```text
.copilot-evidence/default-workflow-attempt.log
```

Accepted review evidence for this lane is bounded to current-head executable
facts:

| Surface | Evidence note |
| --- | --- |
| Branch and PR metadata | Read-only `git` and `gh pr view` output for the current branch, PR state, draft state, merge state, and visible checks. |
| Changed-file scope | `git diff --name-only origin/develop...HEAD` or the workflow's equivalent current-head diff scope. |
| Core AST validation | The focused `SourceCodeGeneratorTest` command from [Validation commands](#validation-commands). |
| NetBeans generator validation | The focused `ProjectCodeGeneratorTest` command from [Validation commands](#validation-commands), plus the generated-source suite when those fixtures changed. |
| Evidence log | The workflow-owned log records what was executed and what exited successfully; durable docs link to the contract instead of copying transient results. |
| Bounded non-claims | Explicit exclusion of full UI automation, visible rendering correctness, grading, creative assessment, lesson completion, broad Tweedle/player decode, and full world execution. |

Treat failed commands, dirty unexpected implementation changes, merge conflicts,
or failed required checks as review blockers that need a focused fix or a
documented workflow no-op decision. Do not convert those blockers into
repository documentation unless the stable source-generator contract itself
changes.

## Generated-source specimen reference

These specimens are reference fragments for the focused assertions. Where the
text says "asserted", match the generated source exactly; where it says
"shape-only", use the specimen to understand intent without treating every
whitespace choice as frozen.

### Repaired cached loop item name

The AST fixture:

```java
ForEachInArrayLoop loop = forEachLoop("COUNT__");
```

is accepted only when generated source repairs both the loop header and body
access. The asserted fragments are:

```java
for(String itemA : new String[]{"red", "blue"}) {
  final String copy=itemA;
}
```

`COUNT__` must not appear in the generated source.

### Escaped Java string literal

A string literal containing a newline, tab, quote, and backslash is emitted as
valid Java source. The asserted fragment is:

```java
"line1\n\t\"quote\"\\backslash"
```

### Listener registration source

The listener registration fixture accepts generated `Scene.java` source with
the exact asserted fragments shown here:

```java
public void handleActiveChanged(Boolean isActive,Integer activationCount) {
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
The `null` listeners are intentional in this fixture. They keep the generated
source deterministic and focused on source generation for listener registration
calls.

### Lambda listener payload source

A payload-probe fixture accepts generated source with this shape-only fragment:

```java
this.addSceneActivationListener((SceneActivationEvent p0) ->
  ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordSceneActivationEventPayload(p0));
```

The test then invokes the handler seam directly and asserts that the same event
object reaches the generated listener body.

## Review checklist: source generation

Use this checklist when changing source generation near the AST generator,
NetBeans project generator, Story API calls, or listener registration.

### 1. Start from a no-Sims checkout

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
Do not fetch Git LFS assets or Sims payloads for these tests. The fixtures create
the Alice projects they need.

### 2. Run focused characterization first

Run the core AST command, then the focused NetBeans command from
[Validation commands](#validation-commands). A useful review starts from the
smallest failing generated snippet or generated file.

### 3. Inspect generated source before runtime seams

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
For Story API listener failures, inspect the generated `Scene.java` assertion
first. Runtime seam failures are useful only after the generated source compiles
and contains the expected listener lambda or method call.

### 4. Keep stronger claims in separate lanes

Do not broaden this documentation or these tests into full UI automation, visible
rendering correctness, Save completion, grading, broad Tweedle/player decode, or
full first-lesson completion. Those behaviors need their own fixtures, evidence
artifacts, and review language.

## Compatibility rules

Changes in this area must preserve these rules:

| Rule | Reason |
| --- | --- |
| Keep the `core/ast` fixture small and static. | The test protects listener dispatch without needing scene state, gallery assets, or playback. |
| Assert listener removal behavior. | The lane protects both registration and unregistration boundaries. |
| Keep generated-source fixtures synthetic and deterministic. | The tests must run in a normal no-Sims checkout without LFS payloads. |
| Keep source assertions narrow. | The characterization protects listener registration source, not wholesale formatting of generated files. |
| Keep assertions focused on representative generated-source shape and compileability. | The lane protects source generation without freezing unrelated formatting. |
| Compile generated Java after text assertions. | Text presence alone does not prove generated Story API source remains type-correct. |
| Use the existing scene activation dispatch seam. | The lane proves runtime participation without test-only dispatch bypasses. |
| Keep headless runtime probes seam-level and explicit. | The probes validate generated listener wiring without overclaiming world execution. |
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
2. deterministic core AST snippet generation and NetBeans generated source
   compileability are preserved, including selected Story API call source;
3. deterministic generated Story API listener registration source compiles; and
4. the generated scene activation listener can participate in one bounded
   headless scene activation dispatch through the existing runtime seam.
