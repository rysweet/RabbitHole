package org.alice.ide.ast.fieldtree;

import org.junit.Test;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.ManagementLevel;
import org.lgna.project.ast.NamedUserType;
import org.lgna.project.ast.UserField;

import static org.junit.Assert.*;

public class FieldtreeNodeTest {

  private UserField createField(String name) {
    UserField field = new UserField();
    field.name.setValue(name);
    field.valueType.setValue(JavaType.DOUBLE_OBJECT_TYPE);
    field.managementLevel.setValue(ManagementLevel.NONE);
    return field;
  }

  private TypeNode createTypeNode(String name) {
    NamedUserType type = new NamedUserType();
    type.name.setValue(name);
    type.superType.setValue(JavaType.getInstance(Object.class));
    return TypeNode.createAndAddToParent(null, type, 0, 0);
  }

  @Test
  public void fieldNode_getDeclaration_returnsField() {
    TypeNode parent = createTypeNode("Parent");
    UserField field = createField("speed");
    FieldNode node = FieldNode.createAndAddToParent(parent, field);
    assertSame(field, node.getDeclaration());
  }

  @Test
  public void fieldNode_getParent_returnsTypeNode() {
    TypeNode parent = createTypeNode("Parent");
    UserField field = createField("speed");
    FieldNode node = FieldNode.createAndAddToParent(parent, field);
    assertSame(parent, node.getParent());
  }

  @Test
  public void fieldNode_toString_containsClassName() {
    TypeNode parent = createTypeNode("Parent");
    UserField field = createField("myField");
    FieldNode node = FieldNode.createAndAddToParent(parent, field);
    assertTrue(node.toString().contains("FieldNode"));
  }

  @Test
  public void fieldNode_toString_containsFieldName() {
    TypeNode parent = createTypeNode("Parent");
    UserField field = createField("myField");
    FieldNode node = FieldNode.createAndAddToParent(parent, field);
    assertTrue(node.toString().contains("myField"));
  }

  @Test
  public void fieldNode_compareTo_alphabetical() {
    TypeNode parent = createTypeNode("Parent");
    FieldNode nodeA = FieldNode.createAndAddToParent(parent, createField("alpha"));
    FieldNode nodeB = FieldNode.createAndAddToParent(parent, createField("bravo"));
    assertTrue(nodeA.compareTo(nodeB) < 0);
    assertTrue(nodeB.compareTo(nodeA) > 0);
  }

  @Test
  public void fieldNode_compareTo_sameNameReturnsZero() {
    TypeNode parent = createTypeNode("Parent");
    FieldNode node1 = FieldNode.createAndAddToParent(parent, createField("same"));
    FieldNode node2 = FieldNode.createAndAddToParent(parent, createField("same"));
    assertEquals(0, node1.compareTo(node2));
  }

  @Test
  public void fieldNode_compareTo_caseInsensitive() {
    TypeNode parent = createTypeNode("Parent");
    FieldNode nodeUpper = FieldNode.createAndAddToParent(parent, createField("ALPHA"));
    FieldNode nodeLower = FieldNode.createAndAddToParent(parent, createField("alpha"));
    assertEquals(0, nodeUpper.compareTo(nodeLower));
  }

  @Test
  public void createAndAddToParent_addsToParentFieldNodes() {
    TypeNode parent = createTypeNode("Parent");
    int before = parent.getFieldNodes().size();
    FieldNode.createAndAddToParent(parent, createField("newField"));
    assertEquals(before + 1, parent.getFieldNodes().size());
  }

  @Test
  public void fieldNode_getDeclarationName() {
    TypeNode parent = createTypeNode("Parent");
    FieldNode node = FieldNode.createAndAddToParent(parent, createField("speed"));
    assertEquals("speed", node.getDeclaration().getName());
  }
}
