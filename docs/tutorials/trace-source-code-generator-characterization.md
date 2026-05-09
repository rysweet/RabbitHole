# Tutorial: Trace Source-Code-Generator Characterization

This tutorial walks through the implemented source-code-generator
characterization lane from a core AST snippet to generated NetBeans source and
launcher evidence.

## Goal

Protect this behavior:

```text
Alice source generation emits deterministic, compiler-shaped Java for selected
AST, generated project, Story API listener, and exported launcher fixtures while
keeping evidence bounded to source generation and headless seams.
```

The tutorial uses synthetic test fixtures. It does not require Sims, Git LFS
payloads, a desktop display, Alice gallery assets, or broad project archives.

## 1. Start with the core AST generator

Open the core source-generator test:

```text
core/ast/src/test/java/org/lgna/project/ast/SourceCodeGeneratorTest.java
```

Find the for-each characterization that starts from a stale cached item name:

```java
ForEachInArrayLoop loop = forEachLoop("COUNT__");
String source = generate(loop);

assertTrue(source, source.contains("for(String itemA : new String[]{\"red\", \"blue\"})"));
assertFalse(source, source.contains("COUNT__"));
```

This fixture documents the compatibility contract: generated Java must repair
the placeholder item name before emitting the loop header or item access body.

## 2. Trace the statement and expression snippets

The same test class protects representative statement and expression output:

```java
UserLocal count = new UserLocal("count", Integer.class, false);
String source = generate(new ExpressionStatement(new AssignmentExpression(
    JavaType.getInstance(Integer.class),
    new LocalAccess(count),
    AssignmentExpression.Operator.ASSIGN,
    new IntegerLiteral(4))));

assertEquals("count=4;", source);
```

These assertions are intentionally small. They catch generated-source regressions
without freezing every whitespace choice or claiming coverage for every AST node.

## 3. Move to generated `Program.java`

Open the NetBeans generated-source test:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorGeneratedSourceTest.java
```

Follow a synthetic project fixture from `.a3p` generation to Java compilation:

```java
Path sourceDirectory = generateProgramSource(
    "synthetic-for-each-loop.a3p",
    programTypeWithForEachLoopMethod(),
    "generated-for-each-loop-src");

Path programPath = sourceDirectory.resolve("Program.java");
String programSource = Files.readString(programPath);

assertTrue(programSource, programSource.contains(
    "for(String itemA : new String[]{\"red\", \"blue\"})"));
compileProgramAndLauncher("generated-for-each-loop-classes", programPath, sourceDirectory);
```

The fixture proves that the selected generated file is source-compatible with
the test-owned classpath. It does not prove full exported-project runtime
behavior.

## 4. Trace Story API listener source

Open the Story API generated-source test:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

Find the listener payload characterization:

```java
assertTrue(sceneSource, sceneSource.contains(
    "this.addSceneActivationListener((SceneActivationEvent p0) ->"));
assertTrue(sceneSource, sceneSource.contains(
    "ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordSceneActivationEventPayload(p0);"));
```

The runtime probe compiles the generated source, loads the generated class, and
invokes a direct headless seam so the test can check the event payload. The seam
does not launch Alice desktop UI, run a lesson, assess student work, or validate
visible rendering.

## 5. Trace exported launcher evidence

Open the project generator launcher test:

```text
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorTest.java
```

The launcher characterization protects stable evidence markers:

```java
assertTrue(launcherSource.contains("\"ALICE_LAUNCHER_EVIDENCE\""));
assertTrue(launcherSource.contains("\"ALICE_LAUNCHER_NO_GO\""));
assertTrue(launcherSource.contains("\"ALICE_LAUNCHER_RENDER_OBSERVATION\""));
assertTrue(launcherSource.contains("evidence(\"program-main-delegated rendering-not-asserted\")"));
```

The final marker includes `rendering-not-asserted` on purpose. The launcher may
record its own marker-pixel observation and delegation gate, but it does not
certify Alice world rendering correctness.

## 6. Run the focused validations

From the repository root, initialize the required grammar submodule:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the core AST lane:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SourceCodeGeneratorTest \
  test
```

Run the focused NetBeans launcher/project-generator lane:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorTest \
  test
```

Use the generated-source suite, including
`ProjectCodeGeneratorStandaloneProjectTest`, when the changed behavior reaches
generated `Program.java`, `Scene.java`, listener payloads, standalone export
fixtures, or source compileability.

## 7. Interpret failures narrowly

Start from the smallest failing artifact:

| Failure | First thing to inspect |
| --- | --- |
| Core snippet assertion | The generated source string from `SourceCodeGeneratorTest`. |
| Generated project compile failure | The temporary `Program.java`, `Scene.java`, or launcher source created by the failing fixture. |
| Listener seam failure | The generated listener lambda or method call before inspecting the runtime seam. |
| Launcher no-go mismatch | The generated `AliceJavaFXLauncher.java` evidence marker and no-go reason. |

Fix only the source-generation behavior under test, or update the
characterization with an explicit compatibility decision. Do not broaden the
failure into UI automation, rendering correctness, grading, creative assessment,
lesson completion, or full world-execution claims.
