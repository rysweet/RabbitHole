# Generated Story API Listener Runtime Dispatch Characterization

This reference defines the bounded, headless NetBeans characterization target
for generated Story API listener wiring. The feature must prove that a synthetic
Alice project can generate scene listener registration source, compile that
generated Java, load the generated scene, invoke listener registration, and
observe one callback through the existing runtime scene activation dispatch seam.

The feature is an evidence lane for listener wiring only. It does not launch the
Alice desktop, run full world playback, assert visible correctness, grade learner
work, or claim export completion.

## Contents

- [Feature intent](#feature-intent)
- [Generated-source contract](#generated-source-contract)
- [Headless dispatch contract](#headless-dispatch-contract)
- [Executable characterization](#executable-characterization)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Examples](#examples)
- [Tutorial: review listener runtime dispatch](#tutorial-review-listener-runtime-dispatch)
- [Compatibility rules](#compatibility-rules)
- [Limits](#limits)

## Feature intent

The feature belongs to the NetBeans generated-source test suite:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

It covers one narrow behavior slice:

1. Build a deterministic synthetic `Program` and `Scene` AST in memory.
2. Write the project to a temporary `.a3p` archive with `IoUtilities.writeProject`.
3. Generate Java source with `ProjectCodeGenerator.generateCode(..., false)`.
4. Assert that generated `Scene.java` contains listener registration source.
5. Compile all generated Java source files with the JDK compiler.
6. Load the generated scene class through the test class loader.
7. Invoke the generated listener registration path through explicit reflection
   targets.
8. Fire `EventManager.sceneActivated()`, the existing headless runtime scene
   activation seam.
9. Assert that the generated listener observes the runtime callback, callback
   count, and `SceneActivationEvent` payload from that same dispatch with a
   bounded `CountDownLatch`.

This is generated listener registration plus headless runtime dispatch
characterization. Passing evidence must come from generated source, generated
Java compilation, explicit listener registration, and the existing scene
activation dispatch path. It must not start Alice, JavaFX, Swing, NetBeans UI,
exported launcher code, a desktop event loop, full world playback, grading, or
visible rendering checks.

## Generated-source contract

The characterization protects source emitted for scene listener registration
calls on `SScene`.

| Synthetic AST call | Required generated source |
| --- | --- |
| `SScene.addTimeListener(TimeListener, Number, AddTimeListener.Detail...)` with a fixture listener and interval `2` | `this.addTimeListener(...,2);` shape with generated listener registration on `this` |
| `SScene.addSceneActivationListener(SceneActivationListener)` with a fixture listener | `this.addSceneActivationListener(...);` shape with generated listener registration on `this` |

Scene activation is the runtime dispatch seam for this lane. Time listener source
may remain in the generated fixture to preserve the existing source-generation
contract, but the headless runtime assertion observes scene activation dispatch.

The calls are placed in a synthetic `Scene` user type assignable to
`org.lgna.story.SScene`. The fixture method is named `handleActiveChanged`,
accepts `Boolean isActive` and `Integer activationCount`, and contains generated
listener registration code. The scene type is reachable through a `Program` field
so `ProjectCodeGenerator` emits both `Program.java` and `Scene.java`.

The accepted output is textually recognizable, Java-compiler valid, and usable by
the headless dispatch seam. A test failure means generated Story API listener
source changed, stopped emitting the listener registration path, no longer
compiles against the current Story API classpath, or no longer participates in
the expected scene activation callback path.

## Headless dispatch contract

The runtime portion uses existing Alice runtime types only. It does not add a
test-only dispatch bypass or production behavior.

| Runtime surface | Contract |
| --- | --- |
| `EventManager.sceneActivated()` | Fires the existing headless scene activation dispatch path used by Story API runtime code. |
| `SceneActivationHandler` | Adapts scene activation events from the runtime dispatch path to registered Story API scene activation listeners. |
| `AbstractEventHandler` | Provides the event handler base behavior used by the runtime dispatch path. |
| `ComponentExecutor` | Delivers callbacks asynchronously, so validation must wait with a bounded timeout. |
| `CountDownLatch` | Observes callback delivery without sleeping indefinitely or depending on timing-only assertions. |

The generated listener must record enough callback evidence to prove dispatch
participation:

1. The callback count reaches the expected value.
2. The latch is released before the bounded timeout.
3. The callback receives a `SceneActivationEvent` payload object from the same
   `EventManager.sceneActivated()` dispatch that releases the latch.
4. Payload assertions stay within the existing event type. The current
   `SceneActivationEvent` exposes no scene-specific public properties, so the
   required payload assertion is object delivery and type.

A direct `SceneActivationHandler.handleEventFire(...)` invocation may be useful
as a lower-level helper characterization, but it does not satisfy this feature's
runtime dispatch contract. Count, latch, and payload evidence for this lane must
all be observed from the generated listener after `EventManager.sceneActivated()`
is fired.

Cleanup is explicit. Registered listener state must not leak into other tests,
and executor-backed asynchronous work must be allowed to complete before the
temporary class loader and generated classes are discarded.

## Executable characterization

The target executable characterizations are:

```text
ProjectCodeGeneratorStoryApiGeneratedSourceTest.generatedSyntheticSceneListenerRegistrationSourceCompiles
ProjectCodeGeneratorStoryApiGeneratedSourceTest.generatedSceneActivationListenerParticipatesInHeadlessRuntimeDispatch
```

`generatedSyntheticSceneListenerRegistrationSourceCompiles` preserves the
generated-source and compiler-validity contract. It succeeds when:

1. `Scene.java` is generated from the synthetic project.
2. `Scene.java` contains the expected listener registration calls.
3. Every generated `.java` file in the temporary source directory compiles.

`generatedSceneActivationListenerParticipatesInHeadlessRuntimeDispatch` is the
runtime feature test. It succeeds when:

1. Generated source is compiled and loaded by the test.
2. The generated scene or generated listener registration method is invoked
   through explicit reflection targets.
3. `EventManager.sceneActivated()` is fired for the generated/headless scene.
4. The generated scene activation listener callback captures the delivered
   `SceneActivationEvent` object and releases a `CountDownLatch` before the
   bounded timeout.
5. The observed callback count and payload type assertions match the expected
   single scene activation dispatch.

Split tests that separately prove `EventManager.sceneActivated()` callback
delivery and direct `SceneActivationHandler.handleEventFire(...)` payload
delivery are transitional evidence only. They should be consolidated or extended
before this feature is considered complete, because direct handler payload
delivery does not prove generated listener participation in the runtime dispatch
seam.

The tests create only temporary files. The generated project archive, source
directory, and compiled classes directory are managed by JUnit's temporary
folder rule and are not persisted in the repository.

## API reference

This feature adds no public Java API and should not require production generator
or runtime API changes. The stable surface is the behavior of existing Alice APIs
when exercised by generated Story API source and test helpers.

| Surface | Contract |
| --- | --- |
| `ProjectCodeGenerator.generateCode(File, File, ..., false)` | Generates Java source for the synthetic Alice project while skipping NetBeans formatting in the test helper. |
| `IoUtilities.writeProject(File, Project)` | Writes the deterministic synthetic AST fixture to a temporary `.a3p` input. |
| `AstUtilities.lookupMethod(...)` | Resolves the Story API methods used by the fixture instead of hard-coding generated Java text as input. |
| `SScene.addTimeListener(TimeListener, Number, AddTimeListener.Detail...)` | Remains source-generatable as a scene instance call with listener, interval, and optional detail arguments. |
| `SScene.addSceneActivationListener(SceneActivationListener)` | Remains source-generatable as a scene instance call with the listener argument and participates in the headless scene activation seam after registration. |
| `EventManager.sceneActivated()` | Provides the runtime dispatch seam for the characterization without desktop startup or full playback. |
| `CountDownLatch.await(timeout, unit)` | Bounds asynchronous callback validation so the test fails instead of hanging. |

Production generator or runtime behavior should not change for this evidence lane
unless implementation exposes a directly related defect. In that case the
behavior change must be documented and covered by focused characterization tests.

## Configuration

Run commands from the repository root. Initialize the Tweedle grammar submodule
before broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

If a surrounding Node-based orchestrator runs the lane, keep the saved memory
setting. Maven does not consume this setting directly, but the automation wrapper
does:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

No Sims, nonfree modules, Git LFS files, GUI display, Xvfb, network access,
external Alice project payloads, desktop runtime, or exported launcher execution
are required for this characterization.

## Validation commands

Run the focused NetBeans characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

When Maven reports missing generated Tweedle parser classes, check the submodule
before changing generated-source tests:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

## Examples

### Expected generated `Scene.java` listener registration shape

The characterization accepts generated listener registration source shaped like:

```java
void handleActiveChanged(Boolean isActive, Integer activationCount) {
  this.addTimeListener(timeListener, 2);
  this.addSceneActivationListener(sceneActivationListener);
}
```

The listener variables are fixture-controlled generated-source inputs. They are
not external scripts, user-supplied class names, file paths, or production API
extensions.

### Expected headless dispatch observation

The runtime characterization follows this shape:

```java
CountDownLatch callbackObserved = new CountDownLatch(1);
AtomicReference<SceneActivationEvent> observedEvent = new AtomicReference<>();

Scene scene = loadGeneratedScene();
invokeGeneratedListenerRegistration(scene, callbackObserved, observedEvent);

EventManager eventManager = getExistingRuntimeEventManager(scene);
eventManager.sceneActivated();

assertTrue(callbackObserved.await(5, TimeUnit.SECONDS));
assertEquals(1, observedCallbackCount.get());
assertTrue(observedEvent.get() instanceof SceneActivationEvent);
```

The example shows the contract, not a new public API. Implementations should use
the exact existing method signatures and payload types exposed by the runtime
seam.

### Review checklist

Use this checklist when reviewing changes that affect generated Story API
listener wiring:

| Question | Accepted answer |
| --- | --- |
| Does the fixture use synthetic AST input? | Yes, the project is built in memory and written to a temporary `.a3p`. |
| Does generated source include listener registration on `this`? | Yes, `Scene.java` contains the expected listener registration calls. |
| Does generated Java compile? | Yes, all generated `.java` files compile with the JDK compiler. |
| Does the runtime assertion use `EventManager.sceneActivated()`? | Yes, dispatch flows through the existing headless runtime seam. |
| Are count and payload observed from the same runtime dispatch? | Yes, the generated listener callback captures both after `EventManager.sceneActivated()` fires. |
| Is callback validation async-safe? | Yes, a bounded `CountDownLatch` observes the callback and fails on timeout. |
| Does the test launch Alice or a GUI toolkit? | No, it never starts desktop runtime, JavaFX, Swing, NetBeans UI, or a display loop. |
| Does the test require Sims, LFS, exported projects, or real project payloads? | No, all inputs are deterministic synthetic fixtures. |

## Tutorial: review listener runtime dispatch

Use this flow when changing generated-source behavior near Story API listener
registration or scene activation dispatch.

### Step 1: Start from a no-Sims checkout

From the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Do not fetch Git LFS assets or Sims payloads for this test. The fixture creates
the Alice project it needs.

### Step 2: Run the focused test class

Run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

### Step 3: Inspect the generated-source contract first

If source assertions fail, inspect generated `Scene.java` before runtime
dispatch. A useful fix preserves the listener registration contract or documents
and tests an intentional compatibility change.

### Step 4: Inspect registration before dispatch

If compilation succeeds but the latch is not released, verify that the generated
scene or generated registration method is loaded and invoked through the expected
reflection targets. Do not replace the dispatch call with direct listener
invocation or direct `SceneActivationHandler.handleEventFire(...)`; that would
prove only payload shape, not participation in the runtime dispatch seam.

### Step 5: Keep failures bounded

Use a bounded latch timeout and assert the callback count and
`SceneActivationEvent` payload after the latch releases. Do not use unbounded
waits, arbitrary sleeps as the only proof, or desktop playback to make the
callback happen.

Do not broaden the test into GUI launch, exported project execution, visible
rendering, grading, or real `.a3p` corpus loading. Those behaviors belong to
separate lanes.

## Compatibility rules

Changes in this area must preserve these rules:

| Rule | Reason |
| --- | --- |
| Keep the fixture synthetic and deterministic. | The test must run in a normal no-Sims checkout without LFS payloads. |
| Keep the source assertion narrow. | The characterization protects listener source generation, not wholesale formatting of generated files. |
| Compile generated Java after text assertions. | Text presence alone does not prove generated Story API source remains type-correct. |
| Use the existing scene activation dispatch seam. | The lane should prove runtime participation without adding test-only dispatch bypasses. |
| Validate asynchronous callbacks with a bounded latch. | Dispatch may use executor-backed delivery and must not race or hang CI. |
| Avoid production rewrites for characterization-only changes. | The lane exists to preserve current Alice 3 behavior unless a defect is directly exposed. |
| Do not launch desktop, playback, grading, or export paths. | Those claims require separate seams and acceptance evidence. |

## Limits

This characterization does not prove that Alice desktop playback works, that a
world renders correctly, that learner work is graded, that exported launchers are
complete, or that all event ordering cases are correct. It proves only that
deterministic generated Story API listener registration source compiles and can
participate in one bounded headless scene activation dispatch through the
existing runtime seam.