# core/ide Test Coverage Guide

This document describes the test infrastructure for `core/ide` line coverage.
It covers the existing test landscape, the three-tier approach for adding new
coverage, and how to use the support classes.

## Background

The `core/ide` module contains the Alice IDE's logic layer: AST operations,
croquet models, codecs, scene editor logic, declaration editors, cascade menus,
resource management, and project URI handling. Many of these packages have no
Swing dependency and are fully testable headless.

The coverage infrastructure uses JaCoCo (0.8.13, configured in the root POM)
and a tiered test strategy to efficiently cover logic code while excluding
untestable Swing view/component packages.

## Coverage measurement

### Running tests with coverage

```bash
# Tests only (fast feedback)
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac" \
  mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false test

# Tests + JaCoCo report
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac" \
  mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false verify
```

The `xvfb-run` wrapper provides a virtual display for tests that bootstrap the
mini-IDE via `TestIdeBootstrap`. Pure headless tests work without it but the
wrapper is harmless. Use the resilient prefix from the
[JavaFX Xvfb Launcher Reference](reference/javafx-xvfb-launcher.md) so parallel
jobs get separate display numbers and the temporary X server accepts local test
clients.

### Reading the report

After `verify`, the JaCoCo CSV report appears at:

```
core/ide/target/site/jacoco/jacoco.csv
```

The HTML report for browsing per-class detail:

```
core/ide/target/site/jacoco/index.html
```

Quick summary from the command line:

```bash
python3 -c "
import csv, sys
with open('core/ide/target/site/jacoco/jacoco.csv') as f:
    r = csv.DictReader(f)
    missed = covered = 0
    for row in r:
        missed += int(row['LINE_MISSED'])
        covered += int(row['LINE_COVERED'])
    total = missed + covered
    print(f'{covered}/{total} lines = {100*covered/total:.1f}%')
"
```

## Existing test infrastructure

The `core/ide/src/test/java/org/alice/ide/coverage/` directory contains ~960
test files organized around four support classes:

### Support classes

| Class | Role |
|-------|------|
| `HeadlessClassExerciseSupport` | Reflection-based class exercise: loads classes, instantiates with default args, invokes methods. Used by Tier 1 and long-tail tests. |
| `ClassLoadingSweepSupport` | Package-aware class discovery: discovers all `.class` files in `core/ide/target/classes`, sweeps by exact package or package tree, supports default-arg instantiation and method invocation. |
| `SingleClassDefaultArgsSweepTestSupport` | Abstract base for generated single-class tests. Delegates to `ClassLoadingSweepSupport.sweepNamedClassesWithDefaultArgs()`. |
| `CompositeCreateViewSweepSupport` | Discovers all concrete `Composite` subclasses, partitions them, instantiates on the EDT, and calls `createView()`. |

### Existing test categories

| Category | Count | Pattern | Support class | Display needed? |
|----------|-------|---------|---------------|-----------------|
| GUI package sweeps | ~129 | `*GuiSweepTest.java` | `ClassLoadingSweepSupport` via `AbstractExactGuiPackageSweepTest` | Yes (`Assume.assumeTrue`) |
| Generated single-class tests | ~542 | `*GeneratedCoverageTest.java` | `SingleClassDefaultArgsSweepTestSupport` | No (headless-safe) |
| Composite create-view sweeps | ~31 | `CompositeCreateViewSweep*Test.java` | `CompositeCreateViewSweepSupport` | Yes (Xvfb) |
| Long-tail headless tests | ~20 | `*LongTailTest.java` | `HeadlessClassExerciseSupport.exercise()` | No |
| Small-class target sweep | 1 | `SmallClassCoverageSweepTest` | `HeadlessClassExerciseSupport.exerciseSweepTargets()` | No |
| Class-loading sweeps | 3 | `ClassLoadingSweepTest`, `FocusedPackageClassLoadingSweepTest`, `LargestPackageClassLoadingSweepTest` | `ClassLoadingSweepSupport` | Yes |
| Mini-IDE method sweep | 1 | `MiniIdeRemainingNonGuiMethodSweepTest` | Direct reflection | No |
| Hand-written reflection tests | 1 | `TopRemainingNonGuiHeadlessSweepTest` | Direct reflection + `HeadlessClassExerciseSupport` | No |

### Abstract base classes

| Base class | Used by |
|------------|---------|
| `AbstractExactGuiPackageSweepTest` | GUI package sweeps |
| `AbstractMiniIdeDefaultArgsSweepTest` | Mini-IDE sweep tests (boots `TestIdeBootstrap`) |
| `AbstractMiniIdeNamedClassMethodSweepTest` | Named-class method sweeps |
| `AbstractMiniIdeExactPackageMethodSweepTest` | Exact-package method sweeps |
| `AbstractCompositeCreateViewSweepPartitionTest` | Composite view partition sweeps |
| `SingleClassDefaultArgsSweepTestSupport` | Generated single-class tests |

## The three tiers for adding coverage

When pushing coverage higher, new tests follow one of three tiers.

### Tier 1 — Sweep target list (`small-class-targets.txt`)

**What it is:** A text file listing fully-qualified class names, one per line.
`HeadlessClassExerciseSupport.exerciseSweepTargets()` loads this file and
exercises each class via reflection: loading, instantiation with default
arguments, static method invocation, and instance method probing.

**File location:**

```
core/ide/src/test/resources/org/alice/ide/coverage/small-class-targets.txt
```

**How to add coverage:** Append fully-qualified class names to the file, one
per line. Each class is exercised independently — there is no automatic
inner-class discovery, so list inner classes explicitly if needed (using `$`
notation).

```text
# Example additions
org.alice.ide.ast.export.TypeInfo
org.alice.ide.ast.export.MemberInfo
org.alice.ide.cascade.fillerinners.PoseFillerInner
org.alice.ide.identifier.IdentifierNameGenerator
```

**What it covers:** Static initializers, constructors, getters, simple methods.
Typically ~20 covered lines per class. Best for small utility/data classes.

**What it skips:** Classes whose names match the exclusion patterns in
`isHeadlessFriendlyName()` — see the full exclusion table below. Key
exclusions: names containing `.views.`, `.swing.`, `.capture.`,
`.sceneeditor.interact.` and suffixes like `View`, `Panel`, `Composite`,
`Operation`, `Menu`, `Cascade`, `FillIn`, `Wizard`, etc.

**Yield:** ~20 lines per class added. Adding 60 classes covers ~1,200 lines.

### Tier 2 — Headless package sweep tests

**What they are:** JUnit 4 test classes that exercise all headless-safe classes
in a specific package. The test enumerates class names explicitly and passes
them to `HeadlessClassExerciseSupport.exercise()`.

**Naming convention:**

```
{PackageDescription}HeadlessSweepTest.java
```

**Location:**

```
core/ide/src/test/java/org/alice/ide/coverage/
```

**Example:**

```java
package org.alice.ide.coverage;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class SceneeditorLogicHeadlessSweepTest {

  private static final String[] CLASS_NAMES = {
      "org.alice.stageide.sceneeditor.SetUpMethodGenerator",
      "org.alice.stageide.sceneeditor.SceneEditorFieldManager",
      "org.alice.stageide.sceneeditor.SceneEditorLifecycleManager"
      // ... discovered from JaCoCo report or:
      // find core/ide/target/classes -path '*/sceneeditor/*.class' | ...
  };

  @Test
  public void exerciseSceneeditorLogicClasses() {
    HeadlessClassExerciseSupport.SmokeStats stats =
        HeadlessClassExerciseSupport.exercise(CLASS_NAMES);
    assertTrue("Expected to load classes from sceneeditor logic, loaded: "
        + stats.getLoadedCount(), stats.getLoadedCount() > 0);
  }
}
```

**Important:** `HeadlessClassExerciseSupport` does not have a
package-discovery method. Sweep tests must enumerate class names explicitly.
Use the JaCoCo HTML report or `find core/ide/target/classes -name '*.class'`
to discover class names in a package.

Note: Existing GUI sweep tests (the `*GuiSweepTest.java` files) use
`ClassLoadingSweepSupport` which *does* have package discovery
(`sweepExactPackage()`, `sweepPackageTree()`). New headless sweep tests use
`HeadlessClassExerciseSupport` which requires explicit class name lists but
provides deeper exercise (instantiation, method invocation, not just loading).

**How to add a new package sweep:**

1. Identify the target package(s) from the JaCoCo report — look for high
   `LINE_MISSED` with non-view class names.
2. Create a new `*HeadlessSweepTest.java` in the coverage directory.
3. List the fully-qualified class names in a `CLASS_NAMES` array.
4. Call `HeadlessClassExerciseSupport.exercise()` with the class names.
5. Assert that `stats.getLoadedCount() > 0` (not exact counts — class
   discovery varies with build state).

**Implemented sweep tests:**

| Test class | Target packages |
|------------|-----------------|
| `SceneeditorLogicHeadlessSweepTest` | `org.alice.stageide.sceneeditor` (non-interact, non-viewmanager) |
| `IdeCommonHeadlessSweepTest` | `org.alice.ide.ast`, `org.alice.ide.identifier`, `org.alice.ide.name`, `org.alice.ide.type`, `org.alice.stageide.ast`, `org.alice.stageide.modelresource` |
| `DeclarationsEditorHeadlessSweepTest` | `org.alice.ide.declarationseditor` |
| `CodeEditorHeadlessSweepTest` | `org.alice.ide.codeeditor` |
| `IdeMemberHeadlessSweepTest` | `org.alice.ide.member` |
| `GalleryBrowserHeadlessSweepTest` | `org.alice.stageide.gallerybrowser` |
| `ResourceManagerHeadlessSweepTest` | `org.alice.ide.resource.manager` |
| `InstanceFactoryHeadlessSweepTest` | `org.alice.ide.instancefactory` |
| `IdeIssueHeadlessSweepTest` | `org.alice.ide.issue` |
| `CroquetModelsMiscHeadlessSweepTest` | `org.alice.ide.croquet.models` (misc sub-packages) |
| `StageideOneshotHeadlessSweepTest` | `org.alice.stageide.oneshot` |
| `StageidePropertiesHeadlessSweepTest` | `org.alice.stageide.properties` |
| `SceneeditorViewmanagerHeadlessSweepTest` | `org.alice.stageide.sceneeditor.viewmanager` |

**Yield:** ~80–200 covered lines per package sweep, depending on package size.

### Tier 3 — Targeted unit tests

**What they are:** Conventional JUnit 4 test classes that exercise specific
logic methods with real inputs and assertions. These go deeper than sweep tests
by covering conditional branches, error paths, and business logic.

**Naming convention:**

```
{ClassName}Test.java  or  {ClassName}LogicTest.java
```

**Location:** Mirror the source package under `core/ide/src/test/java/`.

**Example:**

```java
package org.alice.ide.cascade;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExpressionCascadeManagerTest {

  @Test
  public void nullContextReturnsEmptyCascade() {
    // Test that requesting a cascade with no context gracefully
    // returns an empty or default result rather than throwing.
    // ...
  }

  @Test
  public void literalTypesProduceCorrectFillers() {
    // Verify that standard Java types map to the expected
    // cascade filler inner classes.
    // ...
  }
}
```

**How to add targeted tests:**

1. Pick a class with high uncovered line count from the JaCoCo HTML report.
2. Identify public/package-visible methods with non-trivial logic.
3. Write tests exercising normal paths, edge cases, and error conditions.
4. Use `@Test(expected = ...)` or `Assert.assertThrows()` for expected
   exceptions.
5. Avoid calling methods that create Swing components — test the logic layer
   only.

**Implemented and planned targeted test classes:**

| Test class | What it covers | Status |
|------------|---------------|--------|
| `HtmlEncoderLogicTest` | HTML encoding/escaping utilities | ✅ Implemented |
| `MethodInvocationBlankLogicTest` | One-shot method invocation dispatch logic | ✅ Implemented |
| `SetUpMethodGeneratorLogicTest` | Scene setup code generation paths | Planned |
| `SceneEditorFieldManagerLogicTest` | Field registration and lookup in scene editor | Planned |
| `ExpressionCascadeManagerTest` | Cascade menu construction for expression types | Planned |
| `MarkerUtilitiesTest` | Camera and object marker helper methods | Planned |
| `SceneFieldCodeGeneratorLogicTest` | Scene field initialization code generation | Planned |
| `TreeUtilitiesLogicTest` | Model resource tree traversal utilities | Planned |
| `DeclarationCompositeHistoryTest` | Declaration editor navigation history | Planned |

**Yield:** ~30–100 covered lines per test class, with deeper branch coverage.

## Packages excluded from headless exercise

The following name patterns cause classes to be **skipped** by
`HeadlessClassExerciseSupport.isHeadlessFriendlyName()`. Do not write
Tier 1/2 headless sweep tests targeting classes that match these patterns.
(The GUI sweep tests using `ClassLoadingSweepSupport` do not apply this
filter — they load all classes in a package regardless of name.)

| Pattern | Type | Reason |
|---------|------|--------|
| `.views.` | contains | Swing view layer — requires real display |
| `.swing.` | contains | Direct Swing usage |
| `.capture.` | contains | Screen capture — needs display |
| `.sceneeditor.interact.` | contains | 3D interaction layer — needs OpenGL |
| `MemoryUsage` | contains | Memory monitoring — JVM-internal |
| `WindowDetector` | contains | Window detection — needs display |
| `View` | suffix | Swing views |
| `Pane` | suffix | Swing panes |
| `Panel` | suffix | Swing panels |
| `Frame` | suffix | Swing frames |
| `Dialog` | suffix | Swing dialogs |
| `Renderer` | suffix | Rendering classes |
| `Manipulator` | suffix | 3D interaction manipulators |
| `StencilView` | suffix | Stencil overlay views |
| `Composite` | suffix | Croquet composites (UI-bound) |
| `Operation` | suffix | Croquet operations (UI-bound) |
| `Menu` | suffix | Menu models (UI-bound) |
| `Cascade` | suffix | Cascade menus (UI-bound) |
| `FillIn` | suffix | Cascade fill-in items (UI-bound) |
| `Wizard` | suffix | Multi-step wizard dialogs |

If a class is incorrectly excluded, verify the filter logic in
`HeadlessClassExerciseSupport.isHeadlessFriendlyName()` before adding an
override.

## Troubleshooting

### Tests fail with `HeadlessException`

The test needs a display. Wrap the Maven command with `xvfb-run`:

```bash
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac" \
  mvn -pl core/ide -am test
```

### `TestIdeBootstrap.boot()` throws `NullPointerException`

Ensure the Tweedle grammar submodule is initialized:

```bash
git submodule update --init tweedle-lang
```

### JaCoCo report is empty or missing

Run `verify` instead of `test` — JaCoCo's report goal is bound to the verify
phase:

```bash
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac" \
  mvn -pl core/ide -am verify
```

### Sweep test loads 0 classes

Check that the package name in the test matches actual compiled classes under
`core/ide/target/classes/`. The class discovery walks the compiled output
directory, so a clean build is required if source files were recently added.

### A class throws in its static initializer

`HeadlessClassExerciseSupport` catches `ExceptionInInitializerError` and
records it in `SmokeStats.failures`. The sweep test asserts
`getLoadedCount() > 0`, not that all classes load — so one failing class does
not fail the test. Check the `failures` map for debugging.

## Adding coverage for a new package

Follow this decision tree:

1. **Is the package mostly small data/utility classes?**
   → Add class names to `small-class-targets.txt` (Tier 1)

2. **Is the package a medium-to-large package with many classes?**
   → Create a `*HeadlessSweepTest.java` (Tier 2)

3. **Does the package have a few large logic classes with complex methods?**
   → Write targeted `*Test.java` or `*LogicTest.java` (Tier 3)

4. **Is the package primarily Swing views/components?**
   → Skip it. Coverage gains are not worth the fragility.

For maximum efficiency, combine Tier 2 (broad coverage) with Tier 3 (deep
coverage) for the same package.

## API reference

### HeadlessClassExerciseSupport

The core utility class for Tier 1 and Tier 2 headless tests. Loads classes,
instantiates with default arguments, and invokes methods via reflection.

```java
// Exercise specific classes by fully-qualified name.
static SmokeStats exercise(String... classNames)
static SmokeStats exercise(Collection<String> classNames)

// Exercise all classes listed in small-class-targets.txt.
static SmokeStats exerciseSweepTargets()

// Name-based headless filter (checks for GUI suffixes and package fragments).
private static boolean isHeadlessFriendlyName(String name)

// Delegates to isHeadlessFriendlyName(clazz.getName()) — does not inspect
// the class hierarchy.
private static boolean isHeadlessFriendly(Class<?> clazz)
```

There is no `exercisePackage()` method. Sweep tests must enumerate class
names and pass them to `exercise()`.

### SmokeStats

```java
static final class SmokeStats {
    int getLoadedCount()           // Classes successfully loaded via Class.forName()
    int getInstantiatedCount()     // Classes instantiated via reflection
    int getFailureCount()          // Classes that threw during loading/exercise
    int getDiscoveredTargetCount() // Total classes discovered (for sweep targets)

    void assertLoaded(String... classNames)  // Fails if any named class wasn't loaded
}
```

### ClassLoadingSweepSupport

Package-aware sweep support used by GUI sweep tests and class-loading sweeps.
Discovers classes from `core/ide/target/classes/` at startup and indexes them
by package.

```java
// Sweep all classes in the module.
static SweepResult sweepAllClasses()

// Sweep classes in an exact package (not sub-packages).
static SweepResult sweepExactPackage(String packageName)

// Sweep classes in a package tree (package + all sub-packages).
static SweepResult sweepPackageTree(String packagePrefix)

// Sweep specific named classes with default-arg instantiation.
static SweepResult sweepNamedClassesWithDefaultArgs(String... classNames)
```

## Configuration

No special Maven profiles or properties are needed beyond the standard build.
The tests use:

- **JUnit 4.13.1** — `@Test`, `Assert.*`, `@Rule Timeout`
- **JaCoCo 0.8.13** — configured in root POM, runs automatically with `verify`
- **Xvfb** — needed for `TestIdeBootstrap`-based tests (available in CI)

The `surefire-maven-plugin` picks up test classes matching `*Test.java`
automatically. No surefire configuration changes are required.
