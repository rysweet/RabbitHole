package org.alice.ide.ast.fieldtree;

import org.junit.Test;
import org.lgna.project.ast.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Additional characterization tests for {@link FieldTree}.
 */
public class FieldTreeAdditionalTest {

  @Test
  public void typeCollapseThresholdData_largeTresholdValues() {
    FieldTree.TypeCollapseThresholdData data =
        new FieldTree.TypeCollapseThresholdData(Object.class, Integer.MAX_VALUE, Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, data.getCollapseThreshold());
    assertEquals(Integer.MAX_VALUE, data.getCollapseThresholdForDescendants());
  }

  @Test
  public void typeCollapseThresholdData_zeroThresholds() {
    FieldTree.TypeCollapseThresholdData data =
        new FieldTree.TypeCollapseThresholdData(String.class, 0, 0);
    assertEquals(0, data.getCollapseThreshold());
    assertEquals(0, data.getCollapseThresholdForDescendants());
  }

  @Test
  public void typeCollapseThresholdData_differentTypes() {
    FieldTree.TypeCollapseThresholdData d1 =
        new FieldTree.TypeCollapseThresholdData(String.class, 5, 3);
    FieldTree.TypeCollapseThresholdData d2 =
        new FieldTree.TypeCollapseThresholdData(Integer.class, 10, 7);
    assertNotEquals(d1.getType(), d2.getType());
    assertNotEquals(d1.getCollapseThreshold(), d2.getCollapseThreshold());
  }

  @Test
  public void createFirstClassThreshold_differentTypes() {
    FieldTree.TypeCollapseThresholdData d1 =
        FieldTree.createFirstClassThreshold(String.class);
    FieldTree.TypeCollapseThresholdData d2 =
        FieldTree.createFirstClassThreshold(Integer.class);
    assertNotEquals(d1.getType(), d2.getType());
  }

  @Test
  public void createSecondClassThreshold_differentTypes() {
    FieldTree.TypeCollapseThresholdData d1 =
        FieldTree.createSecondClassThreshold(String.class);
    FieldTree.TypeCollapseThresholdData d2 =
        FieldTree.createSecondClassThreshold(Integer.class);
    assertNotEquals(d1.getType(), d2.getType());
  }

  @Test
  public void createTreeFor_emptyList_rootNotNull() {
    List<UserField> empty = Collections.emptyList();
    Object tree = FieldTree.createTreeFor(empty);
    assertNotNull(tree);
  }

  @Test
  public void createTreeFor_emptyList_rootNotNullAgain() {
    List<UserField> empty = Collections.emptyList();
    Object tree = FieldTree.createTreeFor(empty);
    assertNotNull("Tree from empty list should not be null", tree);
  }

  @Test
  public void firstClassThreshold_largerThanSecondClass() {
    FieldTree.TypeCollapseThresholdData first =
        FieldTree.createFirstClassThreshold(Object.class);
    FieldTree.TypeCollapseThresholdData second =
        FieldTree.createSecondClassThreshold(Object.class);
    assertTrue(first.getCollapseThreshold() >= second.getCollapseThreshold());
  }

  @Test
  public void typeCollapseThresholdData_getType_fromJavaType() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Double.class);
    FieldTree.TypeCollapseThresholdData data =
        new FieldTree.TypeCollapseThresholdData(type, 5, 3);
    assertSame(type, data.getType());
  }

  @Test
  public void typeCollapseThresholdData_negativeThreshold_accepted() {
    FieldTree.TypeCollapseThresholdData data =
        new FieldTree.TypeCollapseThresholdData(Object.class, -1, -1);
    assertEquals(-1, data.getCollapseThreshold());
    assertEquals(-1, data.getCollapseThresholdForDescendants());
  }
}
