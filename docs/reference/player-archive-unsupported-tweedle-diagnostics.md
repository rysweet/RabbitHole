# Player Archive Unsupported Tweedle Diagnostics

This page defines the narrow JSON player archive behavior for manifest-declared
Tweedle types that fail at an explicit unsupported decode boundary.

The covered seam is an argument-bearing explicit `this` method call inside a
JSON `.a3w` type, for example `this.helper(value: 1)` inside a `caller` method.
The archive reader keeps that type unsupported and reports the decoder reason
plus the call-site context with deterministic archive context. This is not
broader Tweedle method-call decode support.

> **[PLANNED - Implementation Pending]** The adjacent arithmetic initializer
> shard below describes the intended behavior after decoder support lands. Today
> this page's current contract remains the unsupported explicit-`this` diagnostic
> boundary.

The planned adjacent supported shard is a non-resource field initialized with
literal-only arithmetic, for example `WholeNumber count <- 1 + 2`, in a
manifest-declared JSON `.a3w` player program type. After implementation, that
source no longer belongs to the unsupported-diagnostics path. The player reader
decodes the field initializer as an AST expression and still rejects broader
initializer forms. This planned shard does not add `.a3c` type archive support.

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

For the planned JSON `.a3w` archive shard whose manifest declares a program type
and whose Tweedle source contains only a literal arithmetic field initializer
such as `WholeNumber count <- 1 + 2`, `IoUtilities.readProject(File)` returns
the decoded project. The returned program type contains the `count` field with
an arithmetic AST initializer. The reader does not evaluate the expression and
does not enable general Tweedle field-initializer or `.a3c` type archive
support.

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

## Planned supported neighboring example

The planned supported neighboring shard is a literal-only arithmetic initializer
on a non-resource field:

```java
class Program extends SProgram {
  WholeNumber count <- 1 + 2;
}
```

After implementation, the archive reader decodes this program type through the
normal JSON player path:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
NamedUserType programType = project.getProgramType();
```

The decoded `Program` type includes the `count` field with a non-null arithmetic
initializer expression. The expression is preserved as AST; it is not folded to
a literal value.

The planned support is allowlisted. The following field initializers still route
to the unsupported path:

```java
WholeNumber count <- otherCount + 2;
WholeNumber count <- this.getCount();
WholeNumber count <- new WholeNumber();
ImageResource picture <- someImage;
AudioResource sound <- sound0;
```

Those forms require identifier binding, call decode, explicit receiver decode,
constructor decode, resource manifest binding, or mixed expression support that
is outside this shard.

## API behavior

`JsonProjectIo` records unsupported Tweedle decode failures for manifest-declared
types as type-name-to-reason diagnostics. Archive-level failures include the
existing type context and the unsupported decoder reason.

After the planned arithmetic initializer shard lands, literal-only arithmetic
field initializers on non-resource fields are decoded before this
unsupported-diagnostics path is used. If the initializer tree contains any
non-literal or non-arithmetic node, the decoder must fail closed with
`UnsupportedTweedleDecodeException`, and `JsonProjectIo` reports the type as
unsupported at the archive boundary.

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
overloads, infer implicit receivers, decode non-literal field initializer
references, decode initializer method calls, bind resource initializers, extend
the planned arithmetic shard to `.a3c` type archives, or add general
Tweedle/player decode support.

Unsupported manifest-declared Tweedle types remain unsupported. The archive
reader reports the reason clearly and fails closed.
