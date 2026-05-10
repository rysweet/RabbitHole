package org.alice.serialization.tweedle;

import org.junit.Test;
import org.lgna.project.code.IdentifiableTweedleNode;
import org.lgna.project.code.InstantiableTweedleNode;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Contract tests verifying the Encoder → TweedleEncoder rename is complete
 * and consistent across all interface boundaries (issue #480).
 *
 * <p>These tests use reflection to verify:
 * <ul>
 *   <li>TweedleEncoder class is loadable and properly named</li>
 *   <li>Old Encoder class is gone (no ambiguous references)</li>
 *   <li>Interface method signatures reference TweedleEncoder</li>
 *   <li>TweedleEncoderDecoder facade instantiates TweedleEncoder</li>
 *   <li>Story-api implementors compile with TweedleEncoder param types</li>
 * </ul>
 *
 * <p>Written before the rename — all tests fail until implementation is complete.
 */
public class TweedleEncoderRenameContractTest {

  // ═══════════════════════════════════════════════════════════════════════════
  // CLASS-LEVEL RENAME VERIFICATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void tweedleEncoderClassIsLoadable() throws Exception {
    Class<?> clazz = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    assertNotNull("TweedleEncoder class must be loadable", clazz);
  }

  @Test
  public void oldEncoderClassInTweedlePackageIsGone() {
    // The old Encoder.java in org.alice.serialization.tweedle must not exist.
    // Note: org.lgna.project.io.Encoder and java.beans.Encoder are separate
    // classes that must NOT be affected by this rename.
    try {
      Class<?> clazz = Class.forName("org.alice.serialization.tweedle.Encoder");
      fail("Old org.alice.serialization.tweedle.Encoder class must not exist after rename, but was found: " + clazz);
    } catch (ClassNotFoundException expected) {
      // This is the correct outcome — the old class should be gone
    }
  }

  @Test
  public void tweedleEncoderSimpleNameIsCorrect() throws Exception {
    Class<?> clazz = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    assertEquals("TweedleEncoder", clazz.getSimpleName());
  }

  @Test
  public void tweedleEncoderSuperclassIsSourceCodeGenerator() throws Exception {
    Class<?> clazz = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    assertEquals("SourceCodeGenerator", clazz.getSuperclass().getSimpleName());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // INTERFACE PARAMETER TYPE VERIFICATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void identifiableTweedleNodeGetCodeIdentifierAcceptsTweedleEncoder() throws Exception {
    Method method = IdentifiableTweedleNode.class.getMethod("getCodeIdentifier",
        Class.forName("org.alice.serialization.tweedle.TweedleEncoder"));
    assertNotNull("IdentifiableTweedleNode.getCodeIdentifier must accept TweedleEncoder param", method);
    assertEquals("Return type must be String", String.class, method.getReturnType());
  }

  @Test
  public void instantiableTweedleNodeEncodeDefinitionAcceptsTweedleEncoder() throws Exception {
    Method method = InstantiableTweedleNode.class.getMethod("encodeDefinition",
        Class.forName("org.alice.serialization.tweedle.TweedleEncoder"));
    assertNotNull("InstantiableTweedleNode.encodeDefinition must accept TweedleEncoder param", method);
    assertEquals("Return type must be void", void.class, method.getReturnType());
  }

  @Test
  public void identifiableTweedleNodeHasExactlyOneMethod() {
    Method[] methods = IdentifiableTweedleNode.class.getDeclaredMethods();
    assertEquals("IdentifiableTweedleNode must declare exactly one method", 1, methods.length);
    assertEquals("getCodeIdentifier", methods[0].getName());
  }

  @Test
  public void instantiableTweedleNodeHasExactlyOneMethod() {
    Method[] methods = InstantiableTweedleNode.class.getDeclaredMethods();
    assertEquals("InstantiableTweedleNode must declare exactly one method", 1, methods.length);
    assertEquals("encodeDefinition", methods[0].getName());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // STORY-API IMPLEMENTOR VERIFICATION
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void jointIdImplementsBothInterfaces() throws Exception {
    Class<?> jointId = Class.forName("org.lgna.story.resources.JointId");
    assertTrue("JointId must implement IdentifiableTweedleNode",
        IdentifiableTweedleNode.class.isAssignableFrom(jointId));
    assertTrue("JointId must implement InstantiableTweedleNode",
        InstantiableTweedleNode.class.isAssignableFrom(jointId));
  }

  @Test
  public void jointIdEncodeDefinitionHasTweedleEncoderParam() throws Exception {
    Class<?> jointId = Class.forName("org.lgna.story.resources.JointId");
    Class<?> encoderClass = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = jointId.getMethod("encodeDefinition", encoderClass);
    assertNotNull("JointId.encodeDefinition must accept TweedleEncoder", method);
  }

  @Test
  public void jointIdGetCodeIdentifierHasTweedleEncoderParam() throws Exception {
    Class<?> jointId = Class.forName("org.lgna.story.resources.JointId");
    Class<?> encoderClass = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = jointId.getMethod("getCodeIdentifier", encoderClass);
    assertNotNull("JointId.getCodeIdentifier must accept TweedleEncoder", method);
  }

  @Test
  public void dynamicJointIdExtendsJointId() throws Exception {
    Class<?> dynamicJointId = Class.forName("org.lgna.story.resources.DynamicJointId");
    Class<?> jointId = Class.forName("org.lgna.story.resources.JointId");
    assertTrue("DynamicJointId must extend JointId",
        jointId.isAssignableFrom(dynamicJointId));
  }

  @Test
  public void dynamicJointIdOverridesGetCodeIdentifierWithTweedleEncoder() throws Exception {
    Class<?> dynamicJointId = Class.forName("org.lgna.story.resources.DynamicJointId");
    Class<?> encoderClass = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = dynamicJointId.getMethod("getCodeIdentifier", encoderClass);
    assertNotNull("DynamicJointId.getCodeIdentifier must accept TweedleEncoder", method);
    assertEquals("DynamicJointId must declare its own getCodeIdentifier",
        dynamicJointId, method.getDeclaringClass());
  }

  @Test
  public void jointArrayIdImplementsInstantiableTweedleNode() throws Exception {
    Class<?> jointArrayId = Class.forName("org.lgna.story.resources.JointArrayId");
    assertTrue("JointArrayId must implement InstantiableTweedleNode",
        InstantiableTweedleNode.class.isAssignableFrom(jointArrayId));
  }

  @Test
  public void jointArrayIdEncodeDefinitionHasTweedleEncoderParam() throws Exception {
    Class<?> jointArrayId = Class.forName("org.lgna.story.resources.JointArrayId");
    Class<?> encoderClass = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = jointArrayId.getMethod("encodeDefinition", encoderClass);
    assertNotNull("JointArrayId.encodeDefinition must accept TweedleEncoder", method);
  }

  @Test
  public void jointIdTransformationPairImplementsInstantiableTweedleNode() throws Exception {
    Class<?> pair = Class.forName("org.lgna.story.implementation.JointIdTransformationPair");
    assertTrue("JointIdTransformationPair must implement InstantiableTweedleNode",
        InstantiableTweedleNode.class.isAssignableFrom(pair));
  }

  @Test
  public void jointIdTransformationPairEncodeDefinitionHasTweedleEncoderParam() throws Exception {
    Class<?> pair = Class.forName("org.lgna.story.implementation.JointIdTransformationPair");
    Class<?> encoderClass = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = pair.getMethod("encodeDefinition", encoderClass);
    assertNotNull("JointIdTransformationPair.encodeDefinition must accept TweedleEncoder", method);
  }

  @Test
  public void poseImplementsInstantiableTweedleNode() throws Exception {
    Class<?> pose = Class.forName("org.lgna.story.Pose");
    assertTrue("Pose must implement InstantiableTweedleNode",
        InstantiableTweedleNode.class.isAssignableFrom(pose));
  }

  @Test
  public void poseEncodeDefinitionHasTweedleEncoderParam() throws Exception {
    Class<?> pose = Class.forName("org.lgna.story.Pose");
    Class<?> encoderClass = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = pose.getMethod("encodeDefinition", encoderClass);
    assertNotNull("Pose.encodeDefinition must accept TweedleEncoder", method);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TWEEDLE ENCODER PUBLIC API SURFACE
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void tweedleEncoderHasEncodeMethod() throws Exception {
    Class<?> clazz = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Class<?> processable = Class.forName("org.lgna.project.code.ProcessableNode");
    Method encode = clazz.getMethod("encode", processable);
    assertNotNull("TweedleEncoder must have encode(ProcessableNode)", encode);
    assertEquals("encode must return String", String.class, encode.getReturnType());
  }

  @Test
  public void tweedleEncoderHasGetFieldReferenceMethod() throws Exception {
    Class<?> clazz = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = clazz.getMethod("getFieldReference", String.class, String.class);
    assertNotNull("TweedleEncoder must have getFieldReference(String, String)", method);
    assertEquals("getFieldReference must return String", String.class, method.getReturnType());
  }

  @Test
  public void tweedleEncoderHasGetUserJointIdentifierMethod() throws Exception {
    Class<?> clazz = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = clazz.getMethod("getUserJointIdentifier", String.class);
    assertNotNull("TweedleEncoder must have getUserJointIdentifier(String)", method);
    assertEquals("getUserJointIdentifier must return String", String.class, method.getReturnType());
  }

  @Test
  public void tweedleEncoderHasAppendNewJointIdMethod() throws Exception {
    Class<?> clazz = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = clazz.getMethod("appendNewJointId", String.class, String.class);
    assertNotNull("TweedleEncoder must have appendNewJointId(String, String)", method);
  }

  @Test
  public void tweedleEncoderHasAppendNewJointArrayIdMethod() throws Exception {
    Class<?> clazz = Class.forName("org.alice.serialization.tweedle.TweedleEncoder");
    Method method = clazz.getMethod("appendNewJointArrayId", String.class, String.class);
    assertNotNull("TweedleEncoder must have appendNewJointArrayId(String, String)", method);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // FACADE WIRING — TweedleEncoderDecoder must instantiate TweedleEncoder
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  public void facadeEncodeProcessableUsesEncoderFromTweedlePackage() throws Exception {
    // The facade must produce output — this can only work if the internal
    // instantiation of TweedleEncoder (not old Encoder) compiles and runs.
    TweedleEncoderDecoder facade = new TweedleEncoderDecoder();
    org.lgna.project.ast.AbstractNode node = facade.decode("class FacadeWiringCheck {}");
    assertTrue(node instanceof org.lgna.project.ast.NamedUserType);

    String encoded = facade.encodeProcessable((org.lgna.project.code.ProcessableNode) node);
    assertNotNull("Facade must produce non-null encoded output", encoded);
    assertTrue("Facade must produce non-empty output", encoded.length() > 0);
    assertTrue("Facade output must contain class name", encoded.contains("FacadeWiringCheck"));
  }
}
