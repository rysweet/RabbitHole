# Characterize the Legacy Program JSON Archive Boundary

Use this guide to add or review focused tests for JSON `.a3w` archives
that advertise `Program`, fail supported decoding, and do not satisfy the narrow
one-image legacy resource recovery predicate.

**Implementation status:** implemented in `JsonProjectIo.Reader.readProject`.

The expected result is failure clarity only. Do not add player implementation,
broaden Tweedle decoding, or claim full legacy archive support when following
this guide.

## Prerequisites

Work in the public archive read seam:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/IoUtilitiesTest.java
```

Use production reader selection:

```java
IoUtilities.readProject(archiveFile);
```

Do not instantiate private `JsonProjectIo` helpers or bypass manifest-based
reader selection.

## Build the smallest unsupported archive

Create a generated JSON `.a3w` archive under the JUnit temporary folder. The
archive contains:

```text
version.txt
manifest.json
src/Program.twe
```

Use manifest metadata for the candidate legacy/player boundary:

```text
metadata.fileType = a3w
description.name = Program
projectStructure.sceneCameraType = WindowCamera
```

Add a `tweedle` `TypeReference` for `Program`:

```java
manifest.resources.add(new TypeReference("Program", "src/Program.twe", "tweedle"));
```

Choose Tweedle source that prevents the manifest-named program type from
decoding through the supported project/type path:

```java
class Program extends MissingSuper {
}
```

Keep the fixture synthetic and deterministic. Do not commit generated `.a3w`
archives, historical binary files, or Git LFS payloads for this test.

## Avoid the narrow recovery predicate

The legacy image-resource recovery path remains supported. A negative test for
this boundary must avoid satisfying all of these conditions at once:

```text
metadata.fileType = a3w
description.name = Program
Program Tweedle decode is unsupported
exactly one image resource is recoverable
```

Good negative fixtures include:

| Fixture | Why it fails closed |
| --- | --- |
| Unsupported `Program` with no resources | No recoverable legacy resource structure exists. |
| Unsupported `Program` with an audio resource | The narrow recovery path only covers exactly one recovered image resource. |
| Unsupported `Program` with multiple image resources | The recovery predicate requires exactly one recovered image resource. |
| `Program` type reference with malformed or unsupported source that cannot produce the manifest-named type | The archive cannot decode the manifest program type or satisfy the one-image resource recovery predicate. |

Do not change or weaken the existing positive recovery characterization for the
one-image legacy case.

## Assert the fail-closed behavior

Read through the public API and assert a checked archive failure:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(archiveFile));
```

Assert stable message substrings:

```java
String message = thrown.getMessage();
assertTrue(message.contains("Unsupported legacy JSON project archive"));
assertTrue(message.contains("Program"));
```

If the fixture uses unsupported Tweedle source, also assert a stable unsupported
decode fragment when one is part of the public archive message:

```java
assertTrue(message.contains("unsupported"));
```

Prefer stable substrings over full-message equality. The message may gain
additional decoded-type or unsupported-type context, but it must continue to name
the unsupported legacy JSON archive boundary and the manifest program name.

## Keep neighboring behavior intact

When reviewing the tests, check that the same suite still protects neighboring
supported behavior:

```text
jsonPlayerReaderDecodesProgramTypeWhenManifestReferencesSimpleTweedleSource
exportedPlayerArchiveImageResourceRemainsRecoverableWhenProgramTypeIsUnsupported
```

The first test protects supported JSON project/type reads. The second protects
the narrow existing legacy recovery behavior. The new negative test belongs
between those boundaries: `Program` archives that fail supported decoding fail
closed unless they match the explicit recovery predicate.

## Run the focused gate

Run from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=IoUtilitiesTest \
  test
```

Then run the module gate:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

## Related references

- [Legacy Program JSON Archive Boundary](../reference/legacy-program-json-archive-boundary.md)
- [Player archive unsupported Tweedle diagnostics](../reference/player-archive-unsupported-tweedle-diagnostics.md)
