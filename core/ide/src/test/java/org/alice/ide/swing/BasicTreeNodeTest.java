package org.alice.ide.swing;

import edu.cmu.cs.dennisc.color.Color4f;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link BasicTreeNode} — scene graph tree node with difference tracking.
 */
public class BasicTreeNodeTest {

  // ---- construction ----

  @Test
  public void constructor_setsNameAndClassName() {
    Object obj = "test";
    BasicTreeNode node = new BasicTreeNode(obj);

    assertNotNull(node.name);
    assertNotNull(node.className);
    assertEquals("java.lang.String", node.className);
  }

  @Test
  public void constructor_setsHashCode() {
    String obj = "hello";
    BasicTreeNode node = new BasicTreeNode(obj);

    assertEquals(obj.hashCode(), node.hashCode);
  }

  // ---- toString ----

  @Test
  public void toString_containsClassName() {
    BasicTreeNode node = new BasicTreeNode("value");

    String result = node.toString();
    assertTrue(result.contains("String"));
  }

  @Test
  public void toString_containsHashCode() {
    Object obj = "test";
    BasicTreeNode node = new BasicTreeNode(obj);

    String result = node.toString();
    assertTrue(result.contains(String.valueOf(obj.hashCode())));
  }

  // ---- equals ----

  @Test
  public void equals_sameHashCode_returnsTrue() {
    String s = "hello";
    BasicTreeNode node1 = new BasicTreeNode(s);
    BasicTreeNode node2 = new BasicTreeNode(s);

    assertEquals(node1, node2);
  }

  @Test
  public void equals_differentHashCode_returnsFalse() {
    BasicTreeNode node1 = new BasicTreeNode("hello");
    BasicTreeNode node2 = new BasicTreeNode("world");

    assertNotEquals(node1, node2);
  }

  @Test
  public void equals_nonTreeNode_returnsFalse() {
    BasicTreeNode node = new BasicTreeNode("hello");
    assertFalse(node.equals("hello"));
  }

  // ---- compareTo ----

  @Test
  public void compareTo_sameHashCode_returnsZero() {
    String s = "same";
    BasicTreeNode node1 = new BasicTreeNode(s);
    BasicTreeNode node2 = new BasicTreeNode(s);

    assertEquals(0, node1.compareTo(node2));
  }

  @Test
  public void compareTo_lessThan_returnsNegative() {
    // We need objects where obj1.hashCode() < obj2.hashCode()
    // Integer hashCode is the value itself
    BasicTreeNode node1 = new BasicTreeNode(1);
    BasicTreeNode node2 = new BasicTreeNode(2);

    assertTrue(node1.compareTo(node2) < 0);
  }

  @Test
  public void compareTo_greaterThan_returnsPositive() {
    BasicTreeNode node1 = new BasicTreeNode(2);
    BasicTreeNode node2 = new BasicTreeNode(1);

    assertTrue(node1.compareTo(node2) > 0);
  }

  @Test
  public void compareTo_nonTreeNode_returnsZero() {
    BasicTreeNode node = new BasicTreeNode("test");
    assertEquals(0, node.compareTo("not a tree node"));
  }

  // ---- isDifferent ----

  @Test
  public void isDifferent_noneByDefault() {
    BasicTreeNode node = new BasicTreeNode("test");
    assertFalse(node.isDifferent());
  }

  @Test
  public void markDifferent_newNode_makesIsDifferentTrue() {
    BasicTreeNode node = new BasicTreeNode("test");
    node.markDifferent(BasicTreeNode.Difference.NEW_NODE);
    assertTrue(node.isDifferent());
    assertEquals(BasicTreeNode.Difference.NEW_NODE, node.difference);
  }

  @Test
  public void markDifferent_attributes_makesIsDifferentTrue() {
    BasicTreeNode node = new BasicTreeNode("test");
    node.markDifferent(BasicTreeNode.Difference.ATTRIBUTES);
    assertTrue(node.isDifferent());
  }

  @Test
  public void markDifferent_none_makesIsDifferentFalse() {
    BasicTreeNode node = new BasicTreeNode("test");
    node.markDifferent(BasicTreeNode.Difference.NEW_NODE);
    node.markDifferent(BasicTreeNode.Difference.NONE);
    assertFalse(node.isDifferent());
  }

  // ---- isDifferent (comparing two nodes) ----

  @Test
  public void isDifferentFromOther_sameHashCode_returnsFalse() {
    BasicTreeNode node1 = new BasicTreeNode("same");
    BasicTreeNode node2 = new BasicTreeNode("same");
    assertFalse(node1.isDifferent(node2));
  }

  @Test
  public void isDifferentFromOther_differentHashCode_returnsTrue() {
    BasicTreeNode node1 = new BasicTreeNode("abc");
    BasicTreeNode node2 = new BasicTreeNode("xyz");
    assertTrue(node1.isDifferent(node2));
  }

  // ---- getMatchingNode by hash ----

  @Test
  public void getMatchingNode_byHashCode_findsItself() {
    Object obj = "findme";
    BasicTreeNode node = new BasicTreeNode(obj);

    BasicTreeNode found = node.getMatchingNode(obj.hashCode());
    assertSame(node, found);
  }

  @Test
  public void getMatchingNode_byHashCode_findsChild() {
    BasicTreeNode parent = new BasicTreeNode("parent");
    Object childObj = "child";
    BasicTreeNode child = new BasicTreeNode(childObj);
    parent.add(child);

    BasicTreeNode found = parent.getMatchingNode(childObj.hashCode());
    assertSame(child, found);
  }

  @Test
  public void getMatchingNode_byHashCode_findsGrandchild() {
    BasicTreeNode root = new BasicTreeNode("root");
    BasicTreeNode mid = new BasicTreeNode("mid");
    Object leafObj = "leaf";
    BasicTreeNode leaf = new BasicTreeNode(leafObj);
    root.add(mid);
    mid.add(leaf);

    BasicTreeNode found = root.getMatchingNode(leafObj.hashCode());
    assertSame(leaf, found);
  }

  @Test
  public void getMatchingNode_byHashCode_notFound_returnsNull() {
    BasicTreeNode node = new BasicTreeNode("only");
    assertNull(node.getMatchingNode(99999));
  }

  // ---- getMatchingNode by BasicTreeNode ----

  @Test
  public void getMatchingNode_byNode_findsMatch() {
    String sameObj = "match";
    BasicTreeNode tree = new BasicTreeNode(sameObj);
    BasicTreeNode target = new BasicTreeNode(sameObj);

    BasicTreeNode found = tree.getMatchingNode(target);
    assertSame(tree, found);
  }

  @Test
  public void getMatchingNode_byNode_findsInChildren() {
    BasicTreeNode parent = new BasicTreeNode("parent");
    String childKey = "child";
    BasicTreeNode child = new BasicTreeNode(childKey);
    parent.add(child);

    BasicTreeNode target = new BasicTreeNode(childKey);
    BasicTreeNode found = parent.getMatchingNode(target);
    assertSame(child, found);
  }

  // ---- hasDifferentChild ----

  @Test
  public void hasDifferentChild_noDifferences_returnsFalse() {
    BasicTreeNode parent = new BasicTreeNode("parent");
    BasicTreeNode child = new BasicTreeNode("child");
    parent.add(child);

    assertFalse(parent.hasDifferentChild());
  }

  @Test
  public void hasDifferentChild_childMarkedDifferent_returnsTrue() {
    BasicTreeNode parent = new BasicTreeNode("parent");
    BasicTreeNode child = new BasicTreeNode("child");
    child.markDifferent(BasicTreeNode.Difference.NEW_NODE);
    parent.add(child);

    assertTrue(parent.hasDifferentChild());
  }

  @Test
  public void hasDifferentChild_grandchildMarkedDifferent_returnsTrue() {
    BasicTreeNode root = new BasicTreeNode("root");
    BasicTreeNode mid = new BasicTreeNode("mid");
    BasicTreeNode leaf = new BasicTreeNode("leaf");
    leaf.markDifferent(BasicTreeNode.Difference.ATTRIBUTES);
    root.add(mid);
    mid.add(leaf);

    assertTrue(root.hasDifferentChild());
  }

  // ---- getAWTColor ----

  @Test
  public void getAWTColor_nullColor_returnsNull() {
    BasicTreeNode node = new BasicTreeNode("test");
    assertNull(node.getAWTColor());
  }

  @Test
  public void getAWTColor_withColor_returnsAWTColor() {
    BasicTreeNode node = new BasicTreeNode("test");
    node.color = new Color4f(1.0f, 0.5f, 0.0f, 1.0f);

    java.awt.Color awtColor = node.getAWTColor();
    assertNotNull(awtColor);
    assertEquals(255, awtColor.getRed());
    assertEquals(127, awtColor.getGreen());
    assertEquals(0, awtColor.getBlue());
  }
}
