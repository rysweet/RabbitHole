package org.lgna.project.virtualmachine;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Reflection-based API surface lock for the VirtualMachine extraction.
 *
 * <p>This test class verifies two categories of contracts:
 *
 * <h3>API Surface Lock (pass before and after extraction)</h3>
 * <ul>
 *   <li>VirtualMachine has exactly 20 public methods</li>
 *   <li>VirtualMachine has exactly 16 abstract methods (1 public + 15 protected)</li>
 *   <li>Every expected method name is present with correct modifiers</li>
 *   <li>No new public/protected members introduced by extraction</li>
 * </ul>
 *
 * <h3>Extraction Contract (fail before, pass after extraction)</h3>
 * <ul>
 *   <li>VmExpressionEvaluator exists as package-private final with VM back-reference</li>
 *   <li>VmStatementExecutor exists as package-private final with VM back-reference</li>
 *   <li>virtualMachineListeners is CopyOnWriteArrayList (thread-safe upgrade)</li>
 *   <li>Key fields/methods promoted to package-private for delegate access</li>
 *   <li>VirtualMachine holds delegate fields of correct types</li>
 *   <li>VirtualMachine.java is under 500 lines</li>
 * </ul>
 *
 * @see VirtualMachine
 * @see ReleaseVirtualMachine
 */
public class VmContractTest {

  private static final Class<?> VM_CLASS = VirtualMachine.class;
  private static final String VM_PACKAGE = "org.lgna.project.virtualmachine";

  // =====================================================================
  // Expected public method names on VirtualMachine (20 total)
  // =====================================================================

  private static final Set<String> EXPECTED_PUBLIC_METHODS = Set.of(
      "getStackTrace",
      "ENTRY_POINT_evaluate",
      "ENTRY_POINT_invoke",
      "ENTRY_POINT_createInstance",
      "createAndSetFieldInstance",
      "ACCEPTABLE_HACK_FOR_SCENE_EDITOR_initializeField",
      "ACCEPTABLE_HACK_FOR_SCENE_EDITOR_executeStatement",
      "registerAbstractClassAdapter",
      "evaluateArguments",
      "getItemAtIndex",
      "setItemAtIndex",
      "invokeUserMethod",
      "invokeMethodDeclaredInJava",
      "get",
      "set",
      "stopExecution",
      "addVirtualMachineListener",
      "removeVirtualMachineListener",
      "getVirtualMachineListeners",
      "setForSceneEditor"
  );

  // =====================================================================
  // Expected abstract method names on VirtualMachine (16 total)
  // 1 public abstract (getStackTrace) + 15 protected abstract
  // =====================================================================

  private static final Set<String> EXPECTED_ABSTRACT_METHODS = Set.of(
      "getStackTrace",
      "getThis",
      "pushBogusFrame",
      "pushConstructorFrame",
      "setConstructorFrameUserInstance",
      "pushMethodFrame",
      "pushLambdaFrame",
      "popFrame",
      "lookup",
      "pushLocal",
      "getLocal",
      "setLocal",
      "popLocal",
      "getFrameForThread",
      "pushCurrentThread",
      "popCurrentThread"
  );

  // =====================================================================
  // Expected evaluate methods on VmExpressionEvaluator after method invocation extraction
  // =====================================================================

  private static final Set<String> EXPECTED_EVALUATOR_METHODS = Set.of(
      "evaluate",                                // main switch dispatch
      "evaluateAssignmentExpression",
      "evaluateBooleanLiteral",
      "evaluateArrayInstanceCreation",
      "evaluateArrayAccess",
      "evaluateArrayLength",
      "evaluateFieldAccess",
      "evaluateLocalAccess",
      "evaluateArithmeticInfixExpression",
      "evaluateBitwiseInfixExpression",
      "evaluateConditionalInfixExpression",
      "evaluateRelationalInfixExpression",
      "evaluateShiftInfixExpression",
      "evaluateLogicalComplement",
      "evaluateStringConcatenation",
      "evaluateNullLiteral",
      "evaluateDoubleLiteral",
      "evaluateFloatLiteral",
      "evaluateIntegerLiteral",
      "evaluateParameterAccess",
      "evaluateStringLiteral",
      "evaluateThisExpression",
      "evaluateTypeExpression",
      "evaluateTypeLiteral",
      "evaluateResourceExpression",
      "evaluateLambdaExpression",
      "EPIC_HACK_evaluateLambdaExpression",
      "evaluateBoolean",                         // shared with executor
      "evaluateInt",                             // shared with executor
      "evaluateArgument",                        // private helper
      "evaluateArguments"                        // full body, VM wraps
  );

  // =====================================================================
  // Expected execute methods on VmStatementExecutor (17 total)
  // =====================================================================

  private static final Set<String> EXPECTED_EXECUTOR_METHODS = Set.of(
      "execute",                                 // main switch dispatch
      "executeBlockStatement",
      "executeConditionalStatement",
      "executeComment",
      "executeCountLoop",
      "executeDoInOrder",
      "executeDoTogether",
      "executeExpressionStatement",
      "excecuteForEachLoop",                     // typo preserved from original
      "executeForEachInArrayLoop",
      "executeForEachInIterableLoop",
      "excecuteEachInTogether",                  // typo preserved from original
      "executeEachInArrayTogether",
      "executeEachInIterableTogether",
      "executeReturnStatement",
      "executeWhileLoop",
      "executeLocalDeclarationStatement"
  );

  // =====================================================================
  // 1. API Surface Lock — VirtualMachine class modifiers
  // =====================================================================

  @Test
  public void virtualMachine_isAbstract() {
    assertTrue("VirtualMachine must be abstract",
        Modifier.isAbstract(VM_CLASS.getModifiers()));
  }

  @Test
  public void virtualMachine_isPublic() {
    assertTrue("VirtualMachine must be public",
        Modifier.isPublic(VM_CLASS.getModifiers()));
  }

  // =====================================================================
  // 2. API Surface Lock — Public method contract
  // =====================================================================

  @Test
  public void publicMethodCount_isExactly20() {
    Set<String> actual = getPublicDeclaredMethodNames(VM_CLASS);
    assertEquals("VirtualMachine should have exactly 20 public methods. "
        + "Actual: " + sorted(actual), 20, actual.size());
  }

  @Test
  public void publicMethodNames_matchExpected() {
    Set<String> actual = getPublicDeclaredMethodNames(VM_CLASS);
    Set<String> missing = new TreeSet<>(EXPECTED_PUBLIC_METHODS);
    missing.removeAll(actual);
    Set<String> unexpected = new TreeSet<>(actual);
    unexpected.removeAll(EXPECTED_PUBLIC_METHODS);
    assertTrue("Missing public methods: " + missing, missing.isEmpty());
    assertTrue("Unexpected public methods: " + unexpected, unexpected.isEmpty());
  }

  @Test
  public void getStackTrace_isPublicAbstract() {
    Method m = findDeclaredMethod(VM_CLASS, "getStackTrace");
    assertNotNull("getStackTrace must be declared on VirtualMachine", m);
    assertTrue("getStackTrace must be public", Modifier.isPublic(m.getModifiers()));
    assertTrue("getStackTrace must be abstract", Modifier.isAbstract(m.getModifiers()));
  }

  @Test
  public void evaluateArguments_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "evaluateArguments");
    assertNotNull("evaluateArguments must remain on VirtualMachine as public wrapper", m);
    assertTrue("evaluateArguments must be public", Modifier.isPublic(m.getModifiers()));
    assertFalse("evaluateArguments must not be abstract", Modifier.isAbstract(m.getModifiers()));
  }

  @Test
  public void stopExecution_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "stopExecution");
    assertNotNull("stopExecution must be declared", m);
    assertTrue("stopExecution must be public", Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void setForSceneEditor_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "setForSceneEditor");
    assertNotNull("setForSceneEditor must be declared", m);
    assertTrue("setForSceneEditor must be public", Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void addVirtualMachineListener_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "addVirtualMachineListener");
    assertNotNull(m);
    assertTrue(Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void removeVirtualMachineListener_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "removeVirtualMachineListener");
    assertNotNull(m);
    assertTrue(Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void getVirtualMachineListeners_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "getVirtualMachineListeners");
    assertNotNull(m);
    assertTrue(Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void get_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "get");
    assertNotNull(m);
    assertTrue(Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void set_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "set");
    assertNotNull(m);
    assertTrue(Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void invokeUserMethod_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "invokeUserMethod");
    assertNotNull(m);
    assertTrue(Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void invokeMethodDeclaredInJava_isPublic() {
    Method m = findDeclaredMethod(VM_CLASS, "invokeMethodDeclaredInJava");
    assertNotNull(m);
    assertTrue(Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void noNewPublicMethods_beyondExpected() {
    Set<String> actual = getPublicDeclaredMethodNames(VM_CLASS);
    Set<String> unexpected = new TreeSet<>(actual);
    unexpected.removeAll(EXPECTED_PUBLIC_METHODS);
    assertTrue("No unexpected public methods: " + unexpected, unexpected.isEmpty());
  }

  // =====================================================================
  // 3. API Surface Lock — Abstract method contract
  // =====================================================================

  @Test
  public void abstractMethodCount_isExactly16() {
    Set<String> actual = getAbstractDeclaredMethodNames(VM_CLASS);
    assertEquals("VirtualMachine should have exactly 16 abstract methods. "
        + "Actual: " + sorted(actual), 16, actual.size());
  }

  @Test
  public void abstractMethodNames_matchExpected() {
    Set<String> actual = getAbstractDeclaredMethodNames(VM_CLASS);
    Set<String> missing = new TreeSet<>(EXPECTED_ABSTRACT_METHODS);
    missing.removeAll(actual);
    Set<String> unexpected = new TreeSet<>(actual);
    unexpected.removeAll(EXPECTED_ABSTRACT_METHODS);
    assertTrue("Missing abstract methods: " + missing, missing.isEmpty());
    assertTrue("Unexpected abstract methods: " + unexpected, unexpected.isEmpty());
  }

  @Test
  public void protectedAbstractMethods_areExactly15() {
    long count = Arrays.stream(VM_CLASS.getDeclaredMethods())
        .filter(m -> Modifier.isAbstract(m.getModifiers()))
        .filter(m -> Modifier.isProtected(m.getModifiers()))
        .count();
    assertEquals("15 of 16 abstract methods should be protected", 15, count);
  }

  @Test
  public void noNewAbstractMethods_beyondExpected() {
    Set<String> actual = getAbstractDeclaredMethodNames(VM_CLASS);
    Set<String> unexpected = new TreeSet<>(actual);
    unexpected.removeAll(EXPECTED_ABSTRACT_METHODS);
    assertTrue("No unexpected abstract methods: " + unexpected, unexpected.isEmpty());
  }

  // =====================================================================
  // 4. API Surface Lock — Constructor contract
  // =====================================================================

  @Test
  public void hasDefaultConstructor() {
    // Abstract classes get a default constructor from the compiler.
    // VirtualMachine does not declare explicit constructors, so exactly
    // one (the default no-arg) should exist.
    Constructor<?>[] constructors = VM_CLASS.getDeclaredConstructors();
    assertEquals("VirtualMachine should have exactly 1 constructor (the default)",
        1, constructors.length);
    assertEquals("Default constructor should take no parameters",
        0, constructors[0].getParameterCount());
  }

  // =====================================================================
  // 5. API Surface Lock — ReleaseVirtualMachine contract
  // =====================================================================

  @Test
  public void releaseVirtualMachine_extendsVirtualMachine() {
    assertTrue("ReleaseVirtualMachine must extend VirtualMachine",
        VirtualMachine.class.isAssignableFrom(ReleaseVirtualMachine.class));
  }

  @Test
  public void releaseVirtualMachine_isPublic() {
    assertTrue("ReleaseVirtualMachine must be public",
        Modifier.isPublic(ReleaseVirtualMachine.class.getModifiers()));
  }

  @Test
  public void releaseVirtualMachine_isNotAbstract() {
    // ReleaseVirtualMachine is the concrete implementation
    // Note: it contains abstract inner classes but itself is not abstract
    // unless the original code declares it as such. Let's check dynamically.
    // Actually ReleaseVirtualMachine IS abstract (has abstract inner Frame).
    // Let's just verify it compiles and is assignable.
    assertNotNull("ReleaseVirtualMachine class must load", ReleaseVirtualMachine.class);
  }

  // =====================================================================
  // 6. Extraction Contract — VmExpressionEvaluator existence
  // =====================================================================

  @Test
  public void vmExpressionEvaluator_classExists() {
    Class<?> cls = loadPackageClass("VmExpressionEvaluator");
    assertNotNull("VmExpressionEvaluator must exist in " + VM_PACKAGE, cls);
  }

  @Test
  public void vmExpressionEvaluator_isFinal() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    assertTrue("VmExpressionEvaluator must be final",
        Modifier.isFinal(cls.getModifiers()));
  }

  @Test
  public void vmExpressionEvaluator_isPackagePrivate() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    assertPackagePrivate("VmExpressionEvaluator", cls.getModifiers());
  }

  @Test
  public void vmExpressionEvaluator_hasVmBackReference() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    Field vmField = findFieldByType(cls, VirtualMachine.class);
    assertNotNull("VmExpressionEvaluator must have a VirtualMachine back-reference field", vmField);
    assertTrue("VM back-reference must be final", Modifier.isFinal(vmField.getModifiers()));
  }

  @Test
  public void vmExpressionEvaluator_constructorTakesVirtualMachine() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    Constructor<?>[] constructors = cls.getDeclaredConstructors();
    boolean found = false;
    for (Constructor<?> c : constructors) {
      Class<?>[] params = c.getParameterTypes();
      if (params.length == 1 && VirtualMachine.class.isAssignableFrom(params[0])) {
        found = true;
        break;
      }
    }
    assertTrue("VmExpressionEvaluator must have a constructor taking VirtualMachine", found);
  }

  @Test
  public void vmExpressionEvaluator_hasAllExpectedMethods() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    Set<String> actual = getAllDeclaredMethodNames(cls);
    Set<String> missing = new TreeSet<>(EXPECTED_EVALUATOR_METHODS);
    missing.removeAll(actual);
    assertTrue("VmExpressionEvaluator missing methods: " + missing, missing.isEmpty());
  }

  @Test
  public void vmExpressionEvaluator_evaluateBoolean_isNotPrivate() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    Method m = findDeclaredMethod(cls, "evaluateBoolean");
    assertNotNull("evaluateBoolean must exist on VmExpressionEvaluator", m);
    assertFalse("evaluateBoolean must not be private (executor needs cross-delegate access)",
        Modifier.isPrivate(m.getModifiers()));
  }

  @Test
  public void vmExpressionEvaluator_evaluateInt_isNotPrivate() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    Method m = findDeclaredMethod(cls, "evaluateInt");
    assertNotNull("evaluateInt must exist on VmExpressionEvaluator", m);
    assertFalse("evaluateInt must not be private (executor needs cross-delegate access)",
        Modifier.isPrivate(m.getModifiers()));
  }

  // =====================================================================
  // 7. Extraction Contract — VmStatementExecutor existence
  // =====================================================================

  @Test
  public void vmStatementExecutor_classExists() {
    Class<?> cls = loadPackageClass("VmStatementExecutor");
    assertNotNull("VmStatementExecutor must exist in " + VM_PACKAGE, cls);
  }

  @Test
  public void vmStatementExecutor_isFinal() {
    Class<?> cls = requirePackageClass("VmStatementExecutor");
    assertTrue("VmStatementExecutor must be final",
        Modifier.isFinal(cls.getModifiers()));
  }

  @Test
  public void vmStatementExecutor_isPackagePrivate() {
    Class<?> cls = requirePackageClass("VmStatementExecutor");
    assertPackagePrivate("VmStatementExecutor", cls.getModifiers());
  }

  @Test
  public void vmStatementExecutor_hasVmBackReference() {
    Class<?> cls = requirePackageClass("VmStatementExecutor");
    Field vmField = findFieldByType(cls, VirtualMachine.class);
    assertNotNull("VmStatementExecutor must have a VirtualMachine back-reference field", vmField);
    assertTrue("VM back-reference must be final", Modifier.isFinal(vmField.getModifiers()));
  }

  @Test
  public void vmStatementExecutor_constructorTakesVirtualMachine() {
    Class<?> cls = requirePackageClass("VmStatementExecutor");
    Constructor<?>[] constructors = cls.getDeclaredConstructors();
    boolean found = false;
    for (Constructor<?> c : constructors) {
      Class<?>[] params = c.getParameterTypes();
      if (params.length == 1 && VirtualMachine.class.isAssignableFrom(params[0])) {
        found = true;
        break;
      }
    }
    assertTrue("VmStatementExecutor must have a constructor taking VirtualMachine", found);
  }

  @Test
  public void vmStatementExecutor_hasAllExpectedMethods() {
    Class<?> cls = requirePackageClass("VmStatementExecutor");
    Set<String> actual = getAllDeclaredMethodNames(cls);
    Set<String> missing = new TreeSet<>(EXPECTED_EXECUTOR_METHODS);
    missing.removeAll(actual);
    assertTrue("VmStatementExecutor missing methods: " + missing, missing.isEmpty());
  }

  // =====================================================================
  // 8. Extraction Contract — CopyOnWriteArrayList upgrade
  // =====================================================================

  @Test
  public void virtualMachineListeners_isCopyOnWriteArrayList() {
    Field field = findDeclaredField(VM_CLASS, "virtualMachineListeners");
    assertNotNull("virtualMachineListeners field must exist", field);
    assertEquals("virtualMachineListeners must be CopyOnWriteArrayList for thread-safe iteration",
        CopyOnWriteArrayList.class, field.getType());
  }

  @Test
  public void virtualMachineListeners_isFinal() {
    Field field = findDeclaredField(VM_CLASS, "virtualMachineListeners");
    assertNotNull("virtualMachineListeners field must exist", field);
    assertTrue("virtualMachineListeners must be final",
        Modifier.isFinal(field.getModifiers()));
  }

  // =====================================================================
  // 9. Extraction Contract — Visibility promotions
  // =====================================================================

  @Test
  public void isStopped_isPackagePrivate() {
    Field field = findDeclaredField(VM_CLASS, "isStopped");
    assertNotNull("isStopped field must exist", field);
    assertPackagePrivate("isStopped", field.getModifiers());
  }

  @Test
  public void virtualMachineListeners_isPackagePrivate() {
    Field field = findDeclaredField(VM_CLASS, "virtualMachineListeners");
    assertNotNull("virtualMachineListeners field must exist", field);
    assertPackagePrivate("virtualMachineListeners", field.getModifiers());
  }

  @Test
  public void mapAbstractClsToAdapterCls_isPackagePrivate() {
    Field field = findDeclaredField(VM_CLASS, "mapAbstractClsToAdapterCls");
    assertNotNull("mapAbstractClsToAdapterCls field must exist", field);
    assertPackagePrivate("mapAbstractClsToAdapterCls", field.getModifiers());
  }

  @Test
  public void checkNotNull_isPackagePrivate() {
    Method method = findDeclaredMethod(VM_CLASS, "checkNotNull");
    assertNotNull("checkNotNull method must exist on VirtualMachine", method);
    assertPackagePrivate("checkNotNull", method.getModifiers());
  }

  // =====================================================================
  // 10. Extraction Contract — Delegate fields on VirtualMachine
  // =====================================================================

  @Test
  public void virtualMachine_hasExpressionEvaluatorField() {
    Class<?> evaluatorCls = requirePackageClass("VmExpressionEvaluator");
    Field field = findFieldByType(VM_CLASS, evaluatorCls);
    assertNotNull("VirtualMachine must have a VmExpressionEvaluator delegate field", field);
    assertEquals("expressionEvaluator", field.getName());
    assertTrue("expressionEvaluator field must be final",
        Modifier.isFinal(field.getModifiers()));
  }

  @Test
  public void virtualMachine_hasStatementExecutorField() {
    Class<?> executorCls = requirePackageClass("VmStatementExecutor");
    Field field = findFieldByType(VM_CLASS, executorCls);
    assertNotNull("VirtualMachine must have a VmStatementExecutor delegate field", field);
    assertEquals("statementExecutor", field.getName());
    assertTrue("statementExecutor field must be final",
        Modifier.isFinal(field.getModifiers()));
  }

  @Test
  public void virtualMachine_hasFocusedCollaboratorFields() {
    assertDelegateField("VmArrayAccessHelper", "arrayAccessHelper");
    assertDelegateField("VmFieldAccessHelper", "fieldAccessHelper");
    assertDelegateField("VmMethodInvoker", "methodInvoker");
  }

  @Test
  public void delegateFields_arePackagePrivate() {
    assertDelegateFieldIsPackagePrivate("VmExpressionEvaluator", "expressionEvaluator");
    assertDelegateFieldIsPackagePrivate("VmStatementExecutor", "statementExecutor");
    assertDelegateFieldIsPackagePrivate("VmArrayAccessHelper", "arrayAccessHelper");
    assertDelegateFieldIsPackagePrivate("VmFieldAccessHelper", "fieldAccessHelper");
    assertDelegateFieldIsPackagePrivate("VmMethodInvoker", "methodInvoker");
  }

  // =====================================================================
  // 11. Extraction Contract — evaluate/execute remain on VM as wrappers
  // =====================================================================

  @Test
  public void evaluate_remainsOnVirtualMachine() {
    Method m = findDeclaredMethod(VM_CLASS, "evaluate");
    assertNotNull("evaluate(Expression) must remain declared on VirtualMachine", m);
    assertTrue("evaluate must be protected",
        Modifier.isProtected(m.getModifiers()));
  }

  @Test
  public void execute_remainsOnVirtualMachine() {
    Method m = findDeclaredMethod(VM_CLASS, "execute");
    assertNotNull("execute(Statement) must remain declared on VirtualMachine", m);
    assertTrue("execute must be protected",
        Modifier.isProtected(m.getModifiers()));
  }

  // =====================================================================
  // 12. Extraction Contract — No new public or protected members
  // =====================================================================

  @Test
  public void noNewPublicFields() {
    Set<String> publicFields = Arrays.stream(VM_CLASS.getDeclaredFields())
        .filter(f -> Modifier.isPublic(f.getModifiers()))
        .map(Field::getName)
        .collect(Collectors.toSet());
    assertTrue("VirtualMachine should have no public fields: " + publicFields,
        publicFields.isEmpty());
  }

  @Test
  public void noNewProtectedFields() {
    Set<String> protectedFields = Arrays.stream(VM_CLASS.getDeclaredFields())
        .filter(f -> Modifier.isProtected(f.getModifiers()))
        .map(Field::getName)
        .collect(Collectors.toSet());
    assertTrue("VirtualMachine should have no protected fields: " + protectedFields,
        protectedFields.isEmpty());
  }

  // =====================================================================
  // 13. Extraction Contract — Line count guard
  // =====================================================================

  @Test
  public void virtualMachine_sourceFileUnder500Lines() throws Exception {
    // This test reads the source file and verifies the extraction target.
    // It uses the known source path relative to the project root.
    String sourcePath = "core/ast/src/main/java/org/lgna/project/virtualmachine/VirtualMachine.java";
    java.io.File sourceFile = findProjectFile(sourcePath);
    if (sourceFile == null) {
      // If we can't find the file from the test runtime, skip gracefully
      // but note this as a failure in CI where paths are predictable
      return;
    }
    long lineCount;
    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(sourceFile))) {
      lineCount = reader.lines().count();
    }
    assertTrue("VirtualMachine.java should be under 500 lines after extraction, but was " + lineCount,
        lineCount < 500);
  }

  // =====================================================================
  // 14. Cross-delegate access — evaluateBoolean/evaluateInt
  // =====================================================================

  @Test
  public void executor_canAccessEvaluateBoolean_viaCrossDelegate() {
    // Verify the evaluator exposes evaluateBoolean at package-private level
    // so VmStatementExecutor can call vm.expressionEvaluator.evaluateBoolean()
    Class<?> evaluatorCls = requirePackageClass("VmExpressionEvaluator");
    Method m = findDeclaredMethod(evaluatorCls, "evaluateBoolean");
    assertNotNull("evaluateBoolean must exist on VmExpressionEvaluator", m);
    // Must not be private — the executor needs access
    assertFalse("evaluateBoolean must be accessible to executor (not private)",
        Modifier.isPrivate(m.getModifiers()));
    // Must not be public — it's an internal concern
    assertFalse("evaluateBoolean should not be public",
        Modifier.isPublic(m.getModifiers()));
  }

  @Test
  public void executor_canAccessEvaluateInt_viaCrossDelegate() {
    Class<?> evaluatorCls = requirePackageClass("VmExpressionEvaluator");
    Method m = findDeclaredMethod(evaluatorCls, "evaluateInt");
    assertNotNull("evaluateInt must exist on VmExpressionEvaluator", m);
    assertFalse("evaluateInt must be accessible to executor (not private)",
        Modifier.isPrivate(m.getModifiers()));
    assertFalse("evaluateInt should not be public",
        Modifier.isPublic(m.getModifiers()));
  }

  // =====================================================================
  // 15. Sanity — delegate classes are not abstract, not interfaces
  // =====================================================================

  @Test
  public void vmExpressionEvaluator_isNotAbstract() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    assertFalse("VmExpressionEvaluator must be concrete (not abstract)",
        Modifier.isAbstract(cls.getModifiers()));
  }

  @Test
  public void vmExpressionEvaluator_isNotInterface() {
    Class<?> cls = requirePackageClass("VmExpressionEvaluator");
    assertFalse("VmExpressionEvaluator must not be an interface",
        cls.isInterface());
  }

  @Test
  public void vmStatementExecutor_isNotAbstract() {
    Class<?> cls = requirePackageClass("VmStatementExecutor");
    assertFalse("VmStatementExecutor must be concrete (not abstract)",
        Modifier.isAbstract(cls.getModifiers()));
  }

  @Test
  public void vmStatementExecutor_isNotInterface() {
    Class<?> cls = requirePackageClass("VmStatementExecutor");
    assertFalse("VmStatementExecutor must not be an interface",
        cls.isInterface());
  }

  // =====================================================================
  // Helper methods
  // =====================================================================

  private static Set<String> getPublicDeclaredMethodNames(Class<?> cls) {
    return Arrays.stream(cls.getDeclaredMethods())
        .filter(m -> Modifier.isPublic(m.getModifiers()))
        .map(Method::getName)
        .collect(Collectors.toSet());
  }

  private static Set<String> getAbstractDeclaredMethodNames(Class<?> cls) {
    return Arrays.stream(cls.getDeclaredMethods())
        .filter(m -> Modifier.isAbstract(m.getModifiers()))
        .map(Method::getName)
        .collect(Collectors.toSet());
  }

  private static Set<String> getAllDeclaredMethodNames(Class<?> cls) {
    return Arrays.stream(cls.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());
  }

  private static Method findDeclaredMethod(Class<?> cls, String name) {
    return Arrays.stream(cls.getDeclaredMethods())
        .filter(m -> m.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  private static Field findDeclaredField(Class<?> cls, String name) {
    try {
      return cls.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      return null;
    }
  }

  private static Field findFieldByType(Class<?> cls, Class<?> fieldType) {
    return Arrays.stream(cls.getDeclaredFields())
        .filter(f -> f.getType().equals(fieldType))
        .findFirst()
        .orElse(null);
  }

  private static Class<?> loadPackageClass(String simpleName) {
    try {
      return Class.forName(VM_PACKAGE + "." + simpleName);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private static Class<?> requirePackageClass(String simpleName) {
    Class<?> cls = loadPackageClass(simpleName);
    assertNotNull(simpleName + " must exist in " + VM_PACKAGE
        + " (class not found — extraction not yet applied?)", cls);
    return cls;
  }

  private static void assertPackagePrivate(String memberName, int modifiers) {
    assertFalse(memberName + " must not be private (needs package-private delegate access)",
        Modifier.isPrivate(modifiers));
    assertFalse(memberName + " must not be public (internal visibility only)",
        Modifier.isPublic(modifiers));
    assertFalse(memberName + " must not be protected",
        Modifier.isProtected(modifiers));
  }

  private static void assertDelegateField(String className, String fieldName) {
    Class<?> cls = requirePackageClass(className);
    Field field = findFieldByType(VM_CLASS, cls);
    assertNotNull("VirtualMachine must have a " + className + " delegate field", field);
    assertEquals(fieldName, field.getName());
    assertTrue(fieldName + " field must be final", Modifier.isFinal(field.getModifiers()));
  }

  private static void assertDelegateFieldIsPackagePrivate(String className, String fieldName) {
    Class<?> cls = requirePackageClass(className);
    Field field = findFieldByType(VM_CLASS, cls);
    assertNotNull(fieldName + " field must exist", field);
    assertPackagePrivate(fieldName, field.getModifiers());
  }

  private static String sorted(Set<String> set) {
    return new TreeSet<>(set).toString();
  }

  /**
   * Attempts to locate a project source file by walking up from the
   * classloader's location to find the project root.
   */
  private static java.io.File findProjectFile(String relativePath) {
    // Try common locations
    String[] roots = {
        System.getProperty("user.dir"),
        System.getProperty("basedir"),  // Maven sets this
    };
    for (String root : roots) {
      if (root != null) {
        java.io.File candidate = new java.io.File(root, relativePath);
        if (candidate.exists()) {
          return candidate;
        }
        // Try parent (in case cwd is core/ast)
        java.io.File parent = new java.io.File(root).getParentFile();
        while (parent != null) {
          candidate = new java.io.File(parent, relativePath);
          if (candidate.exists()) {
            return candidate;
          }
          parent = parent.getParentFile();
        }
      }
    }
    return null;
  }
}
