package org.alice.ide.ast;

import org.junit.Test;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.JavaType;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link EmptyExpression}.
 */
public class EmptyExpressionTest {

  @Test
  public void constructWithAbstractType_getTypeReturnsSame() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(String.class);
    EmptyExpression expr = new EmptyExpression(type);
    assertSame(type, expr.getType());
  }

  @Test
  public void constructWithClass_getTypeReturnsJavaType() {
    EmptyExpression expr = new EmptyExpression(String.class);
    AbstractType<?, ?, ?> type = expr.getType();
    assertNotNull(type);
    assertEquals(JavaType.getInstance(String.class), type);
  }

  @Test
  public void constructWithPrimitiveClass_getTypeReturnsJavaType() {
    EmptyExpression expr = new EmptyExpression(int.class);
    AbstractType<?, ?, ?> type = expr.getType();
    assertNotNull(type);
    assertEquals(JavaType.getInstance(int.class), type);
  }

  @Test
  public void constructWithObjectClass() {
    EmptyExpression expr = new EmptyExpression(Object.class);
    assertNotNull(expr.getType());
    assertEquals(JavaType.getInstance(Object.class), expr.getType());
  }

  @Test
  public void constructWithDoubleClass() {
    EmptyExpression expr = new EmptyExpression(Double.class);
    assertEquals(JavaType.getInstance(Double.class), expr.getType());
  }

  @Test
  public void constructWithBooleanClass() {
    EmptyExpression expr = new EmptyExpression(Boolean.class);
    assertEquals(JavaType.getInstance(Boolean.class), expr.getType());
  }

  @Test
  public void twoExpressionsSameType_typesAreEqual() {
    EmptyExpression e1 = new EmptyExpression(String.class);
    EmptyExpression e2 = new EmptyExpression(String.class);
    assertEquals(e1.getType(), e2.getType());
  }

  @Test
  public void twoExpressionsDifferentType_typesNotEqual() {
    EmptyExpression e1 = new EmptyExpression(String.class);
    EmptyExpression e2 = new EmptyExpression(Integer.class);
    assertNotEquals(e1.getType(), e2.getType());
  }

  @Test
  public void extendsIdeExpression() {
    EmptyExpression expr = new EmptyExpression(String.class);
    assertTrue(expr instanceof IdeExpression);
  }

  @Test
  public void constructWithArrayType() {
    EmptyExpression expr = new EmptyExpression(String[].class);
    assertNotNull(expr.getType());
  }

  @Test
  public void constructWithVoidClass() {
    EmptyExpression expr = new EmptyExpression(void.class);
    assertNotNull(expr.getType());
  }
}
