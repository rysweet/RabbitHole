# Player Archive Unsupported Tweedle Diagnostics

This page defines the narrow JSON player archive behavior for manifest-declared
Tweedle types that fail at an explicit unsupported decode boundary.

The covered seam is an argument-bearing explicit `this` method call inside a
JSON `.a3w` type, for example `this.helper(value: 1)` inside a `caller` method.
The archive reader keeps that type unsupported and reports the decoder reason
plus the call-site context with deterministic archive context. This is not
broader Tweedle method-call decode support.

## Usage

Use this contract when reading, testing, or changing JSON player archive decode
behavior in `core/story-api-migration`.

The public entry point remains:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
```

For a JSON `.a3w` archive whose manifest declares a program type and whose
Tweedle source contains an unsupported argument-bearing explicit `this` call,
`IoUtilities.readProject(File)` throws `IOException`. It does not return a
partial project and does not silently drop the unsupported type.

## Supported archive context

A minimal archive for this diagnostic boundary uses the normal JSON player
shape:

```text
version.txt
manifest.json
src/Program.twe
src/DecodedSibling.twe
```

The manifest identifies the archive as a player archive:

```json
{
  "metadata": {
    "fileType": "a3w"
  },
  "description": {
    "name": "Program"
  },
  "projectStructure": {
    "sceneCameraType": "WindowCamera"
  }
}
```

The manifest type references include at least one `tweedle` type entry for the
expected program type. They may also include supported sibling types. Supported
siblings can decode successfully, but the archive read still fails if the
manifest-declared expected program type is unsupported.

## Unsupported Tweedle example

The selected unsupported seam is a labeled-argument call on explicit `this`:

```java
class Program extends SProgram {
  void caller() {
    this.helper(value: 1);
  }

  void helper(WholeNumber value) {
  }
}
```

The bounded stable decoder reason emitted by the Tweedle decoder is:

```text
Tweedle argument-bearing explicit this method calls
```

The call-site context is separate:

```text
caller.this.helper
```

The player archive reader preserves both pieces at the archive boundary instead
of replacing them with a generic missing-program failure.

## API behavior

`JsonProjectIo` records unsupported Tweedle decode failures for manifest-declared
types as type-name-to-reason diagnostics. Archive-level failures include the
existing type context and the unsupported decoder reason.

Stable `IOException` diagnostics include:

| Diagnostic part | Required behavior |
| --- | --- |
| Expected type | Names the manifest-declared program type, such as `Program`. |
| Decoded types | Lists any manifest-declared types that decoded successfully, such as `DecodedSibling`. |
| Unsupported types | Lists manifest-declared Tweedle type names that reached `UnsupportedTweedleDecodeException`, such as `Program`. |
| Decoder reason | Includes the bounded stable unsupported decode reason emitted by the Tweedle decoder for each unsupported type. |
| Call context | Preserves the affected call-site context, such as `caller.this.helper`. |

The formatter orders unsupported type diagnostics by manifest type name. Add a
multi-unsupported-type characterization before treating that order as a separate
compatibility guarantee.

## Error message shape

Tests should assert stable substrings rather than full-message equality. A
message for the selected seam contains these parts:

```text
Program
DecodedSibling
unsupported
argument-bearing explicit this method calls
caller.this.helper
```

The diagnostic must not include raw archive payloads, full Tweedle source bodies,
filesystem paths outside the archive entry name, stack traces, credentials, or
user-specific data.

## Configuration

There is no runtime configuration for this diagnostic behavior. It uses the
existing JSON player archive reader, Tweedle parser, AST decoder, Maven, and
JUnit configuration.

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

`NODE_OPTIONS` is not an Alice decode setting.

## Validation

Run the focused characterization suite from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Run the module gate before handing off archive reader changes:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

## Non-goals

This feature does not decode argument-bearing method calls, bind labeled
arguments, evaluate argument expressions, apply optional parameters, resolve
overloads, infer implicit receivers, or add general Tweedle/player decode
support.

Unsupported manifest-declared Tweedle types remain unsupported. The archive
reader reports the reason clearly and fails closed.
