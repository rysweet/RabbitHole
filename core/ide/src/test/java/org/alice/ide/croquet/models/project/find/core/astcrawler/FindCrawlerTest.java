package org.alice.ide.croquet.models.project.find.core.astcrawler;

import edu.cmu.cs.dennisc.pattern.Criterion;
import org.alice.ide.croquet.models.project.find.core.SearchResult;
import org.junit.Test;
import org.lgna.project.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Tests for {@link FindCrawler} — AST visitor that matches references against search results.
 */
public class FindCrawlerTest {

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

  // ---- visit with MethodInvocation ----

  @Test
  public void visit_methodInvocation_matchingMethod_addsReference() {
    UserMethod method = createUserMethod("doSomething");
    SearchResult result = new SearchResult(method);

    List<SearchResult> results = new ArrayList<>();
    results.add(result);

    List<Criterion> criteria = Collections.emptyList();
    FindCrawler crawler = new FindCrawler(criteria, results);

    MethodInvocation invocation = new MethodInvocation();
    invocation.method.setValue(method);

    crawler.visit(invocation);

    assertEquals(1, result.getReferences().size());
    assertSame(invocation, result.getReferences().get(0));
  }

  @Test
  public void visit_methodInvocation_noMatchingResult_doesNotAdd() {
    UserMethod method = createUserMethod("doSomething");
    UserMethod otherMethod = createUserMethod("otherMethod");
    SearchResult result = new SearchResult(otherMethod);

    List<SearchResult> results = new ArrayList<>();
    results.add(result);

    List<Criterion> criteria = Collections.emptyList();
    FindCrawler crawler = new FindCrawler(criteria, results);

    MethodInvocation invocation = new MethodInvocation();
    invocation.method.setValue(method);

    crawler.visit(invocation);

    assertTrue(result.getReferences().isEmpty());
  }

  // ---- visit with FieldAccess ----

  @Test
  public void visit_fieldAccess_matchingField_addsReference() {
    UserField field = createUserField("myField");
    SearchResult result = new SearchResult(field);

    List<SearchResult> results = new ArrayList<>();
    results.add(result);

    List<Criterion> criteria = Collections.emptyList();
    FindCrawler crawler = new FindCrawler(criteria, results);

    FieldAccess access = new FieldAccess();
    access.field.setValue(field);

    crawler.visit(access);

    assertEquals(1, result.getReferences().size());
    assertSame(access, result.getReferences().get(0));
  }

  @Test
  public void visit_fieldAccess_noMatchingResult_doesNotAdd() {
    UserField field = createUserField("myField");
    UserField otherField = createUserField("otherField");
    SearchResult result = new SearchResult(otherField);

    List<SearchResult> results = new ArrayList<>();
    results.add(result);

    List<Criterion> criteria = Collections.emptyList();
    FindCrawler crawler = new FindCrawler(criteria, results);

    FieldAccess access = new FieldAccess();
    access.field.setValue(field);

    crawler.visit(access);

    assertTrue(result.getReferences().isEmpty());
  }

  // ---- visit with non-matching crawlable ----

  @Test
  public void visit_nonMatchingCrawlable_doesNothing() {
    UserMethod method = createUserMethod("doSomething");
    SearchResult result = new SearchResult(method);

    List<SearchResult> results = new ArrayList<>();
    results.add(result);

    List<Criterion> criteria = Collections.emptyList();
    FindCrawler crawler = new FindCrawler(criteria, results);

    // NullLiteral is a Crawlable but not MethodInvocation/FieldAccess/LocalAccess
    NullLiteral literal = new NullLiteral();
    crawler.visit(literal);

    assertTrue(result.getReferences().isEmpty());
  }

  // ---- criteria filtering ----

  @Test
  public void visit_methodInvocation_criterionRejects_doesNotAdd() {
    UserMethod method = createUserMethod("doSomething");
    SearchResult result = new SearchResult(method);

    List<SearchResult> results = new ArrayList<>();
    results.add(result);

    // A criterion that always rejects
    List<Criterion> criteria = new ArrayList<>();
    criteria.add((Criterion<Expression>) e -> false);

    FindCrawler crawler = new FindCrawler(criteria, results);

    MethodInvocation invocation = new MethodInvocation();
    invocation.method.setValue(method);

    crawler.visit(invocation);

    assertTrue(result.getReferences().isEmpty());
  }

  @Test
  public void visit_methodInvocation_criterionAccepts_addsReference() {
    UserMethod method = createUserMethod("doSomething");
    SearchResult result = new SearchResult(method);

    List<SearchResult> results = new ArrayList<>();
    results.add(result);

    // A criterion that always accepts
    List<Criterion> criteria = new ArrayList<>();
    criteria.add((Criterion<Expression>) e -> true);

    FindCrawler crawler = new FindCrawler(criteria, results);

    MethodInvocation invocation = new MethodInvocation();
    invocation.method.setValue(method);

    crawler.visit(invocation);

    assertEquals(1, result.getReferences().size());
  }

  // ---- multiple results ----

  @Test
  public void visit_multipleResults_matchesCorrectOne() {
    UserMethod method1 = createUserMethod("method1");
    UserMethod method2 = createUserMethod("method2");
    SearchResult result1 = new SearchResult(method1);
    SearchResult result2 = new SearchResult(method2);

    List<SearchResult> results = new ArrayList<>();
    results.add(result1);
    results.add(result2);

    List<Criterion> criteria = Collections.emptyList();
    FindCrawler crawler = new FindCrawler(criteria, results);

    MethodInvocation invocation = new MethodInvocation();
    invocation.method.setValue(method2);

    crawler.visit(invocation);

    assertTrue(result1.getReferences().isEmpty());
    assertEquals(1, result2.getReferences().size());
  }

  @Test
  public void visit_multipleVisits_accumulatesReferences() {
    UserMethod method = createUserMethod("doSomething");
    SearchResult result = new SearchResult(method);

    List<SearchResult> results = new ArrayList<>();
    results.add(result);

    List<Criterion> criteria = Collections.emptyList();
    FindCrawler crawler = new FindCrawler(criteria, results);

    MethodInvocation inv1 = new MethodInvocation();
    inv1.method.setValue(method);

    MethodInvocation inv2 = new MethodInvocation();
    inv2.method.setValue(method);

    crawler.visit(inv1);
    crawler.visit(inv2);

    assertEquals(2, result.getReferences().size());
  }
}
