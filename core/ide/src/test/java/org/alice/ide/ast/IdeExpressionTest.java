package org.alice.ide.ast;

import org.junit.Test;
import org.lgna.project.ast.FauxExpression;
import org.lgna.project.ast.JavaType;

import static org.junit.Assert.*;

public class IdeExpressionTest {

  @Test
  public void emptyExpressionIsAnIdeExpression() {
    assertTrue(new EmptyExpression(String.class) instanceof IdeExpression);
  }

  @Test
  public void selectedInstanceFactoryExpressionIsAnIdeExpression() {
    assertTrue(new SelectedInstanceFactoryExpression(JavaType.getInstance(String.class)) instanceof IdeExpression);
  }

  @Test
  public void previousValueExpressionIsAnIdeExpression() {
    assertTrue(new PreviousValueExpression(String.class) instanceof IdeExpression);
  }

  @Test
  public void ideExpressionExtendsFauxExpression() {
    assertEquals(FauxExpression.class, IdeExpression.class.getSuperclass());
  }
}
