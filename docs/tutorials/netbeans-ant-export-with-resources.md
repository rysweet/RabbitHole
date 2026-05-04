# Tutorial: Export a Resource-Bearing Alice Project to Ant

This tutorial walks through the observable behavior of a NetBeans Ant export for an Alice project that contains a small resource.

## Contents

- [Goal](#goal)
- [1. Prepare a small Alice project](#1-prepare-a-small-alice-project)
- [2. Export through the NetBeans wizard](#2-export-through-the-netbeans-wizard)
- [3. Inspect generated source](#3-inspect-generated-source)
- [4. Build the generated JAR](#4-build-the-generated-jar)
- [5. Check the packaged resource](#5-check-the-packaged-resource)
- [6. Relate the tutorial to characterization tests](#6-relate-the-tutorial-to-characterization-tests)

## Goal

By the end, you have a generated Java SE Ant project that:

- Compiles against `Alice3Library`.
- Contains generated `Program.java`, `AliceJavaFXLauncher.java`, and `Resources.java`.
- Copies resource bytes into `src/resources/`.
- Packages generated classes and resources into `dist/<project-name>.jar`.

## 1. Prepare a small Alice project

Create or choose an Alice `.a3p` project that includes a resource. A tiny local audio file is enough; the export contract does not require LFS-backed gallery assets.

Example names used below:

```text
Alice project: /home/dev/alice-projects/ResourceWorld.a3p
Resource file inside project: probe.wav
Export destination: /home/dev/netbeans-projects/ExportedResourceWorld
```

Save the Alice project before exporting so the resource bytes are present in the `.a3p` archive.

## 2. Export through the NetBeans wizard

In NetBeans:

1. Choose **File > New Project**.
2. Select **Java Project from Existing Alice Project**.
3. Set the Alice project path to:

   ```text
   /home/dev/alice-projects/ResourceWorld.a3p
   ```

4. Set the destination directory to:

   ```text
   /home/dev/netbeans-projects/ExportedResourceWorld
   ```

5. Finish the wizard.

The wizard expands the project template, rewrites the NetBeans project name, and generates Java source from the Alice project.

## 3. Inspect generated source

Open a terminal:

```bash
cd /home/dev/netbeans-projects/ExportedResourceWorld
find src -maxdepth 2 -type f | sort
```

Expected files include:

```text
src/AliceJavaFXLauncher.java
src/Program.java
src/Resources.java
src/resources/probe.wav
```

Check that `Resources.java` references the packaged classpath location:

```bash
grep 'resources/probe.wav' src/Resources.java
```

The reference should use `resources/probe.wav`. It should not use `/home/dev/alice-projects/ResourceWorld.a3p`, `/tmp/...`, or the original media file path.

## 4. Build the generated JAR

If you are using NetBeans, choose **Clean and Build**.

If you are using terminal Ant and NetBeans is not supplying `Alice3Library`, create a local property file:

```bash
cat > /home/dev/alice3-library.properties <<'EOF'
libs.Alice3Library.classpath=/home/dev/alice3/core/util/target/classes:/home/dev/alice3/core/scenegraph/target/classes:/home/dev/alice3/core/glrender/target/classes:/home/dev/alice3/core/ast/target/classes:/home/dev/alice3/core/story-api/target/classes:/home/dev/alice3/core/tweedle/target/classes:/home/dev/alice3/core/models/target/classes:/home/dev/.m2/repository/org/openjfx/javafx-base/21/javafx-base-21.jar:/home/dev/.m2/repository/org/openjfx/javafx-graphics/21/javafx-graphics-21.jar:/home/dev/.m2/repository/org/openjfx/javafx-media/21/javafx-media-21.jar
libs.Alice3Library.src=/home/dev/alice3/netbeans/target/aliceSource.jar
EOF
```

Build:

```bash
ant -Duser.properties.file=/home/dev/alice3-library.properties jar
```

The output JAR is:

```text
dist/ExportedResourceWorld.jar
```

## 5. Check the packaged resource

List the generated JAR entries:

```bash
jar tf dist/ExportedResourceWorld.jar | sort | grep -E '^(AliceJavaFXLauncher|Program|Resources)\.class$|^resources/'
```

Expected entries:

```text
AliceJavaFXLauncher.class
Program.class
Resources.class
resources/probe.wav
```

Extract the resource and confirm it has content:

```bash
rm -rf /tmp/exported-resource-check
mkdir -p /tmp/exported-resource-check
cd /tmp/exported-resource-check
jar xf /home/dev/netbeans-projects/ExportedResourceWorld/dist/ExportedResourceWorld.jar resources/probe.wav
test -s resources/probe.wav
```

The last command exits successfully when the resource was packaged.

## 6. Relate the tutorial to characterization tests

The automated characterization uses the same observable contract with a synthetic resource created during the test. It does not depend on network access or Git LFS downloads.

The focused test command is:

```bash
mvn -pl netbeans -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest test
```

The reactor-equivalent gate is:

```bash
git submodule update --init tweedle-lang
mvn -pl netbeans -am -DfailIfNoTests=false test
```

Use the tutorial for manual inspection and the smoke test for compatibility protection before refactoring the NetBeans export path.
