package org.alice.ide.croquet.models.numberpad;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.lgna.project.ast.DoubleLiteral;
import org.lgna.project.ast.FieldAccess;
import org.lgna.project.ast.IntegerLiteral;

import java.awt.GraphicsEnvironment;

import static org.junit.Assert.*;

public class NumberModelExtendedTest {

  private DoubleModel doubleModel;
  private IntegerModel integerModel;

  @Before
  public void setUp() {
    Assume.assumeFalse(GraphicsEnvironment.isHeadless());
    doubleModel = DoubleModel.getInstance();
    integerModel = IntegerModel.getInstance();
    doubleModel.setText("");
    integerModel.setText("");
  }

  @Test
  public void doubleModelAcceptsDecimalInput() {
    doubleModel.setText("123.45");

    assertEquals("123.45", doubleModel.getTextField().getText());
    assertTrue(doubleModel.getExpressionValue() instanceof DoubleLiteral);
    assertNull(doubleModel.getExplanationIfOkButtonShouldBeDisabled());
  }

  @Test
  public void doubleModelNegatePrefixesMinusAndStaysValid() {
    doubleModel.setText("12.5");

    doubleModel.negate();

    assertEquals("-12.5", doubleModel.getTextField().getText());
    assertTrue(doubleModel.getExpressionValue() instanceof DoubleLiteral);
    assertNull(doubleModel.getExplanationIfOkButtonShouldBeDisabled());
  }

  @Test
  public void integerModelHandlesLargestIntegerValue() {
    integerModel.setText(String.valueOf(Integer.MAX_VALUE));

    assertTrue(integerModel.getExpressionValue() instanceof IntegerLiteral);
    assertNull(integerModel.getExplanationIfOkButtonShouldBeDisabled());
  }

  @Test
  public void integerModelOverflowLikePositiveStringReturnsFieldAccess() {
    integerModel.setText(String.valueOf((long) Integer.MAX_VALUE + 42L));

    assertTrue(integerModel.getExpressionValue() instanceof FieldAccess);
    assertNull(integerModel.getExplanationIfOkButtonShouldBeDisabled());
  }

  @Test
  public void integerModelOverflowLikeNegativeStringReturnsFieldAccess() {
    integerModel.setText(String.valueOf((long) Integer.MIN_VALUE - 42L));

    assertTrue(integerModel.getExpressionValue() instanceof FieldAccess);
    assertNull(integerModel.getExplanationIfOkButtonShouldBeDisabled());
  }
}
