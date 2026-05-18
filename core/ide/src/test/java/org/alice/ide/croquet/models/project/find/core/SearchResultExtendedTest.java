package org.alice.ide.croquet.models.project.find.core;

import org.junit.Test;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.UserField;
import org.lgna.project.ast.UserMethod;

import static org.junit.Assert.*;

public class SearchResultExtendedTest {

  @Test
  public void constructorWithUserMethodUsesMethodName() {
    UserMethod method = new UserMethod();
    method.name.setValue("testMethod");

    SearchResult result = new SearchResult(method);

    assertSame(method, result.getDeclaration());
    assertEquals("testMethod", result.getName());
  }

  @Test
  public void constructorWithUserFieldUsesFieldName() {
    UserField field = new UserField();
    field.name.setValue("testField");

    SearchResult result = new SearchResult(field);

    assertSame(field, result.getDeclaration());
    assertEquals("testField", result.getName());
  }

  @Test
  public void getReferencesIsInitiallyEmpty() {
    UserMethod method = new UserMethod();
    method.name.setValue("emptyReferences");

    SearchResult result = new SearchResult(method);

    assertNotNull(result.getReferences());
    assertTrue(result.getReferences().isEmpty());
  }

  @Test
  public void addReferenceAddsExpressionToList() {
    UserMethod method = new UserMethod();
    method.name.setValue("addReference");
    SearchResult result = new SearchResult(method);
    NullLiteral reference = new NullLiteral();

    result.addReference(reference);

    assertEquals(1, result.getReferences().size());
    assertSame(reference, result.getReferences().get(0));
  }

  @Test
  public void multipleAddReferenceCallsAccumulate() {
    UserField field = new UserField();
    field.name.setValue("accumulate");
    SearchResult result = new SearchResult(field);
    NullLiteral first = new NullLiteral();
    NullLiteral second = new NullLiteral();

    result.addReference(first);
    result.addReference(second);

    assertEquals(2, result.getReferences().size());
    assertSame(first, result.getReferences().get(0));
    assertSame(second, result.getReferences().get(1));
  }

  @Test
  public void getDeclarationReturnsOriginalDeclaration() {
    UserMethod method = new UserMethod();
    method.name.setValue("originalDeclaration");

    SearchResult result = new SearchResult(method);

    assertSame(method, result.getDeclaration());
  }
}
