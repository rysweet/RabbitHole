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

## 8. Use the diagnostic in recovery evidence

When this tutorial is cited during owner-free recovery, cite it as evidence for
one thing: the generated JSON `.a3w` archive reports an unsupported
argument-bearing explicit `this` call with stable type, reason, and call-site
context.

A no-op recovery is complete only when the current head is clean, mergeability is
clean, required checks are green, and the reference, how-to, tutorial, QA smoke
scenario text, and characterization tests all describe the same boundary. If any
required check is pending, unstable, or failing, report `NOT_MERGE_READY` with
the exact blocker until required checks are green. If the blocker is unrelated to
this archive/player boundary, do not repair these docs or broaden the boundary
claim; the pull request still is not merge-ready while the required check remains
unsettled.

If a recovery repair is needed, keep it on this tutorial's seam. Update the
smallest set of archive/player evidence files and rerun the matching focused
validation. Do not manually merge, add generated archive fixtures to the repo, or
turn this diagnostic into support for broader Tweedle/player decode.
