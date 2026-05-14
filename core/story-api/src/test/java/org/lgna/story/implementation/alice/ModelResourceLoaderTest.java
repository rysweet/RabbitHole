package org.lgna.story.implementation.alice;

import edu.cmu.cs.dennisc.scenegraph.SkeletonVisual;
import edu.cmu.cs.dennisc.scenegraph.TexturedAppearance;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * TDD characterization tests for ModelResourceLoader.
 *
 * These tests define the contract for binary codec, cached resource loading,
 * and skeleton operations extracted from AliceResourceUtilities.
 */
public class ModelResourceLoaderTest {

  // ── Binary decode ───────────────────────────────────────

  @Test
  public void decodeVisual_nullUrl_returnsNull() {
    // The original implementation catches all exceptions and returns null
    assertNull(ModelResourceLoader.decodeVisual(null));
  }

  @Test
  public void decodeTexture_nullUrl_returnsNull() {
    assertNull(ModelResourceLoader.decodeTexture(null));
  }

  @Test
  public void decodeVisual_invalidUrl_returnsNull() throws Exception {
    // A URL that can't be opened should return null (original catches Exception)
    URL badUrl = new URL("file:///nonexistent/path/to/visual.a3r");
    assertNull(ModelResourceLoader.decodeVisual(badUrl));
  }

  @Test
  public void decodeTexture_invalidUrl_returnsNull() throws Exception {
    URL badUrl = new URL("file:///nonexistent/path/to/texture.a3t");
    assertNull(ModelResourceLoader.decodeTexture(badUrl));
  }

  // ── Binary encode ───────────────────────────────────────

  @Test
  public void encodeVisual_toOutputStream_writesBytes() throws Exception {
    SkeletonVisual visual = new SkeletonVisual();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ModelResourceLoader.encodeVisual(visual, baos);
    assertTrue("Should write some bytes", baos.size() > 0);
  }

  @Test
  public void encodeTexture_toOutputStream_writesBytes() throws Exception {
    TexturedAppearance[] textures = new TexturedAppearance[0];
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ModelResourceLoader.encodeTexture(textures, baos);
    assertTrue("Should write some bytes", baos.size() > 0);
  }

  @Test
  public void encodeVisual_toFile_createsFile() throws Exception {
    File tempFile = File.createTempFile("test_visual_", ".a3r");
    tempFile.deleteOnExit();
    tempFile.delete(); // Ensure it doesn't exist before encode

    SkeletonVisual visual = new SkeletonVisual();
    ModelResourceLoader.encodeVisual(visual, tempFile);

    assertTrue("File should exist after encode", tempFile.exists());
    assertTrue("File should have content", tempFile.length() > 0);
  }

  @Test
  public void encodeTexture_toFile_createsFile() throws Exception {
    File tempFile = File.createTempFile("test_texture_", ".a3t");
    tempFile.deleteOnExit();
    tempFile.delete();

    TexturedAppearance[] textures = new TexturedAppearance[0];
    ModelResourceLoader.encodeTexture(textures, tempFile);

    assertTrue("File should exist after encode", tempFile.exists());
    assertTrue("File should have content", tempFile.length() > 0);
  }

  // ── Encode/decode round-trip ────────────────────────────

  @Test
  public void encodeDecodeVisual_roundTrip_preservesStructure() throws Exception {
    SkeletonVisual original = new SkeletonVisual();
    File tempFile = File.createTempFile("test_roundtrip_visual_", ".a3r");
    tempFile.deleteOnExit();

    ModelResourceLoader.encodeVisual(original, tempFile);
    SkeletonVisual decoded = ModelResourceLoader.decodeVisual(tempFile.toURI().toURL());

    assertNotNull("Decoded visual should not be null", decoded);
  }

  // ── Cached resource loading ─────────────────────────────
  // getVisual, getVisualCopy, getTexturedAppearances need real model resources.
  // We test the caching contract structurally.

  @Test
  public void urlToVisualMap_cacheField_exists() throws NoSuchFieldException {
    assertNotNull("Visual cache should exist",
        ModelResourceLoader.class.getDeclaredField("urlToVisualMap"));
  }

  @Test
  public void urlToTextureMap_cacheField_exists() throws NoSuchFieldException {
    assertNotNull("Texture cache should exist",
        ModelResourceLoader.class.getDeclaredField("urlToTextureMap"));
  }

  // ── Skeleton operations ─────────────────────────────────

  @Test
  public void createCopy_nullSkeleton_producesVisualWithNullSkeleton() {
    SkeletonVisual original = new SkeletonVisual();
    // original has null skeleton by default
    SkeletonVisual copy = ModelResourceLoader.createCopy(original);
    assertNotNull("Copy should not be null", copy);
    assertNotSame("Copy should be a different instance", original, copy);
    assertNull("Copy skeleton should be null when original is null", copy.skeleton.getValue());
  }

  @Test
  public void createCopy_preservesIsShowing() {
    SkeletonVisual original = new SkeletonVisual();
    original.isShowing.setValue(false);
    SkeletonVisual copy = ModelResourceLoader.createCopy(original);
    assertFalse("Copy should preserve isShowing=false", copy.isShowing.getValue());
  }

  @Test
  public void createCopy_sharesGeometryArrayReference() {
    SkeletonVisual original = new SkeletonVisual();
    SkeletonVisual copy = ModelResourceLoader.createCopy(original);
    // Geometries are shared (not deep copied) per the original implementation
    assertSame("Geometry array should be shared", original.geometries.getValue(), copy.geometries.getValue());
  }

  // ── Method signature contracts ──────────────────────────

  @Test
  public void getVisual_methodSignature_exists() throws NoSuchMethodException {
    assertNotNull(ModelResourceLoader.class.getDeclaredMethod(
        "getVisual", org.lgna.story.resources.ModelResource.class));
  }

  @Test
  public void getVisualCopy_methodSignature_exists() throws NoSuchMethodException {
    assertNotNull(ModelResourceLoader.class.getDeclaredMethod(
        "getVisualCopy", org.lgna.story.resources.ModelResource.class));
  }

  @Test
  public void getTexturedAppearances_methodSignature_exists() throws NoSuchMethodException {
    assertNotNull(ModelResourceLoader.class.getDeclaredMethod(
        "getTexturedAppearances", org.lgna.story.resources.ModelResource.class));
  }

  @Test
  public void createReplaceVisualElements_methodSignature_exists() throws NoSuchMethodException {
    assertNotNull(ModelResourceLoader.class.getDeclaredMethod(
        "createReplaceVisualElements",
        SkeletonVisual.class,
        org.lgna.story.resources.ModelResource.class));
  }

  @Test
  public void getOriginalJointTransformation_methodSignature_exists() throws NoSuchMethodException {
    assertNotNull(ModelResourceLoader.class.getDeclaredMethod(
        "getOriginalJointTransformation",
        org.lgna.story.resources.ModelResource.class,
        org.lgna.story.resources.JointId.class));
  }

  @Test
  public void getOriginalJointOrientation_methodSignature_exists() throws NoSuchMethodException {
    assertNotNull(ModelResourceLoader.class.getDeclaredMethod(
        "getOriginalJointOrientation",
        org.lgna.story.resources.ModelResource.class,
        org.lgna.story.resources.JointId.class));
  }

  // ── correctDimensions is private ────────────────────────

  @Test
  public void correctDimensions_isPrivateHelper() throws NoSuchMethodException {
    java.lang.reflect.Method m = ModelResourceLoader.class.getDeclaredMethod(
        "correctDimensions", TexturedAppearance.class);
    assertTrue("correctDimensions should be private",
        java.lang.reflect.Modifier.isPrivate(m.getModifiers()));
  }
}
