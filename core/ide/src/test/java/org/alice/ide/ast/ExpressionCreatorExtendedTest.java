package org.alice.ide.ast;

import org.junit.Test;
import org.lgna.project.ast.*;

import static org.junit.Assert.*;

/**
 * Extended tests for {@link ExpressionCreator} constants and factory methods.
 * Only tests the static/final aspects that don't require IDE singletons.
 */
public class ExpressionCreatorExtendedTest {

  @Test
  public void milliDecimalPlaces_isThree() {
    assertEquals(3, ExpressionCreator.MILLI_DECIMAL_PLACES);
  }

  @Test
  public void microDecimalPlaces_isSix() {
    assertEquals(6, ExpressionCreator.MICRO_DECIMAL_PLACES);
  }

  @Test
  public void defaultDecimalPlaces_isMicro() {
    assertEquals(ExpressionCreator.MICRO_DECIMAL_PLACES, ExpressionCreator.DEFAULT_DECIMAL_PLACES);
  }

  @Test
  public void cannotCreateExpressionException_storesValue() {
    Object testVal = "test_value";
    ExpressionCreator.CannotCreateExpressionException ex =
        new ExpressionCreator.CannotCreateExpressionException(testVal);
    assertSame(testVal, ex.getValue());
  }

  @Test
  public void cannotCreateExpressionException_nullValue() {
    ExpressionCreator.CannotCreateExpressionException ex =
        new ExpressionCreator.CannotCreateExpressionException(null);
    assertNull(ex.getValue());
  }

  @Test
  public void cannotCreateExpressionException_integerValue() {
    ExpressionCreator.CannotCreateExpressionException ex =
        new ExpressionCreator.CannotCreateExpressionException(42);
    assertEquals(42, ex.getValue());
  }

  @Test
  public void cannotCreateExpressionException_isException() {
    ExpressionCreator.CannotCreateExpressionException ex =
        new ExpressionCreator.CannotCreateExpressionException("val");
    assertTrue(ex instanceof Exception);
  }

  @Test
  public void cannotCreateExpressionException_complexValue() {
    Object[] arr = {1, "two", 3.0};
    ExpressionCreator.CannotCreateExpressionException ex =
        new ExpressionCreator.CannotCreateExpressionException(arr);
    assertSame(arr, ex.getValue());
  }

  @Test
  public void cannotCreateExpressionException_enumValue() {
    ExpressionCreator.CannotCreateExpressionException ex =
        new ExpressionCreator.CannotCreateExpressionException(ArithmeticInfixExpression.Operator.PLUS);
    assertSame(ArithmeticInfixExpression.Operator.PLUS, ex.getValue());
  }

  @Test
  public void milliDecimalPlaces_lessThanMicro() {
    assertTrue(ExpressionCreator.MILLI_DECIMAL_PLACES < ExpressionCreator.MICRO_DECIMAL_PLACES);
  }

  @Test
  public void defaultDecimalPlaces_positive() {
    assertTrue(ExpressionCreator.DEFAULT_DECIMAL_PLACES > 0);
  }

  @Test
  public void cannotCreateExpressionException_getMessage_isNull() {
    ExpressionCreator.CannotCreateExpressionException ex =
        new ExpressionCreator.CannotCreateExpressionException("someVal");
    // Exception with no message string
    assertNull(ex.getMessage());
  }
}
