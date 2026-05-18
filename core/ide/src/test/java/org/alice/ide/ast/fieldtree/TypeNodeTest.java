package org.alice.ide.ast.fieldtree;

import org.junit.Test;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.UserField;

import static org.junit.Assert.*;

public class TypeNodeTest {

  @Test
  public void createAndAddToParent_addsToParentTypeNodes() {
    RootNode root = new RootNode();

    TypeNode child = TypeNode.createAndAddToParent(root, JavaType.getInstance(String.class), 5, 3);

    assertTrue(root.getTypeNodes().contains(child));
  }

  @Test
  public void getCollapseThreshold_returnsConstructorValue() {
    TypeNode typeNode = new TypeNode(null, JavaType.getInstance(String.class), 7, 4);

    assertEquals(7, typeNode.getCollapseThreshold());
  }

  @Test
  public void getCollapseThresholdForDescendants_returnsConstructorValue() {
    TypeNode typeNode = new TypeNode(null, JavaType.getInstance(String.class), 7, 4);

    assertEquals(4, typeNode.getCollapseThresholdForDescendants());
  }

  @Test
  public void getTypeNodes_initiallyEmpty() {
    TypeNode typeNode = new TypeNode(null, JavaType.getInstance(String.class), 7, 4);

    assertTrue(typeNode.getTypeNodes().isEmpty());
  }

  @Test
  public void getFieldNodes_initiallyEmpty() {
    TypeNode typeNode = new TypeNode(null, JavaType.getInstance(String.class), 7, 4);

    assertTrue(typeNode.getFieldNodes().isEmpty());
  }

  @Test
  public void collapseIfAppropriate_fieldsBelowThreshold_movesToParent() {
    RootNode root = new RootNode();
    TypeNode child = TypeNode.createAndAddToParent(root, JavaType.getInstance(String.class), 5, 5);
    FieldNode.createAndAddToParent(child, createField("alpha"));
    FieldNode.createAndAddToParent(child, createField("beta"));

    root.collapseIfAppropriate();

    assertEquals(0, child.getFieldNodes().size());
    assertEquals(2, root.getFieldNodes().size());
  }

  @Test
  public void collapseIfAppropriate_fieldsAboveThreshold_staysInChild() {
    RootNode root = new RootNode();
    TypeNode child = TypeNode.createAndAddToParent(root, JavaType.getInstance(String.class), 1, 1);
    FieldNode.createAndAddToParent(child, createField("alpha"));
    FieldNode.createAndAddToParent(child, createField("beta"));
    FieldNode.createAndAddToParent(child, createField("gamma"));

    root.collapseIfAppropriate();

    assertEquals(3, child.getFieldNodes().size());
    assertTrue(root.getFieldNodes().isEmpty());
  }

  @Test
  public void removeEmptyTypeNodes_emptyChild_removedFromParent() {
    RootNode root = new RootNode();
    TypeNode child = TypeNode.createAndAddToParent(root, JavaType.getInstance(String.class), 5, 5);

    root.removeEmptyTypeNodes();

    assertFalse(root.getTypeNodes().contains(child));
  }

  @Test
  public void removeEmptyTypeNodes_nonEmptyChild_keptInParent() {
    RootNode root = new RootNode();
    TypeNode child = TypeNode.createAndAddToParent(root, JavaType.getInstance(String.class), 5, 5);
    FieldNode.createAndAddToParent(child, createField("alpha"));

    root.removeEmptyTypeNodes();

    assertTrue(root.getTypeNodes().contains(child));
  }

  @Test
  public void sort_fieldNodesAreSorted() {
    RootNode root = new RootNode();
    FieldNode.createAndAddToParent(root, createField("charlie"));
    FieldNode.createAndAddToParent(root, createField("alpha"));
    FieldNode.createAndAddToParent(root, createField("beta"));

    root.sort();

    assertEquals("alpha", root.getFieldNodes().get(0).getDeclaration().getName());
    assertEquals("beta", root.getFieldNodes().get(1).getDeclaration().getName());
    assertEquals("charlie", root.getFieldNodes().get(2).getDeclaration().getName());
  }

  private static UserField createField(String name) {
    UserField field = new UserField();
    field.name.setValue(name);
    field.valueType.setValue(JavaType.getInstance(String.class));
    return field;
  }
}
