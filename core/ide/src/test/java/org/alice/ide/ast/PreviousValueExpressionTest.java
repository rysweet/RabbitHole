package org.alice.ide.ast;

import org.junit.Test;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.JavaType;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link PreviousValueExpression}.
 */
public class PreviousValueExpressionTest {

  @Test
  public void constructWithAbstractType_getTypeReturnsSame() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(String.class);
    PreviousValueExpression expr = new PreviousValueExpression(type);
    assertSame(type, expr.getType());
  }

  @Test
  public void constructWithClass_getTypeReturnsMatchingJavaType() {
    PreviousValueExpression expr = new PreviousValueExpression(String.class);
    assertEquals(JavaType.getInstance(String.class), expr.getType());
  }

  @Test
  public void constructWithPrimitiveClass() {
    PreviousValueExpression expr = new PreviousValueExpression(double.class);
    assertEquals(JavaType.getInstance(double.class), expr.getType());
  }

  @Test
  public void constructWithIntegerClass() {
    PreviousValueExpression expr = new PreviousValueExpression(Integer.class);
    assertEquals(JavaType.getInstance(Integer.class), expr.getType());
  }

  @Test
  public void constructWithBooleanClass() {
    PreviousValueExpression expr = new PreviousValueExpression(Boolean.class);
    assertEquals(JavaType.getInstance(Boolean.class), expr.getType());
  }

  @Test
  public void extendsIdeExpression() {
    PreviousValueExpression expr = new PreviousValueExpression(String.class);
    assertTrue(expr instanceof IdeExpression);
  }

  @Test
  public void twoInstancesSameType_equal() {
    PreviousValueExpression e1 = new PreviousValueExpression(String.class);
    PreviousValueExpression e2 = new PreviousValueExpression(String.class);
    assertEquals(e1.getType(), e2.getType());
  }

  @Test
  public void twoInstancesDifferentType_notEqual() {
    PreviousValueExpression e1 = new PreviousValueExpression(String.class);
    PreviousValueExpression e2 = new PreviousValueExpression(Integer.class);
    assertNotEquals(e1.getType(), e2.getType());
  }

  @Test
  public void constructWithObjectClass() {
    PreviousValueExpression expr = new PreviousValueExpression(Object.class);
    assertNotNull(expr.getType());
  }

  @Test
  public void constructWithArrayType() {
    PreviousValueExpression expr = new PreviousValueExpression(byte[].class);
    assertNotNull(expr.getType());
  }
}
