package org.alice.ide.ast.draganddrop;

import org.junit.Test;
import org.lgna.project.ast.*;

import static org.junit.Assert.*;

/**
 * Tests for {@link ExpressionPropertyDropSite} — expression property drop site
 * equals, hashCode, toString, and getExpressionProperty.
 */
public class ExpressionPropertyDropSiteTest {

  private ExpressionProperty createExpressionProperty() {
    MethodInvocation invocation = new MethodInvocation();
    return invocation.expression;
  }

  @Test
  public void getExpressionProperty_returnsConstructedProperty() {
    ExpressionProperty prop = createExpressionProperty();
    ExpressionPropertyDropSite site = new ExpressionPropertyDropSite(prop);
    assertSame(prop, site.getExpressionProperty());
  }

  @Test
  public void equals_sameProperty_true() {
    ExpressionProperty prop = createExpressionProperty();
    ExpressionPropertyDropSite a = new ExpressionPropertyDropSite(prop);
    ExpressionPropertyDropSite b = new ExpressionPropertyDropSite(prop);
    assertTrue(a.equals(b));
  }

  @Test
  public void equals_sameObject_true() {
    ExpressionProperty prop = createExpressionProperty();
    ExpressionPropertyDropSite site = new ExpressionPropertyDropSite(prop);
    assertTrue(site.equals(site));
  }

  @Test
  public void equals_differentType_false() {
    ExpressionProperty prop = createExpressionProperty();
    ExpressionPropertyDropSite site = new ExpressionPropertyDropSite(prop);
    assertFalse(site.equals("not a drop site"));
  }

  @Test
  public void equals_null_false() {
    ExpressionProperty prop = createExpressionProperty();
    ExpressionPropertyDropSite site = new ExpressionPropertyDropSite(prop);
    assertFalse(site.equals(null));
  }

  @Test
  public void equals_differentProperty_false() {
    ExpressionProperty prop1 = createExpressionProperty();
    ExpressionProperty prop2 = createExpressionProperty();
    ExpressionPropertyDropSite a = new ExpressionPropertyDropSite(prop1);
    ExpressionPropertyDropSite b = new ExpressionPropertyDropSite(prop2);
    assertFalse(a.equals(b));
  }

  @Test
  public void hashCode_sameProperty_sameHash() {
    ExpressionProperty prop = createExpressionProperty();
    ExpressionPropertyDropSite a = new ExpressionPropertyDropSite(prop);
    ExpressionPropertyDropSite b = new ExpressionPropertyDropSite(prop);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void hashCode_nullProperty_defaultValue() {
    ExpressionPropertyDropSite site = new ExpressionPropertyDropSite((ExpressionProperty) null);
    assertEquals(17, site.hashCode());
  }

  @Test
  public void toString_containsClassName() {
    ExpressionProperty prop = createExpressionProperty();
    ExpressionPropertyDropSite site = new ExpressionPropertyDropSite(prop);
    String str = site.toString();
    assertTrue(str.contains("ExpressionPropertyDropSite"));
    assertTrue(str.contains("expressionProperty="));
  }
}
