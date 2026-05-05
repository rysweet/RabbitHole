# Exported NetBeans Ant Project Behavior

This reference documents the exported Alice 3 NetBeans project behavior that is
protected by characterization tests. The protected slice proves that an exported
project is not only source-generatable and classpath-compilable: its Ant `run`
target also consumes the exported project runtime metadata up to the Alice GUI
launch boundary.

## Contents

- [Scope](#scope)
- [User-visible behavior](#user-visible-behavior)
- [Configuration](#configuration)
- [Executable characterization](#executable-characterization)
- [API reference](#api-reference)
- [Validation commands](#validation-commands)
- [Tutorial: verify exported Ant runtime metadata](#tutorial-verify-exported-ant-runtime-metadata)
- [Compatibility rules](#compatibility-rules)
- [Limits](#limits)

## Scope

The exported project behavior lives in the NetBeans project template:

```text
netbeans/src/main/resources/ProjectTemplate/
```

Characterization coverage lives beside the NetBeans export tests:

```text
netbeans/src/test/java/org/alice/netbeans/project/Alice3ProjectTemplateAntSmokeTest.java
```

The behavior slice covers a generated, LFS-free Alice project exported into the
NetBeans Ant template. It verifies that the exported Ant `run` target:

1. Compiles generated Alice project source and a small runtime probe.
2. Launches the requested `main.class` through the template's Ant `run` target.
3. Applies `run.jvmargs`, including assertions and Alice runtime system
   properties.
4. Resolves the Alice library source-root interpolation used by
   `org.alice.ide.rootDirectory`.
5. Fails loudly if Java exits nonzero instead of accepting `Java Result:` as a
   successful smoke.

This is a behavior-level exported-project contract. It intentionally goes beyond
checking generated source text, compile success, or classpath presence, but it
does not claim full Alice GUI launch coverage.

## User-visible behavior

An exported Alice project is a standard NetBeans Ant Java project. The project
template supplies `build.xml`, `nbproject/build-impl.xml`,
`nbproject/project.xml`, `nbproject/project.properties`, and `manifest.mf`.

For normal users, the exported project opens in NetBeans and runs with:

```text
main.class = AliceJavaFXLauncher
```

For characterization, the test overrides `main.class` with a tiny probe class.
That override uses the same Ant `run` target that NetBeans uses, but stops before
starting the Alice JavaFX/Swing GUI. The probe succeeds only when Ant passes the
runtime configuration expected by exported projects.

| Runtime fact | Expected behavior |
| --- | --- |
| Main class | Defaults to `AliceJavaFXLauncher`; can be overridden by Ant property for smoke probes. |
| Assertions | Enabled by `-ea` from `run.jvmargs`. |
| GlueGen temp cache | Disabled by `-Djogamp.gluegen.UseTempJarCache=false`. |
| Alice root directory | Provided through `-Dorg.alice.ide.rootDirectory="${libs.Alice3Library.src}_root"`. |
| Java module access | Supplied by the exported template's `--add-opens` arguments. |
| Ant failure handling | Nonzero Java execution is not accepted as success; the log must not contain `Java Result:`. |

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

The characterization test writes those properties through
`Alice3LibraryClasspathTestSupport`, using local build outputs and scratch files
instead of Sims, nonfree modules, Git LFS payloads, or external downloads.

When running validation through automation that preserves local Node settings,
keep the repository preference:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

The Maven and Ant behavior itself is Java-based; `NODE_OPTIONS` is preserved for
workflow compatibility with repository automation.

## Executable characterization

The executable characterization is:

```text
Alice3ProjectTemplateAntSmokeTest.exportedProjectAntRunTargetAppliesRuntimeJvmArgumentsUpToGuiBoundary
```

The test creates all inputs under a temporary or target-local smoke directory:

1. Unpack `ProjectTemplate.zip` into a temporary exported project directory.
2. Generate a synthetic `.a3p` project with no LFS or Sims dependencies.
3. Generate exported Java source into the project's `src` directory.
4. Add `AntRuntimeConfigurationProbe.java` to the same source tree.
5. Write deterministic Alice library properties for Ant.
6. Run the exported Ant `run` target with `main.class` set to the probe class.
7. Assert that the Ant log contains a success marker and no `Java Result:`.

The probe checks behavior that can be proven locally:

| Probe assertion | Why it matters |
| --- | --- |
| Java assertions are enabled | Proves `-ea` from `run.jvmargs` reached the launched JVM. |
| `org.alice.ide.rootDirectory` is set | Proves exported runtime system properties are passed by Ant. |
| The root directory value has no unresolved `${...}` placeholder | Proves Ant interpolated the Alice library source property. |
| The root directory ends with `aliceSource.jar_root` | Proves the exported Alice source-root convention is preserved. |
| The probe prints `ANT_RUNTIME_CONFIGURATION_PROBE_OK` | Gives reviewers a deterministic success marker in the Ant log. |

## API reference

There is no new public Java API for this feature. The stable surface is the
exported NetBeans/Ant project contract:

| Surface | Contract |
| --- | --- |
| `ProjectTemplate.zip` | Contains the NetBeans Ant project files used for exported Alice Java projects. |
| `nbproject/project.properties` | Owns `main.class`, `javac.classpath`, `run.classpath`, `run.jvmargs`, and Java release settings. |
| `libs.Alice3Library.classpath` | External Ant/NetBeans library binding for Alice runtime jars. |
| `libs.Alice3Library.src` | External Ant/NetBeans library binding used to derive `org.alice.ide.rootDirectory`. |
| Ant `run` target | Compiles project classes and launches `${main.class}` with `${run.jvmargs}` and `${run.classpath}`. |

Template changes must preserve those names unless a compatibility-breaking
change is explicitly documented and characterized.

## Validation commands

Run focused validation from the repository root. Initialize the Tweedle grammar
submodule before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the focused exported-project Ant smoke:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest#exportedProjectAntRunTargetAppliesRuntimeJvmArgumentsUpToGuiBoundary \
  test
```

Run the relevant no-Sims NetBeans reactor validation:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  test
```

If the focused smoke fails with missing generated Tweedle parser classes, check
the submodule before changing test or template behavior:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

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

### Step 2: Run the focused smoke

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

### Step 3: Interpret the evidence

Treat a passing focused smoke as evidence that the exported Ant project consumes
runtime metadata correctly up to the GUI launch boundary. It proves the exported
`run` target passes assertions, Alice system properties, interpolated library
paths, module-open arguments, and classpath settings to the launched JVM.

Do not treat this smoke as evidence that the full Alice GUI launched, rendered a
window, loaded media, or completed a user workflow. Full desktop evidence stays
in the outside-in QA lane.

### Step 4: Run the broader NetBeans check

After changing the template, generator, or NetBeans export behavior, run:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -pl netbeans -am \
  -DfailIfNoTests=false \
  test
```

Review failures as compatibility signals. Do not weaken the smoke to hide a
runtime configuration regression.

## Compatibility rules

1. Keep exported projects compatible with the current Alice 3 baseline unless a
   behavior change is explicitly documented and tested.
2. Keep the characterization LFS-free and Sims-free.
3. Prefer synthetic `.a3p` inputs and generated local fixtures.
4. Keep `AliceJavaFXLauncher` as the exported default `main.class`.
5. Preserve `run.jvmargs` behavior unless the replacement proves equivalent
   exported-project runtime behavior.
6. Do not accept a nonzero Java launch as a passing Ant smoke.
7. Document GUI-boundary limits honestly; do not claim full GUI launch from a
   probe-main smoke.

## Limits

This feature proves the highest exported-project behavior that is deterministic
in local no-Sims validation: the Ant `run` target launches a class through the
exported runtime configuration. It does not launch Alice's GUI in the smoke test.

Full exported-project GUI launch, interactive run/debug behavior, media-heavy
projects, and display-backed workflows require outside-in desktop QA evidence or
manual review artifacts. Those belong in the Alice desktop QA scenario lane, not
in this headless NetBeans Ant characterization.
