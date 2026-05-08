# Characterize Player Archive Unsupported Tweedle Diagnostics

Use this guide to add or review focused tests for JSON `.a3w` player archives
whose manifest-declared Tweedle type fails at a known unsupported decoder
boundary.

This guide covers the argument-bearing explicit `this` seam:

```java
this.helper(value: 1);
```

It documents failure clarity only. Do not add broader method-call decode support
when following this guide.

## Prerequisites

Work in the historical archive characterization suite:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

Use production reader selection:

```java
IoUtilities.readProject(playerArchiveFile);
```

Do not instantiate private decoder helpers or bypass the JSON archive reader.

## Build the smallest player archive

Create a generated JSON `.a3w` archive in the JUnit temporary folder. The archive
contains:

```text
version.txt
manifest.json
src/Program.twe
src/DecodedSibling.twe
```

Use manifest metadata for a player archive:

```text
metadata.fileType = a3w
description.name = Program
projectStructure.sceneCameraType = WindowCamera
```

Add a `tweedle` `TypeReference` for `Program` and another supported sibling type
such as `DecodedSibling`. The sibling proves the reader can report both decoded
and unsupported manifest-declared type sets in one failure.

## Use the unsupported program source

The `Program` source should contain the unsupported argument-bearing explicit
`this` call:

```java
class Program {
  void helper(WholeNumber value) {
  }

  void run() {
    this.helper(value: 1);
  }
}
```

Use a simple supported sibling source:

```java
class DecodedSibling {
}
```

Keep the fixture synthetic and deterministic. Do not commit generated `.a3w`
archives or add binary corpus payloads for this test.

## Assert the fail-closed archive behavior

Read through the public API and assert a checked archive failure:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
```

Assert stable message substrings:

```java
String message = thrown.getMessage();
assertTrue(message.contains("Program"));
assertTrue(message.contains("DecodedSibling"));
assertTrue(message.contains("unsupported"));
assertTrue(message.contains("argument-bearing explicit this method calls"));
```

Prefer stable substrings over full-message equality. Full messages may gain
additional archive context, but they must continue to name the affected manifest
type and the unsupported decoder reason.

## Keep the boundary conservative

The expected result is failure, not partial success:

```text
IoUtilities.readProject(...) throws IOException
```

Do not assert that a project is returned with a `null` program type. Do not
describe the sibling decode as completed player decode. The sibling name is
diagnostic context only; the unsupported expected program type still fails the
archive read.

## Run the focused gate

Run from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Then run the module gate:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```
