# Generated Story API Listener Source Characterization

This reference documents the headless NetBeans characterization for generated
Story API source that registers scene event listeners. It proves that a
synthetic Alice project can generate listener registration calls into
`Scene.java` and compile the generated Java without launching Alice or depending
on Sims, nonfree resources, Git LFS payloads, or desktop UI.

## Contents

- [Scope](#scope)
- [Generated-source contract](#generated-source-contract)
- [Executable characterization](#executable-characterization)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Examples](#examples)
- [Tutorial: review listener source generation](#tutorial-review-listener-source-generation)
- [Compatibility rules](#compatibility-rules)
- [Limits](#limits)

## Scope

The characterization belongs to the NetBeans generated-source test suite:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

It covers one narrow behavior slice:

1. Build a deterministic synthetic `Program` and `Scene` AST in memory.
2. Write the project to a temporary `.a3p` archive with `IoUtilities.writeProject`.
3. Generate Java source with `ProjectCodeGenerator.generateCode(..., false)`.
4. Assert that generated `Scene.java` preserves Story API listener registration
   calls.
5. Compile all generated Java source files with the JDK compiler.

This is generated-source characterization, not runtime event dispatch coverage.
It does not start Alice, JavaFX, Swing, NetBeans UI, exported launcher code, or a
desktop event loop.

## Generated-source contract

The listener characterization protects source emitted for scene listener
registration calls on `SScene`.

| Synthetic AST call | Required generated source |
| --- | --- |
| `SScene.addTimeListener(TimeListener, Number, AddTimeListener.Detail...)` with a `null` listener and interval `2` | `this.addTimeListener(null,2);` |
| `SScene.addSceneActivationListener(SceneActivationListener)` with a `null` listener | `this.addSceneActivationListener(null);` |

The calls are placed in a synthetic `Scene` user type assignable to
`org.lgna.story.SScene`. The scene type is reachable through a `Program` field
so `ProjectCodeGenerator` emits both `Program.java` and `Scene.java`.

The accepted output is both textually recognizable and Java-compiler valid. A
test failure means the generated Story API listener source changed, stopped
emitting the listener calls, or no longer compiles against the current Story API
classpath.

## Executable characterization

The executable characterization is:

```text
ProjectCodeGeneratorStoryApiGeneratedSourceTest.generatedSyntheticSceneListenerRegistrationSourceCompiles
```

The test creates only temporary files. The generated project archive, source
directory, and compiled classes directory are managed by JUnit's temporary
folder rule and are not persisted in the repository.

The characterization succeeds when:

1. `Scene.java` is generated from the synthetic project.
2. `Scene.java` contains `this.addTimeListener(null,2);`.
3. `Scene.java` contains `this.addSceneActivationListener(null);`.
4. Every generated `.java` file in the temporary source directory compiles.

## API reference

This feature adds no public Java API. The stable surface is the generated-source
behavior of existing Alice APIs and test helpers.

| Surface | Contract |
| --- | --- |
| `ProjectCodeGenerator.generateCode(File, File, ..., false)` | Generates Java source for the synthetic Alice project without taking the desktop launch path. |
| `IoUtilities.writeProject(File, Project)` | Writes the deterministic synthetic AST fixture to a temporary `.a3p` input. |
| `AstUtilities.lookupMethod(...)` | Resolves the Story API methods used by the fixture instead of hard-coding generated Java text as input. |
| `SScene.addTimeListener(TimeListener, Number, AddTimeListener.Detail...)` | Remains source-generatable as a scene instance call with listener, interval, and optional detail arguments. |
| `SScene.addSceneActivationListener(SceneActivationListener)` | Remains source-generatable as a scene instance call with the listener argument. |

Production generator or runtime behavior should not change for this
characterization unless the test exposes a directly related defect. In that case
the behavior change must be documented and covered by focused tests.

## Configuration

Run commands from the repository root. Initialize the Tweedle grammar submodule
before broad Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

If a surrounding Node-based orchestrator runs the lane, keep the saved memory
setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

No Sims, nonfree modules, Git LFS files, GUI display, Xvfb, network access, or
external Alice project payloads are required for this characterization.

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

### Expected generated `Scene.java` listener body

The characterization accepts generated listener registration source shaped like:

```java
void handleActiveChanged(Boolean isActive, Integer activationCount) {
  this.addTimeListener(null,2);
  this.addSceneActivationListener(null);
}
```

The `null` listeners are intentional. They keep the fixture deterministic and
focused on source generation for listener registration calls rather than on
runtime listener callback behavior.

### Review checklist for generated source

Use this checklist when reviewing changes that affect Story API source
generation:

| Question | Accepted answer |
| --- | --- |
| Does the fixture use synthetic AST input? | Yes, the project is built in memory and written to a temporary `.a3p`. |
| Does the generated source include listener calls on `this`? | Yes, `Scene.java` contains the expected `addTimeListener` and `addSceneActivationListener` calls. |
| Does the test compile generated Java? | Yes, all generated `.java` files compile with the JDK compiler. |
| Does the test launch Alice or a GUI toolkit? | No, it stops at generated-source compilation. |
| Does the test require Sims, LFS, or real project payloads? | No, all inputs are deterministic synthetic fixtures. |

## Tutorial: review listener source generation

Use this flow when changing generated-source behavior near Story API listener
registration.

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

### Step 3: Inspect only the listener contract on failure

If the listener characterization fails, inspect the generated `Scene.java`
assertion first. A useful fix preserves the current listener registration
contract or documents and tests an intentional compatibility change.

Do not broaden the test into runtime execution, GUI launch, exported project
execution, or real `.a3p` corpus loading. Those behaviors belong to separate
lanes.

## Compatibility rules

Changes in this area must preserve these rules:

| Rule | Reason |
| --- | --- |
| Keep the fixture synthetic and deterministic. | The test must run in a normal no-Sims checkout without LFS payloads. |
| Keep the assertion narrow. | The characterization protects listener source generation, not wholesale formatting of generated files. |
| Compile generated Java after text assertions. | Text presence alone does not prove the generated Story API source remains type-correct. |
| Avoid production rewrites for characterization-only changes. | The lane exists to preserve current Alice 3 behavior unless a defect is directly exposed. |
| Do not launch desktop or runtime event paths. | GUI and runtime dispatch behavior require different seams and acceptance evidence. |

## Limits

This characterization does not prove that listeners fire at runtime, that event
callback ordering is correct, or that generated projects launch successfully in
Alice. It proves only that deterministic synthetic Story API listener
registration calls are emitted into generated source and compile in the NetBeans
owning module.
