# Player Archive Unsupported Tweedle Diagnostics

This page defines the narrow JSON player archive behavior used by
characterization and migration checks for manifest-declared Tweedle types that
decode successfully, fail at an explicit unsupported decoder boundary, or are
missing from the archive.

The covered seam is an argument-bearing explicit `this` method call inside a
JSON `.a3w` type, for example `this.helper(value: 1)` inside a `caller` method.
The archive reader keeps that type unsupported and reports the decoder reason
plus the call-site context with deterministic archive context. This is not
broader Tweedle method-call decode support.

The adjacent supported shard is a non-resource field initialized with
literal-only arithmetic, for example `WholeNumber count <- 1 + 2`, in a
manifest-declared JSON `.a3w` program or sibling type. That source no longer
belongs to the unsupported-diagnostics path. The player reader decodes the field
initializer as an AST expression and still rejects broader initializer forms.
This shard does not add `.a3c` type archive support.

The bounded evidence validates only these archive/player boundary behaviors:

- JSON `.a3w` manifest type references route through the production archive
  reader entry point, `IoUtilities.readProject(File)`.
- Generated temporary fixtures can cover player program types, sibling types,
  literal arithmetic field initializers, unsupported Tweedle decoder failures,
  and intentionally missing manifest-declared type entries without checked-in
  binary archives.
- Unsupported manifest-declared Tweedle program types fail closed with an
  `IOException` that includes decoded sibling context, unsupported type names,
  the decoder reason, and the call-site context.
- Missing manifest-declared program or sibling type entries fail clearly with
  the missing archive entry name.
- Decoder-level smoke evidence for argument-bearing explicit `this` calls stays
  at the core AST boundary and does not imply archive/player decode success.

Historical Alice archives are loaded only by focused characterization and
migration checks in this contract. This page does not claim general desktop Open
support for historical archives.

## Feature boundary

The feature is bounded archive/player evidence for generated fixtures that enter
through `IoUtilities.readProject(File)`. It is not a broad archive migration
feature and it is not desktop Save/Open automation.

## Recovery readiness guard

Use this page as the source of truth for archive/player boundary evidence during
owner-free recovery work. A literal no-op recovery is valid only when all of
these conditions are true for the current head:

- the local worktree is clean;
- the pull request head matches the inspected commit;
- mergeability is clean;
- required checks are green;
- this boundary evidence is current and no archive/player documentation, QA
  scenario, or characterization contract is stale.

If any required check is pending, unstable, or failing, the recovery result is
`NOT_MERGE_READY` with the exact blocker until required checks are green. If the
blocker is unrelated to this archive/player boundary, do not repair these docs or
broaden the boundary claim; the pull request still is not merge-ready while the
required check remains unsettled.

When a failing check points at this boundary, repair only the affected
archive/player evidence surface: this reference, the matching how-to or
tutorial, generated-fixture characterization tests, or the two QA smoke scenario
contracts. Do not manually merge, add checked-in `.a3w` archives, expand runtime
decode support, or substitute desktop workflow evidence for this archive I/O
contract.

## Boundary claim routing

This reference is the durable home for the retained archive/player boundary
claims: generated fixture evidence, missing-entry diagnostics, gated command-smoke
semantics, and explicit nonclaims. Treat duplicated or displaced evidence as
superseded at the behavior level, not as a reason to broaden this feature.

When adjacent archive/player work touches `archive-fixture-smoke.yaml`, preserve
the `archive-io` wording while carrying forward missing-entry evidence and the
nonclaims below unless a narrower replacement explicitly covers them.

This routing is evidence routing only. It does not prove full
Tweedle/player decode, migration completeness, UI automation, rendering,
grading, Save/Open, or lesson completion.

The implementation should keep three behaviors separate:

| Behavior | Required claim |
| --- | --- |
| Supported JSON `.a3w` manifest-declared type shard | Literal arithmetic field initializers on a manifest-declared `.a3w` program or sibling type decode into an AST initializer without evaluating the expression or enabling general initializer support. |
| Unsupported JSON `.a3w` player shard | Argument-bearing explicit `this` calls fail closed and the `IOException` names the expected type, decoded sibling context, unsupported reason, and call-site context. |
| Missing manifest entry shard | Manifest-declared `src/*.twe` entries are required; a missing program or sibling entry fails clearly with the missing archive entry name. |

QA scenario wording should describe the shared smoke as archive I/O evidence
because it exercises production archive read/write seams. That wording must not
turn the smoke into evidence for project-level desktop workflows, Save/Open
behavior, UI automation, rendering, grading, lesson completion, or full
historical migration.

## Usage

Use this contract when reading, testing, or changing JSON player archive decode
behavior in `core/story-api-migration`.

The characterization suite for this boundary lives under the archive I/O package:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

Approved fixtures for this boundary are generated inside that JUnit test's
temporary folder. There is no stable checked-in archive fixture directory for
this unsupported Tweedle diagnostic; do not commit generated `.a3w` archives or
binary corpus payloads for it.

The public entry point remains:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
```

For a JSON `.a3w` archive whose manifest declares a program type and whose
Tweedle source contains an unsupported argument-bearing explicit `this` call,
`IoUtilities.readProject(File)` throws `IOException`. It does not return a
partial project and does not silently drop the unsupported type.

For the JSON `.a3w` archive shard whose manifest declares a program or sibling
type and whose Tweedle source contains only a literal arithmetic field
initializer such as `WholeNumber count <- 1 + 2`,
`IoUtilities.readProject(File)` returns the decoded project. The returned type
contains the `count` field with an arithmetic AST initializer. The reader does
not evaluate the expression and does not enable general Tweedle
field-initializer or `.a3c` type archive support.

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

Every manifest-declared type entry is required. If the manifest references
`src/Program.twe` or `src/DecodedSibling.twe` and the ZIP entry is absent,
`IoUtilities.readProject(File)` fails with an `IOException` that names the
missing archive entry. The reader must not silently omit missing types or return
a partially decoded project.

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

The contractually stable decoder reason substring emitted by the Tweedle decoder
is:

```text
argument-bearing explicit this method calls
```

The contractually stable call-site context substring is separate:

```text
caller.this.helper
```

The player archive reader preserves both pieces at the archive boundary instead
of replacing them with a generic missing-program failure.

## Supported neighboring example

The supported neighboring shard is a literal-only arithmetic initializer on a
non-resource field:

```java
class Program extends SProgram {
  WholeNumber count <- 1 + 2;
}
```

The archive reader decodes this program type through the normal JSON player
path:

```java
Project project = IoUtilities.readProject(playerArchiveFile);
NamedUserType programType = project.getProgramType();
```

The decoded `Program` type includes the `count` field with a non-null arithmetic
initializer expression. The expression is preserved as AST; it is not folded to
a literal value.

The support is allowlisted. The following field initializers still route to the
unsupported path:

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

Literal-only arithmetic field initializers on non-resource fields are decoded
before this unsupported-diagnostics path is used. If the initializer tree
contains any non-literal or non-arithmetic node, the decoder must fail closed with
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
| Missing entry | Names a missing manifest-declared entry, such as `src/Program.twe`, when the archive omits it. |

The formatter orders unsupported type diagnostics by manifest type name. Add a
multi-unsupported-type characterization before treating that order as a separate
compatibility guarantee.

## Error message shape

Tests should assert stable substrings rather than full-message equality. A
message for the selected seam must contain these contractual substrings:

```text
Program
DecodedSibling
unsupported
argument-bearing explicit this method calls
caller.this.helper
```

Archive entry names for missing manifest-declared content are also stable
substrings, for example:

```text
src/Program.twe
src/DecodedSibling.twe
```

Additional context such as archive path, manifest entry, decoder phase, source
location, or wrapper exception wording is optional and should not be asserted as
part of this contract unless a focused characterization test first makes that
context stable.

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

## Validation commands and smoke scenarios

Run the focused characterization suite from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Run the focused decoder-boundary characterization when changing or citing the
decoder-level unsupported-call evidence:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeCreatesMethodInvocation+zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+zeroArgumentThisMethodCallDecodeRejectsChainedCall \
  test
```

Run the module gate before handing off archive reader changes:

```bash
NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

The QA smoke contracts stay scoped to these two workflows:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh

NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-archive-fixture-smoke --prepare-only

NODE_OPTIONS=--max-old-space-size=32768 \
  bash qa/outside-in/alice-desktop/runners/run-scenario.sh run \
  alice-desktop-tweedle-decoder-boundary-smoke --prepare-only
```

`archive-fixture-smoke` validates only the generated archive fixture evidence for
manifest routing, generated fixture handling, unsupported Tweedle diagnostics,
and missing-entry failures. `tweedle-decoder-boundary-smoke` validates only the
core AST decoder rejection checks for adjacent unsupported method-call forms.

`--prepare-only` and an unset `ALICE_QA_RUN_GATED_SMOKES` produce
`gated-not-run` evidence; they prove the smoke contract shape, not Maven command
success. Set `ALICE_QA_RUN_GATED_SMOKES=1` only in a worktree prepared to run the
configured Maven commands.

## Non-goals

This feature does not prove full Tweedle/player decode, full historical archive
migration completeness, full UI automation, visible rendering correctness,
grading, Save/Open guarantees, or lesson completion.

It also does not decode argument-bearing method calls, bind labeled arguments,
evaluate argument expressions, apply optional parameters, resolve overloads,
infer implicit receivers, decode non-literal field initializer references,
decode initializer method calls, bind resource initializers, extend the
arithmetic shard to `.a3c` type archives, or add general Tweedle/player decode
support.

Unsupported manifest-declared Tweedle types remain unsupported. The archive
reader reports the reason clearly and fails closed.
