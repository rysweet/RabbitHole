# NonCachingTextRenderer Characterization

This reference is the build contract for the `NonCachingTextRenderer`
characterization lane: headless-safe class behavior, buffer constant
identity, glyph cache lifecycle, text data accessors, character iterator
compliance, and GL-boundary skip rules.

## Contents

- [Scope](#scope)
- [Artifact inventory](#artifact-inventory)
- [Class contracts](#class-contracts)
- [API reference](#api-reference)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Scope

This lane characterizes observable `NonCachingTextRenderer` behavior that is
testable without an OpenGL context, display server, or native JOGL bindings.
It documents the headless-safe classes (now top-level after extraction),
static constants, and pure-logic helper methods that form the foundation of
Alice's OpenGL text rendering pipeline.

All 9 inner classes have been extracted to top-level files (see the
[inner class extraction reference](noncaching-text-renderer-inner-class-extraction.md)).
The characterization tests reference these classes directly by their
top-level fully-qualified names rather than `$`-notation inner class paths.

It covers:

| Area | Contract |
| --- | --- |
| Buffer constants | Static constant values and derived buffer sizes are self-consistent: `kSize`, `kQuadsPerBuffer`, `kVertsPerQuad`, `kCoordsPerVertVerts`, `kCoordsPerVertTex`, computed totals, and debug/config flags (`DISABLE_GLYPH_CACHE`, `DRAW_BBOXES`, `CYCLES_PER_FLUSH`, `MAX_VERTICAL_FRAGMENTATION`). |
| `preNormalize` | The static geometry method expands a `Rectangle2D` by floor/ceil rounding + 1px slop on all sides. |
| CharSequenceIterator | The package-private top-level class implements `CharacterIterator` correctly: `first()`, `last()`, `current()`, `next()`, `previous()`, `setIndex()`, `getBeginIndex()`, `getEndIndex()`, `getIndex()`, `clone()`, and empty-sequence edge cases. |
| TextData | The package-private top-level class preserves constructor arguments through accessor methods: `string()`, `origin()`, `origRect()`, `origOriginX()`, `origOriginY()`, and the `used`/`markUsed()`/`clearUsed()` lifecycle. |
| DefaultRenderDelegate | The public top-level class returns `true` from `intensityOnly()` and delegates `getBounds(GlyphVector, FontRenderContext)` to `GlyphVector.getVisualBounds()`. |
| CharacterCache | The package-private top-level class caches `Character` values for codepoints 0–127 and returns fresh `Character.valueOf()` for codepoints > 127. |
| TextRendererGlyph (Glyph) | Not directly tested. Extracted to top-level class. Requires a `NonCachingTextRenderer` instance (GL-dependent). |
| TextRendererGlyphProducer (GlyphProducer) | Not directly tested. Extracted to top-level class. Requires a `NonCachingTextRenderer` instance (GL-dependent). |
| Constructor / Accessors | `getFont()`, `getSmoothing()`, `getMyUseVertexArrays()`, `setUseVertexArrays()`, `antialiased` field, `renderDelegate` field, `mGlyphProducer` field — guarded by `Assume.assumeTrue` (skip in headless CI). |

This lane does not cover:

- OpenGL rendering (`beginRendering`, `endRendering`, `draw3D`, `draw3D_ROBUST`)
- Texture allocation (`Manager.allocateBackingStore`)
- VBO pipeline (`TextRendererQuadRenderer`, formerly `Pipelined_QuadRenderer`)
- Font metrics that require a live `FontRenderContext` from a GL surface
- Mipmap generation or backing store compaction
- Full `getBounds(CharSequence)` with cached string locations

## Artifact inventory

| Artifact | Purpose |
| --- | --- |
| `NonCachingTextRenderer.java` | Production class (~850 lines after full inner class extraction). No inner classes remain. |
| `CharSequenceIterator.java` | Extracted from `CharSequenceIterator` static inner class. Package-private. |
| `TextData.java` | Extracted from `TextData` static inner class. Package-private. |
| `DefaultRenderDelegate.java` | Extracted from `DefaultRenderDelegate` public static inner class. Public. |
| `CharacterCache.java` | Extracted from `CharacterCache` static inner class. Package-private. |
| `Manager.java` | Extracted from `Manager` non-static inner class. Package-private. Has `NonCachingTextRenderer` back-reference. |
| `DebugListener.java` | Extracted from `DebugListener` non-static inner class. Package-private. Has `NonCachingTextRenderer` back-reference. |
| `TextRendererGlyph.java` | Extracted from `Glyph` inner class. Package-private. |
| `TextRendererGlyphProducer.java` | Extracted from `GlyphProducer` inner class. Package-private. |
| `TextRendererQuadRenderer.java` | Extracted from `Pipelined_QuadRenderer` inner class. Package-private. |
| `NonCachingTextRendererCharacterizationTest.java` | Characterization test suite (~480 lines, 49 test methods). |

## Class contracts

### Constants (in NonCachingTextRenderer)

The buffer size constants form a derivation chain:

```text
kQuadsPerBuffer = 100
kVertsPerQuad = 4
kCoordsPerVertVerts = 3
kCoordsPerVertTex = 2

kTotalBufferSizeVerts = kQuadsPerBuffer × kVertsPerQuad = 400
kTotalBufferSizeCoordsVerts = kTotalBufferSizeVerts × kCoordsPerVertVerts = 1200
kTotalBufferSizeCoordsTex = kTotalBufferSizeVerts × kCoordsPerVertTex = 800
kTotalBufferSizeBytesVerts = kTotalBufferSizeCoordsVerts × 4 = 4800
kTotalBufferSizeBytesTex = kTotalBufferSizeCoordsTex × 4 = 3200
kSizeInBytes_OneVertices_VertexData = kCoordsPerVertVerts × 4 = 12
kSizeInBytes_OneVertices_TexData = kCoordsPerVertTex × 4 = 8
kSize = 256
DISABLE_GLYPH_CACHE = true
```

The characterization tests verify each derivation step. If any constant
changes, the tests will fail, alerting reviewers to recalculate downstream
buffer allocations.

### preNormalize (static method in NonCachingTextRenderer)

A static method that expands a `Rectangle2D` by rounding to integer
coordinates and adding 1-pixel slop on all sides:

| Input | Output |
| --- | --- |
| `(0.5, 0.5, 10.0, 10.0)` | `(-1.0, -1.0, 13.0, 13.0)` — floor(min)-1, ceil(max)+1 |
| `(-3.2, -1.8, 5.0, 4.0)` | `(-5.0, -3.0, 8.0, 7.0)` — handles negative coordinates |
| `(0, 0, 10, 10)` | `(-1.0, -1.0, 12.0, 12.0)` — integer input still expands |

### CharSequenceIterator (top-level class, formerly static inner)

A package-private class implementing `java.text.CharacterIterator` for
`CharSequence` inputs. Key behavioral contracts:

| Method | Behavior |
| --- | --- |
| `first()` on empty | Returns `CharacterIterator.DONE`, index stays at 0. |
| `first()` on non-empty | Returns first char, sets index to 0. |
| `last()` on empty | Returns `CharacterIterator.DONE`, index stays at 0. |
| `last()` on non-empty | Returns last char, sets index to `length - 1`. |
| `current()` at end | Returns `CharacterIterator.DONE` when index ≥ length. |
| `next()` past end | Increments index, returns `DONE`. |
| `previous()` at start | Clamps index to 0, returns first char. |
| `setIndex(n)` | Sets index to `n`, returns char at that position. |
| `getBeginIndex()` | Always returns 0. |
| `getEndIndex()` | Returns sequence length. |
| `clone()` | Returns a new `CharSequenceIterator` with the same sequence and current index. |

The tests access this class directly as a top-level class. No reflection
with `$`-notation is needed.

### TextData (top-level class, formerly static inner)

A package-private class storing text rendering metadata:

| Field / Method | Contract |
| --- | --- |
| `string()` | Returns the `str` argument from the constructor, or `null` for single-glyph entries. |
| `origin()` | Returns the `Point` passed at construction. |
| `origRect()` | Returns the `Rectangle2D` passed at construction. |
| `origOriginX()` | Returns `(int) -origRect.getMinX()`. |
| `origOriginY()` | Returns `(int) -origRect.getMinY()`. |
| `used()` / `markUsed()` / `clearUsed()` | Boolean lifecycle: starts `false`, `markUsed()` sets `true`, `clearUsed()` resets to `false`. |
| `unicodeID` | Stores the unicode ID from the constructor (public field). |

### DefaultRenderDelegate (top-level class, formerly public static inner)

A public class implementing `TextRenderer.RenderDelegate`:

| Method | Contract |
| --- | --- |
| `intensityOnly()` | Always returns `true`. |
| `getBounds(CharSequence, Font, FRC)` | Creates a `GlyphVector` via `font.createGlyphVector(frc, new CharSequenceIterator(str))`, then delegates to `getBounds(GlyphVector, FRC)`. |
| `getBounds(String, Font, FRC)` | Creates a `GlyphVector` via `font.createGlyphVector(frc, str)`, then delegates to `getBounds(GlyphVector, FRC)`. |
| `getBounds(GlyphVector, FRC)` | Returns `gv.getVisualBounds()`. |
| `drawGlyphVector(g, gv, x, y)` | Delegates to `g.drawGlyphVector(gv, x, y)`. |
| `draw(g, str, x, y)` | Delegates to `g.drawString(str, x, y)`. |

The bounds methods are tested with non-empty strings to verify non-null,
positive-dimension results. Exact pixel values are not asserted because font
metrics vary across platforms and JDK versions.

### CharacterCache (top-level class, formerly static inner)

A package-private class caching `Character` objects for ASCII codepoints:

| Behavior | Contract |
| --- | --- |
| `cache` array | Length 128 (indices 0–127). Each entry is `Character.valueOf((char) i)`. |
| `valueOf(c)` where `c ≤ 127` | Returns the cached `Character` object (same identity across calls). |
| `valueOf(c)` where `c > 127` | Returns `Character.valueOf(c)` (a fresh object). |
| Boundary: `valueOf((char) 127)` | Returns cached object. |
| Boundary: `valueOf((char) 128)` | Returns non-cached object. |

Tests access this class directly as a top-level class. No reflection with
`$`-notation is needed.

### TextRendererGlyph (extracted from Glyph) — not directly tested

Extracted from inner class `Glyph` into top-level package-private
`TextRendererGlyph.java`. See the [inner class extraction
reference](noncaching-text-renderer-inner-class-extraction.md) for details.

No characterization tests exist for this class because constructing a
`TextRendererGlyph` requires a `NonCachingTextRenderer` instance, which
triggers GL initialization in headless CI.

| Constructor | Fields set |
| --- | --- |
| `TextRendererGlyph(unicodeID, glyphCode, advance, glyphVector, producer, textRenderer)` | Individual unicode glyph with all rendering fields. |
| `TextRendererGlyph(str, needAdvance, textRenderer)` | String fallback glyph for complex text sequences. |

| Method | Contract |
| --- | --- |
| `getUnicodeID()` | Returns the `unicodeID` field. |
| `getGlyphCode()` | Returns the `glyphCode` field. |
| `getAdvance()` | Returns the `advance` field. |
| `clear()` | Sets `glyphRectForTextureMapping` to `null`. |

### TextRendererGlyphProducer (extracted from GlyphProducer) — not directly tested

Extracted from inner class `GlyphProducer` into top-level package-private
`TextRendererGlyphProducer.java`. See the [inner class extraction
reference](noncaching-text-renderer-inner-class-extraction.md) for details.

No characterization tests exist for this class (same GL dependency as
`TextRendererGlyph`).

| Method | Contract |
| --- | --- |
| Constructor | Takes `(int fontLengthInGlyphs, NonCachingTextRenderer textRenderer)`. Allocates `unicodes2Glyphs[512]` filled with `undefined` (-2), and `glyphCache[fontLengthInGlyphs]`. |
| `clearAllCacheEntries()` | Iterates 0–511, setting each `unicodes2Glyphs` entry to `undefined`. |
| `register(glyph)` | Sets `unicodes2Glyphs[glyph.unicodeID] = glyph.glyphCode` and `glyphCache[glyph.glyphCode] = glyph`. |
| `clearCacheEntry(unicodeID)` | Clears the glyph at the given unicode ID and resets the mapping to `undefined`. |
| `getGlyphs(CharSequence)` | Requires `getFontRenderContext()` → **GL-dependent, not tested headlessly**. |
| `getGlyphPixelWidth(char)` | Returns `glyph.getAdvance()` for cached glyphs. For uncached glyphs, falls through to `fontRenderContext` which is never initialized → **throws `InternalError` (FIXME in source)**. |

### Constructor / Accessors — guarded, skipped without GL

The 7 constructor/accessor tests require a `NonCachingTextRenderer` instance.
The constructor calls `new RectanglePacker(new Manager(this),
kSize, kSize)` may fail without native JOGL libraries. Tests guard with
`Assume.assumeTrue("Needs headless JOGL to construct", sharedRenderer != null)`.

| Test | Contract |
| --- | --- |
| `constructor_getFont_returnsSameFont` | `getFont()` returns the `Font` passed to the constructor. |
| `constructor_useVertexArrays_defaultsTrue` | `getMyUseVertexArrays()` defaults to `true`. |
| `constructor_smoothing_defaultsTrue` | `getSmoothing()` defaults to `true`. |
| `constructor_setUseVertexArrays_changes` | `setUseVertexArrays(false)` toggles the value. |
| `constructor_antialiased_storedCorrectly` | `antialiased` field stores the constructor argument (default `false`). |
| `constructor_renderDelegate_isDefaultWhenNull` | When `null` is passed, constructor creates a `DefaultRenderDelegate`. |
| `constructor_glyphProducer_isInitialized` | `mGlyphProducer` field is non-null after construction. |

## API reference

The test suite uses a mix of direct class access and reflection. After Phase 2
extraction, `CharSequenceIterator`, `TextData`, `CharacterCache`, and
`DefaultRenderDelegate` are top-level classes. Tests reference them directly
rather than via `Class.forName` with `$`-notation.

| Class | Access pattern |
| --- | --- |
| `CharSequenceIterator` | Direct constructor access — top-level package-private class. |
| `TextData` | Direct constructor access — top-level package-private class. |
| `DefaultRenderDelegate` | Direct class reference — top-level public class. |
| `CharacterCache` | Direct class access — `valueOf` method and `cache` field. |
| `preNormalize` method | `getDeclaredMethod("preNormalize", Rectangle2D.class)` with `setAccessible(true)` (still a private method in `NonCachingTextRenderer`). |
| Constructor fields | `getDeclaredField("antialiased")`, `getDeclaredField("renderDelegate")`, `getDeclaredField("mGlyphProducer")` with `setAccessible(true)`. |

## Validation commands

### Run the full characterization suite

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=NonCachingTextRendererCharacterizationTest \
  test
```

Expected outcome: 49 test methods. 42 pass, 7 skipped (constructor/accessor
tests requiring JOGL, guarded by `Assume.assumeTrue`). 0 failures, 0
errors.

### Run alongside all core/glrender tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip clean test
```

The characterization suite runs alongside the contract tests and any future
tests without interference.

## Compatibility rules

1. **Do not assert exact font metrics.** Font rendering varies across platforms,
   JDK versions, and installed fonts. Assert `> 0`, `non-null`, or structural
   properties only.

2. **Do not remove `Assume.assumeTrue` guards.** GL-dependent tests must skip
   cleanly in headless CI. Converting them to `@Ignore` loses the ability to
   run them in a full GL environment.

3. **Do not change constant values without recalculating downstream buffers.**
   The constant derivation chain is load-bearing: VBO allocation, texture
   coordinate scaling, and quad vertex positioning all depend on these exact
   values.

4. **Reflection targets for extracted classes use top-level FQNs.** After
   Phase 2 extraction, `Class.forName` paths no longer use `$`-notation
   (e.g., `...NonCachingTextRenderer$CharSequenceIterator`). Instead, tests
   reference classes directly (e.g., `CharSequenceIterator.class`). If a
   class is renamed, update the direct reference.

5. **`CharSequenceIterator` boundary behavior is load-bearing.** The `previous()`
   clamping to index 0 and the `DONE` sentinel for empty sequences are
   used by `TextRendererGlyphProducer.getGlyphs()` during text layout.
   Changes to these behaviors require updating both the tests and all callers.

6. **`DISABLE_GLYPH_CACHE = true` is the current production value.** This means
   `TextRendererGlyphProducer.getGlyphs()` always takes the "punt to robust
   renderer" path. The glyph cache data structures (`unicodes2Glyphs`,
   `glyphCache`) are allocated but effectively unused in production.

## Examples

### Verify a constant change is safe

Before changing `kQuadsPerBuffer` from 100 to 200:

1. Run the characterization suite (see [Validation commands](#validation-commands)).
2. The constant tests fail, showing the old expected values.
3. Recalculate all derived constants.
4. Update the test expectations to match.
5. Verify that VBO allocation in `TextRendererQuadRenderer` uses the same
   constants and that buffer sizes are consistent.

### Add a new headless-safe test for an extracted class

1. Identify the class and its access level.
2. If package-private, access it directly from the test (same package).
3. If the class requires a `NonCachingTextRenderer` instance, guard with
   `Assume.assumeTrue` in case the constructor triggers GL.
4. Assert observable behavior (return values, field state), not implementation
   details.
5. Run the validation command and verify the new test appears in the results.

### Understand why constructor tests are skipped

The 7 constructor/accessor tests require a `NonCachingTextRenderer` instance.
The constructor calls `new RectanglePacker(new Manager(this), kSize, kSize)`,
which may trigger `Manager.allocateBackingStore()`. On a headless CI server
without native JOGL libraries, this fails. The tests guard against this with:

```java
Assume.assumeTrue("Needs headless JOGL to construct", sharedRenderer != null);
```

When the assumption fails, JUnit marks the test as skipped (not failed). In a
full desktop or Xvfb environment with JOGL native libraries, these tests run
and pass.
