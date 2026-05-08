# Generated Story API Runtime State Characterization

This reference describes the headless NetBeans characterization for generated
Story API Java source that mutates deterministic runtime state.

The test extends the existing generated-source evidence lane from
[Generated Story API Listener Source Characterization](./generated-story-api-listener-source-characterization.md).
It proves one additional thing: generated `Program.java` can be compiled,
loaded, invoked directly, and observed through `SProgram` runtime state without
launching Alice or executing a generated JavaFX launcher.

## Contents

- [Scope](#scope)
- [Runtime-state contract](#runtime-state-contract)
- [Executable characterization](#executable-characterization)
- [Reflection boundary](#reflection-boundary)
- [Validation command](#validation-command)
- [Compatibility rules](#compatibility-rules)
- [Limits](#limits)

## Scope

The implementation belongs in:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

The existing `generatedSyntheticStoryApiCallSourceCompiles` test protects
source generation and compilation for this synthetic fixture:

```java
this.setSimulationSpeedFactor(1.5);
```

The runtime-state characterization adds the direct runtime-state check:

1. Build a deterministic synthetic `Program` AST in memory.
2. Write the project to a temporary `.a3p` archive with
   `IoUtilities.writeProject`.
3. Generate Java source with `ProjectCodeGenerator.generateCode(..., false)`.
4. Compile generated `Program.java`.
5. Load and instantiate the generated `Program` class from the compiled
   temporary output, requiring `Program` to resolve from that output.
6. Invoke generated `configureStory()` directly.
7. Assert that `getSimulationSpeedFactor()` returns `1.5`.

This is a headless generated-runtime characterization. It is not a desktop,
launcher, rendering, animation, event-loop, or full world execution test.

## Runtime-state contract

| Synthetic AST call | Required generated source | Runtime assertion |
| --- | --- | --- |
| `SProgram.setSimulationSpeedFactor(Number)` with `1.5` inside `Program.configureStory()` | `this.setSimulationSpeedFactor(1.5);` | After direct invocation of generated `configureStory()` on a generated `Program` instance, `getSimulationSpeedFactor()` returns `1.5`. |

The fixture should use an in-memory `Program` user type assignable to
`org.lgna.story.SProgram`. The fixture method is named `configureStory`, accepts
no parameters, and contains only the simulation speed call. That method shape is
test input; it does not mean Alice startup invoked `configureStory()` or that a
visual world was displayed.

## Executable characterization

The checked-in test method is:

```text
ProjectCodeGeneratorStoryApiGeneratedSourceTest.generatedStoryApiSimulationSpeedCallUpdatesRuntimeStateHeadlessly
```

The characterization should create only temporary files. The generated project
archive, source directory, and compiled classes directory should be managed by
JUnit's temporary folder rule and should not be persisted in the repository.

## Reflection boundary

The runtime check should invoke the generated method directly and make private
or package-private generated members accessible when necessary. The reflection
shape should be explicit so the test does not depend on `configureStory()` being
public:

```java
Class<?> programClass = Class.forName("Program", true, classLoader);
var constructor = programClass.getDeclaredConstructor();
constructor.setAccessible(true);
ProgramImp.ACCEPTABLE_HACK_FOR_NOW_setClassForNextInstance(HeadlessProgramImp.class);
SProgram program = (SProgram) constructor.newInstance();

var configureStory = programClass.getDeclaredMethod("configureStory");
configureStory.setAccessible(true);
configureStory.invoke(program);

assertEquals(1.5, program.getSimulationSpeedFactor(), 0.0);
```

The `HeadlessProgramImp` injection uses the existing one-shot `ProgramImp`
construction seam so generated `Program` instantiation does not create an
onscreen render target. The direct `configureStory()` invocation is the runtime
boundary. The test compiles generated `Program.java` only; it must not launch or
execute the JavaFX launcher. The generated test class loader resolves `Program`
from the compiled temporary output before delegating dependencies to the normal
test classpath.

## Validation command

Run the focused NetBeans characterization from the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

If Maven reports missing generated Tweedle parser classes, first check:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

## Compatibility rules

Changes in this area should preserve these rules:

| Rule | Reason |
| --- | --- |
| Keep the fixture synthetic and deterministic. | The test must run in a normal no-Sims checkout without LFS payloads. |
| Keep the assertion narrow. | The characterization protects one generated `SProgram` state mutation, not wholesale formatting of generated files. |
| Compile generated Java before runtime invocation. | Text presence alone does not prove the generated Story API source remains type-correct. |
| Invoke `configureStory()` directly. | The lane characterizes generated runtime behavior without desktop startup or world playback. |
| Assert `getSimulationSpeedFactor()` exactly against `1.5` using the repository's double assertion style. | The mutation is deterministic and should not depend on rendering, time, or scheduling. |
| Avoid production rewrites for characterization-only changes. | The lane exists to preserve current Alice 3 behavior unless a defect is directly exposed. |

## Limits

This characterization does not prove that Alice launches, scenes render, time
advances, animations play, listeners fire, or exported projects run. It proves
only that deterministic generated Story API Java source can be
compiled, loaded, invoked headlessly through `configureStory()`, and observed
through `getSimulationSpeedFactor()`.
