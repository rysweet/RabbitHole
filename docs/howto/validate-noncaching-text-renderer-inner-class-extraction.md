# Validate NonCachingTextRenderer Inner Class Extraction

Use this guide to verify the extraction of `Glyph`, `GlyphProducer`, and
`Pipelined_QuadRenderer` from `NonCachingTextRenderer` into separate
top-level package-private files.

For the full contract, see the [NonCachingTextRenderer Inner Class Extraction
reference](../reference/noncaching-text-renderer-inner-class-extraction.md).

## When to use this guide

Use this guide when:

- Reviewing changes that extract inner classes from `NonCachingTextRenderer`
- Modifying any of the 3 extracted files (`TextRendererGlyph.java`,
  `TextRendererGlyphProducer.java`, `TextRendererQuadRenderer.java`)
- Changing visibility of fields or methods in `NonCachingTextRenderer` that
  the extracted classes depend on
- Adding new inner classes to `NonCachingTextRenderer` and considering
  whether they should be extracted
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

All 4 files (`NonCachingTextRenderer.java`, `TextRendererGlyph.java`,
`TextRendererGlyphProducer.java`, `TextRendererQuadRenderer.java`) must
compile without errors. Warnings from checkstyle are suppressed by design.

## Step 2: Verify line count

```bash
wc -l core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRenderer.java
```

The target is under 1350 lines. Before extraction: 1842 lines.

## Step 3: Run the characterization tests

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
- Static inner classes (`CharSequenceIterator`, `TextData`, `CharacterCache`,
  `DefaultRenderDelegate`) are unaffected
- `preNormalize` geometry expansion is unchanged
- Constructor/accessor behavior is preserved (when JOGL is available)

## Step 4: Run the full module test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

This catches any compilation or linking errors introduced by the extraction.

## Step 5: Verify new file structure

```bash
ls -la core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/TextRenderer*.java
```

Expected files:

| File | Class visibility | Constructor |
| --- | --- | --- |
| `TextRendererGlyph.java` | package-private | Two constructors, both take `NonCachingTextRenderer textRenderer` |
| `TextRendererGlyphProducer.java` | package-private | `(int fontLengthInGlyphs, NonCachingTextRenderer textRenderer)` |
| `TextRendererQuadRenderer.java` | package-private | `(NonCachingTextRenderer textRenderer)` |

## Step 6: Spot-check enclosing instance pattern

Verify that each extracted class stores the `textRenderer` reference:

```bash
grep -n 'this.textRenderer' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/TextRenderer*.java
```

Each file should have exactly one assignment: `this.textRenderer = textRenderer;`
in its constructor(s).

## Step 7: Verify no public API changes

```bash
grep -c 'public class' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/TextRenderer{Glyph,GlyphProducer,QuadRenderer}.java
```

Expected: 0 matches. All extracted classes use default (package-private)
visibility. The only `public class` in this package remains
`NonCachingTextRenderer`.

## Troubleshooting

### Compilation fails with "cannot find symbol"

A member of `NonCachingTextRenderer` that an extracted class accesses is
still `private`. The extraction requires 14 members to be widened to
package-private. See the [Visibility changes](../reference/noncaching-text-renderer-inner-class-extraction.md#visibility-changes)
table in the reference doc.

### Characterization test reflection fails

If `Class.forName("...NonCachingTextRenderer$Glyph")` throws
`ClassNotFoundException`, it means the inner class was extracted but the
test's reflection target was not updated. Update the test to use the
new top-level class name directly.

### Line count exceeds 1350

Ensure all 3 inner classes (`Glyph`, `GlyphProducer`, `Pipelined_QuadRenderer`)
were removed from `NonCachingTextRenderer.java`. Check for accidental
duplication where the inner class body was left in place after the
extraction.

### `draw()` method is inaccessible

`Pipelined_QuadRenderer.draw()` was `private` in the original inner class.
It must be package-private in `TextRendererQuadRenderer` because
`NonCachingTextRenderer.flushGlyphPipeline()` calls `mPipelinedQuadRenderer.draw()`
to flush the pipeline.

## What this guide does NOT cover

- Extracting additional inner classes (they are small/static and stay in place)
- OpenGL rendering behavior (no functional change)
- Upstream JOGL `TextRenderer` compatibility beyond naming conventions
- Checkstyle enforcement (suppressed by `@SuppressWarnings("CheckStyle")`)
