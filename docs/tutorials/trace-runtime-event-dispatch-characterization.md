# Tutorial: Trace the Runtime Event Dispatch Characterization

This tutorial walks through the headless runtime event dispatch and generated
Story API listener source characterization lane, from virtual-machine listener
callbacks to generated listener source to bounded scene activation dispatch.

## Goal

Trace this behavior end to end:

```text
Alice headless runtime event dispatch proves that a small static story method
dispatches virtual-machine statement events to registered listeners, and that
generated Story API listener registration source compiles and participates in
one bounded headless scene activation dispatch.
```

The tutorial uses synthetic test fixtures. It does not require Sims, Git LFS
payloads, a desktop display, Alice gallery assets, or broad project archives.

## Prerequisites

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## 1. Start with the core AST dispatch proof

Open the headless runtime event test:

```text
core/ast/src/test/java/org/lgna/project/virtualmachine/VirtualMachineHeadlessRuntimeEventTest.java
```

Find the test method:

```text
headlessStaticStoryMethodNotifiesListenerAroundBlockAndCommentStatements
```

This test builds a deterministic AST fixture in memory:

1. Creates a static `UserMethod` with a `BlockStatement` body containing a single
   `Comment` statement.
2. Creates a `ReleaseVirtualMachine` instance.
3. Registers a `VirtualMachineListener` that records each callback as
   `"executing:ClassName"` or `"executed:ClassName"`.

**What to observe:** The test invokes the static method through
`ReleaseVirtualMachine.ENTRY_POINT_invoke(...)` and asserts the recorded event
sequence:

```text
executing:BlockStatement
executing:Comment
executed:Comment
executed:BlockStatement
```

This proves that `statementExecuting` fires before contained statements execute,
and `statementExecuted` fires after. The block wraps its child.

## 2. Trace listener removal

After asserting the initial sequence, the test removes the listener:

```java
vm.removeVirtualMachineListener(listener);
```

It then invokes the same static method again and asserts the recorded event list
has not grown. This proves that `removeVirtualMachineListener(...)` stops future
event delivery for the removed listener.

**Why this matters:** The lane protects both registration and unregistration
behavior. A regression in listener removal would cause callbacks to leak to
detached listeners after user code requests removal.

## 3. Move to the generated listener source

Open the Story API generated source test:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

Find the test method:

```text
generatedSyntheticSceneListenerRegistrationSourceCompiles
```

This test builds a synthetic Alice project entirely in memory:

1. Creates a `Program` type (extending `SProgram`) with a `scene` field whose type
   is a synthetic `Scene` extending `SScene`.
2. The `Scene` type has a `handleActiveChanged` method containing
   `addTimeListener(null, 2)` and `addSceneActivationListener(null)` calls
   resolved through `AstUtilities.lookupMethod(...)`.
3. Writes the project to a temporary `.a3p` with `IoUtilities.writeProject(...)`.
4. Generates Java source with `ProjectCodeGenerator.generateCode(..., false)`.

**What to observe:** The test reads the generated `Scene.java` and asserts these
exact fragments:

```java
this.addTimeListener(null,2);
this.addSceneActivationListener(null);
```

It then compiles every generated `.java` file with the JDK compiler. Compilation
failure means generated source is no longer type-correct against the current
Story API classpath.

The `null` listener arguments are intentional. They keep the assertion narrow:
the test proves the generator emits scene-instance registration calls and that
the generated Java compiles. The `null` values do not exercise runtime dispatch.

## 4. Trace the runtime dispatch fixture

In the same test class, find:

```text
generatedSceneActivationListenerParticipatesInHeadlessRuntimeDispatch
```

This test uses a different fixture that generates a real lambda instead of `null`:

```java
this.addSceneActivationListener((SceneActivationEvent p0) ->
    ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordSceneActivationRuntimeDispatch(p0));
```

**What to observe in sequence:**

### 4a. Compilation, class loading, and listener registration

The generated source is compiled with the JDK compiler. The test loads the
generated `Scene` class from the compiled output using a `URLClassLoader`
configured with the compiled classes directory and the current thread's context
class loader as parent, so it sees both generated classes and the Story API
runtime classpath.

The test then instantiates the generated `Scene` and invokes
`handleActiveChanged(Boolean.TRUE, 1)` via reflection. This runs the generated
method body, which calls `addSceneActivationListener(lambda)` — registering the
lambda that records the runtime dispatch event.

### 4b. Pre-dispatch assertion

Before any dispatch fires, the test asserts:

- The runtime dispatch counter is zero.
- The `CountDownLatch` has not been released.
- No `SceneActivationEvent` payload has been recorded.

This proves that registration alone does not synthesize a callback. A failure
here would mean the generated lambda fires during registration rather than
waiting for actual dispatch.

### 4c. Scene activation dispatch

The test obtains the `EventManager` from the scene's implementation
(`scene.getImplementation().getEventManager()`) and invokes `sceneActivated()`
on it. This fires through the existing headless runtime dispatch path:

```text
EventManager.sceneActivated()
  → SceneActivationHandler
    → AbstractEventHandler (base behavior)
      → ComponentExecutor (async delivery)
        → generated lambda callback
```

### 4d. Post-dispatch assertion

After dispatch, the test waits on a bounded `CountDownLatch` and asserts:

- The dispatch counter is exactly one.
- The recorded event is not null and its class is exactly `SceneActivationEvent`
  (verified with `assertSame` on the `Class` object, not instance identity).
- The latch was released before the timeout.

This proves the generated listener receives one callback with the correct payload
through the existing runtime seam.

## 5. Verify the QA scenario metadata

Open the two scenario files:

```text
qa/outside-in/alice-desktop/scenarios/runtime-event-dispatch-smoke.yaml
qa/outside-in/alice-desktop/scenarios/generated-listener-runtime-dispatch-smoke.yaml
```

Each scenario specifies:

- A unique `id` matching the scenario catalog in `alice-desktop-outside-in-qa.md`.
- A `workflow` value (`runtime-event-dispatch-smoke` or
  `generated-listener-runtime-dispatch-smoke`) registered in
  `validate-scenarios.sh` and `scenario.schema.json`.
- An `automation` section with `mode: gated-command-smoke` and exact `argv`
  entries from the runner allowlist.

**What to observe:** The argv entries are the exact focused Maven commands from
the validation section of the reference doc. They are not arbitrary shell
commands. The runner and validator only accept checked-in workflow values and
explicit argv allowlist entries.

## 6. Review the documentation contract

Open the Python contract test:

```text
tests/test_runtime_event_dispatch_docs_contract.py
```

This test enforces that:

- `docs/index.md` links the feature reference with bounded headless wording.
- The reference doc mentions the guard path, Git-root resolution, and fail-closed
  behavior.
- Documentation stays within the headless/non-claim scope — it must not claim
  desktop runtime execution, visible rendering, full playback, grading, or Save
  completion.

**Why this matters:** The contract prevents documentation from drifting beyond
what the executable tests actually prove.

## 7. Connect the pieces

The full lane connects four layers:

| Layer | Purpose | Key file |
| --- | --- | --- |
| Core AST dispatch | Proves listener callback order and removal | `VirtualMachineHeadlessRuntimeEventTest.java` |
| Generated source | Proves listener registration source compiles | `ProjectCodeGeneratorStoryApiGeneratedSourceTest.java` |
| Runtime dispatch | Proves generated listener participates in scene activation | Same test, `generatedSceneActivation...` method |
| QA + docs contract | Keeps scenarios, docs, and claims consistent | `validate-scenarios.sh`, `test_runtime_event_dispatch_docs_contract.py` |

Each layer has intentional boundaries:

- Core AST dispatch does not require generated source, scene state, or gallery
  assets.
- Generated source does not require runtime dispatch (the `null` fixture variant
  tests source shape only).
- Runtime dispatch requires generated source but not desktop UI, playback, or
  GUI toolkit startup.
- QA scenarios drive the same focused Maven commands through the outside-in
  framework.

## 8. Check what this lane does NOT prove

After tracing the full lane, confirm that your review notes stay within scope:

| Claim | In scope? |
| --- | --- |
| Headless VM listener dispatch for a small static method | ✅ Yes |
| Listener removal stops future delivery | ✅ Yes |
| Generated listener registration source compiles | ✅ Yes |
| Generated scene activation listener receives one callback | ✅ Yes |
| Desktop runtime execution | ❌ No |
| Full world playback | ❌ No |
| Visible scene rendering | ❌ No |
| Save, Save As, or project recovery | ❌ No |
| Grading, creative assessment, or lesson completion | ❌ No |
| Exported launcher execution | ❌ No |
| Broad Tweedle/player decode | ❌ No |
| Full UI automation | ❌ No |

If your change requires claims beyond this table, open a separate evidence lane
with its own fixtures, review language, and documentation contract.

## Next steps

- Run the full validation: see [Run the Runtime Event Dispatch
  Characterization](../howto/run-runtime-event-dispatch-characterization.md).
- Review source generation broadly: see [Characterize Source-Code-Generator
  Behavior](../howto/characterize-source-code-generator.md).
- Understand the reference contract: see [Headless Runtime Dispatch and Generated
  Story API Listener Source Characterization
  reference](../reference/generated-story-api-listener-source-characterization.md).
