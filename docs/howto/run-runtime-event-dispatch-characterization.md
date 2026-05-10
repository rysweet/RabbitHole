# Run the Runtime Event Dispatch Characterization

Use this guide to run and review the headless runtime event dispatch and
generated Story API listener source characterization. This lane validates two
evidence targets without desktop startup, GUI toolkits, Sims, or Git LFS assets.

For the full contract, see the [Headless Runtime Dispatch and Generated Story API
Listener Source Characterization
reference](../reference/generated-story-api-listener-source-characterization.md).

## When to use this guide

Use this guide for changes near:

```text
core/ast/src/main/java/org/lgna/project/virtualmachine/ReleaseVirtualMachine.java
core/ast/src/test/java/org/lgna/project/virtualmachine/VirtualMachineHeadlessRuntimeEventTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

Also run these checks when changing:

- `VirtualMachineListener` callback dispatch in `core/ast`;
- `EventManager.sceneActivated()` or `SceneActivationHandler` runtime dispatch;
- generated `Scene.java` listener registration source in `ProjectCodeGenerator`;
- Story API listener types (`SceneActivationListener`, `TimeListener`) or event
  payload classes.

Do not use this guide for desktop UI automation, visible rendering, Save
workflows, grading, full Tweedle decode, or broad playback evidence. Those claims
require their own evidence lanes.

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the core AST dispatch proof

This validates that a small static story method dispatches virtual-machine
statement events to a registered listener and stops dispatching after listener
removal:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.virtualmachine.VirtualMachineHeadlessRuntimeEventTest \
  test
```

Expected outcome: the test records the event sequence
`executing:BlockStatement → executing:Comment → executed:Comment → executed:BlockStatement`
and confirms that a removed listener receives no further callbacks.

## Run the generated listener source and dispatch proof

This validates that generated Story API listener registration source compiles and
that the generated scene activation listener participates in headless runtime
dispatch:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

Expected outcome: generated `Scene.java` contains listener registration calls,
all generated `.java` files compile, the generated scene activation listener
receives exactly one `SceneActivationEvent` callback after
`EventManager.sceneActivated()` fires, and a bounded `CountDownLatch` observes
the callback without hanging.

## Run the QA scenario smoke tests

The two dispatch scenarios are validated by the outside-in QA framework:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh
```

This validates all scenario YAML files including
`runtime-event-dispatch-smoke` and
`generated-listener-runtime-dispatch-smoke`. Both use the
`gated-command-smoke` automation mode with explicit Maven argv allowlists.

## Run the documentation contract tests

The Python contract test enforces that documentation stays consistent with the
executable characterization:

```bash
python3 -m unittest tests.test_runtime_event_dispatch_docs_contract
```

This checks that `docs/index.md` links the feature reference with bounded
headless wording, that the reference doc mentions the guard path and fail-closed
behavior, and that documentation stays within the headless/non-claim scope.

## Review the results

### Core AST dispatch

| Assertion | Meaning |
| --- | --- |
| Event sequence matches expected order | `ReleaseVirtualMachine` dispatches `statementExecuting` and `statementExecuted` callbacks in statement-tree order. |
| Event list unchanged after listener removal | `removeVirtualMachineListener(...)` stops delivery for the removed listener. |
| No desktop startup required | The static method runs through `ReleaseVirtualMachine` without JavaFX, Swing, or scene rendering. |

### Generated listener source (source-shape assertions)

| Assertion | Meaning |
| --- | --- |
| `Scene.java` contains listener registration calls | `addTimeListener(null,2)` and `addSceneActivationListener(null)` source is present. |
| Generated `.java` files compile | All files compile against the current Story API classpath. |

### Generated listener runtime dispatch (dispatch assertions)

| Assertion | Meaning |
| --- | --- |
| Runtime-dispatch lambda is generated | `addSceneActivationListener((SceneActivationEvent p0) -> ...)` connects to the test's static recorder. |
| Pre-dispatch count is zero | Registration alone does not synthesize a callback. |
| Post-dispatch count is one | `EventManager.sceneActivated()` delivers exactly one callback. |
| Payload type is `SceneActivationEvent` | The recorded event's class is exactly `SceneActivationEvent` (type check, not instance identity). |
| Latch completes within bounded timeout | Asynchronous delivery is validated without unbounded waits. |

## Troubleshooting

### Missing Tweedle grammar classes

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

If the submodule is uninitialized, Maven will fail with missing parser classes.
Always initialize before running characterization.

### Listener event sequence mismatch

If the `core/ast` test fails with an unexpected event order, inspect
`ReleaseVirtualMachine.invoke(...)` and the `VirtualMachineListener` dispatch
path. The fixture expects `BlockStatement` events to wrap `Comment` events.

### Generated source assertion failure

If `Scene.java` does not contain the expected listener registration, inspect
`ProjectCodeGenerator.generateCode(...)` and the Story API method lookup in
`AstUtilities.lookupMethod(...)`. The fixtures use synthetic AST input, so a
failure usually means the generator changed its output shape.

### Latch timeout without callback

If the generated listener latch times out, verify that
`EventManager.sceneActivated()` fires through the same runtime seam and that the
generated registration method was invoked before dispatch. Do not replace the
dispatch proof with a direct handler invocation.

## What this guide does NOT cover

- Desktop runtime execution or GUI toolkit startup
- Full world playback or visible scene rendering
- Save, Save As, or project recovery workflows
- Grading, creative assessment, or lesson completion
- Broad Tweedle/player decode coverage
- Exported launcher execution
- Full UI automation evidence

These behaviors require separate evidence lanes with their own fixtures and
review language.
