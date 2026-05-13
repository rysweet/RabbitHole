# Validate the Graphics2D Delegate Decomposition

This guide walks through verifying the Graphics2D extraction after
implementation or after merging changes to `core/glrender`.

## Prerequisites

- Java 17+ and Maven 3.8+ installed
- The `tweedle-lang` submodule initialized:
  ```bash
  git submodule update --init tweedle-lang
  ```

## Quick validation

Run the module test suite:

```bash
mvn -pl core/glrender -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

This compiles `core/glrender` and all upstream modules, then runs all tests
including `Graphics2DExtractionContractTest`.

## What the contract test verifies

`Graphics2DExtractionContractTest` is a JUnit test in
`core/glrender/src/test/java/edu/cmu/cs/dennisc/render/gl/imp/` that checks:

1. **Line count** — `Graphics2D.java` is under 500 lines.
2. **File inventory** — all five delegate files exist:
   - `ReferencedObject.java`
   - `GlPrimitiveShapeRenderer.java`
   - `GlTessellationRenderer.java`
   - `GlTextRenderer.java`
   - `GlImageRenderer.java`
3. **No public API leak** — delegate classes have no `public` modifier.
4. **Bug preservation** — `disposeForgottenImageGenerators()` still clears
   the text renderer's forgotten map (not the image map), matching the
   original line 1137 behavior.

## Manual verification checklist

If you want to verify beyond the automated tests:

### 1. Check line count

```bash
wc -l core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/Graphics2D.java
```

Expected: under 500 lines.

### 2. Check delegate files exist

```bash
ls -la core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/{ReferencedObject,GlPrimitiveShapeRenderer,GlTessellationRenderer,GlTextRenderer,GlImageRenderer}.java
```

All five files should be present.

### 3. Check no public delegates

```bash
grep -l '^public class' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/Gl{Primitive,Tessellation,Text,Image}*.java
```

Expected: no output (no files match). All delegates are package-private.

### 4. Check synchronized monitors preserved

```bash
grep -n 'synchronized' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/Gl{Text,Image}Renderer.java core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/Graphics2D.java
```

Verify:
- `GlTextRenderer` has `synchronized (forgottenFontToTextRendererMap)` (1 block)
- `GlImageRenderer` has `synchronized (forgottenImageGeneratorToPixelsMap)` (1 block)
- `Graphics2D` has `synchronized (s_matrix)` (1 block, transform code)
- Total: 3 `synchronized` blocks, same monitors as original

### 5. Check static field placement

```bash
grep -rn 's_sineCosineCache' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/
grep -rn 'FLATNESS\|LINE_STROKE' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/
```

- `s_sineCosineCache` should appear only in `GlPrimitiveShapeRenderer.java`
- `FLATNESS` and `LINE_STROKE` should appear only in `GlTessellationRenderer.java`

### 6. Verify dead code removal

```bash
grep -n 'DefaultImageGenerator' core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/Graphics2D.java
```

Expected: no output. The commented-out inner class has been removed.

## Troubleshooting

### Compilation failure in downstream module

If a module outside `core/glrender` fails to compile, check whether it
referenced `Graphics2D` inner classes directly. The extraction removes inner
classes — callers that imported them need updating.

Run a broader build to find these:

```bash
mvn -pl core/glrender -am -amd -DfailIfNoTests=false -Dcheckstyle.skip compile
```

### Test failure: "expected line count under 500"

If the contract test fails on line count, check for accidental additions to
`Graphics2D.java`. The file should contain only coordinator logic — drawing
methods belong in delegates.

### Missing submodule error

If Maven reports missing Tweedle parser classes:

```bash
git submodule status tweedle-lang
git submodule update --init tweedle-lang
```
