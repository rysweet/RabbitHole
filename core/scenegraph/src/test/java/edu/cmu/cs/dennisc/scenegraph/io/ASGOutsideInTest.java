/*******************************************************************************
 * Outside-in test: Verifies the ASG decomposition from an external user perspective.
 * Scenario 1: API contract preservation (all public methods still exist and delegate)
 * Scenario 2: Binary roundtrip fidelity through the facade
 *******************************************************************************/
package edu.cmu.cs.dennisc.scenegraph.io;

import edu.cmu.cs.dennisc.color.Color4f;
import edu.cmu.cs.dennisc.scenegraph.Transformable;
import edu.cmu.cs.dennisc.scenegraph.Vertex;
import edu.cmu.cs.dennisc.texture.TextureCoordinate2f;
import org.alice.math.immutable.Point3;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Outside-in tests for the ASG decomposition into ASGEncoder + ASGDecoder.
 * These tests validate behavior as an external caller would observe it.
 */
public class ASGOutsideInTest {

  // ═══════════════════════════════════════════════════════════════════
  // SCENARIO 1: API Contract Preservation
  // Verify every public method on ASG facade exists and delegates correctly
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void scenario1_allPublicMethodsPreserved() {
    Set<String> expected = new HashSet<>(Arrays.asList(
        "encode", "decode", "decodeZip",
        "encodeVertexArrayInBinary", "decodeVertexArrayInBinary",
        "encodeIntArrayInBinary", "decodeIntArrayInBinary",
        "encodeDoubleArrayInBinary", "decodeDoubleArrayInBinary"
    ));
    Set<String> found = new HashSet<>();
    for (Method m : ASG.class.getMethods()) {
      if (Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())) {
        if (expected.contains(m.getName())) {
          found.add(m.getName());
        }
      }
    }
    assertEquals("All expected public static methods must exist on ASG facade",
        expected, found);
  }

  @Test
  public void scenario1_encoderClassExists() throws Exception {
    Class<?> cls = Class.forName("edu.cmu.cs.dennisc.scenegraph.io.ASGEncoder");
    assertNotNull(cls);
    // Verify it has the 4 encode methods
    for (String name : new String[]{"encode", "encodeVertexArrayInBinary",
        "encodeIntArrayInBinary", "encodeDoubleArrayInBinary"}) {
      boolean exists = false;
      for (Method m : cls.getDeclaredMethods()) {
        if (m.getName().equals(name) && Modifier.isStatic(m.getModifiers())) {
          exists = true;
          break;
        }
      }
      assertTrue("ASGEncoder must have static " + name, exists);
    }
  }

  @Test
  public void scenario1_decoderClassExists() throws Exception {
    Class<?> cls = Class.forName("edu.cmu.cs.dennisc.scenegraph.io.ASGDecoder");
    assertNotNull(cls);
    for (String name : new String[]{"decode", "decodeZip", "decodeVertexArrayInBinary",
        "decodeIntArrayInBinary", "decodeDoubleArrayInBinary"}) {
      boolean exists = false;
      for (Method m : cls.getDeclaredMethods()) {
        if (m.getName().equals(name) && Modifier.isStatic(m.getModifiers())) {
          exists = true;
          break;
        }
      }
      assertTrue("ASGDecoder must have static " + name, exists);
    }
  }

  @Test
  public void scenario1_versionConstantPreserved() {
    assertEquals(1.0, ASG.VERSION, 0.0001);
  }

  @Test
  public void scenario1_facadeLineCountUnder500() throws Exception {
    // Count non-blank lines in ASG.java source
    java.io.File src = new java.io.File(
        "src/main/java/edu/cmu/cs/dennisc/scenegraph/io/ASG.java");
    if (src.exists()) {
      long lines = java.nio.file.Files.lines(src.toPath()).count();
      assertTrue("ASG.java should be under 500 lines, was " + lines, lines < 500);
    }
    // If file not found at relative path, skip silently (CI may run from different dir)
  }

  // ═══════════════════════════════════════════════════════════════════
  // SCENARIO 2: Binary Roundtrip Fidelity Through Facade
  // Complex end-to-end: encode via ASG facade, decode via ASG facade,
  // verify data integrity across all three binary array types + scene graph
  // ═══════════════════════════════════════════════════════════════════

  @Test
  public void scenario2_intArrayRoundtripThroughFacade() {
    int[] input = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeIntArrayInBinary(input, baos);
    byte[] encoded = baos.toByteArray();
    assertTrue("Encoded bytes should be non-empty", encoded.length > 0);

    int[] decoded = ASG.decodeIntArrayInBinary(new ByteArrayInputStream(encoded));
    assertNotNull(decoded);
    assertEquals(input.length, decoded.length);
    // Version 2 applies winding swap: triplet[0] <-> triplet[2]
    assertArrayEquals(new int[]{2, 1, 0, 5, 4, 3, 8, 7, 6}, decoded);
  }

  @Test
  public void scenario2_doubleArrayRoundtripThroughFacade() {
    double[] input = {Math.PI, Math.E, -0.0, Double.MIN_VALUE, 999.999};
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeDoubleArrayInBinary(input, baos);
    byte[] encoded = baos.toByteArray();
    assertTrue("Encoded bytes should be non-empty", encoded.length > 0);

    double[] decoded = ASG.decodeDoubleArrayInBinary(new ByteArrayInputStream(encoded));
    assertArrayEquals("Double values should survive roundtrip exactly", input, decoded, 0.0);
  }

  @Test
  public void scenario2_vertexArrayRoundtripThroughFacade() {
    Color4f red = new Color4f(1.0f, 0.0f, 0.0f, 1.0f);
    TextureCoordinate2f uv = new TextureCoordinate2f(0.25f, 0.75f);
    Vertex v1 = new Vertex(new Point3(1.0, 2.0, 3.0), null, red, null, uv);
    Vertex v2 = Vertex.createXYZ(4.0, 5.0, 6.0);
    Vertex v3 = Vertex.createXYZ(-1.0, -2.0, -3.0);
    Vertex[] input = {v1, v2, v3};

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encodeVertexArrayInBinary(input, baos);
    byte[] encoded = baos.toByteArray();
    assertTrue("Encoded bytes should be non-empty", encoded.length > 0);

    Vertex[] decoded = ASG.decodeVertexArrayInBinary(new ByteArrayInputStream(encoded));
    assertNotNull(decoded);
    assertEquals(3, decoded.length);

    // Verify first vertex (with color and UV)
    assertEquals(1.0, decoded[0].position.x(), 0.0001);
    assertEquals(2.0, decoded[0].position.y(), 0.0001);
    assertEquals(3.0, decoded[0].position.z(), 0.0001);
    assertEquals(1.0f, decoded[0].diffuseColor.red, 0.001f);
    assertEquals(0.0f, decoded[0].diffuseColor.green, 0.001f);
    assertEquals(0.25f, decoded[0].textureCoordinate0.u, 0.001f);
    assertEquals(0.75f, decoded[0].textureCoordinate0.v, 0.001f);

    // Verify positions of remaining vertices
    assertEquals(4.0, decoded[1].position.x(), 0.0001);
    assertEquals(-3.0, decoded[2].position.z(), 0.0001);
  }

  @Test
  public void scenario2_encodeSceneGraphProducesValidZip() {
    // Build a small scene graph hierarchy
    Transformable root = new Transformable();
    Transformable child1 = new Transformable();
    Transformable child2 = new Transformable();
    root.addComponent(child1);
    root.addComponent(child2);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ASG.encode(root, baos);
    byte[] encoded = baos.toByteArray();
    assertTrue("Encoded scene graph should be non-empty", encoded.length > 0);

    // Verify it's a valid ZIP (starts with PK header: 0x50 0x4B)
    assertEquals("ZIP magic byte 1", 0x50, encoded[0] & 0xFF);
    assertEquals("ZIP magic byte 2", 0x4B, encoded[1] & 0xFF);
  }

  @Test
  public void scenario2_emptyArraysRoundtripCleanly() {
    // Edge case: all empty arrays should encode/decode cleanly
    ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    ASG.encodeIntArrayInBinary(new int[]{}, baos1);
    int[] emptyInt = ASG.decodeIntArrayInBinary(new ByteArrayInputStream(baos1.toByteArray()));
    assertNotNull(emptyInt);
    assertEquals(0, emptyInt.length);

    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    ASG.encodeDoubleArrayInBinary(new double[]{}, baos2);
    double[] emptyDouble = ASG.decodeDoubleArrayInBinary(new ByteArrayInputStream(baos2.toByteArray()));
    assertNotNull(emptyDouble);
    assertEquals(0, emptyDouble.length);

    ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
    ASG.encodeVertexArrayInBinary(new Vertex[]{}, baos3);
    Vertex[] emptyVertex = ASG.decodeVertexArrayInBinary(new ByteArrayInputStream(baos3.toByteArray()));
    assertNotNull(emptyVertex);
    assertEquals(0, emptyVertex.length);
  }
}
