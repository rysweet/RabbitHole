package org.lgna.project.virtualmachine;

import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.AssignmentExpression;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.ConstructorBlockStatement;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.IntegerLiteral;
import org.lgna.project.ast.JavaConstructor;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.LocalAccess;
import org.lgna.project.ast.LocalDeclarationStatement;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.ParameterAccess;
import org.lgna.project.ast.ReturnStatement;
import org.lgna.project.ast.StringLiteral;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.ThisExpression;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Characterization tests for VirtualMachine field access, parameter access,
 * and assignment expression evaluation.
 * Documents the current behavior of UserField get/set, FieldAccess,
 * AssignmentExpression, ParameterAccess, and local assignment.
 *
 * <p>Headless scope: no JavaFX, no gallery assets, no scene graph, no display.
 *
 * @see VirtualMachine#getUserField(UserField, Object)
 * @see VirtualMachine#setUserField(UserField, Object, Object)
 * @see VirtualMachine#evaluateFieldAccess(FieldAccess)
 */
public class VmFieldAccessCharacterizationTest {

  private ReleaseVirtualMachine vm;
  private NamedUserType type;

  @Before
  public void setUp() {
    vm = new ReleaseVirtualMachine();
    type = createTypeWithConstructor();
  }

  // --- UserInstance creation ---

  @Test
  public void createInstanceReturnsNonNull() {
    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    assertNotNull("ENTRY_POINT_createInstance should return a UserInstance", instance);
  }

  // --- Direct field get/set via VM ---

  @Test
  public void getFieldReturnsNullBeforeSet() {
    UserField field = new UserField("score", Integer.class, new NullLiteral());
    type.fields.add(field);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);

    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(instance, field);
    Object value = vm.get(field, instance);
    assertNull("Field initialized with NullLiteral should be null", value);
  }

  @Test
  public void setFieldThenGetReturnsNewValue() {
    UserField field = new UserField("score", Integer.class, new NullLiteral());
    type.fields.add(field);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    vm.set(field, instance, 42);

    Object value = vm.get(field, instance);
    assertEquals("Field should hold the value set", 42, value);
  }

  @Test
  public void setFieldOverwritesPreviousValue() {
    UserField field = new UserField("label", JavaType.STRING_TYPE, new StringLiteral("old"));
    type.fields.add(field);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(instance, field);

    assertEquals("old", vm.get(field, instance));

    vm.set(field, instance, "new");
    assertEquals("new", vm.get(field, instance));
  }

  // --- FieldAccess via instance method ---

  @Test
  public void fieldAccessReturnsFieldValueFromInstanceMethod() {
    UserField field = new UserField("counter", Integer.class, new NullLiteral());
    type.fields.add(field);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    vm.set(field, instance, 7);

    // Instance method: return this.counter
    ThisExpression thisExpr = new ThisExpression();
    FieldAccess access = new FieldAccess(thisExpr, field);
    ReturnStatement ret = new ReturnStatement(JavaType.getInstance(Integer.class), access);
    UserMethod getter = new UserMethod("getCounter", Integer.class, new UserParameter[0],
        new BlockStatement(ret));
    type.methods.add(getter);

    Object result = vm.ENTRY_POINT_invoke(instance, getter);
    assertEquals("FieldAccess should return the field value", 7, result);
  }

  // --- AssignmentExpression with FieldAccess LHS ---

  @Test
  public void assignmentExpressionSetsFieldValue() {
    UserField field = new UserField("counter", Integer.class, new NullLiteral());
    type.fields.add(field);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    vm.set(field, instance, 0);

    // Instance method body: this.counter = 99
    ThisExpression thisExpr = new ThisExpression();
    FieldAccess lhs = new FieldAccess(thisExpr, field);
    AssignmentExpression assign = new AssignmentExpression(
        JavaType.getInstance(Integer.class), lhs,
        AssignmentExpression.Operator.ASSIGN, new IntegerLiteral(99));

    UserMethod setter = new UserMethod("setCounter", Void.TYPE, new UserParameter[0],
        new BlockStatement(new ExpressionStatement(assign)));
    type.methods.add(setter);

    vm.ENTRY_POINT_invoke(instance, setter);

    assertEquals("AssignmentExpression should update the field", 99, vm.get(field, instance));
  }

  // --- AssignmentExpression with LocalAccess LHS ---

  @Test
  public void assignmentExpressionSetsLocalValue() {
    UserLocal local = new UserLocal("x", Integer.class, false);
    LocalDeclarationStatement decl = new LocalDeclarationStatement(local, new IntegerLiteral(0));
    LocalAccess localLhs = new LocalAccess(local);
    AssignmentExpression assign = new AssignmentExpression(
        JavaType.getInstance(Integer.class), localLhs,
        AssignmentExpression.Operator.ASSIGN, new IntegerLiteral(77));
    ReturnStatement ret = new ReturnStatement(
        JavaType.getInstance(Integer.class), new LocalAccess(local));

    UserMethod method = new UserMethod("localAssign", Integer.class,
        new UserParameter[0], new BlockStatement(decl, new ExpressionStatement(assign), ret));
    method.isStatic.setValue(true);
    type.methods.add(method);

    Object result = vm.ENTRY_POINT_invoke(null, method);
    assertEquals("Local assignment should update the local variable", 77, result);
  }

  // --- ParameterAccess ---

  @Test
  public void parameterAccessReturnsPassedArgument() {
    UserParameter param = new UserParameter("n", Integer.class);
    ParameterAccess access = new ParameterAccess(param);
    ReturnStatement ret = new ReturnStatement(JavaType.getInstance(Integer.class), access);

    UserMethod method = new UserMethod("identity", Integer.class,
        new UserParameter[]{param}, new BlockStatement(ret));
    method.isStatic.setValue(true);
    type.methods.add(method);

    Object result = vm.ENTRY_POINT_invoke(null, method, 42);
    assertEquals("ParameterAccess should return the passed argument", 42, result);
  }

  @Test
  public void parameterAccessWithMultipleParams() {
    UserParameter a = new UserParameter("a", Integer.class);
    UserParameter b = new UserParameter("b", Integer.class);
    ParameterAccess accessB = new ParameterAccess(b);
    ReturnStatement ret = new ReturnStatement(JavaType.getInstance(Integer.class), accessB);

    UserMethod method = new UserMethod("second", Integer.class,
        new UserParameter[]{a, b}, new BlockStatement(ret));
    method.isStatic.setValue(true);
    type.methods.add(method);

    Object result = vm.ENTRY_POINT_invoke(null, method, 10, 20);
    assertEquals("ParameterAccess should return the correct parameter", 20, result);
  }

  // --- Field initialization via createAndSetFieldInstance ---

  @Test
  public void createAndSetFieldInstanceInitializesFromExpression() {
    UserField field = new UserField("score", Integer.class, new IntegerLiteral(100));
    type.fields.add(field);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    vm.ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField(instance, field);

    assertEquals("Field should be initialized to literal value", 100, vm.get(field, instance));
  }

  // --- Two fields, independent values ---

  @Test
  public void twoFieldsHoldIndependentValues() {
    UserField fieldA = new UserField("a", Integer.class, new NullLiteral());
    UserField fieldB = new UserField("b", Integer.class, new NullLiteral());
    type.fields.add(fieldA);
    type.fields.add(fieldB);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    vm.set(fieldA, instance, 1);
    vm.set(fieldB, instance, 2);

    assertEquals(1, vm.get(fieldA, instance));
    assertEquals(2, vm.get(fieldB, instance));
  }

  // --- Two instances, independent field values ---

  @Test
  public void twoInstancesHaveIndependentFieldValues() {
    UserField field = new UserField("value", Integer.class, new NullLiteral());
    type.fields.add(field);

    UserInstance inst1 = vm.ENTRY_POINT_createInstance(type);
    UserInstance inst2 = vm.ENTRY_POINT_createInstance(type);
    vm.set(field, inst1, 10);
    vm.set(field, inst2, 20);

    assertEquals(10, vm.get(field, inst1));
    assertEquals(20, vm.get(field, inst2));
  }

  // --- Helpers ---

  private static NamedUserType createTypeWithConstructor() {
    NamedUserType userType = new NamedUserType();
    userType.name.setValue("VmFieldTestType");
    userType.superType.setValue(JavaType.OBJECT_TYPE);

    JavaConstructor objectConstructor = JavaConstructor.getInstance(Object.class);
    SuperConstructorInvocationStatement superCall = new SuperConstructorInvocationStatement(objectConstructor);
    ConstructorBlockStatement constructorBody = new ConstructorBlockStatement(superCall);
    NamedUserConstructor constructor = new NamedUserConstructor(new UserParameter[0], constructorBody);
    userType.constructors.add(constructor);

    return userType;
  }
}
