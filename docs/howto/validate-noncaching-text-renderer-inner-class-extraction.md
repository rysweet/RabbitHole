# Validate NonCachingTextRenderer Inner Class Extraction

Use this guide to verify the extraction of all 9 inner classes from
`NonCachingTextRenderer` into separate top-level files.

Phase 1 (PR #523) extracted `Glyph`, `GlyphProducer`, and
`Pipelined_QuadRenderer`. Phase 2 (issue #524) extracts the remaining 6:
`CharSequenceIterator`, `TextData`, `Manager`, `DefaultRenderDelegate`,
`CharacterCache`, and `DebugListener`.

For the full contract, see the [NonCachingTextRenderer Inner Class Extraction
reference](../reference/noncaching-text-renderer-inner-class-extraction.md).

## When to use this guide

Use this guide when:

- Reviewing changes that extract inner classes from `NonCachingTextRenderer`
- Modifying any of the 9 extracted files
- Changing visibility of fields or methods in `NonCachingTextRenderer` that
  the extracted classes depend on
- Updating reflection-based characterization tests after class renames

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Step 1: Verify compilation

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip compile
```

All 10 files (`NonCachingTextRenderer.java` plus 9 extracted class files)
must compile without errors. Warnings from checkstyle are suppressed by design.

## Step 2: Verify line count

```bash
wc -l core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRenderer.java
```

The target is under 900 lines. Before Phase 1: 1842. Before Phase 2: 1318.
Extracting the 6 inner classes (~466 lines) yields approximately 852 lines.

## Step 3: Run the contract tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=InnerClassExtractionContractTest \
  test
```

This verifies:
- All 9 inner classes are absent from `NonCachingTextRenderer`
- All 9 top-level classes exist with correct visibility
- `Manager` and `DebugListener` have `NonCachingTextRenderer` back-reference
  fields
- 16 fields and 1 method are widened to package-private
- Line count is under 900

## Step 4: Run the characterization tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=NonCachingTextRendererCharacterizationTest \
  test
```

Expected: 49 tests, 42 pass, 7 skipped, 0 failures, 0 errors.

The characterization tests verify that:
- Buffer constants have not changed
- `CharSequenceIterator` iteration behavior is preserved (now top-level)
- `TextData` constructor and accessor behavior is preserved (now top-level)
- `DefaultRenderDelegate` bounds delegation is preserved (now top-level)
- `CharacterCache` caching semantics are preserved (now top-level)
- `preNormalize` geometry expansion is unchanged
- Constructor/accessor behavior is preserved (when JOGL is available)

## Step 5: Run the full module test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip clean test
```

This catches any compilation or linking errors introduced by the extraction.

## Step 6: Verify new file structure

```bash
ls -la core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/*.java
```

Expected files (9 extracted + 1 parent):

| File | Class visibility | Back-reference? |
| --- | --- | --- |
| `NonCachingTextRenderer.java` | public | N/A |
| `TextRendererGlyph.java` | package-private | Yes (`textRenderer`) |
| `TextRendererGlyphProducer.java` | package-private | Yes (`textRenderer`) |
| `TextRendererQuadRenderer.java` | package-private | Yes (`textRenderer`) |
| `CharSequenceIterator.java` | package-private | No (static) |
| `TextData.java` | package-private | No (static) |
| `Manager.java` | package-private | Yes (`textRenderer`) |
| `DefaultRenderDelegate.java` | **public** | No (static) |
| `CharacterCache.java` | package-private | No (static) |
| `DebugListener.java` | package-private | Yes (`textRenderer`) |

## Step 7: Spot-check enclosing instance pattern

Verify that classes with back-references store the `textRenderer` reference:

```bash
grep -n 'this.textRenderer' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/{TextRenderer*.java,Manager.java,DebugListener.java}
```

Each file that requires a back-reference should have exactly one assignment:
`this.textRenderer = textRenderer;` in its constructor(s).

## Step 8: Verify no inner classes remain

```bash
grep -c 'class.*implements\|class.*extends\|static class\|class [A-Z]' \
  core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRenderer.java
```

Only the outer `public class NonCachingTextRenderer extends TextRenderer`
declaration should match. No inner class declarations should remain.

## Step 9: Verify no public API changes (except DefaultRenderDelegate)

```bash
grep -c 'public class' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/{TextRenderer*.java,CharSequenceIterator.java,TextData.java,Manager.java,CharacterCache.java,DebugListener.java}
```

Expected: 0 matches for all files except `DefaultRenderDelegate.java` (1 match).
`DefaultRenderDelegate` stays `public` for API compatibility.

## Troubleshooting

### Compilation fails with "cannot find symbol"

A member of `NonCachingTextRenderer` that an extracted class accesses is
still `private`. Phase 2 requires 16 fields and 1 method to be widened to
package-private. See the [Visibility changes](../reference/noncaching-text-renderer-inner-class-extraction.md#visibility-changes)
table in the reference doc.

### Characterization test reflection fails

If `Class.forName("...NonCachingTextRenderer$CharSequenceIterator")` throws
`ClassNotFoundException`, it means the inner class was extracted but the
test's reflection target was not updated to the top-level FQN
(`...CharSequenceIterator`). Same pattern applies for `CharacterCache`.

### Line count exceeds 900

Ensure all 9 inner classes were removed from `NonCachingTextRenderer.java`.
Check for accidental duplication where an inner class body was left in place
after extraction.

### Manager or DebugListener fails to compile

These were non-static inner classes that accessed 16+ fields of the
enclosing instance. Every field access must go through the `textRenderer`
back-reference. Verify that all `private` fields accessed by these classes
have been widened to package-private.

### `draw()` method is inaccessible

`Pipelined_QuadRenderer.draw()` was `private` in the inner class. It must
be package-private in `TextRendererQuadRenderer` because
`NonCachingTextRenderer.flushGlyphPipeline()` calls `mPipelinedQuadRenderer.draw()`.

### DefaultRenderDelegate cannot find CharSequenceIterator

`DefaultRenderDelegate.getBounds(CharSequence, Font, FRC)` creates a
`new CharSequenceIterator(str)`. After extraction, both are top-level
classes in the same package, so the unqualified reference resolves correctly.
If this fails, ensure `CharSequenceIterator.java` exists in the same package.

## What this guide does NOT cover

- OpenGL rendering behavior (no functional change)
- Upstream JOGL `TextRenderer` compatibility beyond naming conventions
- Checkstyle enforcement (suppressed by `@SuppressWarnings("CheckStyle")`)
