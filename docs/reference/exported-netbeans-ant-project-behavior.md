# Exported NetBeans Ant Project Behavior

This reference documents the exported Alice 3 NetBeans Ant project behavior
covered by the PR 423 source-code-generator characterization lane. It specifies
generated project files, `AliceJavaFXLauncher` evidence markers, launcher-owned
render-target marker observation, and deterministic no-go results. It does not
claim full UI automation, full world execution, visible rendering correctness,
Save completion, grading, broad Tweedle/player decoding, or full first-lesson
completion.

## Contents

- [Scope](#scope)
- [User-visible behavior](#user-visible-behavior)
- [Launcher evidence contract](#launcher-evidence-contract)
- [Render-observation contract](#render-observation-contract)
- [Headless and no-go contract](#headless-and-no-go-contract)
- [Configuration](#configuration)
- [Executable characterization](#executable-characterization)
- [API reference](#api-reference)
- [Validation commands](#validation-commands)
- [Tutorial: verify exported launcher evidence](#tutorial-verify-exported-launcher-evidence)
- [Tutorial: verify exported Ant runtime metadata](#tutorial-verify-exported-ant-runtime-metadata)
- [Compatibility rules](#compatibility-rules)
- [Limits](#limits)

## Scope

The exported project behavior is generated from these NetBeans-owned surfaces:

```text
netbeans/src/main/java/org/alice/netbeans/project/ProjectCodeGenerator.java
netbeans/src/main/resources/ProjectTemplate/
```

The focused characterization coverage lives beside the NetBeans export tests:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorGeneratedSourceTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStandaloneProjectTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

The exported-project slice verifies that a generated, LFS-free Alice project:

1. Uses `AliceJavaFXLauncher` as the exported default `main.class`.
2. Generates `Program.java`, `Scene.java`, and launcher source that compile
   against the test-owned classpath.
3. Hands off from launcher `main(String[] args)` to `Application.launch(args)`.
4. Enters `Application.start(Stage)` when JavaFX or test-owned JavaFX stubs
   provide a stage.
5. Validates the primary stage before scene setup or Program delegation.
6. Configures a launcher-owned 64-by-64 observation scene containing a solid
   marker rectangle.
7. Attempts `Stage.show()` and records whether a render target and marker pixel
   could be observed.
8. Delegates to `Program.main(startingArgs)` on the existing background-thread
   pattern only after the launcher marker observation succeeds.
9. Emits stable evidence and no-go markers that do not include project paths,
   command-line arguments, environment variables, user names, or exception stack
   traces.
10. Preserves exported Ant runtime metadata such as Alice library bindings,
    `run.classpath`, `main.class`, and `run.jvmargs`.

This is a generated exported-project behavior contract. Marker-pixel observation
is launcher-owned evidence that a tiny synthetic JavaFX scene was shown and
sampled. It is not proof that an Alice world rendered correctly, that media
loaded, or that a complete desktop workflow ran.

## User-visible behavior

An exported Alice project is a standard NetBeans Ant Java project. The template
supplies:

```text
build.xml
manifest.mf
nbproject/build-impl.xml
nbproject/project.properties
nbproject/project.xml
```

For exported projects, the default entry point is:

```properties
main.class = AliceJavaFXLauncher
```

Running the exported project invokes `AliceJavaFXLauncher`. The launcher stores
the starting arguments, emits deterministic evidence markers, hands control to
JavaFX startup, validates the primary stage, installs the launcher-owned
observation scene, attempts to show the stage, samples the marker pixel when
screen capture is available, and only then delegates to:

```java
Program.main(startingArgs);
```

Program delegation runs on a background thread named:

```text
AliceJavaFXLauncher-ProgramMain
```

The launcher never prints the starting arguments. Evidence output must remain
constant across local worktrees and CI environments.

## Launcher evidence contract

`AliceJavaFXLauncher` writes stable markers to standard output. These markers are
for tests, exported-project smoke logs, and PR review artifacts. They are not a
localized end-user protocol.

| Prefix | Meaning |
| --- | --- |
| `ALICE_LAUNCHER_EVIDENCE` | Positive launcher evidence for a bounded handoff or setup step. |
| `ALICE_LAUNCHER_NO_GO` | Deterministic boundary where the launcher cannot safely proceed. |
| `ALICE_LAUNCHER_RENDER_OBSERVATION` | JSON record describing launcher-owned render-target and marker-pixel observation. |

When JavaFX can start, a stage is available, the observation scene is shown, and
the marker pixel is sampled successfully, evidence appears in this order:

```text
ALICE_LAUNCHER_EVIDENCE main-entered
ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted
ALICE_LAUNCHER_EVIDENCE javafx-application-started
ALICE_LAUNCHER_EVIDENCE stage-received
ALICE_LAUNCHER_EVIDENCE scene-configured observation-marker
ALICE_LAUNCHER_EVIDENCE stage-show-attempted
ALICE_LAUNCHER_RENDER_OBSERVATION {"schema_version":"alice.launcher.render-observation/v1",...}
ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker
ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted
```

The final delegation marker intentionally says `rendering-not-asserted`. The
launcher has proven only its own marker observation and delegation gate. It has
not asserted Alice world rendering correctness.

The generated launcher must not emit success text that says or implies the Alice
world was rendered correctly, a user-visible workflow completed, Save completed,
grading ran, or a lesson was completed.

## Render-observation contract

The generated launcher creates a small JavaFX scene owned entirely by the
launcher:

```java
Group root = new Group(new Rectangle(64.0, 64.0, OBSERVATION_MARKER_COLOR));
return new Scene(root, 64.0, 64.0, OBSERVATION_MARKER_COLOR);
```

The marker color is `Color.rgb(32, 96, 160)`. The launcher samples a fixed point
inside the shown scene using `javafx.scene.robot.Robot`. The render-observation
record uses schema:

```text
alice.launcher.render-observation/v1
```

The JSON fields are:

| Field | Meaning |
| --- | --- |
| `schema_version` | Stable schema string: `alice.launcher.render-observation/v1`. |
| `status` | Observation status such as `shown-target-pixel-observed`, `render-target-absent`, `pixel-observation-unavailable`, `pixel-observation-unsupported`, or `pixel-observation-mismatch`. |
| `renderTargetShowing` | Whether the launcher observed a shown render target before pixel sampling. |
| `pixelsObserved` | Whether the sampled pixel matched the launcher marker color. |
| `missingObservationMechanism` | Stable reason code when observation cannot be completed. |
| `detail` | Human-readable detail without host paths, arguments, user names, or stack traces. |

Accepted marker observation is:

```text
"status":"shown-target-pixel-observed"
"renderTargetShowing":true
"pixelsObserved":true
"missingObservationMechanism":"none"
```

That result proves the launcher showed and sampled its synthetic marker scene. It
does not prove that Alice world content rendered, that project media loaded, or
that any desktop workflow was visible to a user.

## Headless and no-go contract

A no-go result is not success. It is a classified boundary that prevents the
launcher from reporting a stronger claim than the environment can support.

| No-go marker or status | Meaning |
| --- | --- |
| `ALICE_LAUNCHER_NO_GO display-unavailable` | JavaFX launch failed before a stage or render target was available because the display was missing or unusable. |
| `ALICE_LAUNCHER_NO_GO primary-stage-unavailable` | JavaFX invoked `start(...)` with a null primary stage. |
| `ALICE_LAUNCHER_NO_GO render-target-unavailable` | The stage could not be shown or did not report `isShowing()`. |
| `ALICE_LAUNCHER_NO_GO pixel-observation-unavailable` | A shown scene existed, but the launcher lacked enough window, scene, coordinate, or pixel data to sample the marker. |
| `ALICE_LAUNCHER_NO_GO pixel-observation-unsupported` | `Robot` or screen capture was unavailable in the current runtime. |
| `ALICE_LAUNCHER_NO_GO pixel-observation-mismatch` | A pixel was sampled, but it did not match the launcher marker color. |

After a no-go marker, the launcher must not delegate to `Program.main(...)`.
Tests should assert that no Program marker or Program delegation evidence appears
in no-go cases.

The display-unavailable classifier wraps only `Application.launch(args)` startup
failures known to represent headless or missing-display conditions. Unexpected
failures from `Application.start(...)`, scene setup, marker observation,
`Program.main(...)`, or unrelated launch errors are rethrown so real launcher
bugs do not become success-shaped fallbacks.

## Configuration

The exported project uses `nbproject/project.properties` as its project-owned
configuration. The important runtime properties are:

```properties
javac.classpath = ${libs.Alice3Library.classpath}
run.classpath = ${javac.classpath}:${build.classes.dir}
main.class = AliceJavaFXLauncher
run.jvmargs = -ea -Djogamp.gluegen.UseTempJarCache=false -Dorg.alice.ide.rootDirectory="${libs.Alice3Library.src}_root" --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.desktop/sun.awt=ALL-UNNAMED --add-opens=java.base/java.time=ALL-UNNAMED --add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
```

The exported template does not vendor Alice runtime jars into each project.
NetBeans or Ant user properties must provide Alice library bindings:

```properties
libs.Alice3Library.classpath=/path/to/alice/runtime/jars
libs.Alice3Library.src=/path/to/aliceSource.jar
```

The launcher evidence contract does not require new project properties. It is
generated into `AliceJavaFXLauncher` for exported projects.

## Executable characterization

### Source-shape characterization

`ProjectCodeGeneratorTest` verifies the generated launcher source contains:

| Source requirement | Why it matters |
| --- | --- |
| `AliceJavaFXLauncher` extending `Application` | Preserves the exported default launcher shape. |
| `Application.launch(args)` | Proves JavaFX startup is the handoff point. |
| `Program.main(startingArgs)` | Preserves exported Alice program delegation. |
| `ALICE_LAUNCHER_EVIDENCE` and `ALICE_LAUNCHER_NO_GO` helpers | Keeps success and no-go markers distinct. |
| `ALICE_LAUNCHER_RENDER_OBSERVATION` JSON helper | Gives reviewable marker-observation evidence. |
| Observation marker scene and `Robot` sampling code | Protects the launcher-owned marker-pixel gate. |
| No success wording for world rendering correctness | Prevents overclaiming from generated logs. |

The same class also compiles and executes generated launcher source with
test-owned JavaFX stubs so marker-observation success, unsupported pixel
observation, pixel mismatch, and starting-argument delegation remain
characterized.

### Generated source compileability

`ProjectCodeGeneratorGeneratedSourceTest` and
`ProjectCodeGeneratorStoryApiGeneratedSourceTest` generate temporary Alice
project source, assert selected generated snippets, and compile generated Java.
They protect project source shape for user methods, control flow, Story API
calls, listener registrations, and generated listener payload seams.

### Standalone exported-project characterization

`ProjectCodeGeneratorStandaloneProjectTest` compiles generated exported-project
source with test-owned JavaFX stubs and minimal Alice program fixtures. It
verifies:

1. JavaFX launch handoff evidence is printed before JavaFX startup.
2. The JavaFX stub can invoke `Application.start(...)`.
3. A non-null stub stage receives the launcher observation scene.
4. `Stage.show()` is attempted before marker-pixel observation.
5. Program delegation evidence appears only after marker observation succeeds.
6. Program delegation uses the `AliceJavaFXLauncher-ProgramMain` thread.
7. Display-unavailable, null-stage, render-target-unavailable, unsupported
   pixel-observation, and mismatch paths do not run `Program.main(...)`.
8. Template-packaged launcher jars keep `AliceJavaFXLauncher` as the manifest
   main class.

The standalone test may run against real JavaFX modules. In a headless
environment, accepted evidence is a deterministic display-unavailable boundary.
When `xvfb-run` is available, a gated path can prove the launcher-owned marker
observation and Program delegation under Xvfb. Neither path proves Alice world
rendering correctness.

## API reference

There is no new public Java API for this feature. The stable surface is the
exported NetBeans/Ant project contract and generated launcher behavior:

| Surface | Contract |
| --- | --- |
| `AliceJavaFXLauncher` | Generated default exported-project main class. Emits evidence, render-observation, and no-go markers; hands off to JavaFX; validates the stage; configures a launcher-owned marker scene; samples the marker pixel; and delegates to `Program.main(...)` only after the marker gate succeeds. |
| `ALICE_LAUNCHER_EVIDENCE` | Stable prefix for positive launcher evidence. |
| `ALICE_LAUNCHER_NO_GO` | Stable prefix for deterministic no-go boundaries. |
| `ALICE_LAUNCHER_RENDER_OBSERVATION` | Stable prefix for launcher-owned marker observation JSON. |
| `ProjectTemplate.zip` | Contains the NetBeans Ant project files used for exported Alice Java projects. |
| `nbproject/project.properties` | Owns `main.class`, `javac.classpath`, `run.classpath`, `run.jvmargs`, and Java release settings. |
| `libs.Alice3Library.classpath` | External Ant/NetBeans library binding for Alice runtime jars. |
| `libs.Alice3Library.src` | External Ant/NetBeans library binding used to derive `org.alice.ide.rootDirectory`. |
| Ant `run` target | Compiles project classes and launches `${main.class}` with `${run.jvmargs}` and `${run.classpath}`. |

Template or generator changes must preserve those names and marker meanings
unless a compatibility-breaking change is explicitly documented and
characterized.

## Validation commands

Run validation from the repository root. Initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the focused NetBeans generator characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorTest,org.alice.netbeans.project.ProjectCodeGeneratorGeneratedSourceTest,org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest,org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

Run the focused exported-project Ant metadata smoke when template runtime
metadata changes:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest#exportedProjectAntRunTargetAppliesRuntimeJvmArgumentsUpToGuiBoundary \
  test
```

If focused validation fails with missing generated Tweedle parser classes, check
the submodule before changing test or template behavior:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

## Tutorial: verify exported launcher evidence

Use this flow when reviewing a change to `ProjectCodeGenerator.java` or to the
generated `AliceJavaFXLauncher` contract.

### Step 1: Inspect the generated launcher

Open the generated source for an exported project and find:

```text
AliceJavaFXLauncher.java
```

Confirm the launcher uses `Application.launch(args)` in `main(String[] args)`.
Do not accept a launcher that calls `Program.main(...)` directly from `main`.

### Step 2: Check stable markers

The generated launcher should contain:

```text
ALICE_LAUNCHER_EVIDENCE
ALICE_LAUNCHER_NO_GO
ALICE_LAUNCHER_RENDER_OBSERVATION
```

The success path should distinguish JavaFX startup, stage receipt, marker scene
configuration, stage show attempt, marker-pixel observation, and Program
delegation. The no-go path should be clearly separate from success.

### Step 3: Interpret a display-unavailable run

In an environment without a usable display, a launcher run can produce:

```text
ALICE_LAUNCHER_EVIDENCE main-entered
ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted
ALICE_LAUNCHER_RENDER_OBSERVATION {"schema_version":"alice.launcher.render-observation/v1","status":"render-target-absent",...}
ALICE_LAUNCHER_NO_GO display-unavailable
```

This proves only that the launcher attempted JavaFX handoff and stopped at a
classified missing-display boundary. It does not prove marker-pixel observation,
scene setup, Program delegation, or Alice world rendering.

### Step 4: Interpret a marker-observed run

When JavaFX starts, a stage is shown, and the marker pixel is sampled, the
evidence includes:

```text
ALICE_LAUNCHER_EVIDENCE javafx-application-started
ALICE_LAUNCHER_EVIDENCE stage-received
ALICE_LAUNCHER_EVIDENCE scene-configured observation-marker
ALICE_LAUNCHER_EVIDENCE stage-show-attempted
ALICE_LAUNCHER_RENDER_OBSERVATION {"schema_version":"alice.launcher.render-observation/v1","status":"shown-target-pixel-observed",...}
ALICE_LAUNCHER_EVIDENCE pixels-observed shown-stage-marker
ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted
```

This proves launcher-owned marker observation and Program delegation. It does
not prove the generated Alice world rendered correctly.

### Step 5: Keep stronger evidence separate

If a review requires UI automation, full world execution, visible rendering
correctness, Save completion, grading, or lesson-completion evidence, collect it
in a separate explicitly gated lane with its own observable artifacts. Do not
rename the default launcher evidence to imply those broader behaviors.

## Tutorial: verify exported Ant runtime metadata

Use this flow when reviewing or extending exported-project Ant behavior.

### Step 1: Start from a no-Sims checkout

From the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Do not pull Git LFS files or Sims/nonfree payloads for this smoke. The
characterization generates synthetic Alice projects and local fixtures.

### Step 2: Run the focused Ant smoke

Run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest#exportedProjectAntRunTargetAppliesRuntimeJvmArgumentsUpToGuiBoundary \
  test
```

The smoke is accepted only when the Ant run log contains:

```text
ANT_RUNTIME_CONFIGURATION_PROBE_OK
```

The same log must not contain:

```text
Java Result:
```

### Step 3: Interpret the Ant evidence

Treat a passing Ant smoke as evidence that the exported Ant project consumes the
runtime metadata asserted by the probe up to the launcher boundary. It proves the
exported `run` target passes assertions, Alice system properties, and
interpolated library paths to the launched JVM.

Do not treat this smoke as evidence that the generated launcher reached JavaFX
`Application.start(...)`, sampled its marker pixel, delegated to
`Program.main(...)`, or rendered an Alice world.

## Compatibility rules

1. Keep exported projects compatible with the current Alice 3 baseline unless a
   behavior change is explicitly documented and tested.
2. Keep characterization LFS-free and Sims-free.
3. Prefer synthetic `.a3p` inputs and generated local fixtures.
4. Keep `AliceJavaFXLauncher` as the exported default `main.class`.
5. Preserve `Application.launch(args)` as the JavaFX runtime handoff.
6. Validate the primary stage before scene setup, marker observation, or Program
   delegation.
7. Preserve the launcher-owned marker scene and marker-observation gate unless a
   replacement proves at least the same bounded evidence.
8. Preserve `Program.main(startingArgs)` background-thread delegation after the
   marker gate succeeds.
9. Keep no-go markers distinct from evidence markers.
10. Classify only known JavaFX display/headless failures thrown by
    `Application.launch(args)` as display-unavailable no-go.
11. Rethrow unexpected failures from `Application.start(...)`, scene setup,
    marker observation, `Program.main(...)`, or unrelated launch handoff errors.
12. Preserve `run.jvmargs` behavior unless the replacement proves equivalent
    exported-project runtime behavior.
13. Do not accept a nonzero Java launch as a passing Ant smoke.
14. Document display, rendering, Save, grading, and lesson-completion limits
    honestly.

## Limits

The default exported launcher evidence targets the strongest behavior that is
deterministic in focused no-Sims validation: JavaFX runtime handoff, JavaFX
`Application.start(...)` entry when available, primary stage receipt,
launcher-owned marker scene setup, stage-show attempt, marker-pixel observation
or deterministic no-go classification, and Program delegation only after marker
observation succeeds.

The default evidence does not prove Alice world pixels were correct, a full
window workflow was automated, project media loaded, Save completed, grading ran,
a broad Tweedle/player corpus decoded, or a first lesson completed. Those claims
require separate outside-in evidence and explicit artifacts.
