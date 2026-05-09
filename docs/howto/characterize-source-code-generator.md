# Characterize Source-Code-Generator Behavior

Use this guide when adding or reviewing focused characterization for Alice AST
and NetBeans source generation. The goal is to protect generated Java source
behavior without expanding into desktop UI automation, visible rendering,
grading, creative assessment, or lesson-completion claims.

For the complete contract, see the
[Generated Story API and AST Source Characterization reference](../reference/generated-story-api-listener-source-characterization.md).

## When to use this guide

Use this guide for changes near:

```text
core/ast/src/main/java/org/lgna/project/ast/SourceCodeGenerator.java
core/ast/src/test/java/org/lgna/project/ast/SourceCodeGeneratorTest.java
netbeans/src/main/java/org/alice/netbeans/project/ProjectCodeGenerator.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorGeneratedSourceTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStandaloneProjectTest.java
netbeans/src/test/java/org/alice/netbeans/project/ProjectCodeGeneratorStoryApiGeneratedSourceTest.java
```

Do not use this lane for:

- full Alice desktop UI automation;
- visible world-rendering correctness;
- Save, Save As, or project recovery behavior;
- grading, creative assessment, or lesson-completion evidence;
- broad Tweedle/player decode coverage;
- formatting-only churn that does not protect generated-source compatibility.

## 1. Choose one generated-source behavior

Start with the smallest generated-source behavior that downstream code depends
on. Good candidates are source snippets, generated file compileability, or
launcher evidence strings that a future refactor could accidentally break.

| Behavior surface | Useful characterization |
| --- | --- |
| Core AST source snippets | Assert exact representative Java text emitted by `JavaCodeGenerator`. |
| Generated `Program.java` or `Scene.java` | Assert selected source fragments and compile the generated file tree when the fixture owns a complete temporary source tree. |
| Story API calls | Assert generated source for current Story API methods such as `setSimulationSpeedFactor`, `setActiveScene`, listener registration, and event payload lambdas. |
| Exported launcher source | Assert stable evidence markers, no-go markers, launcher class name, Ant `main.class`, and delegation boundaries. |

Keep the fixture synthetic and deterministic. Do not add Alice gallery assets,
Git LFS files, network dependencies, local paths, or checked-in generated
projects.

## 2. Add a core AST source snippet

For direct AST generation, add or update a focused case in
`SourceCodeGeneratorTest`. A useful test builds one AST fixture and asserts the
observable generated Java source:

```java
ForEachInArrayLoop loop = forEachLoop("COUNT__");
loop.body.getValue().statements.add(
    new LocalDeclarationStatement(
        new UserLocal("copy", JavaType.STRING_TYPE),
        new LocalAccess(loop.item.getValue())));

String source = generate(loop);

assertTrue(source, source.contains("for(String itemA : new String[]{\"red\", \"blue\"})"));
assertTrue(source, source.contains("final String copy=itemA;"));
assertFalse(source, source.contains("COUNT__"));
```

This protects the stale cached item-name repair while staying independent of the
desktop, project archives, and rendering.

## 3. Add generated NetBeans project source coverage

When the behavior depends on `ProjectCodeGenerator.generateCode(...)`, create a
temporary synthetic Alice project, generate source into a temporary directory,
then assert and compile the output:

```java
Path sourceDirectory = generateProgramSource(
    "synthetic-for-each-loop.a3p",
    programTypeWithForEachLoopMethod(),
    "generated-for-each-loop-src");

Path programPath = sourceDirectory.resolve("Program.java");
String programSource = Files.readString(programPath);

assertFalse(programSource, programSource.contains("COUNT__"));
assertTrue(programSource, programSource.contains(
    "for(String itemA : new String[]{\"red\", \"blue\"})"));
compileProgramAndLauncher("generated-for-each-loop-classes", programPath, sourceDirectory);
```

The test proves selected generated source and compileability for the synthetic
fixture. It does not prove that an exported Alice world runs correctly.

## 4. Add a headless Story API listener seam

For listener behavior, first assert generated source, then invoke only the
headless seam needed by the test:

```java
String sceneSource = Files.readString(sourceDirectory.resolve("Scene.java"));

assertTrue(sceneSource, sceneSource.contains(
    "this.addSceneActivationListener((SceneActivationEvent p0) ->"));
assertTrue(sceneSource, sceneSource.contains(
    "ProjectCodeGeneratorStoryApiGeneratedSourceTest.recordSceneActivationEventPayload(p0);"));
```

The runtime side may instantiate the generated class, call the explicit handler
seam, and assert the latch or payload. It must not launch the Alice desktop,
claim event-loop scheduling coverage, or assert visible rendering correctness.

## 5. Preserve launcher evidence boundaries

When changing the generated exported-project launcher, keep evidence stable and
bounded:

```text
ALICE_LAUNCHER_EVIDENCE
ALICE_LAUNCHER_NO_GO
ALICE_LAUNCHER_RENDER_OBSERVATION
```

The launcher may prove its own JavaFX handoff, observation marker, no-go reason,
and `Program.main(startingArgs)` delegation gate. It must not print success text
that implies Alice world rendering, Save completion, grading, or lesson
completion.

## 6. Validate the focused scope

Initialize the Tweedle grammar submodule in fresh checkouts:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Run the core AST source-generator characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=SourceCodeGeneratorTest \
  test
```

Run the focused NetBeans project generator characterization:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorTest \
  test
```

If the change touches generated `Program.java`, `Scene.java`, listener payloads,
or generated-source compileability, run the generated-source suite too:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl netbeans -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.netbeans.project.ProjectCodeGeneratorTest,org.alice.netbeans.project.ProjectCodeGeneratorGeneratedSourceTest,org.alice.netbeans.project.ProjectCodeGeneratorStandaloneProjectTest,org.alice.netbeans.project.ProjectCodeGeneratorStoryApiGeneratedSourceTest \
  test
```

Do not use shell timeout wrappers for these commands.

## 7. Keep review proof separate from durable docs

Repository documentation describes the stable behavior contract. Review evidence
for a specific PR or commit belongs in the workflow-owned evidence log, not in a
new durable documentation page.

For source-generator review evidence, include only current-head executable facts:

- branch and PR metadata from read-only commands;
- changed-file scope;
- focused Maven commands that were run;
- whether the commands exited successfully;
- explicit non-claims for UI automation, rendering, grading, creative
  assessment, lesson completion, broad Tweedle/player decode, and full world
  execution.

Do not copy secrets, environment dumps, local private paths, full CI logs,
generated binaries, or generated project directories into docs.
