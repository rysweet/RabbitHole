# Tutorial: Trace the NonCachingTextRenderer Characterization

This tutorial walks through the `NonCachingTextRenderer` characterization test
suite. You will trace how buffer constants, inner class behavior, and glyph
cache lifecycle are tested without an OpenGL context.

For the full contract, see the [NonCachingTextRenderer Characterization
reference](../reference/noncaching-text-renderer-characterization.md).

## Contents

- [Goal](#goal)
- [1. Understand the class structure](#1-understand-the-class-structure)
- [2. Trace constant verification](#2-trace-constant-verification)
- [3. Trace CharSequenceIterator tests](#3-trace-charsequenceiterator-tests)
- [4. Trace TextData tests](#4-trace-textdata-tests)
- [5. Trace DefaultRenderDelegate tests](#5-trace-defaultrenderdelegate-tests)
- [6. Trace CharacterCache tests](#6-trace-charactercache-tests)
- [7. Understand constructor/accessor test boundaries](#7-understand-constructoraccessor-test-boundaries)
- [8. Run the tests](#8-run-the-tests)
- [9. Understand the boundaries](#9-understand-the-boundaries)

## Goal

Understand how the characterization test suite documents
`NonCachingTextRenderer`'s headless-safe behavior at each inner class layer.
After this tutorial you will be able to explain:

- Why buffer constants form a derivation chain and what breaks if one changes
- How `CharSequenceIterator` implements `CharacterIterator` and where boundary
  behavior matters
- How `TextData` acts as a data carrier for glyph texture coordinates
- Why `DefaultRenderDelegate.intensityOnly()` returns `true`
- How `CharacterCache` optimizes character boxing for ASCII text
- Why constructor/accessor tests are skipped in headless CI

Open these source files alongside this guide:

```text
core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRenderer.java
core/glrender/src/test/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRendererCharacterizationTest.java
```

## 1. Understand the class structure

`NonCachingTextRenderer` is a 1842-line class that handles OpenGL text
rendering for Alice. It contains seven inner classes:

```text
NonCachingTextRenderer (1842 lines)
├── CharSequenceIterator  (private static)   — CharacterIterator adapter
├── TextData              (package static)   — glyph texture metadata
├── Manager               (package instance) — BackingStoreManager for RectanglePacker
├── DefaultRenderDelegate (public static)    — default TextRenderer.RenderDelegate
├── Glyph                 (package instance) — single glyph or string chunk
├── GlyphProducer         (package instance) — glyph cache + layout engine
├── CharacterCache        (private static)   — ASCII Character cache
└── Pipelined_QuadRenderer (package instance) — VBO quad batching
```

The characterization tests focus on the four static inner classes
(`CharSequenceIterator`, `TextData`, `DefaultRenderDelegate`,
`CharacterCache`) which need no GL context, plus guarded constructor/accessor
tests which require a `NonCachingTextRenderer` instance.

## 2. Trace constant verification

Open `NonCachingTextRenderer.java` at lines 57–79. Find the constant block:

```java
static final int kSize = 256;
static final int kQuadsPerBuffer = 100;
static final int kCoordsPerVertVerts = 3;
static final int kCoordsPerVertTex = 2;
static final int kVertsPerQuad = 4;
static final int kTotalBufferSizeVerts = kQuadsPerBuffer * kVertsPerQuad;
// ... and so on
```

**Why test constants?** These values are wired into `Pipelined_QuadRenderer`,
which allocates direct `FloatBuffer` instances for VBO rendering. If someone
changes `kQuadsPerBuffer` from 100 to 200 without updating
`kTotalBufferSizeVerts`, the VBO buffer will be half the expected size and
OpenGL will read past the buffer boundary.

The characterization test verifies each derivation:

```java
assertEquals(kQuadsPerBuffer * kVertsPerQuad, kTotalBufferSizeVerts);
assertEquals(kTotalBufferSizeVerts * kCoordsPerVertVerts, kTotalBufferSizeCoordsVerts);
```

This is a snapshot, not a correctness assertion. The test says "this is what
the values are today." A future refactoring that changes these values must
also update the test, which ensures the developer reviews the full derivation
chain.

## 3. Trace CharSequenceIterator tests

Open `NonCachingTextRenderer.java` at line 803. `CharSequenceIterator` is a
private static inner class, so the test accesses it via reflection:

```java
Class<?> iterClass = Class.forName(
    "edu.cmu.cs.dennisc.render.joglrenderer.NonCachingTextRenderer$CharSequenceIterator");
Constructor<?> ctor = iterClass.getDeclaredConstructor(CharSequence.class);
ctor.setAccessible(true);
CharacterIterator iter = (CharacterIterator) ctor.newInstance("Hello");
```

**Navigation trace.** With the string "Hello" (length 5):

1. `first()` → returns `'H'`, sets index to 0.
2. `next()` → increments index to 1, returns `'e'`.
3. `last()` → sets index to 4, returns `'o'`.
4. `previous()` → sets index to 3, returns `'l'`.
5. `setIndex(2)` → sets index to 2, returns `'l'`.

**Empty sequence trace.** With the empty string `""` (length 0):

1. `first()` → length is 0, returns `CharacterIterator.DONE`.
2. `last()` → `Math.max(0, 0 - 1)` = 0, `current()` checks `length == 0`,
   returns `DONE`.
3. `getEndIndex()` → returns 0.

**Boundary: `previous()` at index 0.** Line 847:

```java
mCurrentIndex = Math.max(mCurrentIndex - 1, 0);
```

This clamps to 0 instead of going negative. The test verifies that calling
`previous()` when already at the start returns the first character (not `DONE`)
and does not throw.

**Why this matters.** `GlyphProducer.getGlyphs()` uses a `CharSequenceIterator`
to walk input text during glyph layout (line 1418). If `previous()` threw an
exception at index 0 or `next()` returned wrong values, text rendering would
break silently.

## 4. Trace TextData tests

Open `NonCachingTextRenderer.java` at line 893. `TextData` is package-private,
so the test constructs it directly:

```java
Point origin = new Point(5, 10);
Rectangle2D origRect = new Rectangle2D.Double(-3.0, -7.0, 20.0, 15.0);
TextData data = new TextData("Hello", origin, origRect, 72);
```

**Accessor trace:**

- `data.string()` → `"Hello"` (the text this rectangle contains)
- `data.origin()` → `Point(5, 10)` (baseline position within the rectangle)
- `data.origRect()` → the rectangle passed at construction
- `data.origOriginX()` → `(int) -(-3.0)` = `3` (offset from rectangle edge)
- `data.origOriginY()` → `(int) -(-7.0)` = `7`
- `data.unicodeID` → `72` (the `'H'` codepoint)

**Used lifecycle trace:**

```java
assertFalse(data.used());   // Starts unused
data.markUsed();
assertTrue(data.used());    // After markUsed()
data.clearUsed();
assertFalse(data.used());   // After clearUsed()
```

This lifecycle controls glyph eviction. During rendering, `TextData.markUsed()`
is called for every drawn glyph. After `CYCLES_PER_FLUSH` (100) render cycles,
unused entries are evicted from the `RectanglePacker` to reclaim texture space.

## 5. Trace DefaultRenderDelegate tests

Open `NonCachingTextRenderer.java` at line 1145. `DefaultRenderDelegate` is
public, so the test constructs it directly:

```java
DefaultRenderDelegate delegate = new NonCachingTextRenderer.DefaultRenderDelegate();
assertTrue(delegate.intensityOnly());
```

**Why `intensityOnly()` returns `true`:** When `true`, the backing store
texture uses a single-channel intensity format instead of full RGBA. This
saves 75% of texture memory for text rendering, since text glyphs only need
brightness information.

**Bounds delegation trace:** The test creates a font and `FontRenderContext`:

```java
Font font = new Font("SansSerif", Font.PLAIN, 12);
FontRenderContext frc = new FontRenderContext(null, false, false);
Rectangle2D bounds = delegate.getBounds("Hello", font, frc);
```

Inside `getBounds(String, Font, FRC)` (line 1160):

1. `font.createGlyphVector(frc, "Hello")` → creates a `GlyphVector`.
2. Delegates to `getBounds(GlyphVector, FRC)` (line 1166).
3. Returns `gv.getVisualBounds()`.

The test asserts `bounds.getWidth() > 0` and `bounds.getHeight() > 0` but
not exact pixel values, because font metrics vary across platforms.

## 6. Trace CharacterCache tests

Open `NonCachingTextRenderer.java` at line 1557. `CharacterCache` is a
private static inner class, accessed via reflection:

```java
Class<?> cacheClass = Class.forName(
    "edu.cmu.cs.dennisc.render.joglrenderer.NonCachingTextRenderer$CharacterCache");
Method valueOf = cacheClass.getDeclaredMethod("valueOf", char.class);
valueOf.setAccessible(true);
```

**Cache identity trace:**

```java
Character a1 = (Character) valueOf.invoke(null, 'A');  // codepoint 65
Character a2 = (Character) valueOf.invoke(null, 'A');
assertSame(a1, a2);  // Same object from cache
```

Line 1570: `if (c <= 127) { return cache[c]; }` — returns the pre-allocated
`Character` from the static array.

**Non-cache boundary trace:**

```java
Character c128a = (Character) valueOf.invoke(null, (char) 128);
Character c128b = (Character) valueOf.invoke(null, (char) 128);
// No assertSame — Character.valueOf() may or may not cache beyond 127
```

Line 1573: `return Character.valueOf(c);` — delegates to standard boxing,
which has no identity guarantee for values > 127.

**Why this matters.** `GlyphProducer.getGlyphs()` calls
`CharacterCache.valueOf()` at line 1434 for every character during glyph
layout. Caching ASCII characters avoids autoboxing allocation pressure during
text rendering, which happens every frame.

## 7. Understand constructor/accessor test boundaries

The 7 constructor/accessor tests require an enclosing `NonCachingTextRenderer`
instance. The test setup attempts to construct one:

```java
try {
    sharedRenderer = new NonCachingTextRenderer(TEST_FONT);
} catch (Exception | Error e) {
    sharedRenderer = null;
}
```

The `NonCachingTextRenderer` constructor (line 160) calls:

```java
packer = new RectanglePacker(new Manager(), kSize, kSize);
```

`RectanglePacker` stores the `Manager` but may call
`Manager.allocateBackingStore()` eagerly, which creates a `TextureRenderer`.
Without JOGL native libraries, this fails.

When the assumption fails, JUnit marks the test as **skipped** (not failed).
The test output shows:

```text
Tests run: 49, Failures: 0, Errors: 0, Skipped: 7
```

The 7 skipped tests are:

1. `constructor_getFont_returnsSameFont`
2. `constructor_useVertexArrays_defaultsTrue`
3. `constructor_smoothing_defaultsTrue`
4. `constructor_setUseVertexArrays_changes`
5. `constructor_antialiased_storedCorrectly`
6. `constructor_renderDelegate_isDefaultWhenNull`
7. `constructor_glyphProducer_isInitialized`

In a full GL environment (desktop or Xvfb with JOGL natives), all 49 tests
pass.

## 8. Run the tests

From the repository root:

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=NonCachingTextRendererCharacterizationTest \
  test
```

All test methods either pass or skip without a display server or network
access.

## 9. Understand the boundaries

| Question | Answer |
| --- | --- |
| Does this prove text renders correctly on screen? | No. No GL context, no pixel output. |
| Does this prove `getBounds()` returns accurate pixel measurements? | No. Only that it returns a non-null rectangle with positive dimensions. |
| Does this prove `draw3D()` works? | No. That method requires an active GL context and VBO pipeline. |
| Does this prove the glyph cache improves performance? | No. `DISABLE_GLYPH_CACHE = true` in production; the cache is currently bypassed. |
| Does this prove mipmap generation works? | No. Mipmap is not tested headlessly. |
| Does this prove font loading works? | Partially. `new Font("SansSerif", Font.PLAIN, 12)` succeeds if the JDK has fonts installed. |
| What does it prove? | The inner class contracts (iteration, data storage, cache identity, constant derivation) are correct and consistent, establishing a safety net before any refactoring of this 1842-line class. |

The characterization suite is intentionally focused on headless-safe behavior.
Each test exercises one inner class or constant. Broader tests that require GL
rendering belong in the outside-in QA lane with Xvfb or a desktop environment.
