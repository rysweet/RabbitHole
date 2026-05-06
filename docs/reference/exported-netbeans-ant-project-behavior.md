# Exported NetBeans Ant Project Behavior

This reference documents the exported Alice 3 NetBeans Ant project runtime
contract. It covers the Ant `run` metadata used by exported projects and the
generated `AliceJavaFXLauncher` evidence boundary used to prove JavaFX launcher
handoff, stage validation and minimal scene setup, and deterministic
display-unavailable no-go behavior.

The launcher evidence proves only what the exported project can observe
deterministically: JavaFX launch handoff, `Application.start(...)` entry, primary
stage receipt, minimal scene configuration, and `Program.main(...)` delegation.
It is not rendering evidence, window visibility evidence, media-loading
evidence, or proof of a completed user workflow.

## Contents

- [Scope](#scope)
- [User-visible behavior](#user-visible-behavior)
- [Launcher evidence contract](#launcher-evidence-contract)
- [Headless and display-unavailable contract](#headless-and-display-unavailable-contract)
- [Configuration](#configuration)
- [Executable characterization](#executable-characterization)
- [API reference](#api-reference)
- [Validation commands](#validation-commands)
- [Tutorial: verify exported launcher evidence](#tutorial-verify-exported-launcher-evidence)
- [Tutorial: verify exported Ant runtime metadata](#tutorial-verify-exported-ant-runtime-metadata)
- [Compatibility rules](#compatibility-rules)
- [Limits](#limits)

## Scope

The exported project behavior is generated from two NetBeans-owned surfaces:

```text
netbeans/src/main/java/org/alice/netbeans/project/ProjectCodeGenerator.java
netbeans/src/main/resources/ProjectTemplate/
```

The focused characterization coverage belongs beside the NetBeans export tests:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStandaloneProjectTest.java
netbeans/src/test/java/org/alice/netbeans/project/Alice3ProjectTemplateAntSmokeTest.java
```

The behavior slice covers a generated, LFS-free Alice project exported into the
NetBeans Ant template. It verifies that the exported project:

1. Generates `AliceJavaFXLauncher` as the default `main.class`.
2. Compiles generated Alice project source against the exported runtime
   classpath.
3. Uses JavaFX `Application.launch(args)` as the launcher handoff point.
4. Enters `Application.start(Stage)` when the JavaFX runtime can create a stage.
5. Rejects a missing stage with a deterministic no-go marker.
6. Configures a minimal JavaFX scene before delegating to
   `Program.main(startingArgs)` using the existing background-thread pattern.
7. Emits stable evidence markers that do not expose project paths, command-line
   arguments, environment variables, user names, or exception details.
8. Converts only known JavaFX display-unavailable failures from the
   `Application.launch(args)` handoff into deterministic no-go markers while
   rethrowing unexpected failures.
9. Preserves exported Ant runtime metadata such as assertions, Alice runtime
   system properties, library interpolation, and nonzero Java failure handling.

This is a behavior-level exported-project contract. It intentionally goes beyond
generated source text, classpath presence, or merely reaching `Program.main(...)`,
but it still stops at the scene/setup boundary unless separate display-backed
evidence is collected.

## User-visible behavior

An exported Alice project is a standard NetBeans Ant Java project. The project
template supplies `build.xml`, `nbproject/build-impl.xml`,
`nbproject/project.xml`, `nbproject/project.properties`, and `manifest.mf`.

For normal exported projects, the default entry point is:

```properties
main.class = AliceJavaFXLauncher
```

Running the exported project launches `AliceJavaFXLauncher`. The launcher first
hands control to the JavaFX runtime. When the runtime provides a non-null primary
stage, the launcher configures a minimal scene and delegates the exported Alice
program entry point to `Program.main(startingArgs)` using the existing
background-thread delegation pattern.

In a headless or display-unavailable environment, the launcher reports a
deterministic no-go result instead of reporting success merely because the
launcher process started. A no-go result means the exported launcher reached a
known boundary where UI display evidence cannot be proven in the current
environment.

## Launcher evidence contract

`AliceJavaFXLauncher` writes stable markers to standard output. These markers are
for tests, exported-project smoke logs, and PR review artifacts. They are not a
public application protocol and should not be localized.

All successful evidence lines start with:

```text
ALICE_LAUNCHER_EVIDENCE
```

All deterministic no-go lines start with:

```text
ALICE_LAUNCHER_NO_GO
```

The generated launcher emits the following evidence in order when JavaFX can
enter `Application.start(...)`:

| Evidence marker | Meaning |
| --- | --- |
| `ALICE_LAUNCHER_EVIDENCE main-entered` | The generated launcher `main(String[] args)` was invoked. |
| `ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted` | The launcher called `Application.launch(args)` and handed control to JavaFX startup. |
| `ALICE_LAUNCHER_EVIDENCE javafx-application-started` | JavaFX invoked `Application.start(Stage)`. |
| `ALICE_LAUNCHER_EVIDENCE stage-received` | `Application.start(...)` received a non-null primary stage. |
| `ALICE_LAUNCHER_EVIDENCE scene-configured rendering-not-asserted` | The launcher configured a minimal `Scene` on the primary stage. This is scene/setup evidence only. |
| `ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted` | The launcher delegated to `Program.main(startingArgs)` using the existing background-thread pattern. This is delegation evidence only. |

The generated launcher must not emit a success marker that says or implies a
window was shown, the UI was visible, graphics were rendered, media loaded, or a
desktop workflow completed. Those claims require separate observable evidence.

### Minimal scene setup

The generated launcher creates only the minimal JavaFX objects needed to prove
the scene/setup boundary:

```java
primaryStage.setScene(new Scene(new Group()));
```

This setup is intentionally local and resource-free. It does not load project
media, read files, open URLs, parse command-line arguments, or show a stage as
part of the required evidence path.

### Program delegation

The generated launcher preserves the exported project entry-point behavior:

```java
Program.main(startingArgs);
```

The launcher stores the original command-line arguments for delegation, but it
never prints them. Evidence output must remain constant so logs do not leak
workspace paths, user names, launch arguments, or environment-specific details.

The delegation must preserve the current exported Alice runtime pattern: after
stage validation and minimal scene setup succeed, `Program.main(startingArgs)`
runs on the existing background thread rather than synchronously inside
`Application.start(...)`.

Reaching `Program.main(...)` is not enough to prove scene setup. Scene/setup
evidence and Program delegation evidence are separate markers and should be
reviewed separately.

## Headless and display-unavailable contract

Headless or display-unavailable execution must produce a deterministic no-go
result. A no-go result is not launcher success; it is a classified boundary that
explains why UI display evidence cannot be proven in the current environment.

Known JavaFX initialization failures from the `Application.launch(args)` handoff
caused by a missing or unusable display are reported with:

```text
ALICE_LAUNCHER_NO_GO display-unavailable
```

If JavaFX calls `Application.start(...)` with a null stage, the launcher reports:

```text
ALICE_LAUNCHER_NO_GO primary-stage-unavailable
```

After a no-go marker, the launcher must not delegate to `Program.main(...)`.
Tests should assert that no Program delegation marker is present in no-go cases.

The display-unavailable classifier wraps only the `Application.launch(args)`
handoff. It may convert known JavaFX display/headless initialization failures
from that handoff into `ALICE_LAUNCHER_NO_GO display-unavailable`. It must not
convert unexpected failures from `Application.start(...)`, minimal scene setup,
or `Program.main(...)` into no-go results; those failures are rethrown so real
launcher bugs do not get hidden behind a success-shaped fallback.

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
NetBeans or Ant user properties must provide the Alice library bindings:

```properties
libs.Alice3Library.classpath=/path/to/alice/runtime/jars
libs.Alice3Library.src=/path/to/aliceSource.jar
```

The launcher evidence contract does not require any new project property or user
configuration. It is generated into `AliceJavaFXLauncher` for exported projects.

Display-backed checks are optional. If a developer or CI job provides a usable
display or Xvfb server, that job may add separate assertions or artifacts for
display-backed behavior. Such checks must remain explicitly gated and must not
replace the deterministic headless no-go contract.

## Executable characterization

The launcher characterization is split across source-shape tests and compiled
standalone exported-project tests.

### Source-shape characterization

`ProjectCodeGeneratorTest` verifies the generated `AliceJavaFXLauncher` source
contract. It asserts that the generated source contains:

| Source requirement | Why it matters |
| --- | --- |
| Stable `ALICE_LAUNCHER_EVIDENCE` helpers | Keeps evidence markers deterministic and reviewable. |
| Stable `ALICE_LAUNCHER_NO_GO` helpers | Keeps headless/display-unavailable results distinct from success. |
| `Application.launch(args)` | Proves the generated launcher hands off to the JavaFX runtime. |
| `Application.start(Stage)` evidence | Proves the launcher documents JavaFX runtime entry separately from Program delegation. |
| Null-stage validation | Prevents a missing stage from looking like launcher success. |
| `primaryStage.setScene(...)` | Proves scene/setup boundary evidence exists before Program delegation. |
| `Program.main(startingArgs)` | Preserves the exported Alice program entry-point behavior and existing background-thread delegation pattern. |
| No success wording for visible UI or rendering | Prevents generated logs from overclaiming what the launcher proves. |

These tests also preserve the current characterization that the old proof level
was only JavaFX `Application.start(...)` entry followed by
`Program.main(...)` delegation. That historical characterization prevents future
reviews from treating previous `Program.main(...)` reachability as rendering or
visibility proof.

### Standalone exported-project characterization

`ProjectCodeGeneratorStandaloneProjectTest` compiles and runs generated exported
project source with test-owned JavaFX stubs and minimal Alice program fixtures.
It verifies:

1. JavaFX launch handoff evidence is printed before JavaFX startup.
2. The JavaFX stub can invoke `Application.start(...)`.
3. A non-null stub stage receives minimal scene setup.
4. Program delegation evidence appears only after scene setup evidence.
5. The generated launcher still calls `Program.main(startingArgs)` using the
   existing background-thread delegation pattern.
6. A null-stage stub produces `ALICE_LAUNCHER_NO_GO primary-stage-unavailable`.
7. Null-stage no-go execution does not delegate to `Program.main(...)`.

The standalone test may also execute the generated launcher against real JavaFX
in the local environment. When JavaFX cannot initialize a display, the expected
result is the deterministic display-unavailable no-go marker. When a usable
display is present, the test may assert JavaFX handoff and scene/setup evidence;
it must not claim rendered output unless it captures a display-backed observable
artifact.

### Ant runtime metadata characterization

`Alice3ProjectTemplateAntSmokeTest` verifies the exported Ant `run` target
metadata independently of the launcher scene/setup path. Its smoke probe checks
behavior that can be proven locally without running the generated launcher:

| Probe assertion | Why it matters |
| --- | --- |
| Java assertions are enabled | Proves `-ea` from `run.jvmargs` reached the launched JVM. |
| `org.alice.ide.rootDirectory` is set | Proves exported runtime system properties are passed by Ant. |
| The root directory value has no unresolved `${...}` placeholder | Proves Ant interpolated the Alice library source property. |
| The root directory ends with `aliceSource.jar_root` | Proves the exported Alice source-root convention is preserved. |
| The probe prints `ANT_RUNTIME_CONFIGURATION_PROBE_OK` | Gives reviewers a deterministic success marker in the Ant log. |

## API reference

There is no new public Java API for this feature. The stable surface is the
exported NetBeans/Ant project contract and generated launcher behavior:

| Surface | Contract |
| --- | --- |
| `AliceJavaFXLauncher` | Generated default exported-project main class. Emits evidence/no-go markers, hands off to JavaFX, validates the stage, configures a minimal scene, and delegates to `Program.main(...)` on the existing background thread only after scene setup succeeds. |
| `ALICE_LAUNCHER_EVIDENCE` | Stable prefix for positive launcher evidence. |
| `ALICE_LAUNCHER_NO_GO` | Stable prefix for deterministic no-go boundaries. |
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

Run the focused launcher generation and standalone exported-project checks:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorTest,org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest \
  test
```

Run the focused exported-project Ant metadata smoke when template runtime
metadata changes:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest#exportedProjectAntRunTargetAppliesRuntimeJvmArgumentsUpToGuiBoundary \
  test
```

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

The `alice-ide` focused tests are required only when `EntryPoint` or desktop
headless guard behavior changes.

## Tutorial: verify exported launcher evidence

Use this flow when reviewing a change to `ProjectCodeGenerator.java` or to the
generated `AliceJavaFXLauncher` contract.

### Step 1: Generate or inspect an exported project

Open the generated source for the exported project and find:

```text
AliceJavaFXLauncher.java
```

Confirm the launcher uses `Application.launch(args)` in `main(String[] args)`.
Do not accept a launcher that calls `Program.main(...)` directly from `main`.

### Step 2: Check the evidence markers

The generated launcher should contain the stable prefixes:

```text
ALICE_LAUNCHER_EVIDENCE
ALICE_LAUNCHER_NO_GO
```

The success path should distinguish JavaFX startup, stage receipt, scene setup,
and Program delegation. The no-go path should be clearly separate from success.

### Step 3: Interpret a headless run

In an environment without a usable display, a launcher run can produce:

```text
ALICE_LAUNCHER_EVIDENCE main-entered
ALICE_LAUNCHER_EVIDENCE javafx-launch-attempted
ALICE_LAUNCHER_NO_GO display-unavailable
```

This is the expected deterministic no-go result for a display-unavailable
environment. It proves that the launcher attempted JavaFX handoff and that UI
display evidence cannot be proven there. It does not prove scene setup or Program
delegation.

### Step 4: Interpret a JavaFX-started run

When JavaFX invokes `Application.start(...)` with a primary stage, the evidence
should include:

```text
ALICE_LAUNCHER_EVIDENCE javafx-application-started
ALICE_LAUNCHER_EVIDENCE stage-received
ALICE_LAUNCHER_EVIDENCE scene-configured rendering-not-asserted
ALICE_LAUNCHER_EVIDENCE program-main-delegated rendering-not-asserted
```

This proves the JavaFX runtime called into the application, the launcher received
a stage, the launcher configured a minimal scene, and the launcher delegated to
the generated Alice program. It does not prove the scene was drawn to a display.

### Step 5: Keep display-backed evidence separate

If a review requires display-backed evidence, collect it in a separately gated
Xvfb or real-display path and attach the observable artifact to the review. Do
not rename the default launcher evidence to imply display-backed behavior.

## Tutorial: verify exported Ant runtime metadata

Use this flow when reviewing or extending the exported-project Ant behavior.

### Step 1: Start from a no-Sims checkout

From the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Do not pull Git LFS files or Sims/nonfree payloads for this smoke. The
characterization generates a synthetic Alice project and local Ant fixtures.

### Step 2: Run the focused Ant smoke

Run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
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

The exported template also carries module-open arguments in `run.jvmargs`. Those
arguments are part of the template contract, but the current probe only proves
them if it is expanded to inspect JVM input arguments.

Do not treat this smoke as evidence that the generated launcher reached JavaFX
`Application.start(...)`. Launcher handoff and scene/setup evidence belong to the
launcher characterization tests.

## Compatibility rules

1. Keep exported projects compatible with the current Alice 3 baseline unless a
   behavior change is explicitly documented and tested.
2. Keep characterization LFS-free and Sims-free.
3. Prefer synthetic `.a3p` inputs and generated local fixtures.
4. Keep `AliceJavaFXLauncher` as the exported default `main.class`.
5. Preserve `Application.launch(args)` as the JavaFX runtime handoff.
6. Preserve the `Program.main(startingArgs)` background-thread delegation
   behavior after stage validation and minimal scene setup succeed.
7. Validate the primary stage before scene setup or Program delegation.
8. Keep no-go markers distinct from evidence markers.
9. Classify only known JavaFX display/headless failures thrown by the
   `Application.launch(args)` handoff as display-unavailable no-go.
10. Rethrow unexpected failures from `Application.start(...)`, scene setup,
    `Program.main(...)`, or the launch handoff.
11. Preserve `run.jvmargs` behavior unless the replacement proves equivalent
    exported-project runtime behavior.
12. Do not accept a nonzero Java launch as a passing Ant smoke.
13. Document display and rendering limits honestly.

## Limits

The default exported launcher evidence targets the strongest behavior that is
deterministic in local no-Sims validation: JavaFX runtime handoff, JavaFX
`Application.start(...)` entry when available, primary stage receipt, minimal
scene setup, Program delegation, and deterministic no-go classification when a
display is unavailable.

The default evidence does not prove pixels were drawn, a window was shown to a
user, media-heavy content loaded, or a desktop workflow completed. Those claims
require outside-in desktop QA evidence, Xvfb or display-backed assertions,
captured observable artifacts, or an explicit manual observation record.
