package org.lgna.project.virtualmachine;

import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Comment;
import org.lgna.project.ast.ConstructorBlockStatement;
import org.lgna.project.ast.JavaConstructor;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.NamedUserConstructor;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.SuperConstructorInvocationStatement;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;
import org.lgna.project.ast.UserType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Characterization tests for VirtualMachine StoryAPI adapter dispatch.
 * Documents the current behavior of registerAbstractClassAdapter,
 * ENTRY_POINT_createInstance with adapter, and MethodContext callback.
 *
 * <p>Headless scope: no JavaFX, no gallery assets, no scene graph, no display.
 *
 * @see VirtualMachine#registerAbstractClassAdapter(Class, Class)
 * @see VirtualMachine#createInstance(UserType, UserInstance, java.lang.reflect.Constructor, Object...)
 * @see MethodContext
 */
public class VmStoryApiDispatchCharacterizationTest {

  private ReleaseVirtualMachine vm;
  private NamedUserType type;

  @Before
  public void setUp() {
    vm = new ReleaseVirtualMachine();
    type = createTypeWithJavaSuperclass(TestAbstractBase.class);
  }

  // --- Test-only abstract class and adapter ---

  public static abstract class TestAbstractBase {
    public abstract void doAction();
  }

  public static class TestAdapter extends TestAbstractBase {
    private final MethodContext context;
    private final UserType<?> type;
    private final Object[] arguments;

    public TestAdapter(MethodContext context, UserType<?> type, Object[] arguments) {
      this.context = context;
      this.type = type;
      this.arguments = arguments;
    }

    public MethodContext getContext() { return context; }
    public UserType<?> getUserType() { return type; }
    public Object[] getArguments() { return arguments; }

    @Override
    public void doAction() {
      // no-op for testing
    }
  }

  // --- Adapter registration ---

  @Test
  public void registerAbstractClassAdapterAcceptsAbstractClass() {
    // Should not throw
    vm.registerAbstractClassAdapter(TestAbstractBase.class, TestAdapter.class);
  }

  // --- ENTRY_POINT_createInstance with adapter ---

  @Test
  public void createInstanceWithAdapterReturnsUserInstance() {
    vm.registerAbstractClassAdapter(TestAbstractBase.class, TestAdapter.class);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    assertNotNull("createInstance should succeed with registered adapter", instance);
  }

  @Test
  public void createInstanceWithAdapterSetsJavaInstance() {
    vm.registerAbstractClassAdapter(TestAbstractBase.class, TestAdapter.class);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    Object javaInstance = instance.getJavaInstance();
    assertNotNull("Java instance should be non-null", javaInstance);
    assertTrue("Java instance should be TestAdapter",
        javaInstance instanceof TestAdapter);
  }

  @Test
  public void adapterReceivesMethodContext() {
    vm.registerAbstractClassAdapter(TestAbstractBase.class, TestAdapter.class);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    TestAdapter adapter = (TestAdapter) instance.getJavaInstance();
    assertNotNull("Adapter should receive a MethodContext", adapter.getContext());
  }

  @Test
  public void adapterReceivesUserType() {
    vm.registerAbstractClassAdapter(TestAbstractBase.class, TestAdapter.class);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    TestAdapter adapter = (TestAdapter) instance.getJavaInstance();
    assertEquals("Adapter should receive the UserType", type, adapter.getUserType());
  }

  // --- MethodContext callback ---

  @Test
  public void methodContextCallbackInvokesInstanceMethod() {
    vm.registerAbstractClassAdapter(TestAbstractBase.class, TestAdapter.class);

    // Instance method (not static) — MethodContext passes userInstance as target
    UserMethod instanceHelper = new UserMethod("helperFromCallback", Void.TYPE,
        new UserParameter[0], new BlockStatement(new Comment("callback executed")));
    // NOT setting isStatic — this is an instance method
    type.methods.add(instanceHelper);

    UserInstance instance = vm.ENTRY_POINT_createInstance(type);
    TestAdapter adapter = (TestAdapter) instance.getJavaInstance();
    MethodContext context = adapter.getContext();

    // MethodContext.invokeEntryPoint calls ENTRY_POINT_invoke(userInstance, method)
    // Should not throw for instance methods
    context.invokeEntryPoint(instanceHelper);
    assertNotNull("MethodContext callback should complete without error", context);
  }

  // --- Without adapter, direct constructor invocation ---

  @Test
  public void createInstanceWithoutAdapterUsesDirectConstructor() {
    // type with Object superclass (no adapter registered)
    NamedUserType plainType = createTypeWithObjectSuperclass();
    UserInstance instance = vm.ENTRY_POINT_createInstance(plainType);
    assertNotNull("createInstance without adapter should use direct constructor", instance);
    assertNotNull("Java instance should be an Object", instance.getJavaInstance());
  }

  // --- Helpers ---

  private static NamedUserType createTypeWithJavaSuperclass(Class<?> superclass) {
    NamedUserType userType = new NamedUserType();
    userType.name.setValue("VmStoryApiTestType");
    userType.superType.setValue(JavaType.getInstance(superclass));

    JavaConstructor superConstructor = JavaConstructor.getInstance(superclass);
    SuperConstructorInvocationStatement superCall = new SuperConstructorInvocationStatement(superConstructor);
    ConstructorBlockStatement constructorBody = new ConstructorBlockStatement(superCall);
    NamedUserConstructor constructor = new NamedUserConstructor(new UserParameter[0], constructorBody);
    userType.constructors.add(constructor);

    return userType;
  }

  private static NamedUserType createTypeWithObjectSuperclass() {
    NamedUserType userType = new NamedUserType();
    userType.name.setValue("VmStoryApiPlainType");
    userType.superType.setValue(JavaType.OBJECT_TYPE);

    JavaConstructor objectConstructor = JavaConstructor.getInstance(Object.class);
    SuperConstructorInvocationStatement superCall = new SuperConstructorInvocationStatement(objectConstructor);
    ConstructorBlockStatement constructorBody = new ConstructorBlockStatement(superCall);
    NamedUserConstructor constructor = new NamedUserConstructor(new UserParameter[0], constructorBody);
    userType.constructors.add(constructor);

    return userType;
  }
}
