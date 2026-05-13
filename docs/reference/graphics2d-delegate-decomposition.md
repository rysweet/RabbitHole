# Graphics2D Delegate Decomposition

This reference describes the decomposition of `Graphics2D.java` (1225 lines)
into a thin coordinator plus four package-private delegate classes and one
generic utility class. The coordinator retains ~460 lines.

All classes live in `edu.cmu.cs.dennisc.render.gl.imp`. The delegates are
package-private with no public constructors. They are instantiated by
`Graphics2D` and receive a back-reference for shared state access.

Issue #565 decomposes the class without changing observable behavior. All
rendering, tessellation, text, image, and transform behavior is preserved
identically — including the known bug at line 1137 (cross-map clear).

## Contents

- [Motivation](#motivation)
- [Architecture](#architecture)
- [Class responsibilities](#class-responsibilities)
  - [Graphics2D (coordinator)](#graphics2d-coordinator)
  - [ReferencedObject\<E\>](#referencedobjecte)
  - [GlPrimitiveShapeRenderer](#glprimitiveshaperenderer)
  - [GlTessellationRenderer](#gltessellationrenderer)
  - [GlTextRenderer](#gltextrenderer)
  - [GlImageRenderer](#glimagerenderer)
- [Package-private collaboration](#package-private-collaboration)
- [State access pattern](#state-access-pattern)
- [Static field placement](#static-field-placement)
- [Synchronized block preservation](#synchronized-block-preservation)
- [Known bug preservation](#known-bug-preservation)
- [Dead code removal](#dead-code-removal)
- [Security boundary](#security-boundary)
- [Error handling contract](#error-handling-contract)
- [Configuration](#configuration)
- [Validation](#validation)
- [Acceptance criteria](#acceptance-criteria)
- [Claim boundaries](#claim-boundaries)

## Motivation

The original `Graphics2D.java` contained 1225 lines mixing five concerns:

1. **Primitive shape rendering** — `drawLine`, `drawRect`, `fillRect`,
   `drawOval`, `fillOval`, `drawRoundRect`, `fillRoundRect`, `drawPolygon`,
   `fillPolygon`, `drawPolyline`, `drawArc`, `fillArc`, `clearRect`
2. **Tessellation** — `draw(Shape)`, `fill(Shape)`, GLU tessellation callbacks,
   vertex list management
3. **Text rendering** — `drawString`, `drawChars`, `drawBytes`, font-to-renderer
   lifecycle, `FontRenderContext`, glyph metrics
4. **Image rendering** — `drawImage` overrides, image generator lifecycle,
   remembered/forgotten image maps
5. **Coordinator concerns** — constructor, dispose, transforms, state
   getters/setters, GL update, "not implemented" stubs

This mix made the class difficult to navigate, review, and extend. Each
delegate can now be understood and tested independently.

## Architecture

```text
Graphics2D (~460 lines, coordinator)
├── GlPrimitiveShapeRenderer (package-private)
│   └── Lines, rects, ovals, round-rects, polygons, polylines, arcs, clearRect
├── GlTessellationRenderer (package-private)
│   └── draw(Shape), fill(Shape), GLU tessellation callbacks, vertex lists
├── GlTextRenderer (package-private)
│   └── drawString, drawChars, drawBytes, font lifecycle, FontRenderContext
├── GlImageRenderer (package-private)
│   └── Image generator lifecycle, paint, remember/forget, disposal
└── ReferencedObject<T> (package-private utility)
    └── Reference-counted wrapper used by text and image maps
```

All five new files live alongside `Graphics2D.java` in
`core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/`.

## Class responsibilities

### Graphics2D (coordinator)

The coordinator retains:

| Concern | Approx lines | Methods |
| --- | --- | --- |
| License + imports | ~80 | — |
| Fields + constructor + `initialize()` + `dispose()` + `isValid()` | ~60 | lifecycle |
| "Not implemented" stubs | ~100 | `addRenderingHints`, `clip`, `getDeviceConfiguration`, etc. |
| State getters/setters | ~70 | `getColor`/`setColor`, `getPaint`/`setPaint`, `getFont`/`setFont`, `getStroke`/`setStroke`, `getBackground`/`setBackground`, `getRenderingHints`, `getFontMetrics(Font)` |
| Transform methods | ~70 | `translate`, `rotate`, `scale`, `shear`, `transform`, `get`/`setTransform`, `glUpdateTransform` |
| `drawGlyphVector` orchestration | ~8 | bridges transforms and tessellation |
| `drawImage` bridging methods | ~35 | 8 `drawImage` overrides (6 from `Graphics`, 2 from `Graphics2D`); only `drawImage(Image,int,int,ImageObserver)` calls `imageRenderer.isRemembered` / `remember` / `paint` / `forget`; the other 7 throw `RuntimeException("not implemented")` |
| `disposeForgottenImageGenerators` | ~8 | bug-preserving disposal (see [Known bug preservation](#known-bug-preservation)) |
| `disposeForgottenFonts` bridging | ~3 | delegates to `textRenderer.disposeForgottenFonts()` |
| Utilities | ~8 | `getGL()`, `getFontRenderContext()` |
| Package-private accessors | ~20 | for delegate access to coordinator state |
| Delegate declarations + init | ~10 | 4 delegate fields |

**Delegate fields:**

```java
/* package-private */ final GlPrimitiveShapeRenderer primitiveRenderer;
/* package-private */ final GlTessellationRenderer tessellationRenderer;
/* package-private */ final GlTextRenderer textRenderer;
/* package-private */ final GlImageRenderer imageRenderer;
```

**Constructor wiring (in `initialize()`):**

```java
this.primitiveRenderer = new GlPrimitiveShapeRenderer(this);
this.tessellationRenderer = new GlTessellationRenderer(this);
this.textRenderer = new GlTextRenderer(this);
this.imageRenderer = new GlImageRenderer(this);
```

### ReferencedObject\<E\>

Generic reference-counted wrapper. Tracks a value `E` and an integer reference
count. Used by `GlTextRenderer` (for `TextRendererHolder` instances) and
`GlImageRenderer` (for `Pixels` instances).

```java
class ReferencedObject<E> {
    private final E object;
    private int referenceCount;

    ReferencedObject(E object, int referenceCount) { ... }
    E getObject()              { ... }
    boolean isReferenced()     { return referenceCount > 0; }
    void addReference()        { referenceCount++; }
    void removeReference()     { referenceCount--; }
}
```

Currently a single `private static final` inner class inside `Graphics2D`
(line 944), parameterized with `TextRendererHolder` for text and `Pixels` for
images. Extracted to a top-level package-private class with identical
semantics.

### GlPrimitiveShapeRenderer

Renders geometric primitives directly to OpenGL via `GL.glBegin`/`GL.glEnd`
calls.

**Moved methods:**

| Method | Notes |
| --- | --- |
| `drawLine(int,int,int,int)` | GL_LINES |
| `drawRect(int,int,int,int)` | GL_LINE_LOOP |
| `fillRect(int,int,int,int)` | GL_POLYGON |
| `drawOval(int,int,int,int)` | sine/cosine cache, GL_LINE_LOOP |
| `fillOval(int,int,int,int)` | sine/cosine cache, GL_POLYGON |
| `drawRoundRect(int,int,int,int,int,int)` | GL_LINE_LOOP with arcs |
| `fillRoundRect(int,int,int,int,int,int)` | GL_POLYGON with arcs |
| `drawPolygon(int[],int[],int)` | GL_LINE_LOOP |
| `fillPolygon(int[],int[],int)` | GL_POLYGON |
| `drawPolyline(int[],int[],int)` | GL_LINE_STRIP |
| `drawArc(int,int,int,int,int,int)` | GL_LINE_STRIP |
| `fillArc(int,int,int,int,int,int)` | GL_POLYGON with center vertex |
| `clearRect(int,int,int,int)` | saves/restores paint via `g2d.glSetColor()` |

**Moved static fields:**

- `s_sineCosineCache` — `SineCosineCache` instance initialized at declaration, for oval/arc rendering

**Constructor:**

```java
GlPrimitiveShapeRenderer(Graphics2D g2d) {
    assert g2d != null;
    this.g2d = g2d;
}
```

### GlTessellationRenderer

Handles `draw(Shape)` and `fill(Shape)` using GLU tessellation. Manages the
tessellator lifecycle, callbacks (`begin`, `end`, `vertex`, `error`,
`combine`), and the vertex double-array pool.

**Moved methods:**

| Method | Notes |
| --- | --- |
| `draw(Shape)` | flattens path, tessellates outline |
| `fill(Shape)` | flattens path, tessellates interior |
| `tessellate(Shape, boolean)` | shared tessellation implementation |
| GLU callback inner class | `begin`, `end`, `vertex`, `error`, `combine` |

**Moved static fields:**

- `FLATNESS` — `double`, path flattening tolerance
- `LINE_STROKE` — `BasicStroke(0)`, stroke for outline tessellation

**Constructor:**

```java
GlTessellationRenderer(Graphics2D g2d) {
    assert g2d != null;
    this.g2d = g2d;
}
```

### GlTextRenderer

Manages the `Font` → `TextRendererHolder` lifecycle and renders text strings.
Owns the `activeFontToTextRendererMap` and `forgottenFontToTextRendererMap`.

**Moved methods:**

| Method | Notes |
| --- | --- |
| `drawString(String, float, float)` | acquires `TextRenderer` via holder, calls `glTextRenderer.draw()` |
| `drawChars(char[], int, int, int, int)` | delegates to `drawString` |
| `drawBytes(byte[], int, int, int, int)` | delegates to `drawString` |
| `isRemembered(Font)` | checks `activeFontToTextRendererMap` |
| `remember(Font)` | adds/promotes to active map with ref-count |
| `getBounds(String, Font)` | gets text bounds via `TextRendererHolder` |
| `forget(Font)` | decrements ref-count, moves to forgotten if zero |
| `disposeForgottenFonts()` | disposes forgotten `TextRendererHolder`s |
| `clearForgottenMap()` | package-private, used by bug-preserving code in coordinator |

**Moved inner class:**

- `TextRendererHolder` — wraps `TextRenderer` with GL-context-aware lifecycle.
  Handles lazy creation (`getTextRenderer(Font, GL)`), GL context changes, and
  safe disposal (`disposeTextRendererSafely()` catches `GLException`).

**Moved maps:**

- `activeFontToTextRendererMap` — `Map<Font, ReferencedObject<TextRendererHolder>>`
- `forgottenFontToTextRendererMap` — `Map<Font, ReferencedObject<TextRendererHolder>>`

**Constructor:**

```java
GlTextRenderer(Graphics2D g2d) {
    assert g2d != null;
    this.g2d = g2d;
}
```

### GlImageRenderer

Manages the `Image` → `ImageGenerator` → `Pixels` lifecycle and paints images
to OpenGL via `glDrawPixels`.

**Moved methods:**

| Method | Notes |
| --- | --- |
| `isRemembered(ImageGenerator)` | checks `activeImageGeneratorToPixelsMap` |
| `remember(ImageGenerator)` | creates `Pixels`, adds to active map with ref-count |
| `paint(ImageGenerator, float, float, float)` | GL pixel blit with alpha |
| `forget(ImageGenerator)` | decrements ref-count, moves to forgotten if zero |
| `disposeImageGenerator(ImageGenerator)` | releases `Pixels` (private) |
| `disposeForgottenImageGenerators()` | iterates forgotten map, disposes all |
| `isRemembered(Image)` | resolves via `imageToImageGeneratorMap` |
| `remember(Image)` | creates `BufferedImageTexture` if needed, delegates to `remember(ImageGenerator)` |
| `forget(Image)` | resolves and delegates to `forget(ImageGenerator)` |
| `disposeForgottenImages()` | selective disposal by image map cross-reference |

**Moved maps:**

- `imageToImageGeneratorMap` — `Map<Image, ImageGenerator>`
- `activeImageGeneratorToPixelsMap` — `Map<ImageGenerator, ReferencedObject<Pixels>>`
- `forgottenImageGeneratorToPixelsMap` — `Map<ImageGenerator, ReferencedObject<Pixels>>`

**Constructor:**

```java
GlImageRenderer(Graphics2D g2d) {
    assert g2d != null;
    this.g2d = g2d;
}
```

## Package-private collaboration

Graphics2D exposes the following package-private accessors for delegate use:

```java
/* package-private */ RenderContext getRenderContext()
/* package-private */ int getWidth()
/* package-private */ int getHeight()
/* package-private */ AffineTransform getAffineTransformRef()
/* package-private */ Font getFontField()
/* package-private */ Paint getPaintField()
/* package-private */ Color getBackgroundField()
/* package-private */ Stroke getStrokeField()
/* package-private */ void glSetColor(Color color)
/* package-private */ void glSetPaint(Paint paint)
```

These methods are named with the `Field` suffix to avoid collision with the
public `java.awt.Graphics2D` API (`getFont()` returns a copy; `getFontField()`
returns the internal reference).

No delegate references another delegate. Cross-delegate calls go through
Graphics2D. For example, `drawGlyphVector` in Graphics2D calls both
`translate()` (self) and `tessellationRenderer.fill(shape)`.

## State access pattern

Each delegate stores a `final Graphics2D g2d` back-reference set at
construction. Delegates access mutable state through accessor methods, never
by caching field values:

```java
// Correct — always reads current state
GL gl = g2d.getRenderContext().gl;
Color bg = g2d.getBackgroundField();

// Wrong — would miss mutations
// this.gl = g2d.getRenderContext().gl;  // stale after re-initialize
```

This pattern is required because `Graphics2D.initialize()` can be called
multiple times with different `RenderContext` instances during the object's
lifetime.

## Static field placement

| Field | Moved to | Rationale |
| --- | --- | --- |
| `s_sineCosineCache` | `GlPrimitiveShapeRenderer` | Only user |
| `FLATNESS` | `GlTessellationRenderer` | Only user |
| `LINE_STROKE` | `GlTessellationRenderer` | Only user |
| `DEFAULT_PAINT` | stays in `Graphics2D` | Used by state init |
| `DEFAULT_BACKGROUND` | stays in `Graphics2D` | Used by state init |
| `DEFAULT_FONT` | stays in `Graphics2D` | Used by state init |
| `DEFAULT_STROKE` | stays in `Graphics2D` | Used by state init |
| `s_matrix` | stays in `Graphics2D` | Used by transform methods |

## Synchronized block preservation

Every `synchronized` block in the original code retains its exact monitor
object after extraction:

| Original monitor | After extraction | Owner |
| --- | --- | --- |
| `synchronized (s_matrix)` | same static `double[]` | stays in `Graphics2D` (transform) |
| `synchronized (forgottenFontToTextRendererMap)` | same map instance | `GlTextRenderer` |
| `synchronized (forgottenImageGeneratorToPixelsMap)` | same map instance | `GlImageRenderer` |

The original code has exactly three `synchronized` blocks (lines 847, 1048,
1133). The `s_matrix` lock stays with the transform code in the coordinator.
The two map locks move to their respective delegates. No lock semantics change.

## Known bug preservation

**Line 1137 cross-map clear bug:**

The original `disposeForgottenImageGenerators()` method (line 1132) iterates
`forgottenImageGeneratorToPixelsMap` to dispose image generators, but then
clears `forgottenFontToTextRendererMap` (a text map) instead of
`forgottenImageGeneratorToPixelsMap` (the image map it just iterated). The
clear is also performed while holding the image map's monitor, not the text
map's monitor — a cross-lock access. This is preserved exactly:

```java
// In Graphics2D (coordinator) — preserving the original bug
public void disposeForgottenImageGenerators() {
    imageRenderer.disposeAllForgotten();       // iterates + disposes image map
    textRenderer.clearForgottenMap();          // BUG: clears text map, not image map
    // Note: forgottenImageGeneratorToPixelsMap is never cleared
}
```

A characterization test verifies this behavior. The bug will be fixed in a
separate issue.

## Dead code removal

The 34-line commented-out `DefaultImageGenerator` inner class (original lines
1141–1174) is removed. It was never instantiated and served no documentation
purpose. Line count verified: 34 lines of dead commented-out code.

Small commented-out method stubs (4–6 lines each, e.g., lines 490–493,
517–520) are preserved alongside their replacement methods as evolution
documentation.

## Security boundary

- All delegate classes are package-private (no `public` modifier).
- All delegate constructors are package-private.
- All accessor methods added to Graphics2D are package-private.
- No new public API surface is introduced.
- Constructor parameters are assert-checked for null.

## Error handling contract

All existing error handling is preserved verbatim:

- `RuntimeException` on tessellation error (GLU callback)
- `RuntimeException("TODO")` in `remember(ImageGenerator)` for non-Texture generators
- `RuntimeException("todo")` in `remember(Image)` for non-BufferedImage images
- `Logger.info()` call in `TextRendererHolder.disposeTextRendererSafely()` (catches `GLException`)
- `assert` statements in lifecycle methods (`getBounds`, `forget`, `paint`)

No new exceptions are introduced.

## Configuration

No configuration changes. The decomposition is purely internal. No new system
properties, environment variables, or resource files are introduced.

## Validation

```bash
mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

This command compiles the `core/glrender` module and all upstream dependencies,
runs all existing tests, and verifies that the extraction introduces no
compilation or test failures.

## Acceptance criteria

1. `Graphics2D.java` is under 500 lines.
2. Four delegate classes exist in the same package.
3. `ReferencedObject.java` extracts the generic inner class to a top-level file.
4. `mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip test`
   passes.
5. No public API changes — all new classes and methods are package-private.
6. All `synchronized` blocks retain their original monitor objects.
7. The line 1137 cross-map clear bug is preserved and tested.
8. The dead `DefaultImageGenerator` block is removed.
9. `Graphics2DExtractionContractTest` verifies delegate wiring and the
   bug-preservation contract.

## Claim boundaries

This decomposition claims only internal restructuring of `Graphics2D.java`.
It does **not** claim:

- Any change to rendering output or visual behavior
- Any performance improvement or regression
- Any fix for the line 1137 cross-map clear bug
- Any change to the `java.awt.Graphics2D` public API surface
- Any change to other classes in the `gl.imp` package
