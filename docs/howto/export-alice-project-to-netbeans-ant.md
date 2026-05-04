# Export an Alice Project to a NetBeans Ant Project

Use this guide to convert an Alice `.a3p` project into a Java SE Ant project, build it, and verify its generated resources.

## Contents

- [Prerequisites](#prerequisites)
- [Export from NetBeans](#export-from-netbeans)
- [Inspect the generated project](#inspect-the-generated-project)
- [Build with Ant](#build-with-ant)
- [Configure Alice3Library outside NetBeans](#configure-alice3library-outside-netbeans)
- [Verify packaged resources](#verify-packaged-resources)
- [Troubleshooting](#troubleshooting)

## Prerequisites

Use:

- Java 21.
- A NetBeans installation with the Alice 3 plugin installed.
- An Alice `.a3p` project.
- Apache Ant when building the generated project from a terminal.

The export does not require Git LFS downloads. If the Alice project already contains resources, those bytes are copied from the `.a3p` archive into the generated Java project.

## Export from NetBeans

1. Open NetBeans with the Alice 3 plugin installed.
2. Choose **File > New Project**.
3. Select **Java Project from Existing Alice Project**.
4. Select the source Alice project, for example:

   ```text
   /home/dev/alice-projects/ResourceWorld.a3p
   ```

5. Select a destination project directory, for example:

   ```text
   /home/dev/netbeans-projects/ExportedResourceWorld
   ```

6. Finish the wizard.

The wizard creates the NetBeans project, generates Java source in `src/`, and opens the generated project in NetBeans.

## Inspect the generated project

From a terminal, inspect the destination directory:

```bash
cd /home/dev/netbeans-projects/ExportedResourceWorld
find . -maxdepth 3 -type f | sort
```

A resource-bearing project includes:

```text
./build.xml
./manifest.mf
./nbproject/build-impl.xml
./nbproject/genfiles.properties
./nbproject/project.properties
./nbproject/project.xml
./src/AliceJavaFXLauncher.java
./src/Program.java
./src/Resources.java
./src/resources/probe.wav
```

Check the important generated properties:

```bash
grep -E '^(application.title|dist.jar|javac.classpath|main.class|src.dir) *=' nbproject/project.properties
```

Expected values for `ExportedResourceWorld`:

```properties
application.title=ExportedResourceWorld
dist.jar=${dist.dir}/ExportedResourceWorld.jar
javac.classpath=${libs.Alice3Library.classpath}
main.class=AliceJavaFXLauncher
src.dir=src
```

## Build with Ant

Inside NetBeans, use **Run** or **Clean and Build** on the generated project.

From a terminal, run:

```bash
cd /home/dev/netbeans-projects/ExportedResourceWorld
ant jar
```

The generated artifact is:

```text
dist/ExportedResourceWorld.jar
```

List the packaged entries:

```bash
jar tf dist/ExportedResourceWorld.jar | grep -E '^(Program|AliceJavaFXLauncher|Resources)\.class$|^resources/'
```

Expected output for a resource-bearing project:

```text
AliceJavaFXLauncher.class
Program.class
Resources.class
resources/probe.wav
```

## Configure Alice3Library outside NetBeans

NetBeans resolves `Alice3Library` from the Alice plugin. Terminal Ant builds need the same property values.

Create a local properties file outside the generated project:

```bash
cat > /home/dev/alice3-library.properties <<'EOF'
libs.Alice3Library.classpath=/home/dev/alice3/core/util/target/classes:/home/dev/alice3/core/scenegraph/target/classes:/home/dev/alice3/core/glrender/target/classes:/home/dev/alice3/core/ast/target/classes:/home/dev/alice3/core/story-api/target/classes:/home/dev/alice3/core/tweedle/target/classes:/home/dev/alice3/core/models/target/classes:/home/dev/.m2/repository/org/openjfx/javafx-base/21/javafx-base-21.jar:/home/dev/.m2/repository/org/openjfx/javafx-graphics/21/javafx-graphics-21.jar:/home/dev/.m2/repository/org/openjfx/javafx-media/21/javafx-media-21.jar
libs.Alice3Library.src=/home/dev/alice3/netbeans/target/aliceSource.jar
EOF
```

Use the property file with Ant:

```bash
ant -Duser.properties.file=/home/dev/alice3-library.properties jar
```

Keep local paths in the external property file. Do not commit machine-specific Alice library paths into `nbproject/project.properties`.

## Verify packaged resources

`Resources.java` should reference classpath-relative paths:

```bash
grep 'resources/' src/Resources.java
```

A valid generated reference looks like:

```text
resources/probe.wav
```

The source should not contain the source filesystem path:

```bash
grep -F '/home/dev/alice-projects' src/Resources.java && echo "unexpected local path"
```

The command should print nothing for a valid export.

Confirm the resource bytes are in the JAR:

```bash
jar xf dist/ExportedResourceWorld.jar resources/probe.wav
test -s resources/probe.wav
```

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Ant reports `Property libs.Alice3Library.classpath has not been set` | Build inside NetBeans or pass `-Duser.properties.file=/path/to/alice3-library.properties`. |
| `Program.java` compiles but `Resources.java` is missing | The source `.a3p` has no resources, or the resources were not saved into the archive before export. |
| The JAR is named `Alice3JavaApplication.jar` instead of the project name | Re-run export through the wizard so `application.title` and `dist.jar` are rewritten for the destination directory. |
| JavaFX classes are missing at compile or run time | Add the JavaFX JARs from `Alice3Library` to `libs.Alice3Library.classpath`, or build from NetBeans with the Alice plugin installed. |
| Broad Maven validation fails with missing Tweedle parser classes | Run `git submodule update --init tweedle-lang` from the repository root. |
