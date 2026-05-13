package edu.cmu.cs.dennisc.render.gl.imp;

import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.image.ImageObserver;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * TDD contract tests for issue #565: Extract Graphics2D drawing
 * methods into delegates.
 *
 * Written BEFORE implementation — all tests FAIL initially.
 * They pass once extraction is complete.
 *
 * Contract groups:
 *   1.  ReferencedObject — extracted from inner class to top-level
 *   2.  GlPrimitiveShapeRenderer — lines/rects/ovals/polygons delegate
 *   3.  GlTessellationRenderer — tessellation + draw/fill(Shape) delegate
 *   4.  GlTextRenderer — font lifecycle + drawString delegate
 *   5.  GlImageRenderer — image lifecycle + paint delegate
 *   6.  Inner classes removed from Graphics2D
 *   7.  Graphics2D fields widened from private → package-private
 *   8.  Graphics2D line count under 500
 *   9.  Static fields relocated correctly
 *  10.  Bug preservation (L1137 cross-map clear)
 *  11.  Graphics2D delegate field declarations
 *  12.  Coordinator methods remain in Graphics2D
 */
public class Graphics2DExtractionContractTest {

  private static final String PKG = "edu.cmu.cs.dennisc.render.gl.imp";

  private static Class<?> referencedObjectClass;
  private static Class<?> primitiveRendererClass;
  private static Class<?> tessellationRendererClass;
  private static Class<?> textRendererClass;
  private static Class<?> imageRendererClass;
  private static Class<?> graphics2DClass;

  @BeforeClass
  public static void resolveClasses() {
    referencedObjectClass = tryLoad(PKG + ".ReferencedObject");
    primitiveRendererClass = tryLoad(PKG + ".GlPrimitiveShapeRenderer");
    tessellationRendererClass = tryLoad(PKG + ".GlTessellationRenderer");
    textRendererClass = tryLoad(PKG + ".GlTextRenderer");
    imageRendererClass = tryLoad(PKG + ".GlImageRenderer");
    graphics2DClass = tryLoad(PKG + ".Graphics2D");
  }

  private static Class<?> tryLoad(String fqcn) {
    try {
      return Class.forName(fqcn);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static boolean isPackagePrivate(int mods) {
    return !Modifier.isPublic(mods)
        && !Modifier.isProtected(mods)
        && !Modifier.isPrivate(mods);
  }

  // ══════════════════════════════════════════════════════════════════
  // 1. ReferencedObject — extracted from inner class to top-level
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void referencedObject_classExists() {
    assertNotNull("ReferencedObject must exist as a top-level class",
        referencedObjectClass);
  }

  @Test
  public void referencedObject_isPackagePrivate() {
    assertNotNull("class must exist", referencedObjectClass);
    assertTrue("must be package-private",
        isPackagePrivate(referencedObjectClass.getModifiers()));
  }

  @Test
  public void referencedObject_isGeneric() {
    assertNotNull("class must exist", referencedObjectClass);
    assertEquals("must have exactly one type parameter",
        1, referencedObjectClass.getTypeParameters().length);
  }

  @Test
  public void referencedObject_hasConstructor() {
    assertNotNull("class must exist", referencedObjectClass);
    assertConstructorExists(referencedObjectClass,
        "constructor(Object, int)", Object.class, int.class);
  }

  @Test
  public void referencedObject_hasGetObject() {
    assertNotNull("class must exist", referencedObjectClass);
    assertMethodExists(referencedObjectClass, "getObject", Object.class);
  }

  @Test
  public void referencedObject_hasIsReferenced() {
    assertNotNull("class must exist", referencedObjectClass);
    assertMethodExists(referencedObjectClass, "isReferenced", boolean.class);
  }

  @Test
  public void referencedObject_hasAddReference() {
    assertNotNull("class must exist", referencedObjectClass);
    assertMethodExists(referencedObjectClass, "addReference", void.class);
  }

  @Test
  public void referencedObject_hasRemoveReference() {
    assertNotNull("class must exist", referencedObjectClass);
    assertMethodExists(referencedObjectClass, "removeReference", void.class);
  }

  @Test
  public void referencedObject_sourceFileExists() {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/"
            + "ReferencedObject.java");
    assertNotNull("ReferencedObject.java source file must exist", sourceFile);
  }

  // ══════════════════════════════════════════════════════════════════
  // 2. GlPrimitiveShapeRenderer — lines/rects/ovals/polygons
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void primitiveRenderer_classExists() {
    assertNotNull("GlPrimitiveShapeRenderer must exist as a top-level class",
        primitiveRendererClass);
  }

  @Test
  public void primitiveRenderer_isPackagePrivate() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertTrue("must be package-private",
        isPackagePrivate(primitiveRendererClass.getModifiers()));
  }

  @Test
  public void primitiveRenderer_hasGraphics2DField() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertFieldExists(primitiveRendererClass, "graphics2D", graphics2DClass);
  }

  @Test
  public void primitiveRenderer_hasConstructorWithBackReference() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertConstructorExists(primitiveRendererClass,
        "constructor(Graphics2D)", graphics2DClass);
  }

  @Test
  public void primitiveRenderer_hasDrawLine() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "drawLine",
        void.class, int.class, int.class, int.class, int.class);
  }

  @Test
  public void primitiveRenderer_hasFillRect() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "fillRect",
        void.class, int.class, int.class, int.class, int.class);
  }

  @Test
  public void primitiveRenderer_hasClearRect() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "clearRect",
        void.class, int.class, int.class, int.class, int.class);
  }

  @Test
  public void primitiveRenderer_hasDrawOval() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "drawOval",
        void.class, int.class, int.class, int.class, int.class);
  }

  @Test
  public void primitiveRenderer_hasFillOval() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "fillOval",
        void.class, int.class, int.class, int.class, int.class);
  }

  @Test
  public void primitiveRenderer_hasDrawRoundRect() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "drawRoundRect",
        void.class, int.class, int.class, int.class, int.class,
        int.class, int.class);
  }

  @Test
  public void primitiveRenderer_hasFillRoundRect() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "fillRoundRect",
        void.class, int.class, int.class, int.class, int.class,
        int.class, int.class);
  }

  @Test
  public void primitiveRenderer_hasDrawPolyline() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "drawPolyline",
        void.class, int[].class, int[].class, int.class);
  }

  @Test
  public void primitiveRenderer_hasDrawPolygon() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "drawPolygon",
        void.class, int[].class, int[].class, int.class);
  }

  @Test
  public void primitiveRenderer_hasFillPolygon() {
    assertNotNull("class must exist", primitiveRendererClass);
    assertMethodWithParamsExists(primitiveRendererClass, "fillPolygon",
        void.class, int[].class, int[].class, int.class);
  }

  @Test
  public void primitiveRenderer_hasSineCosineCache() {
    assertNotNull("class must exist", primitiveRendererClass);
    try {
      Field f = primitiveRendererClass.getDeclaredField("s_sineCosineCache");
      assertTrue("s_sineCosineCache must be static",
          Modifier.isStatic(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("s_sineCosineCache must exist in GlPrimitiveShapeRenderer");
    }
  }

  @Test
  public void primitiveRenderer_sourceFileExists() {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/"
            + "GlPrimitiveShapeRenderer.java");
    assertNotNull("GlPrimitiveShapeRenderer.java source file must exist",
        sourceFile);
  }

  // ══════════════════════════════════════════════════════════════════
  // 3. GlTessellationRenderer — tessellation + draw/fill(Shape)
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void tessellationRenderer_classExists() {
    assertNotNull("GlTessellationRenderer must exist as a top-level class",
        tessellationRendererClass);
  }

  @Test
  public void tessellationRenderer_isPackagePrivate() {
    assertNotNull("class must exist", tessellationRendererClass);
    assertTrue("must be package-private",
        isPackagePrivate(tessellationRendererClass.getModifiers()));
  }

  @Test
  public void tessellationRenderer_hasGraphics2DField() {
    assertNotNull("class must exist", tessellationRendererClass);
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertFieldExists(tessellationRendererClass, "graphics2D", graphics2DClass);
  }

  @Test
  public void tessellationRenderer_hasConstructorWithBackReference() {
    assertNotNull("class must exist", tessellationRendererClass);
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertConstructorExists(tessellationRendererClass,
        "constructor(Graphics2D)", graphics2DClass);
  }

  @Test
  public void tessellationRenderer_hasDrawShape() {
    assertNotNull("class must exist", tessellationRendererClass);
    assertMethodWithParamsExists(tessellationRendererClass, "draw",
        void.class, Shape.class);
  }

  @Test
  public void tessellationRenderer_hasFillShape() {
    assertNotNull("class must exist", tessellationRendererClass);
    assertMethodWithParamsExists(tessellationRendererClass, "fill",
        void.class, Shape.class);
  }

  @Test
  public void tessellationRenderer_hasFillPathIterator() {
    assertNotNull("class must exist", tessellationRendererClass);
    assertMethodWithParamsExists(tessellationRendererClass, "fill",
        void.class, PathIterator.class);
  }

  @Test
  public void tessellationRenderer_hasFLATNESS() {
    assertNotNull("class must exist", tessellationRendererClass);
    try {
      Field f = tessellationRendererClass.getDeclaredField("FLATNESS");
      assertTrue("FLATNESS must be static",
          Modifier.isStatic(f.getModifiers()));
      assertTrue("FLATNESS must be final",
          Modifier.isFinal(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("FLATNESS must exist in GlTessellationRenderer");
    }
  }

  @Test
  public void tessellationRenderer_hasLINE_STROKE() {
    assertNotNull("class must exist", tessellationRendererClass);
    try {
      Field f = tessellationRendererClass.getDeclaredField("LINE_STROKE");
      assertTrue("LINE_STROKE must be static",
          Modifier.isStatic(f.getModifiers()));
      assertTrue("LINE_STROKE must be final",
          Modifier.isFinal(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("LINE_STROKE must exist in GlTessellationRenderer");
    }
  }

  @Test
  public void tessellationRenderer_sourceFileExists() {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/"
            + "GlTessellationRenderer.java");
    assertNotNull("GlTessellationRenderer.java source file must exist",
        sourceFile);
  }

  // ══════════════════════════════════════════════════════════════════
  // 4. GlTextRenderer — font lifecycle + drawString
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void textRenderer_classExists() {
    assertNotNull("GlTextRenderer must exist as a top-level class",
        textRendererClass);
  }

  @Test
  public void textRenderer_isPackagePrivate() {
    assertNotNull("class must exist", textRendererClass);
    assertTrue("must be package-private",
        isPackagePrivate(textRendererClass.getModifiers()));
  }

  @Test
  public void textRenderer_hasGraphics2DField() {
    assertNotNull("class must exist", textRendererClass);
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertFieldExists(textRendererClass, "graphics2D", graphics2DClass);
  }

  @Test
  public void textRenderer_hasConstructorWithBackReference() {
    assertNotNull("class must exist", textRendererClass);
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertConstructorExists(textRendererClass,
        "constructor(Graphics2D)", graphics2DClass);
  }

  @Test
  public void textRenderer_hasIsRememberedFont() {
    assertNotNull("class must exist", textRendererClass);
    assertMethodWithParamsExists(textRendererClass, "isRemembered",
        boolean.class, Font.class);
  }

  @Test
  public void textRenderer_hasRememberFont() {
    assertNotNull("class must exist", textRendererClass);
    assertMethodWithParamsExists(textRendererClass, "remember",
        void.class, Font.class);
  }

  @Test
  public void textRenderer_hasForgetFont() {
    assertNotNull("class must exist", textRendererClass);
    assertMethodWithParamsExists(textRendererClass, "forget",
        void.class, Font.class);
  }

  @Test
  public void textRenderer_hasDisposeForgottenFonts() {
    assertNotNull("class must exist", textRendererClass);
    assertMethodExists(textRendererClass, "disposeForgottenFonts", void.class);
  }

  @Test
  public void textRenderer_hasGetBounds() {
    assertNotNull("class must exist", textRendererClass);
    assertMethodWithParamsExists(textRendererClass, "getBounds",
        java.awt.geom.Rectangle2D.class, String.class, Font.class);
  }

  @Test
  public void textRenderer_hasDrawString() {
    assertNotNull("class must exist", textRendererClass);
    assertMethodWithParamsExists(textRendererClass, "drawString",
        void.class, String.class, float.class, float.class);
  }

  @Test
  public void textRenderer_hasActiveFontMap() {
    assertNotNull("class must exist", textRendererClass);
    try {
      Field f = textRendererClass.getDeclaredField("activeFontToTextRendererMap");
      assertNotNull("activeFontToTextRendererMap must exist", f);
    } catch (NoSuchFieldException e) {
      fail("activeFontToTextRendererMap must exist in GlTextRenderer");
    }
  }

  @Test
  public void textRenderer_hasForgottenFontMap() {
    assertNotNull("class must exist", textRendererClass);
    try {
      Field f = textRendererClass.getDeclaredField("forgottenFontToTextRendererMap");
      assertNotNull("forgottenFontToTextRendererMap must exist", f);
    } catch (NoSuchFieldException e) {
      fail("forgottenFontToTextRendererMap must exist in GlTextRenderer");
    }
  }

  @Test
  public void textRenderer_sourceFileExists() {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/"
            + "GlTextRenderer.java");
    assertNotNull("GlTextRenderer.java source file must exist", sourceFile);
  }

  // ══════════════════════════════════════════════════════════════════
  // 5. GlImageRenderer — image lifecycle + paint
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void imageRenderer_classExists() {
    assertNotNull("GlImageRenderer must exist as a top-level class",
        imageRendererClass);
  }

  @Test
  public void imageRenderer_isPackagePrivate() {
    assertNotNull("class must exist", imageRendererClass);
    assertTrue("must be package-private",
        isPackagePrivate(imageRendererClass.getModifiers()));
  }

  @Test
  public void imageRenderer_hasGraphics2DField() {
    assertNotNull("class must exist", imageRendererClass);
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertFieldExists(imageRendererClass, "graphics2D", graphics2DClass);
  }

  @Test
  public void imageRenderer_hasConstructorWithBackReference() {
    assertNotNull("class must exist", imageRendererClass);
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertConstructorExists(imageRendererClass,
        "constructor(Graphics2D)", graphics2DClass);
  }

  @Test
  public void imageRenderer_hasIsRememberedImageGenerator() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodWithParamsExists(imageRendererClass, "isRemembered",
        boolean.class,
        edu.cmu.cs.dennisc.image.ImageGenerator.class);
  }

  @Test
  public void imageRenderer_hasRememberImageGenerator() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodWithParamsExists(imageRendererClass, "remember",
        void.class,
        edu.cmu.cs.dennisc.image.ImageGenerator.class);
  }

  @Test
  public void imageRenderer_hasForgetImageGenerator() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodWithParamsExists(imageRendererClass, "forget",
        void.class,
        edu.cmu.cs.dennisc.image.ImageGenerator.class);
  }

  @Test
  public void imageRenderer_hasPaint() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodWithParamsExists(imageRendererClass, "paint",
        void.class,
        edu.cmu.cs.dennisc.image.ImageGenerator.class,
        float.class, float.class, float.class);
  }

  @Test
  public void imageRenderer_hasDisposeForgottenImageGenerators() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodExists(imageRendererClass,
        "disposeForgottenImageGenerators", void.class);
  }

  @Test
  public void imageRenderer_hasIsRememberedImage() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodWithParamsExists(imageRendererClass, "isRemembered",
        boolean.class, java.awt.Image.class);
  }

  @Test
  public void imageRenderer_hasRememberImage() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodWithParamsExists(imageRendererClass, "remember",
        void.class, java.awt.Image.class);
  }

  @Test
  public void imageRenderer_hasForgetImage() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodWithParamsExists(imageRendererClass, "forget",
        void.class, java.awt.Image.class);
  }

  @Test
  public void imageRenderer_hasDisposeForgottenImages() {
    assertNotNull("class must exist", imageRendererClass);
    assertMethodExists(imageRendererClass,
        "disposeForgottenImages", void.class);
  }

  @Test
  public void imageRenderer_hasImageToImageGeneratorMap() {
    assertNotNull("class must exist", imageRendererClass);
    try {
      Field f = imageRendererClass.getDeclaredField("imageToImageGeneratorMap");
      assertNotNull("imageToImageGeneratorMap must exist", f);
    } catch (NoSuchFieldException e) {
      fail("imageToImageGeneratorMap must exist in GlImageRenderer");
    }
  }

  @Test
  public void imageRenderer_hasActiveImageGeneratorMap() {
    assertNotNull("class must exist", imageRendererClass);
    try {
      Field f = imageRendererClass.getDeclaredField("activeImageGeneratorToPixelsMap");
      assertNotNull("activeImageGeneratorToPixelsMap must exist", f);
    } catch (NoSuchFieldException e) {
      fail("activeImageGeneratorToPixelsMap must exist in GlImageRenderer");
    }
  }

  @Test
  public void imageRenderer_hasForgottenImageGeneratorMap() {
    assertNotNull("class must exist", imageRendererClass);
    try {
      Field f = imageRendererClass.getDeclaredField("forgottenImageGeneratorToPixelsMap");
      assertNotNull("forgottenImageGeneratorToPixelsMap must exist", f);
    } catch (NoSuchFieldException e) {
      fail("forgottenImageGeneratorToPixelsMap must exist in GlImageRenderer");
    }
  }

  @Test
  public void imageRenderer_sourceFileExists() {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/"
            + "GlImageRenderer.java");
    assertNotNull("GlImageRenderer.java source file must exist", sourceFile);
  }

  // ══════════════════════════════════════════════════════════════════
  // 6. Inner classes removed from Graphics2D
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void innerClass_ReferencedObject_removed() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertInnerClassAbsent("ReferencedObject");
  }

  @Test
  public void innerClass_TextRendererHolder_removed() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertInnerClassAbsent("TextRendererHolder");
  }

  // ══════════════════════════════════════════════════════════════════
  // 7. Graphics2D fields/methods widened to package-private
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void graphics2D_renderContext_isPackagePrivate() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertFieldWidened("renderContext");
  }

  @Test
  public void graphics2D_hasGetWidthAccessor() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertPackagePrivateMethod("getWidth");
  }

  @Test
  public void graphics2D_hasGetHeightAccessor() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertPackagePrivateMethod("getHeight");
  }

  @Test
  public void graphics2D_hasGetAffineTransformRef() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertPackagePrivateMethod("getAffineTransformRef");
  }

  @Test
  public void graphics2D_hasGetFontField() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertPackagePrivateMethod("getFontField");
  }

  @Test
  public void graphics2D_hasGetPaintField() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertPackagePrivateMethod("getPaintField");
  }

  @Test
  public void graphics2D_hasGetBackgroundField() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertPackagePrivateMethod("getBackgroundField");
  }

  @Test
  public void graphics2D_hasGetStrokeField() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertPackagePrivateMethod("getStrokeField");
  }

  @Test
  public void graphics2D_glSetColor_isPackagePrivate() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Method m = graphics2DClass.getDeclaredMethod("glSetColor", Color.class);
      assertTrue("glSetColor must be package-private",
          isPackagePrivate(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("glSetColor(Color) must exist in Graphics2D");
    }
  }

  @Test
  public void graphics2D_glSetPaint_isPackagePrivate() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Method m = graphics2DClass.getDeclaredMethod("glSetPaint", Paint.class);
      assertTrue("glSetPaint must be package-private",
          isPackagePrivate(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("glSetPaint(Paint) must exist in Graphics2D");
    }
  }

  // ══════════════════════════════════════════════════════════════════
  // 8. Graphics2D line count under 500
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void graphics2D_lineCount_under500() throws Exception {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/"
            + "Graphics2D.java");
    assertNotNull("Must find Graphics2D.java", sourceFile);
    long lineCount = Files.lines(sourceFile).count();
    assertTrue(
        "Graphics2D.java must be under 500 lines (actual: "
            + lineCount + ")",
        lineCount < 500);
  }

  // ══════════════════════════════════════════════════════════════════
  // 9. Static fields relocated correctly
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void graphics2D_noSineCosineCache() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertFieldAbsent(graphics2DClass, "s_sineCosineCache",
        "s_sineCosineCache should move to GlPrimitiveShapeRenderer");
  }

  @Test
  public void graphics2D_noFLATNESS() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertFieldAbsent(graphics2DClass, "FLATNESS",
        "FLATNESS should move to GlTessellationRenderer");
  }

  @Test
  public void graphics2D_noLINE_STROKE() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertFieldAbsent(graphics2DClass, "LINE_STROKE",
        "LINE_STROKE should move to GlTessellationRenderer");
  }

  @Test
  public void graphics2D_retainsDEFAULT_PAINT() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Field f = graphics2DClass.getDeclaredField("DEFAULT_PAINT");
      assertTrue("DEFAULT_PAINT must be static",
          Modifier.isStatic(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("DEFAULT_PAINT must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsDEFAULT_BACKGROUND() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Field f = graphics2DClass.getDeclaredField("DEFAULT_BACKGROUND");
      assertTrue("DEFAULT_BACKGROUND must be static",
          Modifier.isStatic(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("DEFAULT_BACKGROUND must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsDEFAULT_FONT() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Field f = graphics2DClass.getDeclaredField("DEFAULT_FONT");
      assertTrue("DEFAULT_FONT must be static",
          Modifier.isStatic(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("DEFAULT_FONT must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsDEFAULT_STROKE() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Field f = graphics2DClass.getDeclaredField("DEFAULT_STROKE");
      assertTrue("DEFAULT_STROKE must be static",
          Modifier.isStatic(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("DEFAULT_STROKE must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsS_matrix() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Field f = graphics2DClass.getDeclaredField("s_matrix");
      // Converted from static to instance field to eliminate synchronized contention on hot path
      assertFalse("s_matrix should be instance field (perf optimization)",
          Modifier.isStatic(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("s_matrix must remain in Graphics2D");
    }
  }

  // ══════════════════════════════════════════════════════════════════
  // 10. Bug preservation (L1137 cross-map clear)
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void graphics2D_disposeForgottenImageGenerators_remains() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Method m = graphics2DClass.getDeclaredMethod("disposeForgottenImageGenerators");
      assertNotNull("disposeForgottenImageGenerators must remain in Graphics2D "
          + "to preserve cross-map bug", m);
    } catch (NoSuchMethodException e) {
      fail("disposeForgottenImageGenerators must remain in Graphics2D "
          + "to preserve the L1137 cross-map clear bug");
    }
  }

  // ══════════════════════════════════════════════════════════════════
  // 11. Graphics2D delegate field declarations
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void graphics2D_hasPrimitiveRendererField() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertNotNull("GlPrimitiveShapeRenderer must exist", primitiveRendererClass);
    assertFieldExists(graphics2DClass, "primitiveRenderer",
        primitiveRendererClass);
  }

  @Test
  public void graphics2D_hasTessellationRendererField() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertNotNull("GlTessellationRenderer must exist",
        tessellationRendererClass);
    assertFieldExists(graphics2DClass, "tessellationRenderer",
        tessellationRendererClass);
  }

  @Test
  public void graphics2D_hasTextRendererField() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertNotNull("GlTextRenderer must exist", textRendererClass);
    assertFieldExists(graphics2DClass, "textRenderer", textRendererClass);
  }

  @Test
  public void graphics2D_hasImageRendererField() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    assertNotNull("GlImageRenderer must exist", imageRendererClass);
    assertFieldExists(graphics2DClass, "imageRenderer", imageRendererClass);
  }

  // ══════════════════════════════════════════════════════════════════
  // 12. Coordinator methods remain in Graphics2D
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void graphics2D_retainsDrawGlyphVector() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Method m = graphics2DClass.getDeclaredMethod("drawGlyphVector",
          java.awt.font.GlyphVector.class, float.class, float.class);
      assertNotNull("drawGlyphVector must remain in Graphics2D "
          + "(bridges translate and fill)", m);
    } catch (NoSuchMethodException e) {
      fail("drawGlyphVector must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsDrawImageBridging() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      Method m = graphics2DClass.getDeclaredMethod("drawImage",
          java.awt.Image.class, int.class, int.class,
          ImageObserver.class);
      assertNotNull("drawImage(Image,int,int,ImageObserver) must remain "
          + "in Graphics2D as bridging method", m);
    } catch (NoSuchMethodException e) {
      fail("drawImage bridging method must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsTranslateMethods() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("translate", int.class, int.class);
      graphics2DClass.getDeclaredMethod("translate",
          double.class, double.class);
    } catch (NoSuchMethodException e) {
      fail("translate methods must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsGetSetTransform() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("getTransform");
      graphics2DClass.getDeclaredMethod("setTransform",
          AffineTransform.class);
    } catch (NoSuchMethodException e) {
      fail("get/setTransform must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsInitialize() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("initialize",
          java.awt.Dimension.class);
    } catch (NoSuchMethodException e) {
      fail("initialize must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsDispose() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("dispose");
    } catch (NoSuchMethodException e) {
      fail("dispose must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsIsValid() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("isValid");
    } catch (NoSuchMethodException e) {
      fail("isValid must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsGetGL() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("getGL");
    } catch (NoSuchMethodException e) {
      fail("getGL must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsGetSetPaint() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("getPaint");
      graphics2DClass.getDeclaredMethod("setPaint", Paint.class);
    } catch (NoSuchMethodException e) {
      fail("get/setPaint must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsGetSetFont() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("getFont");
      graphics2DClass.getDeclaredMethod("setFont", Font.class);
    } catch (NoSuchMethodException e) {
      fail("get/setFont must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsGetSetStroke() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("getStroke");
      graphics2DClass.getDeclaredMethod("setStroke", Stroke.class);
    } catch (NoSuchMethodException e) {
      fail("get/setStroke must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsGetSetBackground() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("getBackground");
      graphics2DClass.getDeclaredMethod("setBackground", Color.class);
    } catch (NoSuchMethodException e) {
      fail("get/setBackground must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsFontRenderContext() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("getFontRenderContext");
    } catch (NoSuchMethodException e) {
      fail("getFontRenderContext must remain in Graphics2D");
    }
  }

  @Test
  public void graphics2D_retainsFontMetrics() {
    assertNotNull("Graphics2D must exist", graphics2DClass);
    try {
      graphics2DClass.getDeclaredMethod("getFontMetrics", Font.class);
    } catch (NoSuchMethodException e) {
      fail("getFontMetrics must remain in Graphics2D");
    }
  }

  // ══════════════════════════════════════════════════════════════════
  // Dead code removal
  // ══════════════════════════════════════════════════════════════════

  @Test
  public void graphics2D_deadDefaultImageGenerator_removed() throws Exception {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/gl/imp/"
            + "Graphics2D.java");
    assertNotNull("Must find Graphics2D.java", sourceFile);
    String content = new String(Files.readAllBytes(sourceFile));
    assertFalse(
        "Commented-out DefaultImageGenerator class (34 lines of dead code) "
            + "should be removed",
        content.contains("class DefaultImageGenerator"));
  }

  // ══════════════════════════════════════════════════════════════════
  // Helpers
  // ══════════════════════════════════════════════════════════════════

  private void assertConstructorExists(Class<?> clazz, String desc,
      Class<?>... params) {
    try {
      clazz.getDeclaredConstructor(params);
    } catch (NoSuchMethodException e) {
      fail(clazz.getSimpleName() + " must have " + desc);
    }
  }

  private void assertMethodExists(Class<?> clazz, String name,
      Class<?> returnType) {
    try {
      Method m = clazz.getDeclaredMethod(name);
      assertEquals(name + " return type", returnType, m.getReturnType());
    } catch (NoSuchMethodException e) {
      fail(clazz.getSimpleName() + " must have " + name + "()");
    }
  }

  private void assertMethodWithParamsExists(Class<?> clazz, String name,
      Class<?> returnType, Class<?>... params) {
    try {
      Method m = clazz.getDeclaredMethod(name, params);
      assertEquals(name + " return type", returnType, m.getReturnType());
    } catch (NoSuchMethodException e) {
      fail(clazz.getSimpleName() + " must have " + name + "(...)");
    }
  }

  private void assertFieldExists(Class<?> clazz, String fieldName,
      Class<?> expectedType) {
    try {
      Field f = clazz.getDeclaredField(fieldName);
      assertEquals(fieldName + " type", expectedType, f.getType());
    } catch (NoSuchFieldException e) {
      fail(clazz.getSimpleName() + " must have field '" + fieldName + "'");
    }
  }

  private void assertFieldAbsent(Class<?> clazz, String fieldName,
      String reason) {
    try {
      clazz.getDeclaredField(fieldName);
      fail("Field '" + fieldName + "' should not exist in "
          + clazz.getSimpleName() + ": " + reason);
    } catch (NoSuchFieldException e) {
      // expected
    }
  }

  private void assertInnerClassAbsent(String simpleName) {
    for (Class<?> inner : graphics2DClass.getDeclaredClasses()) {
      if (simpleName.equals(inner.getSimpleName())) {
        fail("Inner class '" + simpleName
            + "' must be extracted to a top-level class");
      }
    }
  }

  private void assertFieldWidened(String fieldName) {
    try {
      Field f = graphics2DClass.getDeclaredField(fieldName);
      assertTrue("'" + fieldName + "' must be package-private (not private)",
          isPackagePrivate(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("Field '" + fieldName + "' must exist in Graphics2D");
    }
  }

  private void assertPackagePrivateMethod(String methodName) {
    for (Method m : graphics2DClass.getDeclaredMethods()) {
      if (m.getName().equals(methodName)) {
        assertTrue("'" + methodName + "' must be package-private",
            isPackagePrivate(m.getModifiers()));
        return;
      }
    }
    fail("Method '" + methodName + "' must exist in Graphics2D");
  }

  private Path findSourceFile(String relativePath) {
    Path cwd = Paths.get(System.getProperty("user.dir"));
    for (Path p = cwd; p != null; p = p.getParent()) {
      Path candidate = p.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      if (p.equals(p.getRoot())) {
        break;
      }
    }
    return null;
  }
}
