# Exported NetBeans Ant Project Behavior

This reference documents the exported Alice 3 NetBeans Ant project contract. It
covers generated-project launcher evidence, the bounded no-Sims Ant build proof
for generated NetBeans projects, the Ant `run` metadata used by exported
projects, and the generated
`AliceJavaFXLauncher` evidence boundary used to prove JavaFX launcher handoff,
stage validation, launcher-owned marker observation, and deterministic no-go
behavior.

The launcher evidence proves only what the exported project can observe
deterministically: JavaFX launch handoff, `Application.start(...)` entry, primary
stage receipt, launcher marker-scene setup, marker-pixel observation when
available, and `Program.main(...)` delegation. It is not Alice-world rendering
evidence, window visibility evidence, media-loading evidence, or proof of a
completed user workflow.

Implementation status: the wired outside-in scenario
`alice-desktop-exported-project-smoke` uses workflow
`exported-project-ant-build-smoke` and runs
`Alice3ProjectTemplateAntSmokeTest`. Cite it only as bounded exported Ant build
evidence: it does not validate the installer, full GUI export journey, rendered
pixels, or visible window behavior.

## Contents

- [Scope](#scope)
- [User-visible behavior](#user-visible-behavior)
- [Launcher evidence contract](#launcher-evidence-contract)
- [Render-observation contract](#render-observation-contract)
- [Headless and no-go contract](#headless-and-no-go-contract)
- [Configuration](#configuration)
- [Executable characterization](#executable-characterization)
- [Exported Ant build proof contract](#exported-ant-build-proof-contract)
- [API reference](#api-reference)
- [Validation commands](#validation-commands)
- [Review checklist: exported launcher evidence](#review-checklist-exported-launcher-evidence)
- [Review checklist: exported Ant runtime metadata](#review-checklist-exported-ant-runtime-metadata)
- [Tutorial: verify exported launcher evidence](#tutorial-verify-exported-launcher-evidence)
- [Tutorial: verify target exported Ant build evidence](#tutorial-verify-target-exported-ant-build-evidence)
- [Recovery finalization](#recovery-finalization)
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
netbeans/src/test/java/org/alice/netbeans/project/Alice3ProjectTemplateAntSmokeTest.java
```

The behavior slice covers generated, LFS-free Alice projects exported into the
NetBeans Ant template. The wired outside-in QA lane runs the bounded Ant build
proof when gated, while launcher/generator tests continue to cover the
generated-source and JavaFX handoff evidence portion. Together they verify that
the exported project:

1. Generates `AliceJavaFXLauncher` as the default `main.class`.
2. Compiles generated Alice project source against the exported runtime
   classpath.
3. Uses JavaFX `Application.launch(args)` as the launcher handoff point.
4. Enters `Application.start(Stage)` when the JavaFX runtime can create a stage.
5. Rejects a missing stage with a deterministic no-go marker.
6. Configures a launcher-owned 64-by-64 marker scene before delegating to
   `Program.main(startingArgs)` using the existing background-thread pattern.
7. Attempts `Stage.show()` and records whether a render target and marker pixel
   could be observed.
8. Emits stable evidence markers that do not expose project paths, command-line
   arguments, environment variables, user names, or exception details.
9. Converts only known JavaFX display-unavailable failures from the
   `Application.launch(args)` handoff into deterministic no-go markers while
   rethrowing unexpected failures.
10. Preserves exported Ant runtime metadata such as assertions, Alice runtime
   system properties, library interpolation, and nonzero Java failure handling.
11. Executes the real generated NetBeans Ant `jar`, `run`, `run-test-with-main`,
    and `clean` targets against a synthetic exported project.
12. Produces concrete build output under the exported project, including compiled
    classes and a distributable jar with the expected generated entries and
    manifest main class.
13. Packages generated project resources into the jar and proves the Ant `run`
    classpath can load them.

This is a behavior-level exported-project contract. Marker-pixel observation is
launcher-owned evidence that a tiny synthetic JavaFX scene was shown and sampled.
It is not proof that an Alice world rendered correctly, that media loaded, or
that a complete desktop workflow ran.

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

String fields in the render-observation JSON, including `detail`, must escape
quotes, backslashes, and JSON control characters. For example, a JavaFX Robot
failure detail that contains `"`, `\`, or a newline is emitted with `\"`, `\\`,
and `\n` escapes inside the JSON string, not as raw control characters.

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
observation, render-observation JSON escaping, pixel mismatch, and
starting-argument delegation remain characterized.

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

### Exported Ant runtime metadata characterization

`Alice3ProjectTemplateAntSmokeTest` exercises the packaged NetBeans Ant template
with synthetic Alice projects and local library bindings. It verifies exported
project compile, jar, run, resource packaging, test-main, clean, wizard export,
and runtime metadata behavior without requiring Sims or LFS payloads.

For the runtime configuration boundary, the focused smoke overrides
`main.class` with a probe and verifies that the exported Ant `run` target passes
assertions, Alice system properties, interpolated Alice library paths, and the
expected classpath to the launched JVM. That smoke stops at Ant/JVM metadata; it
does not prove JavaFX `Application.start(...)`, launcher marker observation,
Program delegation, or Alice world rendering.

## Exported Ant build proof contract

`Alice3ProjectTemplateAntSmokeTest` is the bounded no-Sims proof for the
exported Ant/NetBeans project build path. The proof does not stop at generator
classpath contracts. It expands the packaged
`ProjectTemplate.zip`, generate Alice project source into the template, write
local `libs.Alice3Library.*` Ant bindings, and execute the template's generated
Ant targets with the Ant launcher.

The proof is accepted only when the real exported project produces concrete
build output:

| Ant target or path | Required evidence |
| --- | --- |
| `jar` | `build/classes/Program.class`, `build/classes/AliceJavaFXLauncher.class`, and `dist/Alice3JavaApplication.jar` or the wizard-selected project jar exist. |
| jar manifest | The generated jar manifest has `Main-Class: AliceJavaFXLauncher`. |
| jar contents | The generated jar contains `Program.class`, `AliceJavaFXLauncher.class`, and generated resource entries when the source project has resources. |
| Ant command-line hint | The `jar` target log includes the command-line classpath hint, the generated jar path, `AliceJavaFXLauncher`, `story-api`, and `javafx-graphics`. |
| `run` | The test overrides `main.class` to a headless probe, compiles it through the exported Ant project, and prints `ANT_RUN_PROBE_OK org.lgna.story.SProgram args=1`. |
| resource `run` | The resource probe prints `ANT_RESOURCE_PROBE_OK audio.x_wav alice ant resource` after loading the generated resource through the Ant runtime classpath. |
| runtime metadata `run` | The runtime probe prints `ANT_RUNTIME_CONFIGURATION_PROBE_OK ...aliceSource.jar_root`, proving assertions and Alice root-directory interpolation reached the launched JVM. |
| `run-test-with-main` | The exported test-main path compiles test classes and prints `ANT_TEST_MAIN_PROBE_OK org.lgna.story.SProgram ...aliceSource.jar_root`. |
| `clean` | The `clean` target removes `build/` and `dist/` while preserving generated source and `build.xml`. |

Every Ant execution must exit zero. A log containing `Java Result:` is a failure,
not a passing smoke with a warning. A timeout is also a failure and must name the
Ant target that did not terminate.

The proof remains bounded:

1. It is no-Sims and uses synthetic generated `.a3p` inputs.
2. It uses temporary or Maven `target/` project directories, not user project
   locations.
3. It does not validate an installer.
4. It does not drive the full GUI export journey.
5. It does not prove visible rendering, media playback, grading, creative
   assessment, or lesson completion.
6. It replaces the launcher main class with probes for headless Ant `run`
   assertions; launcher JavaFX handoff remains covered by the launcher
   characterization tests.

### Executable blockers

If the focused Maven command cannot execute the proof, record the exact command,
the failing target or prerequisite, and the missing condition. Use these blocker
names in review notes or PR evidence:

| Blocker | Meaning | Next executable step |
| --- | --- | --- |
| `tweedle-grammar-submodule-missing` | Generated Tweedle parser inputs are unavailable. | `git submodule update --init tweedle-lang && test -d tweedle-lang/Grammar` |
| `project-template-zip-missing` | `target/classes/org/alice/netbeans/ProjectTemplate.zip` is absent from the NetBeans test classpath. | Re-run the focused Maven command so resources are processed, then inspect the NetBeans test resources phase. |
| `alice3-library-classpath-artifact-missing` | The generated `Alice3Library.xml` references a required runtime artifact that is not present in reactor outputs or the test classpath. | Run the no-Sims reactor command with `-pl netbeans -am`; if the application distribution is required for the wider check, run `NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip -pl core/resources -am package`. |
| `ant-target-failed` | The exported Ant target exited nonzero. | Preserve `command.log` or the Surefire report and name the target: `jar`, `run`, `run-test-with-main`, or `clean`. |
| `ant-target-timeout` | The exported Ant target did not terminate within the bounded test timeout. | Preserve the Ant log and target name; do not claim build evidence. |
| `generated-jar-output-missing` | The Ant target exited but the required jar, class, manifest, or resource output was absent. | Preserve the assertion message and generated project listing. |

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
| Ant `jar` target | Compiles generated project classes and packages the exported jar. |
| Ant `run` target | Compiles project classes and launches `${main.class}` with `${run.jvmargs}` and `${run.classpath}`. |
| Ant `run-test-with-main` target | Compiles test classes and launches the configured test main class against the exported project classpath. |
| Ant `clean` target | Removes generated Ant build output while preserving generated source and project metadata. |

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

Command for the exported-project Ant build proof:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl netbeans -am \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest \
  test
```

The outside-in QA scenario executes the same focused proof through the gated
scenario runner:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-exported-project-smoke \
  --evidence-dir qa/outside-in/alice-desktop/evidence/exported-project-ant-build
```

For review evidence, `status.txt` records the gated scenario result, exit code,
and focused Maven argv; `command.log` records the Maven, Surefire, and Ant output
from that argv. The command remains a bounded Ant/template build proof; it is
not installer validation or full GUI export journey evidence.

Run the broader no-Sims NetBeans reactor validation after launcher generator,
template, or NetBeans export behavior changes:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  test
```

If focused validation fails with missing generated Tweedle parser classes, check
the submodule before changing test or template behavior:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

## Review checklist: exported launcher evidence

Use this checklist when reviewing a change to `ProjectCodeGenerator.java` or to
the generated `AliceJavaFXLauncher` contract.

### 1. Inspect the generated launcher

Open the generated source for an exported project and find:

```text
AliceJavaFXLauncher.java
```

Confirm the launcher uses `Application.launch(args)` in `main(String[] args)`.
Do not accept a launcher that calls `Program.main(...)` directly from `main`.

### 2. Check stable markers

The generated launcher should contain:

```text
ALICE_LAUNCHER_EVIDENCE
ALICE_LAUNCHER_NO_GO
ALICE_LAUNCHER_RENDER_OBSERVATION
```

The success path should distinguish JavaFX startup, stage receipt, marker scene
configuration, stage show attempt, marker-pixel observation, and Program
delegation. The no-go path should be clearly separate from success.

### 3. Interpret a display-unavailable run

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

### 4. Interpret a marker-observed run

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

### 5. Keep stronger evidence separate

If a review requires UI automation, full world execution, visible rendering
correctness, Save completion, grading, or lesson-completion evidence, collect it
in a separate explicitly gated lane with its own observable artifacts. Do not
rename the default launcher evidence to imply those broader behaviors.

## Review checklist: exported Ant runtime metadata

Use this checklist when reviewing or extending exported-project Ant behavior.

## Tutorial: verify target exported Ant build evidence

Use this flow when reviewing the wired exported-project Ant behavior. The
current outside-in smoke runs `Alice3ProjectTemplateAntSmokeTest` when the gated
command is intentionally enabled.

### 1. Start from a no-Sims checkout

From the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Do not pull Git LFS files or Sims/nonfree payloads for this smoke. The
characterization generates synthetic Alice projects and local fixtures.

### Step 2: Run the target focused Ant build smoke

Run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl netbeans -am \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest \
  test
```

The smoke is accepted only when the generated Ant project produces compiled
classes, a distributable jar, the expected manifest, resource entries when
resources are present, successful probe markers, and no nonzero Java result.
Review the Surefire failure or `command.log` for these markers:

```text
ANT_RUN_PROBE_OK
ANT_RESOURCE_PROBE_OK
ANT_RUNTIME_CONFIGURATION_PROBE_OK
ANT_TEST_MAIN_PROBE_OK
```

No Ant log may contain:

```text
Java Result:
```

### Step 3: Interpret the Ant build evidence

A passing Ant smoke is evidence that the generated NetBeans Ant project can
compile generated Alice source, package a jar, run headless probes through the
exported runtime classpath, load generated resources, and clean generated Ant
outputs.

Do not treat this smoke as evidence that the generated launcher reached JavaFX
`Application.start(...)`, sampled its marker pixel, delegated to
`Program.main(...)`, or rendered an Alice world.

Treat a failed run as an executable blocker, not as partial success. The blocker
must include the exact Maven command, the Ant target or prerequisite that failed,
and the missing condition, such as `project-template-zip-missing`,
`alice3-library-classpath-artifact-missing`, `ant-target-failed`,
`ant-target-timeout`, or `generated-jar-output-missing`.

Do not treat this smoke as evidence that a user completed the GUI export flow,
that an installer works, or that a JavaFX window rendered. Launcher handoff and
scene/setup evidence belong to the launcher characterization tests; GUI export
journey evidence belongs to a separate display-backed or manual outside-in lane.

## Recovery finalization

When a review branch already contains the exported NetBeans Ant smoke feature,
finalize recovery through the bounded readiness, contract, and smoke checks in
[Finalize exported NetBeans Ant smoke
recovery](../howto/finalize-exported-netbeans-ant-smoke-recovery.md).

Recovery finalization is accepted only as current-head evidence. It must name the
branch and commit under review, confirm the Tweedle grammar submodule is present,
validate the scenario catalog and shell contracts, run the gated
`alice-desktop-exported-project-smoke` scenario, run the focused
`Alice3ProjectTemplateAntSmokeTest` command, complete three
`SEEK -> VALIDATE -> FIX` quality-audit cycles with a clean final cycle, confirm
documentation impact, verify focused diff scope, verify the pull request
description evidence, verify GitHub Actions are green for the exact same head
SHA, and evaluate the collected evidence with
`scripts/pr389_recovery_gate.py`.

If those checks pass and no implementation patch is required, the handoff uses a
current-head no-op justification instead of inventing a source change. If any
gate is missing, stale, pending, failed, or SHA-mismatched, the handoff remains
`NOT_MERGE_READY` with explicit blockers.

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
