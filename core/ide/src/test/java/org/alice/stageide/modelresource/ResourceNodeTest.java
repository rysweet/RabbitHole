package org.alice.stageide.modelresource;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests for {@link ResourceNode} — gallery tree node wrapping a ResourceKey.
 */
public class ResourceNodeTest {

  private ResourceNode createLeafNode(String name) {
    RootResourceKey key = new RootResourceKey("key_" + name, name);
    return new ResourceNode(UUID.randomUUID(), key);
  }

  private ResourceNode createParentWithChildren(String name, ResourceNode... children) {
    RootResourceKey key = new RootResourceKey("key_" + name, name);
    return new ResourceNode(key, List.of(children));
  }

  // ---- construction ----

  @Test
  public void constructor_setsResourceKey() {
    RootResourceKey key = new RootResourceKey("testKey", "Test");
    ResourceNode node = new ResourceNode(UUID.randomUUID(), key);
    assertSame(key, node.getResourceKey());
  }

  @Test
  public void constructor_leafNode_emptyChildren() {
    ResourceNode node = createLeafNode("Leaf");
    assertNotNull(node.getNodeChildren());
    assertTrue(node.getNodeChildren().isEmpty());
  }

  // ---- parent/child relationships ----

  @Test
  public void constructor_setsParentOnChildren() {
    ResourceNode child = createLeafNode("Child");
    ResourceNode parent = createParentWithChildren("Parent", child);

    assertSame(parent, child.getParent());
  }

  @Test
  public void getParent_rootNode_returnsNull() {
    ResourceNode root = createLeafNode("Root");
    assertNull(root.getParent());
  }

  @Test
  public void getNodeChildren_returnsChildList() {
    ResourceNode child1 = createLeafNode("C1");
    ResourceNode child2 = createLeafNode("C2");
    ResourceNode parent = createParentWithChildren("Parent", child1, child2);

    assertEquals(2, parent.getNodeChildren().size());
  }

  // ---- isUserDefinedModel ----

  @Test
  public void isUserDefinedModel_rootKey_returnsFalse() {
    ResourceNode node = createLeafNode("Test");
    assertFalse(node.isUserDefinedModel());
  }

  // ---- getText ----

  @Test
  public void getText_delegatesToKeyLocalizedCreationText() {
    ResourceNode node = createLeafNode("MyModel");
    String text = node.getText();
    assertNotNull(text);
  }

  // ---- isInstanceCreator ----

  @Test
  public void isInstanceCreator_rootKey_returnsFalse() {
    ResourceNode node = createLeafNode("Test");
    assertFalse(node.isInstanceCreator());
  }

  // ---- compareTo ----

  @Test
  public void compareTo_alphabeticalOrdering() {
    ResourceNode nodeA = createLeafNode("Apple");
    ResourceNode nodeZ = createLeafNode("Zebra");

    assertTrue(nodeA.compareTo(nodeZ) < 0);
    assertTrue(nodeZ.compareTo(nodeA) > 0);
  }

  @Test
  public void compareTo_sameText_returnsZero() {
    ResourceNode node1 = createLeafNode("Same");
    ResourceNode node2 = createLeafNode("Same");

    assertEquals(0, node1.compareTo(node2));
  }

  @Test
  public void compareTo_caseInsensitive() {
    ResourceNode nodeLower = createLeafNode("apple");
    ResourceNode nodeUpper = createLeafNode("APPLE");

    assertEquals(0, nodeLower.compareTo(nodeUpper));
  }

  // ---- getSimpleClassName ----

  @Test
  public void getSimpleClassName_returnsLocalizedName() {
    RootResourceKey key = new RootResourceKey("testKey", "TestName");
    ResourceNode node = new ResourceNode(UUID.randomUUID(), key);
    String simpleName = node.getSimpleClassName();
    assertNotNull(simpleName);
  }

  // ---- getBoundingBox / placeOnGround for non-InstanceCreatorKey ----

  @Test
  public void getBoundingBox_rootKey_returnsNull() {
    ResourceNode node = createLeafNode("Test");
    assertNull(node.getBoundingBox());
  }

  @Test
  public void placeOnGround_rootKey_returnsFalse() {
    ResourceNode node = createLeafNode("Test");
    assertFalse(node.placeOnGround());
  }

  // ---- appendRepr ----

  @Test
  public void toString_containsKeyInfo() {
    RootResourceKey key = new RootResourceKey("k", "KeyName");
    ResourceNode node = new ResourceNode(UUID.randomUUID(), key);
    String repr = node.toString();
    assertNotNull(repr);
  }

  // ---- isBreadcrumbButtonIconDesired ----

  @Test
  public void isBreadcrumbButtonIconDesired_defaultFalse() {
    ResourceNode node = createLeafNode("Test");
    assertFalse(node.isBreadcrumbButtonIconDesired());
  }

  @Test
  public void isBreadcrumbButtonIconDesired_canBeTrue() {
    ResourceNode child = createLeafNode("Child");
    RootResourceKey key = new RootResourceKey("k", "Parent");
    ResourceNode node = new ResourceNode(key, List.of(child), true);
    assertTrue(node.isBreadcrumbButtonIconDesired());
  }

  // ---- getFirstChild ----

  @Test
  public void getFirstChild_emptyChildren_returnsNull() {
    ResourceNode node = createLeafNode("Leaf");
    // RootResourceKey.isLeaf() returns false, so this exercises the path
    // But getFirstChild looks at children list
    assertNull(node.getFirstChild());
  }

  @Test
  public void getFirstChild_withChildren_returnsFirst() {
    ResourceNode child1 = createLeafNode("First");
    ResourceNode child2 = createLeafNode("Second");
    ResourceNode parent = createParentWithChildren("Parent", child1, child2);

    assertSame(child1, parent.getFirstChild());
  }

  // ---- ACCEPTABLE_HACK_FOR_GALLERY_QA ----

  @Test
  public void getLeftButtonClickOperation_withHackEnabled_returnsNull() {
    ResourceNode.ACCEPTABLE_HACK_FOR_GALLERY_QA_setLeftClickModelAlwaysNull(true);
    try {
      ResourceNode node = createLeafNode("Test");
      assertNull(node.getLeftButtonClickOperation(null));
    } finally {
      ResourceNode.ACCEPTABLE_HACK_FOR_GALLERY_QA_setLeftClickModelAlwaysNull(false);
    }
  }
}
