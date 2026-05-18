package org.alice.ide.croquet.models.cascade.arithmetic;

import org.junit.Test;
import org.lgna.project.ast.ArithmeticInfixExpression;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class ArithmeticUtilitiesAdditionalTest {

  @Test
  public void privateConstructorThrowsAssertionError() throws Exception {
    Constructor<ArithmeticUtilities> constructor = ArithmeticUtilities.class.getDeclaredConstructor();
    constructor.setAccessible(true);

    try {
      constructor.newInstance();
      fail("Expected constructor invocation to fail");
    } catch (InvocationTargetException e) {
      assertTrue(e.getCause() instanceof AssertionError);
    }
  }

  @Test
  public void doubleOperatorGroupsCoverExpectedOperatorSet() {
    EnumSet<ArithmeticInfixExpression.Operator> actual = EnumSet.noneOf(ArithmeticInfixExpression.Operator.class);
    actual.addAll(java.util.Arrays.asList(ArithmeticUtilities.PRIME_TIME_DOUBLE_ARITHMETIC_OPERATORS));
    actual.addAll(java.util.Arrays.asList(ArithmeticUtilities.TUCKED_AWAY_DOUBLE_ARITHMETIC_OPERATORS));

    EnumSet<ArithmeticInfixExpression.Operator> expected = EnumSet.of(
        ArithmeticInfixExpression.Operator.PLUS,
        ArithmeticInfixExpression.Operator.MINUS,
        ArithmeticInfixExpression.Operator.TIMES,
        ArithmeticInfixExpression.Operator.REAL_DIVIDE,
        ArithmeticInfixExpression.Operator.REAL_REMAINDER,
        ArithmeticInfixExpression.Operator.INTEGER_DIVIDE,
        ArithmeticInfixExpression.Operator.INTEGER_REMAINDER);

    assertEquals(expected, actual);
  }

  @Test
  public void integerOperatorGroupsCoverExpectedOperatorSet() {
    EnumSet<ArithmeticInfixExpression.Operator> actual = EnumSet.noneOf(ArithmeticInfixExpression.Operator.class);
    actual.addAll(java.util.Arrays.asList(ArithmeticUtilities.PRIME_TIME_INTEGER_ARITHMETIC_OPERATORS));
    actual.addAll(java.util.Arrays.asList(ArithmeticUtilities.TUCKED_AWAY_INTEGER_ARITHMETIC_OPERATORS));

    EnumSet<ArithmeticInfixExpression.Operator> expected = EnumSet.of(
        ArithmeticInfixExpression.Operator.PLUS,
        ArithmeticInfixExpression.Operator.MINUS,
        ArithmeticInfixExpression.Operator.TIMES,
        ArithmeticInfixExpression.Operator.INTEGER_DIVIDE,
        ArithmeticInfixExpression.Operator.INTEGER_REMAINDER);

    assertEquals(expected, actual);
  }

  @Test
  public void tuckedAwayOperatorGroupsExcludePrimeTimeBasics() {
    for (ArithmeticInfixExpression.Operator operator : ArithmeticUtilities.TUCKED_AWAY_DOUBLE_ARITHMETIC_OPERATORS) {
      assertNotEquals(ArithmeticInfixExpression.Operator.PLUS, operator);
      assertNotEquals(ArithmeticInfixExpression.Operator.MINUS, operator);
      assertNotEquals(ArithmeticInfixExpression.Operator.TIMES, operator);
    }
    for (ArithmeticInfixExpression.Operator operator : ArithmeticUtilities.TUCKED_AWAY_INTEGER_ARITHMETIC_OPERATORS) {
      assertNotEquals(ArithmeticInfixExpression.Operator.PLUS, operator);
      assertNotEquals(ArithmeticInfixExpression.Operator.MINUS, operator);
      assertNotEquals(ArithmeticInfixExpression.Operator.TIMES, operator);
    }
  }

  @Test
  public void operatorArraysAreDistinctInstances() {
    assertNotSame(ArithmeticUtilities.PRIME_TIME_DOUBLE_ARITHMETIC_OPERATORS,
        ArithmeticUtilities.TUCKED_AWAY_DOUBLE_ARITHMETIC_OPERATORS);
    assertNotSame(ArithmeticUtilities.PRIME_TIME_INTEGER_ARITHMETIC_OPERATORS,
        ArithmeticUtilities.TUCKED_AWAY_INTEGER_ARITHMETIC_OPERATORS);
  }

  @Test
  public void tuckedAwayOperatorArraysDoNotContainDuplicates() {
    assertEquals(
        ArithmeticUtilities.TUCKED_AWAY_DOUBLE_ARITHMETIC_OPERATORS.length,
        java.util.Arrays.stream(ArithmeticUtilities.TUCKED_AWAY_DOUBLE_ARITHMETIC_OPERATORS).distinct().count());
    assertEquals(
        ArithmeticUtilities.TUCKED_AWAY_INTEGER_ARITHMETIC_OPERATORS.length,
        java.util.Arrays.stream(ArithmeticUtilities.TUCKED_AWAY_INTEGER_ARITHMETIC_OPERATORS).distinct().count());
  }
}
