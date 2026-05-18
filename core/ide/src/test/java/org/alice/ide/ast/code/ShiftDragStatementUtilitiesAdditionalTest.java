package org.alice.ide.ast.code;

import org.alice.ide.ast.draganddrop.BlockStatementIndexPair;
import org.junit.Test;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.ExpressionStatement;
import org.lgna.project.ast.NullLiteral;
import org.lgna.project.ast.Statement;

import static org.junit.Assert.*;

/**
 * Additional characterization tests for {@link ShiftDragStatementUtilities}.
 */
public class ShiftDragStatementUtilitiesAdditionalTest {

  private Statement createDummyStatement() {
    return new ExpressionStatement(new NullLiteral());
  }

  @Test
  public void calculateShiftMoveCount_sameBlock_fromAfterTo_returnsCountRemaining() {
    BlockStatement block = new BlockStatement();
    block.statements.add(createDummyStatement());
    block.statements.add(createDummyStatement());
    block.statements.add(createDummyStatement());

    BlockStatementIndexPair from = new BlockStatementIndexPair(block, 2);
    BlockStatementIndexPair to = new BlockStatementIndexPair(block, 0);
    int count = ShiftDragStatementUtilities.calculateShiftMoveCount(from, to);
    assertEquals(1, count); // size(3) - fromIndex(2) = 1
  }

  @Test
  public void calculateShiftMoveCount_sameBlock_fromBeforeTo_returnsZero() {
    BlockStatement block = new BlockStatement();
    block.statements.add(createDummyStatement());
    block.statements.add(createDummyStatement());
    block.statements.add(createDummyStatement());

    BlockStatementIndexPair from = new BlockStatementIndexPair(block, 0);
    BlockStatementIndexPair to = new BlockStatementIndexPair(block, 2);
    int count = ShiftDragStatementUtilities.calculateShiftMoveCount(from, to);
    assertEquals(0, count);
  }

  @Test
  public void calculateShiftMoveCount_sameBlock_sameIndex_returnsZero() {
    BlockStatement block = new BlockStatement();
    block.statements.add(createDummyStatement());
    block.statements.add(createDummyStatement());

    BlockStatementIndexPair from = new BlockStatementIndexPair(block, 1);
    BlockStatementIndexPair to = new BlockStatementIndexPair(block, 1);
    int count = ShiftDragStatementUtilities.calculateShiftMoveCount(from, to);
    assertEquals(0, count);
  }

  @Test
  public void calculateShiftMoveCount_differentBlocks_returnsCount() {
    BlockStatement fromBlock = new BlockStatement();
    fromBlock.statements.add(createDummyStatement());
    fromBlock.statements.add(createDummyStatement());
    fromBlock.statements.add(createDummyStatement());

    BlockStatement toBlock = new BlockStatement();

    BlockStatementIndexPair from = new BlockStatementIndexPair(fromBlock, 1);
    BlockStatementIndexPair to = new BlockStatementIndexPair(toBlock, 0);
    int count = ShiftDragStatementUtilities.calculateShiftMoveCount(from, to);
    assertEquals(2, count); // statements at index 1 and 2
  }

  @Test
  public void calculateShiftMoveCount_differentBlocks_fromLastIndex() {
    BlockStatement fromBlock = new BlockStatement();
    fromBlock.statements.add(createDummyStatement());
    fromBlock.statements.add(createDummyStatement());

    BlockStatement toBlock = new BlockStatement();

    BlockStatementIndexPair from = new BlockStatementIndexPair(fromBlock, 1);
    BlockStatementIndexPair to = new BlockStatementIndexPair(toBlock, 0);
    int count = ShiftDragStatementUtilities.calculateShiftMoveCount(from, to);
    assertEquals(1, count);
  }

  @Test
  public void calculateShiftMoveCount_sameBlock_fromIndex0_singleElement_returnsZero() {
    BlockStatement block = new BlockStatement();
    block.statements.add(createDummyStatement());

    BlockStatementIndexPair from = new BlockStatementIndexPair(block, 0);
    BlockStatementIndexPair to = new BlockStatementIndexPair(block, 0);
    int count = ShiftDragStatementUtilities.calculateShiftMoveCount(from, to);
    assertEquals(0, count);
  }

  @Test
  public void calculateShiftMoveCount_differentBlocks_emptyFromBlock() {
    BlockStatement fromBlock = new BlockStatement();
    BlockStatement toBlock = new BlockStatement();

    BlockStatementIndexPair from = new BlockStatementIndexPair(fromBlock, 0);
    BlockStatementIndexPair to = new BlockStatementIndexPair(toBlock, 0);
    int count = ShiftDragStatementUtilities.calculateShiftMoveCount(from, to);
    assertEquals(0, count);
  }
}
