# NonCachingTextRenderer Inner Class Extraction

This reference documents the extraction of all 9 inner classes from
`NonCachingTextRenderer` into separate top-level package-private files in
`edu.cmu.cs.dennisc.render.joglrenderer`. Phase 1 (issue #514 / PR #523)
extracted the 3 largest non-static inner classes. Phase 2 (issue #524)
extracted the 6 remaining inner classes, bringing `NonCachingTextRenderer`
under 900 lines.

## Contents

- [Motivation](#motivation)
- [Extracted classes](#extracted-classes)
- [File inventory](#file-inventory)
- [Enclosing instance pattern](#enclosing-instance-pattern)
- [Visibility changes](#visibility-changes)
- [Type reference updates](#type-reference-updates)
- [Validation commands](#validation-commands)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)

## Motivation

`NonCachingTextRenderer.java` was originally 1842 lines with 9 inner classes.
Phase 1 (PR #523) extracted the 3 largest non-static inner classes (`Glyph`,
`GlyphProducer`, `Pipelined_QuadRenderer`), reducing the file to ~1318 lines.

Phase 2 (issue #524) completes the extraction by moving the remaining 6 inner
classes to their own top-level files:

- 4 static classes: `CharSequenceIterator`, `TextData`, `CharacterCache`,
  `DefaultRenderDelegate`
- 2 non-static classes: `Manager`, `DebugListener`

After both phases, `NonCachingTextRenderer.java` is under 900 lines
(down from 1318), containing only the public API methods, rendering entry
points, and field declarations. Every inner class has been extracted into
its own file.

## Extracted classes

### Phase 1 (issue #514 / PR #523)

| Original inner class | New top-level class | Static? | Purpose |
| --- | --- | --- | --- |
| `Glyph` | `TextRendererGlyph` | non-static | Represents a unicode glyph or character substring for rendering. Manages glyph texture upload, texture-mapped quad emission, and glyph vector lifecycle. |
| `GlyphProducer` | `TextRendererGlyphProducer` | non-static | Manages the glyph cache: unicode→glyph-code mapping, glyph fabrication from `GlyphVector`, and cache eviction. |
| `Pipelined_QuadRenderer` | `TextRendererQuadRenderer` | non-static | Batched OpenGL quad pipeline. Buffers texture coordinates and vertex data, flushes via VBO or immediate-mode GL. |

### Phase 2 (issue #524)

| Original inner class | New top-level class | Static? | Back-ref? | Visibility |
| --- | --- | --- | --- | --- |
| `CharSequenceIterator` | `CharSequenceIterator` | static | No | package-private |
| `TextData` | `TextData` | static | No | package-private |
| `Manager` | `Manager` | **non-static** | Yes | package-private |
| `DefaultRenderDelegate` | `DefaultRenderDelegate` | static | No | **public** (API compat) |
| `CharacterCache` | `CharacterCache` | static | No | package-private |
| `DebugListener` | `DebugListener` | **non-static** | Yes | package-private |

## File inventory

| File | Role | Approx lines |
| --- | --- | --- |
| `NonCachingTextRenderer.java` | Parent class — no inner classes remain. Owns field declarations, constructor, and all rendering entry points. | ~850 |
| `TextRendererGlyph.java` | Extracted `Glyph`. Package-private. | ~200 |
| `TextRendererGlyphProducer.java` | Extracted `GlyphProducer`. Package-private. | ~170 |
| `TextRendererQuadRenderer.java` | Extracted `Pipelined_QuadRenderer`. Package-private. | ~165 |
| `CharSequenceIterator.java` | Extracted `CharSequenceIterator`. Package-private static. Implements `CharacterIterator` for `CharSequence` inputs. | ~90 |
| `TextData.java` | Extracted `TextData`. Package-private static. Value object for cached text rectangles. | ~70 |
| `Manager.java` | Extracted `Manager`. Package-private. Implements `BackingStoreManager`, has `NonCachingTextRenderer` back-reference. | ~185 |
| `DefaultRenderDelegate.java` | Extracted `DefaultRenderDelegate`. **Public** static. Implements `TextRenderer.RenderDelegate`. | ~40 |
| `CharacterCache.java` | Extracted `CharacterCache`. Package-private static. Fast `Character` boxing for ASCII ≤127. | ~25 |
| `DebugListener.java` | Extracted `DebugListener`. Package-private. Implements `GLEventListener`, has `NonCachingTextRenderer` back-reference. | ~60 |
| `InnerClassExtractionContractTest.java` | Contract test suite. Verifies all 9 inner classes are absent, all 9 top-level classes exist, visibility rules, and line count. | ~700 |
| `NonCachingTextRendererCharacterizationTest.java` | Characterization test suite. Updated reflection targets from `$`-inner-class notation to top-level FQNs. | ~480 |

All source files reside in `core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/`.

## Enclosing instance pattern

### Phase 1 classes (non-static → back-reference)

Each non-static inner class previously held an implicit
`NonCachingTextRenderer.this` reference. After extraction, each top-level
class takes an explicit `NonCachingTextRenderer textRenderer` parameter in
its constructor:

```java
// TextRendererGlyph — unicode glyph constructor
TextRendererGlyph(int unicodeID, int glyphCode, float advance,
                  GlyphVector singleUnicodeGlyphVector,
                  TextRendererGlyphProducer producer,
                  NonCachingTextRenderer textRenderer) { ... }

// TextRendererGlyphProducer
TextRendererGlyphProducer(int fontLengthInGlyphs,
                          NonCachingTextRenderer textRenderer) { ... }

// TextRendererQuadRenderer
TextRendererQuadRenderer(NonCachingTextRenderer textRenderer) { ... }
```

The field is named `textRenderer` (not `renderer`) to avoid shadowing the
`TextureRenderer renderer` local variables used throughout `Glyph.draw3D()`
and `Pipelined_QuadRenderer.drawVertexArrays()`.

### Phase 2 classes

**Static classes (no back-reference needed):** `CharSequenceIterator`,
`TextData`, `CharacterCache`, and `DefaultRenderDelegate` were `static`
inner classes. They had no implicit outer reference and require no
constructor changes.

**Non-static classes (back-reference added):**

```java
// Manager — implements BackingStoreManager
Manager(NonCachingTextRenderer textRenderer) { ... }

// DebugListener — implements GLEventListener
DebugListener(GL gl, Frame frame, NonCachingTextRenderer textRenderer) { ... }
```

`Manager` accesses 16+ fields of `NonCachingTextRenderer` (render state,
cached colors, packer, glyph producer) through its `textRenderer`
back-reference.

`DebugListener` accesses `packer`, `getBackingStore()`, and
`mPipelinedQuadRenderer` through its `textRenderer` back-reference.

### Member access mapping (Phase 2 additions)

| Before (inner class) | After (top-level class) | Used by |
| --- | --- | --- |
| `renderDelegate` | `textRenderer.renderDelegate` | `Manager` (already Phase 1 widened) |
| `mipmap` | `textRenderer.mipmap` | `Manager` |
| `smoothing` | `textRenderer.smoothing` | `Manager` |
| `inBeginEndPair` | `textRenderer.inBeginEndPair` | `Manager` |
| `isOrthoMode` | `textRenderer.isOrthoMode` | `Manager` |
| `beginRenderingWidth` | `textRenderer.beginRenderingWidth` | `Manager` |
| `beginRenderingHeight` | `textRenderer.beginRenderingHeight` | `Manager` |
| `beginRenderingDepthTestDisabled` | `textRenderer.beginRenderingDepthTestDisabled` | `Manager` |
| `haveCachedColor` | `textRenderer.haveCachedColor` | `Manager` |
| `cachedR` / `cachedG` / `cachedB` / `cachedA` | `textRenderer.cachedR` etc. | `Manager` |
| `cachedColor` | `textRenderer.cachedColor` | `Manager` |
| `needToResetColor` | `textRenderer.needToResetColor` | `Manager` |
| `stringLocations` | `textRenderer.stringLocations` | `Manager` |
| `mGlyphProducer` | `textRenderer.mGlyphProducer` | `Manager` |
| `flush()` | `textRenderer.flush()` | `Manager` (already public) |
| `clearUnusedEntries()` | `textRenderer.clearUnusedEntries()` | `Manager` |
| `getMyUseVertexArrays()` | `textRenderer.getMyUseVertexArrays()` | `Manager` (already public) |
| `is15Available(gl)` | `textRenderer.is15Available(gl)` | `Manager` (already Phase 1 widened) |
| `isExtensionAvailable_GL_VERSION_1_5` | `textRenderer.isExtensionAvailable_GL_VERSION_1_5` | `Manager` (already Phase 1 widened) |
| `packer` | `textRenderer.packer` | `Manager`, `DebugListener` (already Phase 1 widened) |
| `getBackingStore()` | `textRenderer.getBackingStore()` | `DebugListener` (already Phase 1 widened) |
| `mPipelinedQuadRenderer` | `textRenderer.mPipelinedQuadRenderer` | `DebugListener` (already package-private) |
| `NonCachingTextRenderer.CharSequenceIterator(str)` | `new CharSequenceIterator(str)` | `DefaultRenderDelegate` |
| `NonCachingTextRenderer.CharacterCache.cache[c]` | `CharacterCache.cache[c]` | `CharacterCache` |

## Visibility changes

### Phase 1 widened members (14 total)

8 fields (6 instance, 2 static) and 6 methods (5 instance, 1 static) were
widened from `private` to package-private for the Phase 1 extracted classes.
See the original tables below.

#### Instance fields (6)

| Field | Type | Accessed by |
| --- | --- | --- |
| `font` | `Font` | `TextRendererGlyph`, `TextRendererGlyphProducer` |
| `packer` | `RectanglePacker` | `TextRendererGlyph`, `DebugListener` |
| `renderDelegate` | `RenderDelegate` | `TextRendererGlyph` |
| `singleUnicode` | `char[]` | `TextRendererGlyph`, `TextRendererGlyphProducer` |
| `useVertexArrays` | `boolean` | `TextRendererQuadRenderer` |
| `isExtensionAvailable_GL_VERSION_1_5` | `boolean` | `TextRendererQuadRenderer`, `Manager` |

#### Static fields (2)

| Field | Type | Accessed by |
| --- | --- | --- |
| `DISABLE_GLYPH_CACHE` | `static boolean` | `TextRendererGlyphProducer` |
| `DRAW_BBOXES` | `static boolean` | `TextRendererGlyph` |

#### Methods (6)

| Method | Accessed by |
| --- | --- |
| `getBackingStore()` | `TextRendererGlyph`, `TextRendererQuadRenderer`, `DebugListener` |
| `getGraphics2D()` | `TextRendererGlyph` |
| `draw3D_ROBUST(String, float, float, float, float)` | `TextRendererGlyph` |
| `normalize(Rectangle2D)` | `TextRendererGlyph` |
| `is15Available(GL)` | `TextRendererQuadRenderer`, `Manager` |
| `preNormalize(Rectangle2D)` (static) | `TextRendererGlyph` |

### Phase 2 additional widened members (17 total)

16 fields and 1 method are widened from `private` to package-private for
the Phase 2 extracted classes (primarily `Manager` and `DebugListener`).

#### Instance fields (16)

| Field | Type | Accessed by |
| --- | --- | --- |
| `mipmap` | `boolean` | `Manager` |
| `smoothing` | `boolean` | `Manager` |
| `inBeginEndPair` | `boolean` | `Manager` |
| `isOrthoMode` | `boolean` | `Manager` |
| `beginRenderingWidth` | `int` | `Manager` |
| `beginRenderingHeight` | `int` | `Manager` |
| `beginRenderingDepthTestDisabled` | `boolean` | `Manager` |
| `haveCachedColor` | `boolean` | `Manager` |
| `cachedR` | `float` | `Manager` |
| `cachedG` | `float` | `Manager` |
| `cachedB` | `float` | `Manager` |
| `cachedA` | `float` | `Manager` |
| `cachedColor` | `Color` | `Manager` |
| `needToResetColor` | `boolean` | `Manager` |
| `stringLocations` | `Map<String, Rect>` | `Manager` |
| `mGlyphProducer` | `TextRendererGlyphProducer` | `Manager` |

#### Methods (1)

| Method | Accessed by |
| --- | --- |
| `clearUnusedEntries()` | `Manager` |

### Members that do NOT need Phase 2 widening

| Member | Current visibility | Reason |
| --- | --- | --- |
| `getFontRenderContext()` | `public` | Already accessible from any class. |
| `getMyUseVertexArrays()` | `public` | Already accessible from any class. Used by `Manager.beginMovement()`. |
| `flush()` | `public` | Already accessible from any class. Used by `Manager.preExpand()` and `Manager.beginMovement()`. |
| `renderDelegate` | package-private | Already widened in Phase 1. Used by `Manager.allocateBackingStore()`. |
| `packer` | package-private | Already widened in Phase 1. Used by `Manager.additionFailed()` and `DebugListener.display()`. |
| `isExtensionAvailable_GL_VERSION_1_5` | package-private | Already widened in Phase 1. Used by `Manager.beginMovement()`. |
| `mPipelinedQuadRenderer` | package-private | Already has default access (no modifier). Used by `DebugListener.dispose()`. |
| `getBackingStore()` | package-private | Already widened in Phase 1. Used by `DebugListener.display()`. |
| `is15Available(GL)` | package-private | Already widened in Phase 1. Used by `Manager.beginMovement()`. |

All buffer-size constants (`kTotalBuffer*`, `kSizeInBytes*`, `kTotalBufferSizeVerts`,
`kSize`, `kQuadsPerBuffer`, `kVertsPerQuad`, `kCoordsPerVertVerts`,
`kCoordsPerVertTex`) were already package-private (no modifier) and require
no change.

### Pipelined_QuadRenderer.draw()

`draw()` was `private` in the inner class. It is now package-private in
`TextRendererQuadRenderer` because `NonCachingTextRenderer.flushGlyphPipeline()`
calls `mPipelinedQuadRenderer.draw()` to flush the pipeline.

## Type reference updates

### Phase 1 references (within NonCachingTextRenderer)

| Old reference | New reference |
| --- | --- |
| `NonCachingTextRenderer.Glyph` | `TextRendererGlyph` |
| `NonCachingTextRenderer.GlyphProducer` | `TextRendererGlyphProducer` |
| `NonCachingTextRenderer.Pipelined_QuadRenderer` | `TextRendererQuadRenderer` |
| `new NonCachingTextRenderer.Glyph(...)` | `new TextRendererGlyph(..., this)` |
| `new NonCachingTextRenderer.GlyphProducer(n)` | `new TextRendererGlyphProducer(n, this)` |
| `new NonCachingTextRenderer.Pipelined_QuadRenderer()` | `new TextRendererQuadRenderer(this)` |

### Phase 2 references

#### Within NonCachingTextRenderer

| Old reference | New reference | Locations |
| --- | --- | --- |
| `NonCachingTextRenderer.Manager()` | `new Manager(this)` | Constructor |
| `NonCachingTextRenderer.DefaultRenderDelegate()` | `new DefaultRenderDelegate()` | Constructor |
| `NonCachingTextRenderer.DebugListener(gl, dbgFrame)` | `new DebugListener(gl, dbgFrame, this)` | `debug()` method |
| `NonCachingTextRenderer.TextData` casts | `TextData` | `getBounds()`, `clearUnusedEntries()`, `draw3D_ROBUST()` (7 sites) |

#### Cross-file references

| File | Old reference | New reference |
| --- | --- | --- |
| `TextRendererGlyphProducer.java` | `NonCachingTextRenderer.CharSequenceIterator` | `CharSequenceIterator` |
| `TextRendererGlyphProducer.java` | `NonCachingTextRenderer.CharacterCache.valueOf()` | `CharacterCache.valueOf()` |
| `TextRendererGlyph.java` | `NonCachingTextRenderer.TextData` | `TextData` (3 sites) |
| `InnerClassExtractionContractTest.java` | `assertInnerClassPresent` for 6 Phase 2 classes | `assertInnerClassAbsent` |
| `InnerClassExtractionContractTest.java` | `NonCachingTextRenderer$CharSequenceIterator` (iter field type) | top-level `CharSequenceIterator` class |
| `InnerClassExtractionContractTest.java` | `lineCount < 1350` | `lineCount < 900` |
| `NonCachingTextRendererCharacterizationTest.java` | `NonCachingTextRenderer.DefaultRenderDelegate` | `DefaultRenderDelegate` |
| `NonCachingTextRendererCharacterizationTest.java` | `NonCachingTextRenderer.TextData` | `TextData` |
| `NonCachingTextRendererCharacterizationTest.java` | `Class.forName("...$CharSequenceIterator")` | `Class.forName("...CharSequenceIterator")` |
| `NonCachingTextRendererCharacterizationTest.java` | `Class.forName("...$CharacterCache")` | `Class.forName("...CharacterCache")` |

No inner class `$`-notation references remain anywhere in the codebase.

## Validation commands

### Run the contract test suite

```bash
git submodule update --init tweedle-lang
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=InnerClassExtractionContractTest \
  test
```

This verifies all 9 inner classes are absent from `NonCachingTextRenderer`,
all 9 top-level classes exist, visibility is correct, back-reference fields
exist for `Manager` and `DebugListener`, and line count is under 900.

**Note:** The design spec originally proposed a 500-line threshold, but
extracting ~466 lines of inner classes from 1318 lines yields ~852 lines.
The threshold is set to 900 to accommodate the actual remaining code.

### Run the characterization test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=NonCachingTextRendererCharacterizationTest \
  test
```

Expected: 49 tests, 42 pass, 7 skipped (constructor/accessor tests requiring
JOGL). 0 failures, 0 errors.

### Run the full module test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip clean test
```

This verifies the extraction compiles cleanly with all module dependencies
and that no existing tests break. The `-Dcheckstyle.skip` flag is used
because the extracted files carry `@SuppressWarnings("CheckStyle")` to
preserve comparison compatibility with the upstream JOGL `TextRenderer`.

### Verify line count target

```bash
wc -l core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRenderer.java
```

Expected: under 900 lines (down from 1842 before Phase 1, 1318 before Phase 2).
Extracting the 6 inner classes (~466 lines) yields approximately 852 lines.

## Compatibility rules

1. **All new classes except `DefaultRenderDelegate` are package-private.** No
   public API surface changes. External code that depends on
   `NonCachingTextRenderer` sees no difference.

2. **`DefaultRenderDelegate` stays `public`.** It was `public static` as an
   inner class and implements `TextRenderer.RenderDelegate`. Changing its
   visibility would break external code that passes custom `RenderDelegate`
   instances or references `NonCachingTextRenderer.DefaultRenderDelegate`
   directly.

3. **Thread safety is unchanged.** The explicit `textRenderer` field is
   equivalent to the implicit `Outer.this` reference that non-static inner
   classes hold. No new synchronization is introduced or removed.

4. **`@SuppressWarnings("CheckStyle")` is applied to all extracted files.**
   These files are near-verbatim copies of JOGL `TextRenderer` internals.
   Checkstyle enforcement would generate hundreds of warnings for naming
   conventions (`mPipelinedQuadRenderer`, `kTotalBufferSizeVerts`) that
   must match upstream JOGL for maintainability.

5. **Field name `mGlyphProducer` is preserved.** The characterization test
   accesses this field by name via reflection. Renaming it would break
   `constructor_glyphProducer_isInitialized`.

6. **Static constants remain in `NonCachingTextRenderer`.** The buffer size
   constants and debug flags are referenced from multiple classes. They stay
   in the parent class and are qualified (`NonCachingTextRenderer.kTotalBufferSizeVerts`)
   from the extracted classes.

7. **The `FIXME` in `TextRendererGlyphProducer.fontRenderContext` is preserved.** The
   field was never initialized in the original code. This bug is documented
   in the characterization reference and is not introduced or fixed by the
   extraction.

8. **No inner classes remain in `NonCachingTextRenderer`.** All 9 original
   inner classes have been extracted. The file now contains only field
   declarations, constructors, and method implementations.

9. **16 private fields widened to package-private in Phase 2.** All widened
   fields hold only GL render state (booleans, floats, color caches). No
   fields hold credentials, user data, or security-sensitive state. All
   classes remain in the same package, so no new public API surface is
   created.

10. **`final` is preserved on fields that were `final`.** The fields
    `stringLocations` and `mGlyphProducer` (now widened) retain their `final`
    modifier. No `final` modifiers are added or removed.

## Examples

### Instantiating Phase 1 extracted classes

Before extraction:

```java
mPipelinedQuadRenderer = new NonCachingTextRenderer.Pipelined_QuadRenderer();
mGlyphProducer = new NonCachingTextRenderer.GlyphProducer(numGlyphs);
```

After extraction:

```java
mPipelinedQuadRenderer = new TextRendererQuadRenderer(this);
mGlyphProducer = new TextRendererGlyphProducer(numGlyphs, this);
```

### Instantiating Phase 2 extracted classes

Before extraction:

```java
packer = new RectanglePacker(new NonCachingTextRenderer.Manager(), kSize, kSize);
renderDelegate = new NonCachingTextRenderer.DefaultRenderDelegate();
dbgCanvas.addGLEventListener(new NonCachingTextRenderer.DebugListener(gl, dbgFrame));
```

After extraction:

```java
packer = new RectanglePacker(new Manager(this), kSize, kSize);
renderDelegate = new DefaultRenderDelegate();
dbgCanvas.addGLEventListener(new DebugListener(gl, dbgFrame, this));
```

### Accessing outer fields from Manager

Before extraction (in `Manager.endMovement()`):

```java
if (inBeginEndPair) {
  if (isOrthoMode) {
    ((TextureRenderer) newBackingStore).beginOrthoRendering(
        beginRenderingWidth, beginRenderingHeight,
        beginRenderingDepthTestDisabled);
  }
  if (haveCachedColor) {
    if (cachedColor == null) {
      ((TextureRenderer) newBackingStore).setColor(cachedR, cachedG, cachedB, cachedA);
    }
  }
}
```

After extraction:

```java
if (textRenderer.inBeginEndPair) {
  if (textRenderer.isOrthoMode) {
    ((TextureRenderer) newBackingStore).beginOrthoRendering(
        textRenderer.beginRenderingWidth, textRenderer.beginRenderingHeight,
        textRenderer.beginRenderingDepthTestDisabled);
  }
  if (textRenderer.haveCachedColor) {
    if (textRenderer.cachedColor == null) {
      ((TextureRenderer) newBackingStore).setColor(
          textRenderer.cachedR, textRenderer.cachedG,
          textRenderer.cachedB, textRenderer.cachedA);
    }
  }
}
```

### TextData cast in clearUnusedEntries

Before extraction:

```java
final NonCachingTextRenderer.TextData data =
    (NonCachingTextRenderer.TextData) rect.getUserData();
```

After extraction:

```java
final TextData data = (TextData) rect.getUserData();
```

### CharSequenceIterator self-reference in clone()

Before extraction:

```java
final NonCachingTextRenderer.CharSequenceIterator iter =
    new NonCachingTextRenderer.CharSequenceIterator(mSequence);
```

After extraction:

```java
final CharSequenceIterator iter = new CharSequenceIterator(mSequence);
```

### CharacterCache self-reference in valueOf()

Before extraction:

```java
return NonCachingTextRenderer.CharacterCache.cache[c];
```

After extraction:

```java
return CharacterCache.cache[c];
```
