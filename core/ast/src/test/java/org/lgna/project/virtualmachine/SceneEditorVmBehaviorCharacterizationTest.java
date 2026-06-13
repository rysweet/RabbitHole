package org.lgna.project.virtualmachine;

import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.AstUtilities;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaMethod;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.ManagementLevel;
import org.lgna.project.ast.MethodInvocation;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class SceneEditorVmBehaviorCharacterizationTest {

  private ReleaseVirtualMachine vm;
  private NamedUserType type;

  @Before
  public void setUp() {
    vm = new ReleaseVirtualMachine();
    type = VmTestSupport.createTypeWithConstructor("SceneEditorVmBehaviorType");
  }

  @Test
  public void fieldLookupUsesManagedJavaInstancesOnly() {
    UserField managed = new UserField("managed", String.class, new StringLiteral("managed-value"));
    managed.managementLevel.setValue(ManagementLevel.MANAGED);
    UserField unmanaged = new UserField("unmanaged", String.class, new StringLiteral("unmanaged-value"));
    type.fields.add(managed);
    type.fields.add(unmanaged);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    instance.ensureInverseMapExists();

    assertEquals(managed, instance.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_getFieldForInstanceInJava("managed-value"));
    assertNull(instance.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_getFieldForInstanceInJava("unmanaged-value"));
  }

  @Test
  public void initializedManagedFieldIsAddedToExistingLookup() {
    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    instance.ensureInverseMapExists();

    UserField field = new UserField("added", String.class, new StringLiteral("added-value"));
    field.managementLevel.setValue(ManagementLevel.MANAGED);
    type.fields.add(field);

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(instance, field);

    assertEquals(field, instance.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_getFieldForInstanceInJava("added-value"));
  }

  @Test
  public void reassigningManagedFieldPreservesPreviousLookupEntry() {
    UserField field = new UserField("managed", String.class, new StringLiteral("first-value"));
    field.managementLevel.setValue(ManagementLevel.MANAGED);
    type.fields.add(field);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    instance.ensureInverseMapExists();
    vm.set(field, instance, "second-value");

    assertEquals(field, instance.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_getFieldForInstanceInJava("first-value"));
    assertEquals(field, instance.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_getFieldForInstanceInJava("second-value"));
  }

  @Test
  public void sceneEditorStatementExecutionFiresEvents() {
    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    VmTestSupport.RecordingListener listener = new VmTestSupport.RecordingListener();
    vm.addVirtualMachineListener(listener);
    BlockStatement statement = new BlockStatement(new Comment("scene-editor statement"));

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(instance, statement);

    assertEquals(Arrays.asList(
        "executing:BlockStatement",
        "executing:Comment",
        "executed:Comment",
        "executed:BlockStatement"),
        listener.statementEvents);
  }

  @Test
  public void sceneEditorFieldInitializationPreservesInitializerValue() {
    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    UserField field = new UserField("score", Integer.class, new IntegerLiteral(7));
    type.fields.add(field);

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(instance, field);

    assertEquals(7, vm.get(field, instance));
  }

  @Test
  public void runningWorldEvaluationFailsFastOnMethodInvocationError() {
    assertThrows(LgnaVmMethodInvocationException.class,
        () -> vm.ENTRY_POINT_evaluate(null, new Expression[]{failingInvocation()}));
  }

  @Test
  public void sceneEditorEvaluationContinuesPastMethodInvocationError() {
    vm.setForSceneEditor();

    Object[] results = vm.ENTRY_POINT_evaluate(null, new Expression[]{failingInvocation(), new IntegerLiteral(9)});

    assertNull(results[0]);
    assertEquals(9, results[1]);
  }

  @Test
  public void runningWorldInvocationFailsFastOnMethodInvocationError() {
    UserMethod method = VmTestSupport.createStaticProcedure("failsFast", new BlockStatement(
        new ExpressionStatement(failingInvocation())));

    assertThrows(LgnaVmMethodInvocationException.class, () -> vm.ENTRY_POINT_invoke(null, method));
  }

  @Test
  public void sceneEditorInvocationContinuesPastMethodInvocationError() {
    vm.setForSceneEditor();
    UserMethod method = VmTestSupport.createStaticProcedure("continues", new BlockStatement(
        new ExpressionStatement(failingInvocation())));

    assertNull(vm.ENTRY_POINT_invoke(null, method));
  }

  @Test
  public void sceneEditorStatementExecutionContinuesPastMethodInvocationError() {
    vm.setForSceneEditor();
    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    VmTestSupport.RecordingListener listener = new VmTestSupport.RecordingListener();
    vm.addVirtualMachineListener(listener);

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(instance, new BlockStatement(
        new ExpressionStatement(failingInvocation()),
        new Comment("still executes")));

    assertEquals(Arrays.asList(
        "executing:BlockStatement",
        "executing:ExpressionStatement",
        "executed:ExpressionStatement",
        "executing:Comment",
        "executed:Comment",
        "executed:BlockStatement"),
        listener.statementEvents);
  }

  @Test
  public void sceneEditorLocalDeclarationContinuesPastInitializerInvocationError() {
    vm.setForSceneEditor();
    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    VmTestSupport.RecordingListener listener = new VmTestSupport.RecordingListener();
    vm.addVirtualMachineListener(listener);
    UserLocal local = new UserLocal("brokenLocal", Object.class, false);

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement(instance, new BlockStatement(
        new LocalDeclarationStatement(local, failingInvocation()),
        new Comment("still executes")));

    assertEquals(Arrays.asList(
        "executing:BlockStatement",
        "executing:LocalDeclarationStatement",
        "executed:LocalDeclarationStatement",
        "executing:Comment",
        "executed:Comment",
        "executed:BlockStatement"),
        listener.statementEvents);
  }

  @Test
  public void runningWorldFieldInitializationFailsFastOnInitializerError() {
    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    UserField field = new UserField("broken", Object.class, failingInvocation());

    assertThrows(LgnaVmMethodInvocationException.class, () -> vm.createAndSetFieldInstance(instance, field));
  }

  @Test
  public void sceneEditorFieldInitializationContinuesPastInitializerError() {
    vm.setForSceneEditor();
    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    UserField field = new UserField("broken", Object.class, failingInvocation());

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(instance, field);

    assertNull(vm.get(field, instance));
  }

  private static MethodInvocation failingInvocation() {
    JavaMethod method = JavaMethod.getInstance(ThrowingInvocationTarget.class, "fail");
    return AstUtilities.createStaticMethodInvocation(method);
  }

  public static class ThrowingInvocationTarget {
    public static void fail() {
      throw new IllegalStateException("boom");
    }
  }
}
