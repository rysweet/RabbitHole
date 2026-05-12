# Characterize NonCachingTextRenderer

Use this guide to run and review the `NonCachingTextRenderer` characterization
tests. These tests validate buffer constants, extracted class behavior, and
glyph cache lifecycle without an OpenGL context, display server, or native
JOGL bindings.

For the full contract, see the [NonCachingTextRenderer Characterization
reference](../reference/noncaching-text-renderer-characterization.md).

## When to use this guide

Use this guide for changes near:

```text
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRenderer.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/TextRendererGlyph.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/TextRendererGlyphProducer.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/TextRendererQuadRenderer.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/CharSequenceIterator.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/TextData.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/Manager.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/DefaultRenderDelegate.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/CharacterCache.java
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/DebugListener.java
```

For the inner class extraction details, see [Validate NonCachingTextRenderer
Inner Class Extraction](validate-noncaching-text-renderer-inner-class-extraction.md).

Also run these checks when changing:

- Any buffer size constant (`kSize`, `kQuadsPerBuffer`, `kVertsPerQuad`,
  `kCoordsPerVertVerts`, `kCoordsPerVertTex`, or any `kTotal*` / `kSizeInBytes*`
  derived constant);
- `CharSequenceIterator` iteration or boundary behavior;
- `TextData` constructor, accessor, or `used` lifecycle methods;
- `DefaultRenderDelegate` bounds delegation or `intensityOnly()` return value;
- `CharacterCache` range, array size, or `valueOf()` caching threshold;
- `Glyph` constructor fields, `clear()`, `getAdvance()`, or `getGlyphCode()`;
- `GlyphProducer` constructor, `register()`, `clearCacheEntry()`, or
  `clearAllCacheEntries()`;
- Any rename of an extracted class in the package.

Do not use this guide for OpenGL rendering, texture allocation, VBO pipeline,
mipmap generation, or full `getBounds` with cached string locations. Those
behaviors require a live GL context and are outside this characterization lane.

## Before you start

Run commands from the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
export NODE_OPTIONS=--max-old-space-size=32768
```

## Run the characterization suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=NonCachingTextRendererCharacterizationTest \
  test
```

Expected outcome: 49 test methods total. 42 pass, 7 skipped (constructor/accessor
tests requiring JOGL). 0 failures, 0 errors. BUILD SUCCESS.

## Run alongside all core/glrender tests

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am -DfailIfNoTests=false test
```

The characterization suite is the first test file in `core/glrender` and runs
alongside any future tests without interference.

## Review the results

### Constants

| Assertion category | Meaning |
| --- | --- |
| Buffer size derivation | Each derived constant equals the product of its inputs. A failure means a constant was changed without updating dependent values. |
| `kSize` value | The initial texture backing store dimension (256). A change affects `RectanglePacker` allocation. |
| `DISABLE_GLYPH_CACHE` | Currently `true`. Changing this to `false` activates the per-glyph cache path in `TextRendererGlyphProducer.getGlyphs()`. |

### CharSequenceIterator (top-level class)

| Assertion category | Meaning |
| --- | --- |
| Navigation methods | `first()`, `last()`, `next()`, `previous()`, `setIndex()` position the iterator correctly and return the expected character. |
| Boundary: empty sequence | `first()` and `last()` return `CharacterIterator.DONE`; `getEndIndex()` returns 0. |
| Boundary: past-end | `next()` past the last character returns `DONE`. |
| Boundary: before-start | `previous()` at index 0 clamps to 0, returns the first character. |
| Clone independence | Cloned iterator has the same position but modifying one does not affect the other. |

### TextData (top-level class)

| Assertion category | Meaning |
| --- | --- |
| Constructor round-trip | `string()`, `origin()`, `origRect()`, `unicodeID` return the values passed to the constructor. |
| Derived origin | `origOriginX()` and `origOriginY()` return `(int) -origRect.getMinX()` and `(int) -origRect.getMinY()`. |
| Used lifecycle | `used()` starts `false`; `markUsed()` sets it `true`; `clearUsed()` resets to `false`. |

### DefaultRenderDelegate (top-level class)

| Assertion category | Meaning |
| --- | --- |
| `intensityOnly()` | Returns `true`. This controls whether the text renderer uses intensity-only textures for performance. |
| `getBounds` delegation | Non-null rectangle with positive width and height for non-empty text. Exact values are platform-dependent. |

### CharacterCache (top-level class)

| Assertion category | Meaning |
| --- | --- |
| Cache identity | `valueOf(c)` for `c` in 0–127 returns the same `Character` object across calls (`assertSame`). |
| Non-cache boundary | `valueOf((char) 128)` returns a new `Character` object (no identity guarantee). |
| Cache array length | `cache.length == 128`. |

### Constructor / Accessors (skipped without GL)

| Assertion category | Meaning |
| --- | --- |
| `getFont()` | Returns the `Font` passed to the constructor. |
| `getMyUseVertexArrays()` | Defaults to `true`; `setUseVertexArrays(false)` toggles it. |
| `getSmoothing()` | Defaults to `true`. |
| `antialiased` field | Stores the constructor argument (default `false`). |
| `renderDelegate` field | When `null` is passed, constructor creates a `DefaultRenderDelegate`. |
| `mGlyphProducer` field | Non-null after construction. |

## Troubleshooting

### All 49 tests are skipped

The test class accesses extracted top-level classes and uses reflection for
`NonCachingTextRenderer` private members. If the JDK blocks reflective access,
all tests may be skipped. Verify that the JDK allows reflective access to
private members:

```bash
java -version
# Ensure JDK 11+ without restrictive module flags
```

### Constructor/accessor tests are skipped

This is expected in headless CI. These tests require a `NonCachingTextRenderer`
instance, which triggers `RectanglePacker` → `Manager.allocateBackingStore()`.
Without native JOGL libraries or a display server, the constructor fails.

To run these tests:

1. Use a machine with a display (or Xvfb).
2. Ensure JOGL native libraries are on the classpath.
3. The `Assume.assumeTrue` guard will pass and the tests will execute.

### Font metric assertion failures

The `DefaultRenderDelegate` tests assert `> 0` for bounds width and height,
not exact pixel values. If a test fails with bounds == 0:

1. Check that at least one font is available to the JDK.
2. On minimal Docker images, install `fontconfig` and a base font package.

### Class not found after extraction

After Phase 2 extraction, all 9 inner classes are top-level. Tests reference
them directly (e.g., `CharSequenceIterator.class`, `TextData.class`). No
`Class.forName` with `$`-notation should remain in the test suite.

## What this guide does NOT cover

- OpenGL rendering (`beginRendering`, `endRendering`, `draw3D`)
- Texture allocation or backing store management
- VBO pipeline (`TextRendererQuadRenderer`, formerly `Pipelined_QuadRenderer`)
- Full text bounds with cached string locations
- Font selection or `getFont()` / `setFont()` (the `font` field is `final`)
- Mipmap generation
- Desktop UI, Save workflows, or project IO
- Visible scene rendering
