# Tutorial: Trace the Silver Thread Edit-Save-Readback Test

This tutorial walks through the connected silver thread test that chains
procedure editing through production save to archive readback. You will trace
the test method to understand how the three operations — edit, save, readback —
compose into a single headless proof.

For the full contract, see the [Silver Thread Edit-Save-Readback Test
reference](../reference/silver-thread-edit-save-readback-test.md).

## Contents

- [Goal](#goal)
- [1. Understand the motivation](#1-understand-the-motivation)
- [2. Trace the starter project](#2-trace-the-starter-project)
- [3. Trace the EatmeEditProcedure invocation](#3-trace-the-eatmeeditprocedure-invocation)
- [4. Trace the post-edit verification](#4-trace-the-post-edit-verification)
- [5. Trace the singleton reset](#5-trace-the-singleton-reset)
- [6. Trace the TestProjectApplication bootstrap](#6-trace-the-testprojectapplication-bootstrap)
- [7. Trace the production save](#7-trace-the-production-save)
- [8. Trace the readback and fidelity verification](#8-trace-the-readback-and-fidelity-verification)
- [9. Trace the proof artifact](#9-trace-the-proof-artifact)
- [10. Run the test](#10-run-the-test)
- [11. Compare with adjacent silver thread tests](#11-compare-with-adjacent-silver-thread-tests)
- [12. Understand the boundaries](#12-understand-the-boundaries)

## Goal

Understand how `SilverThreadEditSaveReadbackTest` proves that a comment
appended by `EatmeEditProcedure` survives the production save pipeline and
remains present when the saved archive is reopened. After this tutorial you will
be able to explain why the test chains `EatmeEditProcedure.run()` into
`ProjectApplication.saveProjectTo` instead of testing each in isolation, what
the `resetApplicationSingleton()` call enables, and why the readback assertion
is the strongest claim in the chain.

Open the source in
`core/ide/src/test/java/org/alice/ide/SilverThreadEditSaveReadbackTest.java`
alongside this guide.

## 1. Understand the motivation

The existing test suite has two independent proofs:

- `EatmeEditProcedureTest` proves that `EatmeEditProcedure.run()` can append a
  `Comment` to a procedure and write the result to an `.a3p` archive. But it
  does not pass the edited project through the production save pipeline.

- `SilverThreadSaveTest` proves that `ProjectApplication.saveProjectTo` can
  save a project and that the AST survives a readback. But it builds its project
  with direct AST manipulation, not through `EatmeEditProcedure`.

Neither test proves the operations compose. Specifically:

- Does `EatmeEditProcedure` produce an archive that `TestProjectApplication` can
  load?
- Does the Application singleton state from `EatmeEditProcedure.run()` interfere
  with `TestProjectApplication` construction?
- Does the edited AST survive the production save pipeline's
  `ensureProjectCodeUpToDate`, `createThumbnail`, and archive serialization?

`SilverThreadEditSaveReadbackTest` answers all three questions in one test
method.

## 2. Trace the starter project

Find the project creation:

```java
Project project = projectWithSceneMethod("eatmeFirstLesson");
```

This helper (copied from `EatmeEditProcedureTest`) builds:

```text
Program (NamedUserType extending SProgram)
  └─ field: Scene (NamedUserType extending SScene)
       └─ method: eatmeFirstLesson(void)
            └─ body: BlockStatement
                 └─ Comment("existing")
```

The `Comment("existing")` statement is the pre-existing content. After the edit,
the method body will have two statements: the original `Comment("existing")` and
the appended `Comment("silver-thread-edit-save-readback-proof")`.

The project is written to a temp file:

```java
File starterFile = new File(tempDir, "starter.a3p");
IoUtilities.writeProject(starterFile, project, ...);
```

This creates the `.a3p` archive that `EatmeEditProcedure` will load.

## 3. Trace the EatmeEditProcedure invocation

Find the edit invocation:

```java
int status = EatmeEditProcedure.run(new String[]{
    "--project", starterFile.getAbsolutePath(),
    "--procedure-selector", "scene.eatmeFirstLesson",
    "--edit-spec", "append-comment:silver-thread-edit-save-readback-proof",
    "--evidence-dir", evidenceDir.getAbsolutePath(),
    "--json"
}, out, err);
```

Key points:

- **`--project`** points to the starter `.a3p` just written.
- **`--procedure-selector`** identifies `scene.eatmeFirstLesson` — the method
  built by `projectWithSceneMethod`.
- **`--edit-spec "append-comment:silver-thread-edit-save-readback-proof"`** tells
  `EatmeEditProcedure` to append a `Comment` to the method body. The text after
  the colon (`silver-thread-edit-save-readback-proof`) becomes the `Comment`
  text. This is a caller-controlled value, not an internal default.
- **`--evidence-dir`** is where `EatmeEditProcedure` writes `edited-project.a3p`
  and its JSON evidence.
- **`--json`** enables structured JSON output to stdout.
- **`run()` is a static method** on `EatmeEditProcedure` (the constructor is
  private). The params are named `out`/`err`, not `stdout`/`stderr`. Note:
  `run()` is currently package-private in `org.alice.tools`; it must be promoted
  to `public` so this test (in `org.alice.ide`) can call it.

Internally, `EatmeEditProcedure.run()` calls `ensureCroquetApplication()` (call
at lines 163–165, definition at lines 215–251 of `EatmeEditProcedure.java`),
which installs an anonymous `Application` singleton. This is a side effect that
must be cleaned up before the save phase.

## 4. Trace the post-edit verification

After the edit, the test verifies the edited project independently of the save
phase:

```java
assertEquals(0, status);
File editedFile = new File(evidenceDir, "edited-project.a3p");
assertTrue(editedFile.exists());

Project editedProject = IoUtilities.readProject(editedFile);
// Navigate to eatmeFirstLesson method body
// Assert 2 statements: existing + appended
// Assert second statement is Comment("silver-thread-edit-save-readback-proof")
```

This intermediate check catches edit failures before they cascade into the save
phase. If `EatmeEditProcedure` failed, the test fails here with a clear message
rather than a confusing failure in `saveProjectTo`.

## 5. Trace the singleton reset

Find the reset:

```java
resetApplicationSingleton();
```

This uses reflection to null the `Application.singleton` static field:

```java
private static void resetApplicationSingleton() throws Exception {
    Field field = Application.class.getDeclaredField("singleton");
    field.setAccessible(true);
    field.set(null, null);
}
```

Why is this needed? `EatmeEditProcedure.run()` installs an anonymous
`Application` via `ensureCroquetApplication()`. The `Application` class enforces
a single instance — constructing `TestProjectApplication` without clearing the
singleton would throw `IllegalStateException: singleton already set`.

This is the same pattern used in `SilverThreadSaveTest` (lines 127–128). The
reset is safe because the anonymous Application from `EatmeEditProcedure` is no
longer needed — the test is transitioning from the edit phase to the save phase.

## 6. Trace the TestProjectApplication bootstrap

Find the application bootstrap:

```java
TestProjectApplication application = applicationWith(editedProject, new InMemoryProjectLoader());
```

The `applicationWith` helper:

1. Calls `resetApplicationSingleton()` (already done, but safe to call twice).
2. Constructs `TestProjectApplication(editedProject)` — a headless subclass of
   `ProjectApplication` that overrides:
   - `createFrameTitleGenerator()` → returns constant string (avoids null document frame)
   - `updateTitle()` → no-op
   - `createThumbnail()` → 1×1 `BufferedImage`
   - `ensureProjectCodeUpToDate()` → no-op
   - `forceProjectCodeUpToDate()` → no-op
3. Calls `installLoader()` to set the `uriProjectLoader` field via reflection to
   an `InMemoryProjectLoader` that returns `isNewProject() = false`.

Note a critical difference from `SilverThreadSaveTest`: the project passed to
`TestProjectApplication` is the **edited project read back from
`EatmeEditProcedure`'s output**, not a directly constructed synthetic project.
This is what makes the test "connected" — the save phase consumes the edit
phase's output.

Also find the singleton initialization:

```java
RecentProjectCountState.getInstance().setValueTransactionlessly(10);
```

This is required because `saveProjectTo` records the save in the recent projects
list, which needs an initialized count. Same pattern as `SilverThreadSaveTest`
(line 85).

## 7. Trace the production save

Find the save call:

```java
File savedFile = new File(tempDir, "saved.a3p");
application.saveProjectTo(savedFile);
```

The production `saveProjectTo` path executes:

1. `ProjectSaveTargetPlan.choose()` — selects save strategy.
2. URI adoption — sets `uriProjectLoader` to a `FileProjectLoader` for the
   target file.
3. `ensureProjectCodeUpToDate()` — no-op in `TestProjectApplication`.
4. `createThumbnail()` — returns 1×1 stub in `TestProjectApplication`.
5. `IoUtilities.writeProject(file, project, dataSources)` — writes the `.a3p`
   archive with thumbnail and manifest.
6. `backupSavedProject()` — creates save backup.
7. `RecentProjectsListData.handleSave()` — records in recent projects.

Steps 1, 2, 5, 6, and 7 are production code. This is the same path that runs
when a student clicks File → Save.

The test asserts:

```java
assertTrue(savedFile.exists());
assertTrue(savedFile.length() > 0);
```

## 8. Trace the readback and fidelity verification

Find the readback:

```java
Project reopened = IoUtilities.readProject(savedFile);
```

Then the navigation to the edited method:

```java
NamedUserType programType = reopened.getProgramType();
// Navigate: programType → fields[0] (Scene) → valueType → findMethod("eatmeFirstLesson")
UserMethod method = ...;
BlockStatement body = (BlockStatement) method.body.getValue();
```

And the critical assertion:

```java
assertEquals(2, body.statements.size());
Comment secondStatement = (Comment) body.statements.get(1);
assertEquals("silver-thread-edit-save-readback-proof", secondStatement.text.getValue());
```

This is the strongest assertion in the test. It proves:

- The edited `.a3p` from `EatmeEditProcedure` was loadable by
  `TestProjectApplication`.
- The production save pipeline (`saveProjectTo`) preserved the AST including the
  appended comment.
- The standard archive reader (`IoUtilities.readProject`) can deserialize the
  production-saved archive.
- The comment text survived the full chain: edit → save → readback.

The first statement `Comment("existing")` is also implicitly verified by the
statement count of 2 — if the edit had replaced instead of appending, the count
would be wrong.

## 9. Trace the proof artifact

Find the artifact write:

```java
File proofFile = new File(evidenceDir, "edit-save-readback-proof.json");
// Write JSON with schema_version, chain_steps, timestamp
```

The proof artifact is a machine-readable record of all three chain steps. Its
schema version `"eatme.alice-edit-save-readback-proof/v1"` follows the
`EatmeEditProcedure` naming convention. Each chain step (`edit`, `save`,
`readback`) records its status and key facts.

The artifact is written to the `TemporaryFolder` evidence directory and is
automatically cleaned up after the test. During the test, it is validated:

```java
assertTrue(proofFile.exists());
JsonObject proof = JsonParser.parseReader(new FileReader(proofFile)).getAsJsonObject();
assertEquals("eatme.alice-edit-save-readback-proof/v1",
    proof.get("schema_version").getAsString());
```

## 10. Run the test

From the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.SilverThreadEditSaveReadbackTest \
  test
```

The test passes without a display server or network access.

## 11. Compare with adjacent silver thread tests

| Dimension | `EatmeEditProcedureTest` | `SilverThreadSaveTest` | `SilverThreadLaunchBuildRunTest` | **This test** |
| --- | --- | --- | --- | --- |
| Edit path | `EatmeEditProcedure.run()` | Direct AST | Direct AST | `EatmeEditProcedure.run()` |
| Save path | `IoUtilities.writeProject` (inside tool) | `saveProjectTo` | `IoUtilities.writeProject` | `saveProjectTo` |
| Readback | Yes — edited output | Yes — saved output | Yes — saved output | Yes — **production-saved** output of **tool-edited** project |
| VM execution | No | No | Yes | No |
| Singleton reset | No | Yes | No | Yes (between phases) |
| Chain coverage | Edit only | Save only | Create → run → save | **Edit → Save → Readback** |
| Starter project type | Synthetic `eatmeFirstLesson` | Synthetic program | `indiaMinimum.a3p` (real) | Synthetic `eatmeFirstLesson` |

The four tests are complementary. Each owns a different slice of the silver
thread. This test uniquely proves the edit-to-save handoff.

## 12. Understand the boundaries

After tracing the test, confirm you can answer:

| Question | Answer |
| --- | --- |
| Does this prove rendering works? | No. Thumbnail is a 1×1 stub. |
| Does this prove `ensureProjectCodeUpToDate` works? | No. It is stubbed as a no-op. |
| Does this prove the VM can execute the saved project? | No. `SilverThreadLaunchBuildRunTest` covers that. |
| Does this prove object placement works? | No. `EatmeEditProcedureTest` covers that. |
| Does this prove real starter projects work? | No. Uses a synthetic project. |
| Does this prove the edit and save compose correctly? | **Yes.** The appended comment survives the full chain. |
| Why is `resetApplicationSingleton()` needed? | `EatmeEditProcedure` installs an anonymous Application that blocks `TestProjectApplication` construction. |
| What happens if the edit fails silently? | The intermediate readback check catches it before the save phase. |
| What does the proof artifact prove? | It documents that all three chain steps passed in a single test run. |

The test is intentionally focused on the one gap the existing silver thread
tests leave open: the edit-to-save handoff. It does not re-prove rendering,
VM execution, object placement, or desktop UI automation — those are owned by
adjacent tests.
