package edu.cmu.cs.dennisc.render.joglrenderer;

import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.font.GlyphVector;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

/**
 * TDD contract tests for issue #514: Extract 3 largest inner classes
 * from NonCachingTextRenderer into separate top-level files.
 *
 * Written BEFORE implementation — all tests FAIL initially.
 * They pass once extraction is complete.
 *
 * Contract groups:
 *   1. TextRendererGlyph – extracted from Glyph inner class
 *   2. TextRendererGlyphProducer – extracted from GlyphProducer inner class
 *   3. TextRendererQuadRenderer – extracted from Pipelined_QuadRenderer inner class
 *   4. Inner classes removed from NonCachingTextRenderer
 *   5. Remaining inner classes preserved
 *   6. NonCachingTextRenderer members widened from private → package-private
 *   7. Field type changes for mGlyphProducer / mPipelinedQuadRenderer
 *   8. NonCachingTextRenderer line count under 1350
 */
public class InnerClassExtractionContractTest {

  private static final String PKG = "edu.cmu.cs.dennisc.render.joglrenderer";

  private static Class<?> glyphClass;
  private static Class<?> glyphProducerClass;
  private static Class<?> quadRendererClass;

  @BeforeClass
  public static void resolveExtractedClasses() {
    glyphClass = tryLoad(PKG + ".TextRendererGlyph");
    glyphProducerClass = tryLoad(PKG + ".TextRendererGlyphProducer");
    quadRendererClass = tryLoad(PKG + ".TextRendererQuadRenderer");
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

  // ── 1. TextRendererGlyph ──────────────────────────────────────────

  @Test
  public void textRendererGlyph_classExists() {
    assertNotNull("TextRendererGlyph must exist as a top-level class", glyphClass);
  }

  @Test
  public void textRendererGlyph_isPackagePrivate() {
    assertNotNull("class must exist", glyphClass);
    assertTrue("must be package-private",
        isPackagePrivate(glyphClass.getModifiers()));
  }

  @Test
  public void textRendererGlyph_hasSuppressWarningsCheckStyle() throws Exception {
    assertNotNull("class must exist", glyphClass);
    assertSourceContainsSuppressWarnings("TextRendererGlyph.java");
  }

  @Test
  public void textRendererGlyph_hasTextRendererField() {
    assertNotNull("class must exist", glyphClass);
    assertFieldExists(glyphClass, "textRenderer", NonCachingTextRenderer.class);
  }

  @Test
  public void textRendererGlyph_hasProducerFieldOfExtractedType() {
    assertNotNull("TextRendererGlyph must exist", glyphClass);
    assertNotNull("TextRendererGlyphProducer must exist", glyphProducerClass);
    assertFieldExists(glyphClass, "producer", glyphProducerClass);
  }

  @Test
  public void textRendererGlyph_hasUnicodeConstructor() {
    assertNotNull("TextRendererGlyph must exist", glyphClass);
    assertNotNull("TextRendererGlyphProducer must exist", glyphProducerClass);
    assertConstructorExists(glyphClass,
        "constructor(int, int, float, GlyphVector, TextRendererGlyphProducer, NonCachingTextRenderer)",
        int.class, int.class, float.class, GlyphVector.class,
        glyphProducerClass, NonCachingTextRenderer.class);
  }

  @Test
  public void textRendererGlyph_hasStringConstructor() {
    assertNotNull("TextRendererGlyph must exist", glyphClass);
    assertConstructorExists(glyphClass,
        "constructor(String, boolean, NonCachingTextRenderer)",
        String.class, boolean.class, NonCachingTextRenderer.class);
  }

  @Test
  public void textRendererGlyph_hasPublicGetUnicodeID() {
    assertNotNull("class must exist", glyphClass);
    assertPublicMethod(glyphClass, "getUnicodeID", int.class);
  }

  @Test
  public void textRendererGlyph_hasPublicGetGlyphCode() {
    assertNotNull("class must exist", glyphClass);
    assertPublicMethod(glyphClass, "getGlyphCode", int.class);
  }

  @Test
  public void textRendererGlyph_hasPublicGetAdvance() {
    assertNotNull("class must exist", glyphClass);
    assertPublicMethod(glyphClass, "getAdvance", float.class);
  }

  @Test
  public void textRendererGlyph_hasPublicDraw3D() {
    assertNotNull("class must exist", glyphClass);
    assertPublicMethodWithParams(glyphClass, "draw3D", float.class,
        float.class, float.class, float.class, float.class);
  }

  @Test
  public void textRendererGlyph_hasPublicClear() {
    assertNotNull("class must exist", glyphClass);
    assertPublicMethod(glyphClass, "clear", void.class);
  }

  // ── 2. TextRendererGlyphProducer ──────────────────────────────────

  @Test
  public void textRendererGlyphProducer_classExists() {
    assertNotNull("TextRendererGlyphProducer must exist as a top-level class",
        glyphProducerClass);
  }

  @Test
  public void textRendererGlyphProducer_isPackagePrivate() {
    assertNotNull("class must exist", glyphProducerClass);
    assertTrue("must be package-private",
        isPackagePrivate(glyphProducerClass.getModifiers()));
  }

  @Test
  public void textRendererGlyphProducer_hasSuppressWarningsCheckStyle() throws Exception {
    assertNotNull("class must exist", glyphProducerClass);
    assertSourceContainsSuppressWarnings("TextRendererGlyphProducer.java");
  }

  @Test
  public void textRendererGlyphProducer_hasTextRendererField() {
    assertNotNull("class must exist", glyphProducerClass);
    assertFieldExists(glyphProducerClass, "textRenderer", NonCachingTextRenderer.class);
  }

  @Test
  public void textRendererGlyphProducer_hasConstructor() {
    assertNotNull("class must exist", glyphProducerClass);
    assertConstructorExists(glyphProducerClass,
        "constructor(int, NonCachingTextRenderer)",
        int.class, NonCachingTextRenderer.class);
  }

  @Test
  public void textRendererGlyphProducer_undefinedConstant_isNegativeTwo() {
    assertNotNull("class must exist", glyphProducerClass);
    try {
      Field f = glyphProducerClass.getDeclaredField("undefined");
      f.setAccessible(true);
      assertTrue("undefined must be static", Modifier.isStatic(f.getModifiers()));
      assertEquals("undefined must be -2", -2, f.getInt(null));
    } catch (NoSuchFieldException | IllegalAccessException e) {
      fail("TextRendererGlyphProducer must have static int undefined = -2");
    }
  }

  @Test
  public void textRendererGlyphProducer_glyphCacheComponentType() {
    assertNotNull("TextRendererGlyphProducer must exist", glyphProducerClass);
    assertNotNull("TextRendererGlyph must exist", glyphClass);
    try {
      Field f = glyphProducerClass.getDeclaredField("glyphCache");
      assertTrue("glyphCache must be an array", f.getType().isArray());
      assertEquals("glyphCache element type must be TextRendererGlyph",
          glyphClass, f.getType().getComponentType());
    } catch (NoSuchFieldException e) {
      fail("glyphCache field must exist in TextRendererGlyphProducer");
    }
  }

  @Test
  public void textRendererGlyphProducer_iterFieldType() {
    assertNotNull("class must exist", glyphProducerClass);
    try {
      Field f = glyphProducerClass.getDeclaredField("iter");
      Class<?> charSeqIter = Class.forName(
          PKG + ".NonCachingTextRenderer$CharSequenceIterator");
      assertEquals("iter must be NonCachingTextRenderer.CharSequenceIterator",
          charSeqIter, f.getType());
    } catch (NoSuchFieldException | ClassNotFoundException e) {
      fail("iter field of type CharSequenceIterator must exist: " + e);
    }
  }

  @Test
  public void textRendererGlyphProducer_hasPublicGetGlyphs() {
    assertNotNull("class must exist", glyphProducerClass);
    assertPublicMethodWithParams(glyphProducerClass, "getGlyphs",
        java.util.List.class, CharSequence.class);
  }

  @Test
  public void textRendererGlyphProducer_hasPublicRegister() {
    assertNotNull("TextRendererGlyphProducer must exist", glyphProducerClass);
    assertNotNull("TextRendererGlyph must exist", glyphClass);
    try {
      Method m = glyphProducerClass.getDeclaredMethod("register", glyphClass);
      assertTrue("register must be public", Modifier.isPublic(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("register(TextRendererGlyph) must exist");
    }
  }

  @Test
  public void textRendererGlyphProducer_hasPublicClearCacheEntry() {
    assertNotNull("class must exist", glyphProducerClass);
    assertPublicMethodWithParams(glyphProducerClass, "clearCacheEntry",
        void.class, int.class);
  }

  @Test
  public void textRendererGlyphProducer_hasPublicClearAllCacheEntries() {
    assertNotNull("class must exist", glyphProducerClass);
    assertPublicMethod(glyphProducerClass, "clearAllCacheEntries", void.class);
  }

  @Test
  public void textRendererGlyphProducer_hasPublicGetGlyphPixelWidth() {
    assertNotNull("class must exist", glyphProducerClass);
    assertPublicMethodWithParams(glyphProducerClass, "getGlyphPixelWidth",
        float.class, char.class);
  }

  // ── 3. TextRendererQuadRenderer ───────────────────────────────────

  @Test
  public void textRendererQuadRenderer_classExists() {
    assertNotNull("TextRendererQuadRenderer must exist as a top-level class",
        quadRendererClass);
  }

  @Test
  public void textRendererQuadRenderer_isPackagePrivate() {
    assertNotNull("class must exist", quadRendererClass);
    assertTrue("must be package-private",
        isPackagePrivate(quadRendererClass.getModifiers()));
  }

  @Test
  public void textRendererQuadRenderer_hasSuppressWarningsCheckStyle() throws Exception {
    assertNotNull("class must exist", quadRendererClass);
    assertSourceContainsSuppressWarnings("TextRendererQuadRenderer.java");
  }

  @Test
  public void textRendererQuadRenderer_hasTextRendererField() {
    assertNotNull("class must exist", quadRendererClass);
    assertFieldExists(quadRendererClass, "textRenderer", NonCachingTextRenderer.class);
  }

  @Test
  public void textRendererQuadRenderer_hasConstructor() {
    assertNotNull("class must exist", quadRendererClass);
    assertConstructorExists(quadRendererClass,
        "constructor(NonCachingTextRenderer)",
        NonCachingTextRenderer.class);
  }

  @Test
  public void textRendererQuadRenderer_hasPublicGlTexCoord2f() {
    assertNotNull("class must exist", quadRendererClass);
    assertPublicMethodWithParams(quadRendererClass, "glTexCoord2f",
        void.class, float.class, float.class);
  }

  @Test
  public void textRendererQuadRenderer_hasPublicGlVertex3f() {
    assertNotNull("class must exist", quadRendererClass);
    assertPublicMethodWithParams(quadRendererClass, "glVertex3f",
        void.class, float.class, float.class, float.class);
  }

  @Test
  public void textRendererQuadRenderer_drawIsPackagePrivate() {
    assertNotNull("class must exist", quadRendererClass);
    try {
      Method m = quadRendererClass.getDeclaredMethod("draw");
      assertTrue("draw() must be package-private (called from flushGlyphPipeline)",
          isPackagePrivate(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("draw() must exist in TextRendererQuadRenderer");
    }
  }

  @Test
  public void textRendererQuadRenderer_hasPublicDispose() {
    assertNotNull("class must exist", quadRendererClass);
    assertPublicMethod(quadRendererClass, "dispose", void.class);
  }

  // ── 4. Inner classes removed from NonCachingTextRenderer ──────────

  @Test
  public void innerClass_Glyph_removed() {
    assertInnerClassAbsent("Glyph");
  }

  @Test
  public void innerClass_GlyphProducer_removed() {
    assertInnerClassAbsent("GlyphProducer");
  }

  @Test
  public void innerClass_Pipelined_QuadRenderer_removed() {
    assertInnerClassAbsent("Pipelined_QuadRenderer");
  }

  // ── 5. Remaining inner classes preserved ──────────────────────────

  @Test
  public void innerClass_CharSequenceIterator_preserved() {
    assertInnerClassPresent("CharSequenceIterator");
  }

  @Test
  public void innerClass_TextData_preserved() {
    assertInnerClassPresent("TextData");
  }

  @Test
  public void innerClass_DefaultRenderDelegate_preserved() {
    assertInnerClassPresent("DefaultRenderDelegate");
  }

  @Test
  public void innerClass_CharacterCache_preserved() {
    assertInnerClassPresent("CharacterCache");
  }

  @Test
  public void innerClass_Manager_preserved() {
    assertInnerClassPresent("Manager");
  }

  @Test
  public void innerClass_DebugListener_preserved() {
    assertInnerClassPresent("DebugListener");
  }

  // ── 6. NonCachingTextRenderer members widened ─────────────────────

  @Test
  public void field_font_isPackagePrivateFinal() {
    assertFieldWidened("font");
    assertFieldFinal("font");
  }

  @Test
  public void field_packer_isPackagePrivate() {
    assertFieldWidened("packer");
  }

  @Test
  public void field_renderDelegate_isPackagePrivateFinal() {
    assertFieldWidened("renderDelegate");
    assertFieldFinal("renderDelegate");
  }

  @Test
  public void field_singleUnicode_isPackagePrivateFinal() {
    assertFieldWidened("singleUnicode");
    assertFieldFinal("singleUnicode");
  }

  @Test
  public void field_DISABLE_GLYPH_CACHE_isPackagePrivateStatic() {
    assertFieldWidened("DISABLE_GLYPH_CACHE");
    assertFieldStatic("DISABLE_GLYPH_CACHE");
  }

  @Test
  public void field_DRAW_BBOXES_isPackagePrivateStatic() {
    assertFieldWidened("DRAW_BBOXES");
    assertFieldStatic("DRAW_BBOXES");
  }

  @Test
  public void field_isExtensionAvailable_GL_VERSION_1_5_isPackagePrivate() {
    assertFieldWidened("isExtensionAvailable_GL_VERSION_1_5");
  }

  @Test
  public void method_getBackingStore_isPackagePrivate() {
    assertMethodWidened("getBackingStore");
  }

  @Test
  public void method_getGraphics2D_isPackagePrivate() {
    assertMethodWidened("getGraphics2D");
  }

  @Test
  public void method_getFontRenderContext_remainsPublic() {
    // getFontRenderContext is already public — extraction should not change it
    try {
      Method m = NonCachingTextRenderer.class.getDeclaredMethod("getFontRenderContext");
      assertTrue("getFontRenderContext should remain public",
          Modifier.isPublic(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("getFontRenderContext must exist");
    }
  }

  @Test
  public void method_normalize_isPackagePrivate() {
    assertMethodWidened("normalize");
  }

  @Test
  public void method_preNormalize_isStaticPackagePrivate() {
    try {
      Method m = NonCachingTextRenderer.class.getDeclaredMethod(
          "preNormalize", java.awt.geom.Rectangle2D.class);
      assertTrue("preNormalize must be static",
          Modifier.isStatic(m.getModifiers()));
      assertTrue("preNormalize must be package-private",
          isPackagePrivate(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("preNormalize(Rectangle2D) must exist");
    }
  }

  @Test
  public void method_draw3D_ROBUST_isPackagePrivate() {
    assertMethodWidened("draw3D_ROBUST");
  }

  @Test
  public void method_is15Available_isPackagePrivate() {
    assertMethodWidened("is15Available");
  }

  // ── 7. Field type changes ─────────────────────────────────────────

  @Test
  public void field_mGlyphProducer_typeIsTextRendererGlyphProducer() {
    assertNotNull("TextRendererGlyphProducer must exist", glyphProducerClass);
    try {
      Field f = NonCachingTextRenderer.class.getDeclaredField("mGlyphProducer");
      assertEquals("mGlyphProducer type must be TextRendererGlyphProducer",
          glyphProducerClass, f.getType());
    } catch (NoSuchFieldException e) {
      fail("mGlyphProducer field must exist in NonCachingTextRenderer");
    }
  }

  @Test
  public void field_mPipelinedQuadRenderer_typeIsTextRendererQuadRenderer() {
    assertNotNull("TextRendererQuadRenderer must exist", quadRendererClass);
    try {
      Field f = NonCachingTextRenderer.class.getDeclaredField("mPipelinedQuadRenderer");
      assertEquals("mPipelinedQuadRenderer type must be TextRendererQuadRenderer",
          quadRendererClass, f.getType());
    } catch (NoSuchFieldException e) {
      fail("mPipelinedQuadRenderer field must exist in NonCachingTextRenderer");
    }
  }

  // ── 8. Line count ─────────────────────────────────────────────────

  @Test
  public void nonCachingTextRenderer_lineCount_under1350() throws Exception {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/"
            + "NonCachingTextRenderer.java");
    assertNotNull("Must find NonCachingTextRenderer.java", sourceFile);
    long lineCount = Files.lines(sourceFile).count();
    assertTrue(
        "NonCachingTextRenderer.java must be under 1350 lines (actual: "
            + lineCount + ")",
        lineCount < 1350);
  }

  // ── Assertion helpers ─────────────────────────────────────────────

  private void assertSourceContainsSuppressWarnings(String fileName) throws Exception {
    Path sourceFile = findSourceFile(
        "core/glrender/src/main/java/edu/cmu/cs/dennisc/render/joglrenderer/"
            + fileName);
    assertNotNull("Must find " + fileName, sourceFile);
    String source = new String(Files.readAllBytes(sourceFile));
    assertTrue(fileName + " must have @SuppressWarnings(\"CheckStyle\")",
        source.contains("@SuppressWarnings(\"CheckStyle\")"));
  }

  private void assertContains(String[] values, String expected) {
    for (String v : values) {
      if (expected.equals(v)) {
        return;
      }
    }
    fail("Expected @SuppressWarnings to contain \"" + expected + "\"");
  }

  private void assertFieldExists(Class<?> clazz, String name, Class<?> expectedType) {
    try {
      Field f = clazz.getDeclaredField(name);
      assertEquals(clazz.getSimpleName() + "." + name + " type",
          expectedType, f.getType());
    } catch (NoSuchFieldException e) {
      fail(clazz.getSimpleName() + " must have field '" + name + "'");
    }
  }

  private void assertConstructorExists(Class<?> clazz, String desc, Class<?>... params) {
    try {
      clazz.getDeclaredConstructor(params);
    } catch (NoSuchMethodException e) {
      fail(clazz.getSimpleName() + " must have " + desc);
    }
  }

  private void assertPublicMethod(Class<?> clazz, String name, Class<?> returnType) {
    try {
      Method m = clazz.getDeclaredMethod(name);
      assertTrue(name + " must be public", Modifier.isPublic(m.getModifiers()));
      assertEquals(name + " return type", returnType, m.getReturnType());
    } catch (NoSuchMethodException e) {
      fail(clazz.getSimpleName() + " must have " + name + "()");
    }
  }

  private void assertPublicMethodWithParams(Class<?> clazz, String name,
      Class<?> returnType, Class<?>... params) {
    try {
      Method m = clazz.getDeclaredMethod(name, params);
      assertTrue(name + " must be public", Modifier.isPublic(m.getModifiers()));
      assertEquals(name + " return type", returnType, m.getReturnType());
    } catch (NoSuchMethodException e) {
      fail(clazz.getSimpleName() + " must have " + name + "(...)");
    }
  }

  private void assertInnerClassAbsent(String simpleName) {
    for (Class<?> inner : NonCachingTextRenderer.class.getDeclaredClasses()) {
      if (simpleName.equals(inner.getSimpleName())) {
        fail("Inner class '" + simpleName
            + "' must be extracted to a top-level class");
      }
    }
  }

  private void assertInnerClassPresent(String simpleName) {
    for (Class<?> inner : NonCachingTextRenderer.class.getDeclaredClasses()) {
      if (simpleName.equals(inner.getSimpleName())) {
        return;
      }
    }
    fail("Inner class '" + simpleName
        + "' must be preserved in NonCachingTextRenderer");
  }

  private void assertFieldWidened(String fieldName) {
    try {
      Field f = NonCachingTextRenderer.class.getDeclaredField(fieldName);
      assertTrue("'" + fieldName + "' must be package-private (not private)",
          isPackagePrivate(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("Field '" + fieldName + "' must exist in NonCachingTextRenderer");
    }
  }

  private void assertFieldFinal(String fieldName) {
    try {
      Field f = NonCachingTextRenderer.class.getDeclaredField(fieldName);
      assertTrue("'" + fieldName + "' must be final",
          Modifier.isFinal(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("Field '" + fieldName + "' must exist");
    }
  }

  private void assertFieldStatic(String fieldName) {
    try {
      Field f = NonCachingTextRenderer.class.getDeclaredField(fieldName);
      assertTrue("'" + fieldName + "' must be static",
          Modifier.isStatic(f.getModifiers()));
    } catch (NoSuchFieldException e) {
      fail("Field '" + fieldName + "' must exist");
    }
  }

  private void assertMethodWidened(String methodName) {
    for (Method m : NonCachingTextRenderer.class.getDeclaredMethods()) {
      if (m.getName().equals(methodName)) {
        assertTrue("'" + methodName + "' must be package-private (not private)",
            isPackagePrivate(m.getModifiers()));
        return;
      }
    }
    fail("Method '" + methodName + "' must exist in NonCachingTextRenderer");
  }

  private Path findSourceFile(String relativePath) {
    Path cwd = Paths.get(System.getProperty("user.dir"));
    for (Path p = cwd; p != null; p = p.getParent()) {
      Path candidate = p.resolve(relativePath);
      if (Files.exists(candidate)) {
        return candidate;
      }
      // Stop at root or after reasonable depth
      if (p.equals(p.getRoot())) {
        break;
      }
    }
    return null;
  }
}
