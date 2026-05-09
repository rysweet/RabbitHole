# Tutorial: Trace a Player Archive Unsupported This-Call Diagnostic

This tutorial walks through the JSON player archive diagnostic for an unsupported
argument-bearing explicit `this` Tweedle method call.

The goal is to prove this behavior:

```text
A JSON .a3w archive that declares Program as its player type fails closed when
Program contains `this.helper(value: 1)` inside a `caller` method, and the
IOException names Program plus the unsupported decoder reason and call-site
context.
```

## 1. Start with the unsupported call

Use a small Tweedle program:

```java
class Program extends SProgram {
  void caller() {
    this.helper(value: 1);
  }

  void helper(WholeNumber value) {
  }
}
```

The important syntax is the labeled argument on explicit `this`:

```java
this.helper(value: 1);
```

That call remains unsupported. The contractually stable decoder reason substring
is:

```text
argument-bearing explicit this method calls
```

The contractually stable call-site context substring is:

```text
caller.this.helper
```

## 2. Add one decodable sibling

Use a simple sibling type:

```java
class DecodedSibling extends SScene {
}
```

The sibling is not a success claim for the archive. It only gives the archive
reader useful context to report alongside the unsupported `Program` type.

## 3. Keep the feature bounded

This diagnostic is one shard of the archive/player boundary feature. Its scope
is:

- unsupported argument-bearing explicit `this` call reporting;
- decoded sibling context in a fail-closed archive read;
- missing manifest-entry evidence in the sibling characterization path;
- gated command-smoke wording that distinguishes `gated-not-run` metadata from
  enabled Maven success evidence.

Shared QA smoke wording should call this archive I/O evidence when it exercises
production archive read/write seams. It should not imply desktop workflow
behavior, Save/Open behavior, or full archive migration.

Do not use this tutorial as evidence for full Tweedle/player decode, historical
archive migration completeness, UI automation, visible rendering correctness,
grading, Save/Open guarantees, or lesson completion.

## 4. Package both types in a JSON player archive

Create a temporary `.a3w` archive with:

```text
version.txt
manifest.json
src/Program.twe
src/DecodedSibling.twe
```

The manifest names `Program` as the player type and references both Tweedle
sources as manifest-declared type references.

Generate this archive in the characterization test's temporary folder. Do not
add a checked-in `.a3w` fixture for this unsupported diagnostic.

## 5. Read through IoUtilities

Read through the public production API:

```java
IOException thrown = assertThrows(
    IOException.class,
    () -> IoUtilities.readProject(playerArchiveFile));
```

The archive reader fails closed because the expected player type is unsupported.
It does not return a partially decoded `Project`.

## 6. Check the diagnostic

Assert stable message fragments. These fragments are contractual; archive path,
manifest entry, decoder phase, source location, and wrapper exception text are
optional context:

```java
String message = thrown.getMessage();
assertTrue(message.contains("Program"));
assertTrue(message.contains("DecodedSibling"));
assertTrue(message.contains("unsupported"));
assertTrue(message.contains("argument-bearing explicit this method calls"));
assertTrue(message.contains("caller.this.helper"));
```

Those assertions prove that the archive failure identifies the expected program
type, preserves decoded sibling context, names the unsupported type set, and
surfaces the decoder reason plus the call-site context.

## 7. Avoid broader claims

Do not add tutorial text or tests that imply support for:

- argument-bearing calls;
- labeled argument binding;
- optional parameters;
- overload resolution;
- non-`this` targets;
- implicit receiver calls;
- full Tweedle/player decode.

The finished behavior is clearer failure reporting for an unsupported boundary,
not new method-call decode support.

This tutorial also does not prove historical archive migration completeness,
full UI automation, visible rendering correctness, grading, Save/Open
guarantees, or lesson completion. Use it only as bounded archive/player boundary
evidence for the generated JSON `.a3w` fixture and its explicit unsupported
diagnostic.

## 8. Use the diagnostic in PR #463 recovery evidence

When this tutorial is cited during PR #463 recovery, cite it as evidence for one
thing: the generated JSON `.a3w` archive reports an unsupported argument-bearing
explicit `this` call with stable type, reason, and call-site context.

PR #463 recovery is a focused repair of the existing branch against current
`origin/develop`. It is not a no-op recovery, owner-free bypass, replacement pull
request, or manual merge. The final evidence names the repaired branch SHA and
the `origin/develop` base SHA that was used for reconciliation.

Keep two lists separate in the evidence. Archive/player evidence surfaces are the
bounded docs, scenarios, and characterization tests that prove this behavior.
Repair diff files are the files changed to make the recovery pass, which may also
include the PR gate, its tests, runner allowlists, or schema entries.

If the archive fixture smoke scenario has conflict wording, keep only wording
that preserves this boundary:

```text
generated JSON .a3w manifest routing
unsupported Tweedle diagnostics
missing-entry failures
archive I/O evidence
```

Do not accept wording that turns this tutorial into evidence for full
Tweedle/player decode, historical archive migration completeness, UI automation,
visible rendering correctness, grading, Save/Open guarantees, or lesson
completion.

After the repair, rerun the focused Python contracts, Alice desktop
scenario/schema contracts, the story-api-migration characterization, and the
core/ast decoder-boundary characterization listed in the how-to. Record each
validation with its command, outcome, and final branch SHA. Do not manually merge,
add generated archive fixtures to the repo, or turn this diagnostic into support
for broader Tweedle/player decode.
