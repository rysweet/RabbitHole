package org.alice.ide.croquet.edits.ast;

import org.alice.ide.ast.draganddrop.BlockStatementIndexPair;
import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.Expression;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.NullLiteral;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class InsertStatementEditExtendedTest {

  @Test
  public void constructorStoresBlockStatementIndexStatementAndExpressions() {
    BlockStatement blockStatement = new BlockStatement();
    BlockStatementIndexPair pair = new BlockStatementIndexPair(blockStatement, 3);
    ExpressionStatement statement = new ExpressionStatement(new NullLiteral());
    Expression[] initialExpressions = {new NullLiteral(), new NullLiteral()};

    InsertStatementEdit<?> edit = new InsertStatementEdit<>(null, pair, statement, initialExpressions);

    assertSame(blockStatement, edit.getBlockStatement());
    assertEquals(3, edit.getSpecifiedIndex());
    assertSame(statement, edit.getStatement());
    assertSame(initialExpressions, edit.getInitialExpressions());
  }

  @Test
  public void constructorWithoutExpressionsUsesEmptyArrayAndFalseEnveloping() throws Exception {
    BlockStatementIndexPair pair = new BlockStatementIndexPair(new BlockStatement(), 0);
    ExpressionStatement statement = new ExpressionStatement(new NullLiteral());

    InsertStatementEdit<?> edit = new InsertStatementEdit<>(null, pair, statement);

    assertNotNull(edit.getInitialExpressions());
    assertEquals(0, edit.getInitialExpressions().length);
    assertFalse(readIsEnveloping(edit));
  }

  @Test
  public void constructorWithEnvelopingFlagStoresTrue() throws Exception {
    BlockStatementIndexPair pair = new BlockStatementIndexPair(new BlockStatement(), 1);
    ExpressionStatement statement = new ExpressionStatement(new NullLiteral());

    InsertStatementEdit<?> edit = new InsertStatementEdit<>(null, pair, statement, new Expression[]{}, true);

    assertTrue(readIsEnveloping(edit));
  }

  @Test
  public void constructorPreservesAtEndIndex() {
    BlockStatementIndexPair pair = new BlockStatementIndexPair(new BlockStatement(), InsertStatementEdit.AT_END);
    ExpressionStatement statement = new ExpressionStatement(new NullLiteral());

    InsertStatementEdit<?> edit = new InsertStatementEdit<>(null, pair, statement);

    assertEquals(InsertStatementEdit.AT_END, edit.getSpecifiedIndex());
  }

  @Test
  public void atEndConstantMatchesShortMaxValue() {
    assertEquals(Short.MAX_VALUE, InsertStatementEdit.AT_END);
  }

  private boolean readIsEnveloping(InsertStatementEdit<?> edit) throws Exception {
    Field field = InsertStatementEdit.class.getDeclaredField("isEnveloping");
    field.setAccessible(true);
    return field.getBoolean(edit);
  }
}
