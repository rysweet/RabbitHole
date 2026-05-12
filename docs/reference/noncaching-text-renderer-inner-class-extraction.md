# NonCachingTextRenderer Inner Class Extraction

This reference documents the extraction of 3 inner classes from
`NonCachingTextRenderer` (issue #514) into separate top-level
package-private files in `edu.cmu.cs.dennisc.render.joglrenderer`.

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

`NonCachingTextRenderer.java` was 1842 lines with 3 large non-static inner
classes (`Glyph`, `GlyphProducer`, `Pipelined_QuadRenderer`) totaling ~500
lines. These inner classes held implicit references to the enclosing
`NonCachingTextRenderer` instance and accessed its private fields and methods.

Extracting them reduces `NonCachingTextRenderer` to under 1350 lines, making
the file navigable and each class independently reviewable, while preserving
all runtime behavior.

## Extracted classes

| Original inner class | New top-level class | Lines extracted | Purpose |
| --- | --- | --- | --- |
| `Glyph` (L1201–1392) | `TextRendererGlyph` | ~192 | Represents a unicode glyph or character substring for rendering. Manages glyph texture upload, texture-mapped quad emission, and glyph vector lifecycle. |
| `GlyphProducer` (L1394–1555) | `TextRendererGlyphProducer` | ~162 | Manages the glyph cache: unicode→glyph-code mapping, glyph fabrication from `GlyphVector`, and cache eviction. Produces `TextRendererGlyph` lists for input strings. |
| `Pipelined_QuadRenderer` (L1577–1734) | `TextRendererQuadRenderer` | ~158 | Batched OpenGL quad pipeline. Buffers texture coordinates and vertex data, flushes via VBO or immediate-mode GL when the buffer is full. |

## File inventory

| File | Role | Approx lines |
| --- | --- | --- |
| `NonCachingTextRenderer.java` | Parent class, still owns remaining inner classes — static: `CharacterCache`, `CharSequenceIterator`, `TextData`, `DefaultRenderDelegate`; non-static: `Manager`, `DebugListener` — and all rendering entry points. | <1350 |
| `TextRendererGlyph.java` | Extracted `Glyph`. Package-private class. | ~200 |
| `TextRendererGlyphProducer.java` | Extracted `GlyphProducer`. Package-private class. | ~170 |
| `TextRendererQuadRenderer.java` | Extracted `Pipelined_QuadRenderer`. Package-private class. | ~165 |
| `NonCachingTextRendererCharacterizationTest.java` | Characterization test suite. Updated reflection targets for renamed classes. | ~480 |

All files reside in `core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/`.

## Enclosing instance pattern

Each inner class previously held an implicit `NonCachingTextRenderer.this`
reference. After extraction, each top-level class takes an explicit
`NonCachingTextRenderer textRenderer` parameter in its constructor:

```java
// TextRendererGlyph — unicode glyph constructor
TextRendererGlyph(int unicodeID, int glyphCode, float advance,
                  GlyphVector singleUnicodeGlyphVector,
                  TextRendererGlyphProducer producer,
                  NonCachingTextRenderer textRenderer) { ... }

// TextRendererGlyph — string fallback constructor
TextRendererGlyph(String str, boolean needAdvance,
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

Member accesses that previously used the implicit outer reference now go
through the explicit field:

| Before (inner class) | After (top-level class) |
| --- | --- |
| `getFontRenderContext()` | `textRenderer.getFontRenderContext()` |
| `getBackingStore()` | `textRenderer.getBackingStore()` |
| `getGraphics2D()` | `textRenderer.getGraphics2D()` |
| `draw3D_ROBUST(...)` | `textRenderer.draw3D_ROBUST(...)` |
| `getMyUseVertexArrays()` | `textRenderer.getMyUseVertexArrays()` |
| `is15Available(gl)` | `textRenderer.is15Available(gl)` |
| `font` | `textRenderer.font` |
| `packer` | `textRenderer.packer` |
| `renderDelegate` | `textRenderer.renderDelegate` |
| `singleUnicode` | `textRenderer.singleUnicode` |
| `mPipelinedQuadRenderer` | `textRenderer.mPipelinedQuadRenderer` |
| `useVertexArrays` | `textRenderer.useVertexArrays` |
| `isExtensionAvailable_GL_VERSION_1_5` | `textRenderer.isExtensionAvailable_GL_VERSION_1_5` |
| `DRAW_BBOXES` | `NonCachingTextRenderer.DRAW_BBOXES` |
| `DISABLE_GLYPH_CACHE` | `NonCachingTextRenderer.DISABLE_GLYPH_CACHE` |
| `preNormalize(rect)` | `NonCachingTextRenderer.preNormalize(rect)` |
| `normalize(rect)` | `textRenderer.normalize(rect)` |
| `kTotalBufferSizeCoordsVerts` | `NonCachingTextRenderer.kTotalBufferSizeCoordsVerts` |
| `kTotalBufferSizeCoordsTex` | `NonCachingTextRenderer.kTotalBufferSizeCoordsTex` |
| `kTotalBufferSizeBytesVerts` | `NonCachingTextRenderer.kTotalBufferSizeBytesVerts` |
| `kTotalBufferSizeBytesTex` | `NonCachingTextRenderer.kTotalBufferSizeBytesTex` |
| `kTotalBufferSizeVerts` | `NonCachingTextRenderer.kTotalBufferSizeVerts` |
| `kSizeInBytes_OneVertices_VertexData` | `NonCachingTextRenderer.kSizeInBytes_OneVertices_VertexData` |
| `kSizeInBytes_OneVertices_TexData` | `NonCachingTextRenderer.kSizeInBytes_OneVertices_TexData` |

## Visibility changes

14 members of `NonCachingTextRenderer` are widened from `private` to
package-private (default access) so the extracted classes can reach them.
This breaks down as 8 fields (6 instance, 2 static) and 6 methods
(5 instance, 1 static).

### Fields (8)

#### Instance fields (6)

| Field | Type | Accessed by |
| --- | --- | --- |
| `font` | `Font` | `TextRendererGlyph`, `TextRendererGlyphProducer` |
| `packer` | `RectanglePacker` | `TextRendererGlyph` |
| `renderDelegate` | `RenderDelegate` | `TextRendererGlyph` |
| `singleUnicode` | `char[]` | `TextRendererGlyph`, `TextRendererGlyphProducer` |
| `useVertexArrays` | `boolean` | `TextRendererQuadRenderer` |
| `isExtensionAvailable_GL_VERSION_1_5` | `boolean` | `TextRendererQuadRenderer` |

#### Static fields (2)

| Field | Type | Accessed by |
| --- | --- | --- |
| `DISABLE_GLYPH_CACHE` | `static boolean` | `TextRendererGlyphProducer` |
| `DRAW_BBOXES` | `static boolean` | `TextRendererGlyph` |

### Methods (6)

#### Instance methods (5)

| Method | Accessed by |
| --- | --- |
| `getBackingStore()` | `TextRendererGlyph`, `TextRendererQuadRenderer` |
| `getGraphics2D()` | `TextRendererGlyph` |
| `draw3D_ROBUST(String, float, float, float, float)` | `TextRendererGlyph` |
| `normalize(Rectangle2D)` | `TextRendererGlyph` |
| `is15Available(GL)` | `TextRendererQuadRenderer` |

#### Static methods (1)

| Method | Accessed by |
| --- | --- |
| `preNormalize(Rectangle2D)` | `TextRendererGlyph` |

### Members that do NOT need widening

| Member | Current visibility | Reason |
| --- | --- | --- |
| `getFontRenderContext()` | `public` | Already accessible from any class. |
| `getMyUseVertexArrays()` | `public` | Already accessible from any class. |
| `mPipelinedQuadRenderer` | package-private | Already has default access (no modifier). |
| `mGlyphProducer` | `private` | Only accessed by `NonCachingTextRenderer` itself, not by extracted classes. Type changes from `GlyphProducer` to `TextRendererGlyphProducer` but visibility stays `private`. |

All buffer-size constants (`kTotalBuffer*`, `kSizeInBytes*`, `kTotalBufferSizeVerts`,
`kSize`, `kQuadsPerBuffer`, `kVertsPerQuad`, `kCoordsPerVertVerts`,
`kCoordsPerVertTex`) were already package-private (no modifier) and require
no change.

### Pipelined_QuadRenderer.draw()

`draw()` was `private` in the inner class. It is now package-private in
`TextRendererQuadRenderer` because `NonCachingTextRenderer.flushGlyphPipeline()`
calls `mPipelinedQuadRenderer.draw()` to flush the pipeline at line 686.

## Type reference updates

All references within `NonCachingTextRenderer` to the old inner class names
are updated:

| Old reference | New reference |
| --- | --- |
| `NonCachingTextRenderer.Glyph` | `TextRendererGlyph` |
| `NonCachingTextRenderer.GlyphProducer` | `TextRendererGlyphProducer` |
| `NonCachingTextRenderer.Pipelined_QuadRenderer` | `TextRendererQuadRenderer` |
| `new NonCachingTextRenderer.Glyph(...)` | `new TextRendererGlyph(..., this)` |
| `new NonCachingTextRenderer.GlyphProducer(n)` | `new TextRendererGlyphProducer(n, this)` |
| `new NonCachingTextRenderer.Pipelined_QuadRenderer()` | `new TextRendererQuadRenderer(this)` |

References to remaining inner classes are unchanged:

- `NonCachingTextRenderer.CharacterCache`
- `NonCachingTextRenderer.CharSequenceIterator`
- `NonCachingTextRenderer.TextData`
- `NonCachingTextRenderer.DefaultRenderDelegate`
- `NonCachingTextRenderer.Manager`
- `NonCachingTextRenderer.DebugListener`

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

Expected: 49 tests, 42 pass, 7 skipped (constructor/accessor tests requiring
JOGL). 0 failures, 0 errors.

### Run the full module test suite

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

This verifies the extraction compiles cleanly with all module dependencies
and that no existing tests break. The `-Dcheckstyle.skip` flag is used
because the extracted files carry `@SuppressWarnings("CheckStyle")` to
preserve comparison compatibility with the upstream JOGL `TextRenderer`.

### Verify line count target

```bash
wc -l core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/NonCachingTextRenderer.java
```

Expected: under 1350 lines.

## Compatibility rules

1. **All new classes are package-private.** No public API surface changes.
   External code that depends on `NonCachingTextRenderer` sees no difference.

2. **Thread safety is unchanged.** The explicit `textRenderer` field is
   equivalent to the implicit `Outer.this` reference that non-static inner
   classes hold. No new synchronization is introduced or removed.

3. **`@SuppressWarnings("CheckStyle")` is applied to all extracted files.**
   These files are near-verbatim copies of JOGL `TextRenderer` internals.
   Checkstyle enforcement would generate hundreds of warnings for naming
   conventions (`mPipelinedQuadRenderer`, `kTotalBufferSizeVerts`) that
   must match upstream JOGL for maintainability.

4. **Field name `mGlyphProducer` is preserved.** The characterization test
   accesses this field by name via reflection. Renaming it would break
   `constructor_glyphProducer_isInitialized`.

5. **Static constants remain in `NonCachingTextRenderer`.** The buffer size
   constants and debug flags are referenced from multiple classes. They stay
   in the parent class and are qualified (`NonCachingTextRenderer.kTotalBufferSizeVerts`)
   from the extracted classes.

6. **The `FIXME` in `TextRendererGlyphProducer.fontRenderContext` is preserved.** The
   field was never initialized in the original code. This bug is documented
   in the characterization reference and is not introduced or fixed by the
   extraction.

7. **Remaining inner classes are not extracted.** `CharacterCache`,
   `CharSequenceIterator`, `TextData`, `DefaultRenderDelegate`, `Manager`,
   and `DebugListener` are either small, static, or both. Extracting them
   would add files without meaningful reduction in complexity.

## Examples

### Instantiating an extracted class (from within NonCachingTextRenderer)

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

### Accessing an outer field from an extracted class

Before extraction (in `Glyph.upload()`):

```java
final Rectangle2D origBBox = preNormalize(renderDelegate.getBounds(gv, getFontRenderContext()));
final Rectangle2D bbox = normalize(origBBox);
packer.add(rect);
```

After extraction (in `TextRendererGlyph.upload()`):

```java
final Rectangle2D origBBox = NonCachingTextRenderer.preNormalize(
    textRenderer.renderDelegate.getBounds(gv, textRenderer.getFontRenderContext()));
final Rectangle2D bbox = textRenderer.normalize(origBBox);
textRenderer.packer.add(rect);
```

### Circular dependency resolution

`TextRendererGlyph` and `TextRendererGlyphProducer` reference each other:
- `TextRendererGlyph.upload()` calls `producer.register(this)`
- `TextRendererGlyphProducer.getGlyph()` creates `new TextRendererGlyph(...)`

This circular dependency resolves naturally because both classes are top-level
in the same package. No forward declarations, interfaces, or dependency
inversion are needed.

### Cross-class field access from Glyph

`TextRendererGlyph.draw3D()` creates a `TextRendererQuadRenderer` if one
does not exist:

```java
if (textRenderer.mPipelinedQuadRenderer == null) {
  textRenderer.mPipelinedQuadRenderer = new TextRendererQuadRenderer(textRenderer);
}
```

This replaces the original:

```java
if (mPipelinedQuadRenderer == null) {
  mPipelinedQuadRenderer = new NonCachingTextRenderer.Pipelined_QuadRenderer();
}
```
