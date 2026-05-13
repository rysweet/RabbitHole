/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package edu.cmu.cs.dennisc.scenegraph.io;

import edu.cmu.cs.dennisc.color.Color4f;
import edu.cmu.cs.dennisc.scenegraph.Component;
import edu.cmu.cs.dennisc.scenegraph.Transformable;
import edu.cmu.cs.dennisc.scenegraph.Vertex;
import edu.cmu.cs.dennisc.texture.TextureCoordinate2f;
import org.alice.math.immutable.AffineMatrix4x4;
import org.alice.math.immutable.Point3;
import org.alice.math.immutable.Vector3f;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * TDD tests for decomposing ASG.java into ASGEncoder + ASGDecoder delegates.
 *
 * Structure tests (marked STRUCTURAL) will FAIL until the new classes are created.
 * Characterization tests capture existing behavior and must PASS before and after.
 */
public class ASGDecompositionTest {
  private static final double EPSILON = 0.000001;

  // ═══════════════════════════════════════════════════════════════════
  // STRUCTURAL: These tests FAIL until ASGEncoder/ASGDecoder exist.
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void asgEncoderClassExistsInPackage() throws Exception {
    Class<?> cls = Class.forName("edu.cmu.cs.dennisc.scenegraph.io.ASGEncoder");
    assertNotNull("ASGEncoder class should exist", cls);
  }

  @Test
  public void asgDecoderClassExistsInPackage() throws Exception {
    Class<?> cls = Class.forName("edu.cmu.cs.dennisc.scenegraph.io.ASGDecoder");
    assertNotNull("ASGDecoder class should exist", cls);
  }

  @Test
  public void asgEncoderHasStaticEncodeMethod() throws Exception {
    Class<?> cls = Class.forName("edu.cmu.cs.dennisc.scenegraph.io.ASGEncoder");
    boolean found = false;
    for (Method m : cls.getDeclaredMethods()) {
      if (m.getName().equals("encode") && Modifier.isStatic(m.getModifiers())) {
        found = true;
        break;
      }
    }
    assertTrue("ASGEncoder should have a static encode method", found);
  }

  @Test
  public void asgDecoderHasStaticDecodeMethod() throws Exception {
    Class<?> cls = Class.forName("edu.cmu.cs.dennisc.scenegraph.io.ASGDecoder");
    boolean found = false;
    for (Method m : cls.getDeclaredMethods()) {
      if (m.getName().equals("decode") && Modifier.isStatic(m.getModifiers())) {
        found = true;
        break;
      }
    }
    assertTrue("ASGDecoder should have a static decode method", found);
  }

  @Test
  public void asgEncoderHasBinaryEncodeMethods() throws Exception {
    Class<?> cls = Class.forName("edu.cmu.cs.dennisc.scenegraph.io.ASGEncoder");
    assertStaticMethodExists(cls, "encodeIntArrayInBinary");
    assertStaticMethodExists(cls, "encodeDoubleArrayInBinary");
    assertStaticMethodExists(cls, "encodeVertexArrayInBinary");
  }

  @Test
  public void asgDecoderHasBinaryDecodeMethods() throws Exception {
    Class<?> cls = Class.forName("edu.cmu.cs.dennisc.scenegraph.io.ASGDecoder");
    assertStaticMethodExists(cls, "decodeIntArrayInBinary");
    assertStaticMethodExists(cls, "decodeDoubleArrayInBinary");
    assertStaticMethodExists(cls, "decodeVertexArrayInBinary");
  }

  // ═══════════════════════════════════════════════════════════════════
  // API PRESERVATION: ASG public contract must remain intact.
  // These PASS now and must continue to PASS after decomposition.
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void asgVersionConstantIsOne() {
    assertEquals(1.0, ASG.VERSION, EPSILON);
  }

  @Test
  public void asgHasPublicStaticEncodeToStream() throws Exception {
    assertPublicStaticMethod(ASG.class, "encode", Component.class, OutputStream.class);
  }

  @Test
  public void asgHasPublicStaticEncodeToFile() throws Exception {
    assertPublicStaticMethod(ASG.class, "encode", Component.class, File.class);
  }

  @Test
  public void asgHasPublicStaticEncodeToPath() throws Exception {
    assertPublicStaticMethod(ASG.class, "encode", Component.class, String.class);
  }

  @Test
  public void asgHasPublicStaticDecodeFromStreamWithMap() throws Exception {
    assertPublicStaticMethod(ASG.class, "decode", InputStream.class, HashMap.class);
  }

  @Test
  public void asgHasPublicStaticDecodeZip() throws Exception {
    assertPublicStaticMethod(ASG.class, "decodeZip", InputStream.class);
  }

  @Test
  public void asgHasPublicStaticDecodeFromFile() throws Exception {
    assertPublicStaticMethod(ASG.class, "decode", File.class);
  }

  @Test
  public void asgHasPublicStaticDecodeFromPath() throws Exception {
    assertPublicStaticMethod(ASG.class, "decode", String.class);
  }

  @Test
  public void asgHasPublicStaticEncodeVertexArrayInBinary() throws Exception {
    assertPublicStaticMethod(ASG.class, "encodeVertexArrayInBinary", Vertex[].class, OutputStream.class);
  }

  @Test
  public void asgHasPublicStaticDecodeVertexArrayInBinary() throws Exception {
    assertPublicStaticMethod(ASG.class, "decodeVertexArrayInBinary", InputStream.class);
  }

  @Test
  public void asgHasPublicStaticEncodeIntArrayInBinary() throws Exception {
    assertPublicStaticMethod(ASG.class, "encodeIntArrayInBinary", int[].class, OutputStream.class);
  }

  @Test
  public void asgHasPublicStaticDecodeIntArrayInBinary() throws Exception {
    assertPublicStaticMethod(ASG.class, "decodeIntArrayInBinary", InputStream.class);
  }

  @Test
  public void asgHasPublicStaticEncodeDoubleArrayInBinary() throws Exception {
    assertPublicStaticMethod(ASG.class, "encodeDoubleArrayInBinary", double[].class, OutputStream.class);
  }

  @Test
  public void asgHasPublicStaticDecodeDoubleArrayInBinary() throws Exception {
    assertPublicStaticMethod(ASG.class, "decodeDoubleArrayInBinary", InputStream.class);
  }

  // ═══════════════════════════════════════════════════════════════════
  // BINARY INT ARRAY: Characterization tests for encode/decode.
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void intArrayBinaryRoundtripAppliesWindingSwap() {
    // Encoder writes version 2; decoder v2 swaps triplet[0] <-> triplet[2]
    int[] original = {0, 1, 2, 3, 4, 5};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeIntArrayInBinary(original, baos);
    int[] decoded = ASG.decodeIntArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
    assertArrayEquals(new int[]{2, 1, 0, 5, 4, 3}, decoded);
  }

  @Test
  public void intArrayBinarySingleTriplet() {
    int[] original = {10, 20, 30};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeIntArrayInBinary(original, baos);
    int[] decoded = ASG.decodeIntArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
    // After winding swap: [30, 20, 10]
    assertArrayEquals(new int[]{30, 20, 10}, decoded);
  }

  @Test
  public void intArrayBinaryEmptyArray() {
    int[] original = {};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeIntArrayInBinary(original, baos);
    int[] decoded = ASG.decodeIntArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
    assertNotNull(decoded);
    assertEquals(0, decoded.length);
  }

  @Test(expected = RuntimeException.class)
  public void intArrayBinaryInvalidVersionThrowsRuntimeException() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    dos.writeInt(99); // invalid version
    dos.flush();
    ASG.decodeIntArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
  }

  // ═══════════════════════════════════════════════════════════════════
  // BINARY DOUBLE ARRAY: Characterization tests for encode/decode.
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void doubleArrayBinaryRoundtripPreservesExactValues() {
    double[] original = {1.5, 2.7, 3.14159, -0.001, Double.MAX_VALUE};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeDoubleArrayInBinary(original, baos);
    double[] decoded = ASG.decodeDoubleArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
    assertArrayEquals(original, decoded, 0.0);
  }

  @Test
  public void doubleArrayBinarySingleElement() {
    double[] original = {42.0};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeDoubleArrayInBinary(original, baos);
    double[] decoded = ASG.decodeDoubleArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
    assertArrayEquals(original, decoded, 0.0);
  }

  @Test
  public void doubleArrayBinaryEmptyArray() {
    double[] original = {};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeDoubleArrayInBinary(original, baos);
    double[] decoded = ASG.decodeDoubleArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
    assertNotNull(decoded);
    assertEquals(0, decoded.length);
  }

  @Test
  public void doubleArrayBinaryVersion1ReturnsNull() throws Exception {
    // Version 1 for double array is a no-op returning null
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    dos.writeInt(1); // version 1
    dos.flush();
    double[] decoded = ASG.decodeDoubleArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
    // Version 1 comment says "there was no version 1" — returns null
    assertTrue("decodeDoubleArrayInBinary version 1 should return null", decoded == null);
  }

  @Test(expected = RuntimeException.class)
  public void doubleArrayBinaryInvalidVersionThrowsRuntimeException() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    dos.writeInt(99); // invalid version
    dos.flush();
    ASG.decodeDoubleArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
  }

  // ═══════════════════════════════════════════════════════════════════
  // BINARY VERTEX ARRAY: Characterization tests for encode/decode.
  // Position-only vertices avoid the normal double→float mismatch bug.
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void vertexArrayBinaryRoundtripPositionOnly() {
    Vertex v1 = Vertex.createXYZ(1.0, 2.0, 3.0);
    Vertex v2 = Vertex.createXYZ(-4.0, -5.0, -6.0);
    Vertex[] original = {v1, v2};

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeVertexArrayInBinary(original, baos);
    Vertex[] decoded = ASG.decodeVertexArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));

    assertNotNull(decoded);
    assertEquals(2, decoded.length);
    assertPointEquals(new Point3(1.0, 2.0, 3.0), decoded[0].position);
    assertPointEquals(new Point3(-4.0, -5.0, -6.0), decoded[1].position);
  }

  @Test
  public void vertexArrayBinaryRoundtripWithDiffuseColorAndTexCoord() {
    Color4f color = new Color4f(1.0f, 0.5f, 0.25f, 1.0f);
    TextureCoordinate2f tc = new TextureCoordinate2f(0.5f, 0.75f);
    Point3 pos = new Point3(10.0, 20.0, 30.0);
    // Using position + diffuseColor + texCoord (no normal to avoid double→float bug)
    Vertex v = new Vertex(pos, null, color, null, tc);
    Vertex[] original = {v};

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeVertexArrayInBinary(original, baos);
    Vertex[] decoded = ASG.decodeVertexArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));

    assertNotNull(decoded);
    assertEquals(1, decoded.length);
    assertPointEquals(pos, decoded[0].position);
    assertEquals(color.red, decoded[0].diffuseColor.red, 0.0001f);
    assertEquals(color.green, decoded[0].diffuseColor.green, 0.0001f);
    assertEquals(color.blue, decoded[0].diffuseColor.blue, 0.0001f);
    assertEquals(color.alpha, decoded[0].diffuseColor.alpha, 0.0001f);
    assertEquals(tc.u, decoded[0].textureCoordinate0.u, 0.0001f);
    assertEquals(tc.v, decoded[0].textureCoordinate0.v, 0.0001f);
  }

  @Test
  public void vertexArrayBinaryEmptyArray() {
    Vertex[] original = {};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeVertexArrayInBinary(original, baos);
    Vertex[] decoded = ASG.decodeVertexArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
    assertNotNull(decoded);
    assertEquals(0, decoded.length);
  }

  @Test(expected = RuntimeException.class)
  public void vertexArrayBinaryInvalidVersionThrowsRuntimeException() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    dos.writeInt(99); // invalid version
    dos.flush();
    ASG.decodeVertexArrayInBinary(new ByteArrayInputStream(baos.toByteArray()));
  }

  // ═══════════════════════════════════════════════════════════════════
  // FULL ENCODE/DECODE ROUNDTRIP: Scenegraph serialization contract.
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void encodeProducesNonEmptyZipOutput() {
    Transformable original = new Transformable();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encode(original, baos);
    byte[] encoded = baos.toByteArray();
    assertTrue("Encoded output should not be empty", encoded.length > 0);
  }

  // Pre-existing bug: encoder writes class="edu.cmu.cs.dennisc.math.Matrix4d"
  // for AffineMatrix4x4 values, but that legacy class no longer exists.
  // The decoder fails with ClassNotFoundException wrapped in RuntimeException.
  // These tests document this known limitation — behavior must be preserved
  // (not fixed) during the decomposition.

  @Test(expected = RuntimeException.class)
  public void decodeTransformableFailsDueToLegacyMatrix4dClassName() {
    Transformable original = new Transformable();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encode(original, baos);
    ASG.decodeZip(new ByteArrayInputStream(baos.toByteArray()));
  }

  @Test(expected = RuntimeException.class)
  public void decodeTransformableWithTranslationFailsDueToLegacyMatrix4d() {
    Transformable original = new Transformable();
    original.setLocalTransformation(AffineMatrix4x4.createTranslation(10.0, 20.0, 30.0));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encode(original, baos);
    ASG.decodeZip(new ByteArrayInputStream(baos.toByteArray()));
  }

  @Test(expected = RuntimeException.class)
  public void decodeParentChildHierarchyFailsDueToLegacyMatrix4d() {
    Transformable parent = new Transformable();
    parent.setLocalTransformation(AffineMatrix4x4.createTranslation(1.0, 0.0, 0.0));
    Transformable child = new Transformable();
    child.setLocalTransformation(AffineMatrix4x4.createTranslation(0.0, 2.0, 0.0));
    parent.addComponent(child);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encode(parent, baos);
    ASG.decodeZip(new ByteArrayInputStream(baos.toByteArray()));
  }

  // ═══════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════

  private static void assertPublicStaticMethod(Class<?> cls, String name, Class<?>... paramTypes) throws Exception {
    Method m = cls.getMethod(name, paramTypes);
    assertTrue(name + " should be public", Modifier.isPublic(m.getModifiers()));
    assertTrue(name + " should be static", Modifier.isStatic(m.getModifiers()));
  }

  private static void assertStaticMethodExists(Class<?> cls, String name) {
    boolean found = false;
    for (Method m : cls.getDeclaredMethods()) {
      if (m.getName().equals(name) && Modifier.isStatic(m.getModifiers())) {
        found = true;
        break;
      }
    }
    assertTrue(cls.getSimpleName() + " should have static method " + name, found);
  }

  private static void assertPointEquals(Point3 expected, Point3 actual) {
    assertEquals("x", expected.x(), actual.x(), EPSILON);
    assertEquals("y", expected.y(), actual.y(), EPSILON);
    assertEquals("z", expected.z(), actual.z(), EPSILON);
  }
}
