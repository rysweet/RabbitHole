# Tutorial: Trace the Project Archive Reopen/Edit Seam

This tutorial walks through the bounded project archive journey protected by
`ProjectOpenSaveExportJourneyTest` and `FileProjectLoaderTest`.

Read the contract first in
[Project Archive Reopen/Edit Seam](../reference/project-archive-reopen-edit-seam.md).
Use the command guide in
[Validate the Project Archive Reopen/Edit Seam](../howto/validate-project-archive-reopen-edit-seam.md).

## Contents

- [Goal](#goal)
- [1. Create a deterministic project](#1-create-a-deterministic-project)
- [2. Write the original archive](#2-write-the-original-archive)
- [3. Reopen through FileProjectLoader](#3-reopen-through-fileprojectloader)
- [4. Edit and write a second archive](#4-edit-and-write-a-second-archive)
- [5. Reopen and export](#5-reopen-and-export)
- [6. Check invalid and classification paths](#6-check-invalid-and-classification-paths)
- [7. Run the focused tests](#7-run-the-focused-tests)

## Goal

Protect the archive IO behavior that Alice needs before higher-level desktop
workflows can depend on it:

```text
valid .a3p archive -> file-backed reopen -> in-memory edit -> second .a3p write
-> second reopen -> .a3w export -> production readback
```

This tutorial does not use Alice desktop, Save menu automation, rendering,
grading, or first-lesson UI behavior.

## 1. Create a deterministic project

Use a synthetic `Project` with a named program type and the standard Alice
program superclass:

```java
private static NamedUserType programType(String name) {
  NamedUserType type = new NamedUserType();
  type.name.setValue(name);
  type.superType.setValue(JavaType.getInstance(SProgram.class));
  return type;
}
```

Create all archives under a test-owned working directory:

```java
Path workingDirectory = Files.createDirectories(Path.of(
    "target",
    "headless-project-journey",
    UUID.randomUUID().toString()));

File originalProjectFile = workingDirectory.resolve("classroom.a3p").toFile();
File savedProjectFile = workingDirectory.resolve("classroom-copy.a3p").toFile();
File exportedProjectFile = workingDirectory.resolve("classroom-export.a3w").toFile();
```

## 2. Write the original archive

Write the project with production archive IO:

```java
Project originalProject = new Project(programType("ClassroomProgram"), Project.SceneCameraType.WindowCamera);
IoUtilities.writeProject(originalProjectFile, originalProject);
```

The test should not commit generated archives. The archive exists only under the
test working directory.

## 3. Reopen through FileProjectLoader

Expose the protected loader seam with a test subclass:

```java
private static class TestFileProjectLoader extends FileProjectLoader {
  TestFileProjectLoader(File file) {
    super(file);
  }

  Project loadNow() {
    return load();
  }
}
```

Reopen the archive and assert project-owned state:

```java
Project loadedProject = new TestFileProjectLoader(originalProjectFile).loadNow();

assertNotNull(loadedProject);
assertEquals("ClassroomProgram", loadedProject.getProgramType().getName());
```

This assertion proves the file-backed loader reached the project archive reader.

## 4. Edit and write a second archive

Apply a deterministic in-memory edit to the reopened project, then write a new
project archive:

```java
loadedProject.getProgramType().name.setValue("EditedClassroomProgram");
IoUtilities.writeProject(savedProjectFile, loadedProject);
```

The edit must be asserted after reopening the second archive. Checking only that
`savedProjectFile` exists would miss stale-write regressions.

## 5. Reopen and export

Reopen the edited archive through the same loader seam:

```java
Project savedProject = new TestFileProjectLoader(savedProjectFile).loadNow();

assertNotNull(savedProject);
assertEquals("EditedClassroomProgram", savedProject.getProgramType().getName());
```

Export the reopened project and read it back through production IO:

```java
IoUtilities.exportProject(exportedProjectFile, savedProject);

Project exportedProject = IoUtilities.readProject(exportedProjectFile);
assertNotNull(exportedProject);
assertEquals("EditedClassroomProgram", exportedProject.getProgramType().getName());
```

The export readback is archive IO evidence only. It does not prove player
runtime behavior or rendering correctness.

## 6. Check invalid and classification paths

Use `FileProjectLoaderTest` for the direct loader contract.

Corrupt archive rejection:

```java
File corruptProject = temporaryFolder.newFile("corrupt-generated-world.a3p");
Files.writeString(corruptProject.toPath(), "not an Alice project archive", StandardCharsets.UTF_8);

Project rejectedProject = new FileProjectLoader(corruptProject).load();

assertNull(rejectedProject);
```

IO failure hook:

```java
Project project = loader.load();

assertNull(project);
assertEquals(corruptProject, loader.file);
assertTrue(loader.exception instanceof IOException);
```

URI and backup classification:

```java
FileProjectLoader loader = new FileProjectLoader(projectFile);

assertEquals(projectFile.toURI(), loader.getUri());
assertFalse(loader.shouldBeSaved());
assertFalse(loader.isBackup());
assertEquals(projectFile, loader.getMainProjectFile());
```

Keep these assertions about loader behavior. Do not add desktop Save, chooser,
rendering, grading, or lesson-completion checks to this test class.

## 7. Run the focused tests

From the repository root:

```bash
git submodule update --init tweedle-lang

NODE_OPTIONS=--max-old-space-size=32768 mvn -DincludeSims=false -Dinstall4j.skip \
  -pl core/ide -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.ide.ProjectOpenSaveExportJourneyTest,org.alice.ide.uricontent.FileProjectLoaderTest \
  test
```

The focused tests complete the tutorial when the edited project state survives
the second reopen and the loader still rejects invalid archives clearly.
