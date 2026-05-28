package org.lgna.ik.poser;

import org.junit.Test;
import org.lgna.project.ast.*;

import static org.junit.Assert.*;

/**
 * Tests for {@link PoseAstUtilities} — pose-related AST utility methods.
 */
public class PoseAstUtilitiesTest {

  @Test
  public void strikePoseMethodName_isCorrect() {
    assertEquals("strikePose", PoseAstUtilities.STRIKE_POSE_METHOD_NAME);
  }

  @Test
  public void isStrikePoseMethod_nullMethod_returnsFalse() {
    assertFalse(PoseAstUtilities.isStrikePoseMethod(null));
  }

  @Test
  public void isStrikePoseMethod_userMethod_returnsFalse() {
    UserMethod method = new UserMethod();
    method.name.setValue("strikePose");
    method.returnType.setValue(JavaType.VOID_TYPE);
    method.managementLevel.setValue(ManagementLevel.NONE);
    // Only JavaMethod instances can return true
    assertFalse(PoseAstUtilities.isStrikePoseMethod(method));
  }

  @Test
  public void isStrikePoseMethod_wrongNameUserMethod_returnsFalse() {
    UserMethod method = new UserMethod();
    method.name.setValue("doSomething");
    method.returnType.setValue(JavaType.VOID_TYPE);
    method.managementLevel.setValue(ManagementLevel.NONE);
    assertFalse(PoseAstUtilities.isStrikePoseMethod(method));
  }
}
