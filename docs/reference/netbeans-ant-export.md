# NetBeans Ant Export Reference

This reference describes the Alice 3 NetBeans project export contract: the wizard input, generated Ant project layout, Alice library configuration, resource packaging, and executable characterization boundary.

## Contents

- [Scope](#scope)
- [User-visible behavior](#user-visible-behavior)
- [Generated project layout](#generated-project-layout)
- [Project metadata contract](#project-metadata-contract)
- [Alice3Library configuration](#alice3library-configuration)
- [Resource generation and packaging](#resource-generation-and-packaging)
- [API reference](#api-reference)
- [Characterization tests](#characterization-tests)
- [Compatibility rules](#compatibility-rules)

## Scope

The NetBeans export path converts an existing Alice `.a3p` project into a Java SE Ant project that can be opened by NetBeans, compiled by Ant, and packaged as a JAR with an `AliceJavaFXLauncher` `Main-Class` manifest entry.

The durable contract is owned by the NetBeans module:

```text
netbeans/
```

The exported project uses the packaged template:

```text
netbeans/src/main/resources/ProjectTemplate/
```

The behavior is characterized by:

```text
netbeans/src/test/java/org/alice/netbeans/project/Alice3ProjectTemplateAntSmokeTest.java
```

## User-visible behavior

In NetBeans, the Alice plugin registers a project template named **Java Project from Existing Alice Project** under the standard Java project templates. The wizard accepts an existing Alice `.a3p` project and a destination project directory.

When the wizard finishes, it:

1. Creates the destination directory if needed, or reuses it when no template entries conflict.
2. Expands `ProjectTemplate.zip` into that directory.
3. Renames generated NetBeans metadata to match the project directory name.
4. Creates `src/`.
5. Generates Java source from the Alice project.
6. Copies Alice resources into classpath-relative locations under `src/resources/`.
7. Returns the exported project root and `src/` directory to NetBeans for opening.

The export does not download media, gallery assets, or Git LFS content. Resource bytes come from the input `.a3p` project.

## Generated project layout

A resource-bearing export named `ExportedResourceWorld` has this minimum layout:

```text
ExportedResourceWorld/
|-- build.xml
|-- manifest.mf
|-- nbproject/
|   |-- build-impl.xml
|   |-- genfiles.properties
|   |-- project.properties
|   `-- project.xml
`-- src/
    |-- AliceJavaFXLauncher.java
    |-- Program.java
    |-- Resources.java
    `-- resources/
        `-- probe.wav
```

`Resources.java` and `src/resources/...` are generated only when the Alice project contains resources.

The generated JAR for this example is:

```text
ExportedResourceWorld/dist/ExportedResourceWorld.jar
```

The JAR includes:

```text
Program.class
AliceJavaFXLauncher.class
Resources.class
resources/probe.wav
```

The manifest `Main-Class` is `AliceJavaFXLauncher`.

## Project metadata contract

The wizard rewrites the template metadata so the exported project name and distribution JAR match the destination directory.

For `ExportedResourceWorld`, `nbproject/project.properties` contains:

```properties
src.dir=src
build.dir=build
build.classes.dir=${build.dir}/classes
javac.classpath=${libs.Alice3Library.classpath}
main.class=AliceJavaFXLauncher
application.title=ExportedResourceWorld
dist.jar=${dist.dir}/ExportedResourceWorld.jar
run.classpath=${javac.classpath}:${build.classes.dir}
```

Formatting in the properties file may use NetBeans line continuations. The semantic contract is the property value, not the exact whitespace.

`nbproject/project.xml` names the Java SE project after the destination directory:

```xml
<name>ExportedResourceWorld</name>
```

## Alice3Library configuration

Exported projects compile against a NetBeans library named `Alice3Library`.

The plugin registers the library at:

```text
netbeans/src/main/resources/org/alice/netbeans/Alice3Library.xml
```

The classpath volume includes the Alice runtime and third-party dependencies needed by generated programs. The descriptor is the source of truth; at the time of writing it includes:

```text
jackson-core.jar
jackson-databind.jar
jackson-annotations.jar
jackson-datatype-jsr310.jar
jai-codec.jar
jai-core.jar
commons-text.jar
commons-lang3.jar
gluegen-rt.jar
jogl-all.jar
util.jar
scenegraph.jar
glrender.jar
ast.jar
story-api.jar
tweedle.jar
models.jar
javafx-base.jar
javafx-graphics.jar
javafx-media.jar
```

When building an exported project outside the NetBeans IDE, supply the library properties through a user properties file instead of editing `nbproject/project.properties`. The classpath must include every required classpath entry from `Alice3Library.xml`; an incomplete file can let some generated projects compile while failing others at compile or run time.

This excerpt shows the property shape and the current JavaFX artifact version. It is not a complete classpath:

```properties
libs.Alice3Library.classpath=/home/dev/alice3/core/util/target/classes:/home/dev/alice3/core/story-api/target/classes:/home/dev/.m2/repository/org/openjfx/javafx-graphics/21.0.7/javafx-graphics-21.0.7.jar
libs.Alice3Library.src=/home/dev/alice3/netbeans/target/aliceSource.jar
```

Use the platform path separator for `libs.Alice3Library.classpath`: `:` on Linux/macOS and `;` on Windows.

The characterization tests generate their terminal Ant property file from the `Alice3Library.xml` classpath volume through `Alice3LibraryClasspathTestSupport`, so the test binding stays aligned with the plugin descriptor instead of hard-coding a partial dependency list.

The generated `run.jvmargs` uses `libs.Alice3Library.src` to derive the Alice root directory:

```properties
-Dorg.alice.ide.rootDirectory="${libs.Alice3Library.src}_root"
```

## Resource generation and packaging

`ProjectCodeGenerator` reads the `.a3p` archive with `IoUtilities.readProject(...)`. If the project contains resources, it creates a `ResourcesTypeWrapper`, adds the generated resource type to the named user types, and writes resource bytes below `src/resources/`.

Resource paths are classpath-relative. Generated source references look like:

```text
resources/probe.wav
```

Generated source must not embed local filesystem paths such as a temporary checkout directory or the original media file location.

The Ant `jar` target copies the resource files into the distribution JAR at the same classpath-relative path. Runtime code can load the packaged resource through the application class loader:

```java
ClassLoader loader = Thread.currentThread().getContextClassLoader();
URL resource = loader.getResource("resources/probe.wav");
```

## API reference

### `Alice3ProjectTemplateWizardIterator`

Location:

```text
netbeans/src/main/java/org/alice/netbeans/Alice3ProjectTemplateWizardIterator.java
```

Template registration:

```java
@TemplateRegistration(
    folder = "Project/Standard",
    displayName = "Java Project from Existing Alice Project",
    description = "Alice3ProjectTemplateDescription.html",
    iconBase = "org/alice/netbeans/aliceIcon.png",
    content = "ProjectTemplate.zip")
```

Programmatic instantiation uses these wizard properties:

| Property | Type | Meaning |
| --- | --- | --- |
| `targetTemplate` | `FileObject` | The packaged `ProjectTemplate.zip` template. |
| `projdir` | `File` | Destination project directory. |
| `aliceProjectFile` | `File` | Source Alice `.a3p` project to convert. |

The wizard returns a `Set<FileObject>` containing the project directory and generated `src/` directory.

### `ProjectCodeGenerator.generateCode(...)`

Location:

```text
netbeans/src/main/java/org/alice/netbeans/project/ProjectCodeGenerator.java
```

Public entry point:

```java
public static Collection<FileObject> generateCode(
    File aliceProjectFile,
    File javaSrcDirectory,
    ProgressHandle progressHandle)
    throws IOException, VersionNotSupportedException
```

The generator:

1. Reads the Alice project archive.
2. Validates generated Java identifiers and destination paths.
3. Writes one `.java` file for each generated type.
4. Writes `AliceJavaFXLauncher.java`.
5. Writes `Resources.java` and classpath-relative resource files when resources exist.
6. Returns source files that NetBeans should open.

Generation fails with `IOException` when an output path would escape `src/`, when duplicate generated source names collide, or when a generated destination already exists.

## Characterization tests

The exported Ant project contract is protected by `Alice3ProjectTemplateAntSmokeTest`.

The smoke coverage includes:

| Test behavior | Contract protected |
| --- | --- |
| Template export builds with populated `Alice3Library` | `javac.classpath` resolves through `${libs.Alice3Library.classpath}` and Ant can compile generated Alice code. |
| Resource project Ant JAR packages resources | `Resources.class` and classpath-relative resource bytes are present in `dist/*.jar`. |
| Wizard-driven resource export | The real wizard path creates the project layout, rewrites metadata, generates resources, and packages the output JAR. |

Focused validation command:

```bash
mvn -pl netbeans -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=org.alice.netbeans.project.Alice3ProjectTemplateAntSmokeTest test
```

Reactor-equivalent validation command:

```bash
git submodule update --init tweedle-lang
mvn -pl netbeans -am -DfailIfNoTests=false test
```

## Compatibility rules

Preserve these rules when changing the NetBeans export path:

1. Exported projects continue to use standard NetBeans Java SE Ant metadata.
2. `javac.classpath` continues to reference `${libs.Alice3Library.classpath}` instead of hard-coded Alice paths.
3. `main.class` remains `AliceJavaFXLauncher`.
4. `application.title` and `dist.jar` track the destination project directory name.
5. Alice resources are copied below `src/resources/` and packaged into the JAR at classpath-relative paths.
6. `Resources.java` references packaged resource paths, not local filesystem paths.
7. Ant `jar` produces a JAR containing generated classes, generated launcher classes, and resource bytes.
8. Tests use synthetic local resources or existing non-LFS fixtures; they do not require Git LFS downloads or network access.
