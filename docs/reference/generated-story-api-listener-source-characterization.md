# Generated Story API and AST Source Characterization

This reference documents the RabbitHole PR 423 source-code-generator
characterization lane. The lane proves deterministic Java source generation from
synthetic Alice AST fixtures, generated NetBeans project source compileability,
and a small set of headless listener/event seams. It does not prove full Alice UI
automation, full world execution, visible rendering correctness, Save
completion, grading, broad Tweedle/player decoding, or full first-lesson
completion.

## Contents

- [Feature intent](#feature-intent)
- [Generated-source contract](#generated-source-contract)
- [Headless event-seam contract](#headless-event-seam-contract)
- [Executable characterization](#executable-characterization)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Validation commands](#validation-commands)
- [Generated-source specimen reference](#generated-source-specimen-reference)
- [Review checklist: source generation](#review-checklist-source-generation)
- [Compatibility rules](#compatibility-rules)
- [Limits](#limits)

## Feature intent

The characterization is split between the AST generator and the NetBeans project
generator:

```text
core/ast/src/test/java/org/lgna/project/ast/SourceCodeGeneratorTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorGeneratedSourceTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

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
| `ForEachInArrayLoop` with explicit item name `item` | Preserves the explicit name in both the loop header and local access body. |
| Local declaration, conditional, count loop, while loop, return, and disabled statement fixtures | Emit stable representative Java snippets, including disabled code inside the existing block-comment form. |
| String, integer, float, double, null, type, array, logical, arithmetic, relational, and conditional expressions | Emit Java literals and operators that remain valid source and preserve special primitive names such as `Integer.MAX_VALUE`, `Float.NaN`, and `Double.NEGATIVE_INFINITY`. |
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
| Scene activation event payload | The generated listener receives the exact `SceneActivationEvent` instance fired through the handler seam. |
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
ProjectCodeGeneratorGeneratedSourceTest
ProjectCodeGeneratorStoryApiGeneratedSourceTest
```

The tests create only temporary files. Generated `.a3p` inputs, source
directories, compiled classes, and marker files are managed by JUnit temporary
folders and are not committed.

The characterization succeeds when:

1. Core AST fixtures produce the protected Java snippets.
2. Synthetic NetBeans project source is generated into temporary directories.
3. Generated source contains the expected Story API and listener calls.
4. Generated `.java` files compile with the JDK compiler.
5. Headless listener seams receive the expected latch or payload evidence.

## API reference

This lane adds no new public Java API. The stable surface is generated-source
behavior of existing Alice APIs and test-owned helpers.

| Surface | Contract |
| --- | --- |
| `JavaCodeGenerator` | Emits Java source snippets for core AST statements, expressions, and user types. |
| `ProjectCodeGenerator.generateCode(File, File, ..., false)` | Generates Java source for deterministic synthetic Alice projects while the tests skip NetBeans formatting. |
| `ProjectCodeGenerator.generateLauncher(File)` | Writes the generated exported-project launcher source used by NetBeans generator characterization. |
| `IoUtilities.writeProject(File, Project)` | Writes deterministic synthetic AST fixtures to temporary `.a3p` inputs. |
| `AstUtilities.lookupMethod(...)` | Resolves Story API methods used by fixtures rather than hard-coding Java reflection metadata. |
| `SProgram`, `SScene`, `SModel`, listener event classes | Provide the existing Story API methods and event payload types used by generated source fixtures. |

Production generator or runtime behavior should not change for this evidence lane
unless the implementation exposes a directly related source-generation defect. In
that case, the behavior change must be documented and covered by focused tests.

## Configuration

Run commands from the repository root. Initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Keep the saved Node memory setting for wrapper-driven validation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

The focused source-generator characterization does not require Sims, nonfree
modules, Git LFS files, GUI display, Xvfb, network access, or external Alice
project payloads.

## Validation commands

Run the focused core AST generator characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SourceCodeGeneratorTest \
  test
```

Run the focused NetBeans source and exported-project generator characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorTest,org.alice.netbeans.project.ProjectCodeGeneratorGeneratedSourceTest,org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest,org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

When Maven reports missing generated Tweedle parser classes, check the submodule
before changing generated-source tests:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

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

From the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Do not fetch Git LFS assets or Sims payloads for these tests. The fixtures create
the Alice projects they need.

### 2. Run focused characterization first

Run the core AST command, then the focused NetBeans command from
[Validation commands](#validation-commands). A useful review starts from the
smallest failing generated snippet or generated file.

### 3. Inspect generated source before runtime seams

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
| Keep fixtures synthetic and deterministic. | The tests must run in a normal no-Sims checkout without LFS payloads. |
| Keep assertions focused on representative generated-source shape and compileability. | The lane protects source generation without freezing unrelated formatting. |
| Compile generated Java after text assertions when a complete source tree exists. | Text presence alone does not prove generated source remains type-correct. |
| Keep headless runtime probes seam-level and explicit. | The probes validate generated listener wiring without overclaiming world execution. |
| Avoid production rewrites for characterization-only changes. | The lane exists to preserve current Alice 3 behavior unless a defect is directly exposed. |
| Do not launch desktop UI or rendering paths from this lane. | GUI and rendering behavior require separate outside-in evidence. |

## Limits

This characterization proves bounded generated-source behavior: core AST snippet
generation, NetBeans generated source compileability, selected Story API call
source, generated listener source, and narrow headless event-seam payload
delivery. It does not prove visible rendering correctness, full Alice world
execution, Save completion, grading, broad Tweedle/player decoding, full
first-lesson completion, or complete desktop workflow behavior.
