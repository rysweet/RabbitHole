package org.lgna.project.virtualmachine;

import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.JavaField;
import org.lgna.project.ast.JavaMethod;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.RelationalInfixExpression;
import org.lgna.project.ast.UserField;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class VmCollaboratorBoundaryCharacterizationTest {
  private ReleaseVirtualMachine vm;

  @Before
  public void setUp() {
    vm = new ReleaseVirtualMachine();
  }

  @Test
  public void javaFieldSetAndGetUseReflectedFieldOnJavaInstance() {
    JavaField field = JavaField.getInstance(JavaFieldBox.class, "value");
    JavaFieldBox box = new JavaFieldBox();

    vm.set(field, box, "updated");

    assertEquals("updated", vm.get(field, box));
    assertEquals("updated", box.value);
  }

  @Test
  public void javaArraySetConvertsUserInstanceToItsJavaPeerBeforeStorage() {
    Object[] values = new Object[1];
    UserInstance instance = vm.ENTRY_POINT_createInstance(VmTestSupport.createTypeWithJavaSuperclass("PeerBacked", PlainJavaPeer.class));

    vm.setItemAtIndex(JavaType.getInstance(Object[].class), values, 0, instance);

    assertEquals(PlainJavaPeer.class, values[0].getClass());
  }

  @Test
  public void javaVarargsMethodReceivesEmptyArrayWhenNoVariableArgumentsAreSupplied() {
    JavaMethod method = JavaMethod.getInstance(VarargsTarget.class, "count", String.class, String[].class);

    Object result = vm.invokeMethodDeclaredInJava(null, method, "prefix");

    assertEquals("prefix:0", result);
  }

  @Test
  public void runningVmRethrowsFieldInitializerFailure() {
    UserField field = new UserField("broken", Boolean.class, failingRelationalExpression());
    UserInstance instance = vm.ENTRY_POINT_createInstance(VmTestSupport.createTypeWithConstructor("RunningInitializerFailure"));

    try {
      vm.createAndSetFieldInstance(instance, field);
      fail("Running VM should rethrow initializer failures");
    } catch (LgnaVmNullPointerException e) {
      assertEquals("right operand is null.", e.getMessage());
    }
  }

  @Test
  public void sceneEditorVmContinuesPastFieldInitializerFailure() {
    vm.setForSceneEditor();
    UserField field = new UserField("broken", Boolean.class, failingRelationalExpression());
    UserInstance instance = vm.ENTRY_POINT_createInstance(VmTestSupport.createTypeWithConstructor("SceneEditorInitializerFailure"));

    vm.createAndSetFieldInstance(instance, field);
  }

  private RelationalInfixExpression failingRelationalExpression() {
    return new RelationalInfixExpression(
        new org.lgna.project.ast.IntegerLiteral(1),
        RelationalInfixExpression.Operator.LESS,
        new NullLiteral(),
        Integer.class,
        Integer.class);
  }

  public static class JavaFieldBox {
    public String value = "initial";
  }

  public static class PlainJavaPeer {
  }

  public static class VarargsTarget {
    public static String count(String prefix, String... values) {
      return prefix + ":" + values.length;
    }
  }
}
