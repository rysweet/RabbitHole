package org.alice.ide.ast;

import org.junit.Test;
import org.lgna.project.ast.FauxExpression;
import org.lgna.project.ast.JavaType;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link IdeExpression} hierarchy.
 */
public class IdeExpressionHierarchyTest {

  @Test
  public void ideExpression_extendsFauxExpression() {
    assertTrue(FauxExpression.class.isAssignableFrom(IdeExpression.class));
  }

  @Test
  public void ideExpression_isAbstract() {
    assertTrue(java.lang.reflect.Modifier.isAbstract(IdeExpression.class.getModifiers()));
  }

  @Test
  public void emptyExpression_extendsIdeExpression() {
    assertTrue(IdeExpression.class.isAssignableFrom(EmptyExpression.class));
  }

  @Test
  public void selectedInstanceFactoryExpression_extendsIdeExpression() {
    assertTrue(IdeExpression.class.isAssignableFrom(SelectedInstanceFactoryExpression.class));
  }

  @Test
  public void previousValueExpression_extendsIdeExpression() {
    assertTrue(IdeExpression.class.isAssignableFrom(PreviousValueExpression.class));
  }

  @Test
  public void emptyExpression_isConcrete() {
    assertFalse(java.lang.reflect.Modifier.isAbstract(EmptyExpression.class.getModifiers()));
  }

  @Test
  public void selectedInstanceFactoryExpression_isConcrete() {
    assertFalse(java.lang.reflect.Modifier.isAbstract(
        SelectedInstanceFactoryExpression.class.getModifiers()));
  }

  @Test
  public void previousValueExpression_isConcrete() {
    assertFalse(java.lang.reflect.Modifier.isAbstract(
        PreviousValueExpression.class.getModifiers()));
  }

  @Test
  public void emptyExpression_instancePassesInstanceOfCheck() {
    EmptyExpression expr = new EmptyExpression(String.class);
    assertTrue(expr instanceof IdeExpression);
    assertTrue(expr instanceof FauxExpression);
  }

  @Test
  public void allSubclasses_inAstPackage() {
    assertEquals("org.alice.ide.ast", IdeExpression.class.getPackage().getName());
    assertEquals("org.alice.ide.ast", EmptyExpression.class.getPackage().getName());
    assertEquals("org.alice.ide.ast", SelectedInstanceFactoryExpression.class.getPackage().getName());
    assertEquals("org.alice.ide.ast", PreviousValueExpression.class.getPackage().getName());
  }
}
