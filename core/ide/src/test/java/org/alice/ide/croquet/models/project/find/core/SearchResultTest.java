package org.alice.ide.croquet.models.project.find.core;

import org.junit.Test;
import org.lgna.project.ast.*;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for {@link SearchResult} — wraps a declaration and tracks expression references.
 */
public class SearchResultTest {

  private UserMethod createUserMethod(String name) {
    UserMethod method = new UserMethod();
    method.name.setValue(name);
    method.returnType.setValue(JavaType.VOID_TYPE);
    method.managementLevel.setValue(ManagementLevel.NONE);
    return method;
  }

  private UserField createUserField(String name) {
    UserField field = new UserField();
    field.name.setValue(name);
    field.valueType.setValue(JavaType.getInstance(Object.class));
    return field;
  }

  // ---- construction with valid declaration types ----

  @Test
  public void constructor_withUserMethod_succeeds() {
    UserMethod method = createUserMethod("testMethod");
    SearchResult result = new SearchResult(method);
    assertNotNull(result);
    assertEquals("testMethod", result.getName());
  }

  @Test
  public void constructor_withUserField_succeeds() {
    UserField field = createUserField("testField");
    SearchResult result = new SearchResult(field);
    assertNotNull(result);
    assertEquals("testField", result.getName());
  }

  @Test
  public void constructor_withUserParameter_succeeds() {
    UserParameter param = new UserParameter();
    param.name.setValue("param1");
    param.valueType.setValue(JavaType.getInstance(String.class));
    SearchResult result = new SearchResult(param);
    assertNotNull(result);
    assertEquals("param1", result.getName());
  }

  @Test
  public void constructor_withUserLocal_succeeds() {
    UserLocal local = new UserLocal();
    local.name.setValue("localVar");
    local.valueType.setValue(JavaType.getInstance(int.class));
    SearchResult result = new SearchResult(local);
    assertNotNull(result);
    assertEquals("localVar", result.getName());
  }

  // ---- getDeclaration ----

  @Test
  public void getDeclaration_returnsOriginal() {
    UserMethod method = createUserMethod("myMethod");
    SearchResult result = new SearchResult(method);
    assertSame(method, result.getDeclaration());
  }

  // ---- references ----

  @Test
  public void getReferences_initiallyEmpty() {
    UserMethod method = createUserMethod("myMethod");
    SearchResult result = new SearchResult(method);
    assertTrue(result.getReferences().isEmpty());
  }

  @Test
  public void addReference_addsToList() {
    UserMethod method = createUserMethod("myMethod");
    SearchResult result = new SearchResult(method);

    MethodInvocation invocation = new MethodInvocation();
    invocation.method.setValue(method);
    result.addReference(invocation);

    List<Expression> refs = result.getReferences();
    assertEquals(1, refs.size());
    assertSame(invocation, refs.get(0));
  }

  @Test
  public void addReference_multipleReferences_allTracked() {
    UserField field = createUserField("myField");
    SearchResult result = new SearchResult(field);

    FieldAccess access1 = new FieldAccess();
    access1.field.setValue(field);
    FieldAccess access2 = new FieldAccess();
    access2.field.setValue(field);

    result.addReference(access1);
    result.addReference(access2);

    assertEquals(2, result.getReferences().size());
    assertSame(access1, result.getReferences().get(0));
    assertSame(access2, result.getReferences().get(1));
  }

  // ---- getName ----

  @Test
  public void getName_returnsDeclarationName() {
    UserMethod method = createUserMethod("compute");
    SearchResult result = new SearchResult(method);
    assertEquals("compute", result.getName());
  }

  @Test
  public void getName_fieldDeclaration() {
    UserField field = createUserField("count");
    SearchResult result = new SearchResult(field);
    assertEquals("count", result.getName());
  }
}
