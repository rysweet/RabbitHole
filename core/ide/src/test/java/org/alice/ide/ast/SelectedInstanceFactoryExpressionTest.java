package org.alice.ide.ast;

import org.junit.Test;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.JavaType;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link SelectedInstanceFactoryExpression}.
 */
public class SelectedInstanceFactoryExpressionTest {

  @Test
  public void constructWithType_getRequiredTypeReturnsSame() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(String.class);
    SelectedInstanceFactoryExpression expr = new SelectedInstanceFactoryExpression(type);
    assertSame(type, expr.getRequiredType());
  }

  @Test
  public void getType_returnsRequiredType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Integer.class);
    SelectedInstanceFactoryExpression expr = new SelectedInstanceFactoryExpression(type);
    assertSame(type, expr.getType());
  }

  @Test
  public void getRequiredType_equalsGetType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Double.class);
    SelectedInstanceFactoryExpression expr = new SelectedInstanceFactoryExpression(type);
    assertEquals(expr.getRequiredType(), expr.getType());
  }

  @Test
  public void constructWithObjectType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Object.class);
    SelectedInstanceFactoryExpression expr = new SelectedInstanceFactoryExpression(type);
    assertNotNull(expr.getType());
    assertNotNull(expr.getRequiredType());
  }

  @Test
  public void constructWithPrimitiveType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(boolean.class);
    SelectedInstanceFactoryExpression expr = new SelectedInstanceFactoryExpression(type);
    assertEquals(type, expr.getType());
  }

  @Test
  public void extendsIdeExpression() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(String.class);
    SelectedInstanceFactoryExpression expr = new SelectedInstanceFactoryExpression(type);
    assertTrue(expr instanceof IdeExpression);
  }

  @Test
  public void twoInstancesSameType_requiredTypesEqual() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(String.class);
    SelectedInstanceFactoryExpression e1 = new SelectedInstanceFactoryExpression(type);
    SelectedInstanceFactoryExpression e2 = new SelectedInstanceFactoryExpression(type);
    assertEquals(e1.getRequiredType(), e2.getRequiredType());
  }

  @Test
  public void twoInstancesDifferentTypes_notEqual() {
    SelectedInstanceFactoryExpression e1 =
        new SelectedInstanceFactoryExpression(JavaType.getInstance(String.class));
    SelectedInstanceFactoryExpression e2 =
        new SelectedInstanceFactoryExpression(JavaType.getInstance(Integer.class));
    assertNotEquals(e1.getRequiredType(), e2.getRequiredType());
  }

  @Test
  public void constructWithArrayType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(int[].class);
    SelectedInstanceFactoryExpression expr = new SelectedInstanceFactoryExpression(type);
    assertNotNull(expr.getType());
    assertSame(type, expr.getRequiredType());
  }
}
