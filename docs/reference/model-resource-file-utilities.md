# ModelResourceFileUtilities reference

`ModelResourceFileUtilities` is a package-private utility class in
`org.lgna.story.resourceutilities` that owns file-path resolution, output-file
creation, and JAR-entry writing for the model resource export pipeline.

The class was extracted from `ModelResourceExporter` to separate file I/O
concerns from export orchestration. All methods are static. The class has no
state and a private constructor.

For the exporter contract and supported API, see the
[Model resource exporter reference](model-resource-exporter.md).

## When to use this class

Use `ModelResourceFileUtilities` when you need to:

- resolve the output path for a generated Java source, compiled class, or XML
  resource file inside a package directory tree;
- create or validate an output file with parent-directory creation;
- add files or directory trees as entries in a JAR output stream.

Do not use this class directly from code outside
`org.lgna.story.resourceutilities`. The class is package-private. External
callers should use `ModelResourceExporter`, which delegates to these utilities
internally.

## API

| Method | Purpose |
| --- | --- |
| `ensureOutputFile(File outputFile, String description)` | Creates parent directories if needed, creates the file if it does not exist, and throws `IOException` if the path is not a regular file. |
| `add(File source, JarOutputStream target, String destPathPrefix, boolean recursive)` | Adds a file or directory tree to a JAR output stream under the given destination prefix. Normalizes path separators and handles recursive directory traversal. |
| `getJavaCodeDir(String root, String packageString)` | Returns the `File` for the package directory under `root`, using `JavaCodeUtilities` to convert the package to a directory path. |
| `getJavaClassFile(String root, String packageString, String javaClassName)` | Returns the `File` for a compiled `.class` file in the package directory under `root`. |
| `getJavaFile(String root, String packageString, String javaClassName)` | Returns the `File` for a `.java` source file in the package directory under `root`. |
| `getXMLFile(String root, String packageString, String className)` | Returns the `File` for the XML resource description in the resource subdirectory under `root`. Appends a trailing separator to `root` if missing. |

## Method details

### ensureOutputFile

```java
static void ensureOutputFile(File outputFile, String description) throws IOException
```

Prepares an output path for writing. The method:

1. Creates parent directories via `FileUtilities.createParentDirectoriesIfNecessary`.
2. Creates the file if it does not already exist.
3. Throws `IOException` if creation fails or the path is not a regular file
   (for example, if the path points to a directory).

The `description` parameter is included in the exception message to help
callers distinguish which output stage failed.

**Example:**

```java
File xmlFile = ModelResourceFileUtilities.getXMLFile(root, packageString, className);
ModelResourceFileUtilities.ensureOutputFile(xmlFile, "XML resource");
// xmlFile is now guaranteed to be a writable regular file
```

### add (public entry point)

```java
static void add(File source, JarOutputStream target, String destPathPrefix, boolean recursive)
    throws IOException
```

Adds `source` to the JAR stream. When `source` is a directory and `recursive`
is `true`, the entire subtree is added. The `destPathPrefix` is prepended to
each entry name after normalizing path separators (backslash to forward slash)
and stripping leading separators.

A `null` prefix is treated as an empty string. Duplicate directory entries
log a `ZipException` message to standard error but do not abort the operation.

### add (recursive helper)

```java
static void add(File source, JarOutputStream target, String root,
                String destPathPrefix, boolean recursive) throws IOException
```

Internal overload called by the public `add`. The `root` parameter is the
absolute path of the original source directory, used to compute relative entry
names. Callers outside the class should use the four-argument overload.

### getJavaCodeDir

```java
static File getJavaCodeDir(String root, String packageString)
```

Returns the directory that corresponds to `packageString` under `root`. For
package `org.lgna.story.resources.prop` and root `/output/src`, returns
`/output/src/org/lgna/story/resources/prop`.

### getJavaClassFile

```java
static File getJavaClassFile(String root, String packageString, String javaClassName)
```

Returns the `.class` file path. For class name `TestPropResource` with
package `org.lgna.story.resources.prop` and root `/output/classes`, returns
`/output/classes/org/lgna/story/resources/prop/TestPropResource.class`.

### getJavaFile

```java
static File getJavaFile(String root, String packageString, String javaClassName)
```

Returns the `.java` source file path. Same conventions as `getJavaClassFile`
but with a `.java` extension.

### getXMLFile

```java
static File getXMLFile(String root, String packageString, String className)
```

Returns the XML resource file path inside the resource subdirectory. The
resource subdirectory is resolved by `ModelResourceIoUtilities`. For class
name `TestProp`, package `org.lgna.story.resources.prop`, and root
`/output/resources`, the returned file is located at:

```
/output/resources/org/lgna/story/resources/prop/resources/TestProp.xml
```

A trailing separator is appended to `root` when it does not already end with
one.

## Relationship to ModelResourceExporter

`ModelResourceExporter` calls these utilities in two workflows:

| Exporter method | Utilities used |
| --- | --- |
| `createJavaCode(String root)` | `getJavaFile` to resolve the output path for the generated `.java` source. |
| `createXMLFile(String root, boolean forceRebuild)` | `getXMLFile` to resolve the XML output path, then `ensureOutputFile` to create the file before writing. |

The JAR `add` methods are used by the broader model-resource packaging
pipeline when bundling exported resources into gallery JARs.

## Configuration

There is no runtime configuration. Path conventions are derived from
`JavaCodeUtilities.getDirectoryStringForPackage` and
`ModelResourceIoUtilities.getResourceSubDirWithSeparator`.

## Validation

`ModelResourceFileUtilities` has a dedicated test class,
`ModelResourceFileUtilitiesTest`, which covers all public methods including
path resolution, JAR entry writing, output-file creation, and edge cases.
The exporter workflows that delegate to these utilities are also exercised
by `ModelExportTest`. Run both test classes after modifying this class:

```sh
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn \
  -pl core/model-loading -am \
  -DfailIfNoTests=false \
  -Dcheckstyle.skip \
  test
```

When modifying style-sensitive code, also run checkstyle:

```sh
mvn checkstyle:check -Dcheckstyle.config.location=checkstyle.xml \
  -pl core/model-loading
```
