# Legacy Program JSON Archive Boundary

This page defines the fail-closed archive boundary for JSON `.a3w`
archives whose manifest advertises `Program`, where supported decoding fails and
the archive does not satisfy the narrow one-image legacy resource recovery
predicate.

**Implementation status:** implemented in `JsonProjectIo.Reader.readProject`.

The covered seam is `JsonProjectIo` through the public project read API:

```java
Project project = IoUtilities.readProject(archiveFile);
```

When the manifest names `Program`, supported decoding fails, and the archive
cannot satisfy the narrow one-image resource recovery predicate,
`IoUtilities.readProject(File)` will throw `IOException` with a stable
unsupported legacy archive message. It must not return a partial project shell.

## Usage

Use this contract when changing JSON `.a3w` archive reads in
`core/story-api-migration`, especially code that touches manifest names, Tweedle
type references, unsupported Tweedle decode, or legacy resource recovery.

Callers keep using the existing public API:

```java
try {
  Project project = IoUtilities.readProject(archiveFile);
  NamedUserType programType = project.getProgramType();
} catch (IOException e) {
  if (e.getMessage().contains("Unsupported legacy JSON project archive")) {
    // The manifest's Program entry could not decode and did not match recovery.
  }
  throw e;
}
```

Tests should assert the checked failure, not a `null` program type, only after a
`Program` archive fails supported decoding and falls outside the explicitly
recovered one-image resource case.

## Archive shape

The boundary applies to JSON project archives with this manifest context:

| Manifest field | Boundary value |
| --- | --- |
| `metadata.fileType` | `a3w` |
| `description.name` | `Program` |
| `projectStructure.sceneCameraType` | Optional; missing structure still defaults to `WindowCamera` for otherwise supported archives. |
| `resources` | May include `TypeReference` entries, image/audio references, model references, or unsupported reference types. |

A minimal unsupported fixture can contain:

```text
version.txt
manifest.json
src/Program.twe
```

with a manifest `TypeReference` for `Program` and Tweedle source that reaches an
unsupported decoder boundary, such as an unknown superclass:

```java
class Program extends MissingSuper {
}
```

The name `Program` is only a candidate legacy/player signal after supported
decoding fails. The reader must first allow valid archives named `Program` to
decode through the supported JSON project/type path.

## API behavior

`IoUtilities.readProject(File)` selects `JsonProjectIo` for JSON `.a3w` archives.
`JsonProjectIo.Reader.readProject(boolean)` then follows this order:

1. Read `manifest.json` and manifest-declared Tweedle type references, then read
   manifest-backed binary resources only after the archive stays on a supported
   or explicitly recovered path.
2. Return a normal `Project` when the manifest-named program type decodes through
   the supported JSON project/type path.
3. Treat `Program` as a legacy/player candidate only if the manifest-named
   program type cannot decode through the supported path.
4. Preserve the narrow legacy recovery path when the archive is an `.a3w` named
   `Program`, supported decoding failed, and exactly one image resource is
   recoverable.
5. Throw `IOException` for all other unsupported `Program` archives.

The explicit failure is part of the API contract:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(legacyProgramArchive));

assertTrue(thrown.getMessage().contains("Unsupported legacy JSON project archive"));
assertTrue(thrown.getMessage().contains("Program"));
```

The public contract is the checked `IOException` and stable message fragments.
Cause preservation is not part of the public contract; tests should not
assert an exact cause class.

## Supported neighboring behavior

This boundary does not change supported JSON project/type reads.

These shapes remain supported:

| Shape | Result |
| --- | --- |
| `.a3w` manifest names a decodable program type and references matching Tweedle source | `IoUtilities.readProject(File)` returns a `Project` with a non-null program type. |
| `.a3w` manifest names `Program`, and `Program` decodes through the supported Tweedle slice | `IoUtilities.readProject(File)` returns a decoded project. |
| `.a3w` manifest names `Program`, supported decoding fails, and exactly one image resource is recoverable | The existing legacy recovery path returns a resource-only `Project` with `null` program type. |
| `.a3c` JSON type archive with supported Tweedle source | `IoUtilities.readType(File)` returns the decoded type/resource pair. |

The narrow recovery path exists for compatibility with the characterized legacy
image-resource case only. It is not a general player archive reader and not a
general legacy project converter.

## Unsupported example

An unsupported archive can advertise `Program` and include a non-recoverable
resource shape or unsupported program source:

```java
ProjectManifest manifest = new ProjectManifest();
manifest.description.name = "Program";
manifest.metadata.fileType = IoUtilities.EXPORT_EXTENSION;
manifest.projectStructure.sceneCameraType = Project.SceneCameraType.WindowCamera;
manifest.resources.add(new TypeReference("Program", "src/Program.twe", "tweedle"));
```

```java
class Program extends MissingSuper {
}
```

Reading that archive fails closed:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(archiveFile));

assertTrue(thrown.getMessage().contains("Unsupported legacy JSON project archive"));
assertTrue(thrown.getMessage().contains("Program"));
```

The reader must not return `new Project(null, ...)` for this shape unless
supported decoding failed and the archive satisfies the narrow one-image legacy
resource recovery predicate.

## Error message shape

Tests should assert stable substrings rather than full-message equality. The
message contains:

```text
Unsupported legacy JSON project archive
Program
```

When available, the message can include useful archive context such as decoded
type names, unsupported manifest-declared Tweedle type names, or the bounded
unsupported Tweedle decode reason.

The diagnostic must not include raw archive payloads, full Tweedle source bodies,
private filesystem paths, stack traces, credentials, or user-specific data.

## Configuration

There is no runtime configuration for this boundary. It uses the existing
`IoUtilities`, `JsonProjectIo`, Tweedle parser, and JUnit configuration.

From a fresh checkout or worktree, initialize the Tweedle grammar submodule
before Maven validation:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Automation can keep the saved Node memory setting:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

`NODE_OPTIONS` is not an Alice archive reader setting.

## Validation

Run the focused characterization from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=IoUtilitiesTest \
  test
```

Run the module gate before handing off archive reader changes:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

## Non-goals

This boundary does not add player runtime support, full legacy archive support,
general Tweedle decode, broad resource recovery, `.a3c` behavior changes, archive
migration, or editable project reconstruction for old player shapes.

Unsupported `Program` archives outside supported decoding and the narrow
one-image resource recovery path remain unsupported. The reader identifies that
fact with a checked `IOException` and fails closed.

## See also

- [Player archive unsupported Tweedle diagnostics](./player-archive-unsupported-tweedle-diagnostics.md)
- [Characterize the Legacy Program JSON Archive Boundary](../howto/characterize-legacy-program-json-archive-boundary.md)
- [Tutorial: Trace the Legacy Program JSON Archive Boundary](../tutorials/trace-legacy-program-json-archive-boundary.md)
