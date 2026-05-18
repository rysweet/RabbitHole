package org.alice.ide.ast.draganddrop;

import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.Statement;

import static org.junit.Assert.*;

/**
 * Extended characterization tests for {@link BlockStatementIndexPair}.
 */
public class BlockStatementIndexPairExtendedTest {

  @Test
  public void constructor_storesBlockStatementAndIndex() {
    BlockStatement block = new BlockStatement();
    BlockStatementIndexPair pair = new BlockStatementIndexPair(block, 0);
    assertSame(block, pair.getBlockStatement());
    assertEquals(0, pair.getIndex());
  }

  @Test
  public void constructor_positiveIndex() {
    BlockStatement block = new BlockStatement();
    BlockStatementIndexPair pair = new BlockStatementIndexPair(block, 5);
    assertEquals(5, pair.getIndex());
  }

  @Test
  public void constructor_zeroIndex() {
    BlockStatement block = new BlockStatement();
    BlockStatementIndexPair pair = new BlockStatementIndexPair(block, 0);
    assertEquals(0, pair.getIndex());
  }

  @Test
  public void getBlockStatement_returnsSameInstance() {
    BlockStatement block = new BlockStatement();
    BlockStatementIndexPair pair = new BlockStatementIndexPair(block, 0);
    assertSame(pair.getBlockStatement(), pair.getBlockStatement());
  }

  @Test
  public void createInstanceFromChildStatement_findsCorrectIndex() {
    BlockStatement block = new BlockStatement();
    Statement s1 = new ExpressionStatement(new NullLiteral());
    Statement s2 = new ExpressionStatement(new NullLiteral());
    Statement s3 = new ExpressionStatement(new NullLiteral());
    block.statements.add(s1);
    block.statements.add(s2);
    block.statements.add(s3);

    BlockStatementIndexPair pair = BlockStatementIndexPair.createInstanceFromChildStatement(s2);
    assertSame(block, pair.getBlockStatement());
    assertEquals(1, pair.getIndex());
  }

  @Test
  public void createInstanceFromChildStatement_firstChild() {
    BlockStatement block = new BlockStatement();
    Statement s1 = new ExpressionStatement(new NullLiteral());
    block.statements.add(s1);

    BlockStatementIndexPair pair = BlockStatementIndexPair.createInstanceFromChildStatement(s1);
    assertEquals(0, pair.getIndex());
  }

  @Test
  public void createInstanceFromChildStatement_lastChild() {
    BlockStatement block = new BlockStatement();
    Statement s1 = new ExpressionStatement(new NullLiteral());
    Statement s2 = new ExpressionStatement(new NullLiteral());
    Statement s3 = new ExpressionStatement(new NullLiteral());
    block.statements.add(s1);
    block.statements.add(s2);
    block.statements.add(s3);

    BlockStatementIndexPair pair = BlockStatementIndexPair.createInstanceFromChildStatement(s3);
    assertEquals(2, pair.getIndex());
  }

  @Test
  public void twoPairs_sameBlock_differentIndex_notEqual() {
    BlockStatement block = new BlockStatement();
    BlockStatementIndexPair p1 = new BlockStatementIndexPair(block, 0);
    BlockStatementIndexPair p2 = new BlockStatementIndexPair(block, 1);
    // They are different objects with different indices
    assertNotSame(p1, p2);
  }

  @Test
  public void twoPairs_differentBlocks_sameIndex() {
    BlockStatement b1 = new BlockStatement();
    BlockStatement b2 = new BlockStatement();
    BlockStatementIndexPair p1 = new BlockStatementIndexPair(b1, 0);
    BlockStatementIndexPair p2 = new BlockStatementIndexPair(b2, 0);
    assertNotSame(p1.getBlockStatement(), p2.getBlockStatement());
  }

  @Test
  public void largeIndex_accepted() {
    BlockStatement block = new BlockStatement();
    BlockStatementIndexPair pair = new BlockStatementIndexPair(block, 1000);
    assertEquals(1000, pair.getIndex());
  }
}
