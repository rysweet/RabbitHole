package org.alice.stageide;

import org.junit.Test;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.JavaType;
import org.lgna.story.*;

import static org.junit.Assert.*;

public class StoryTypeComparatorTest {

  @Test
  public void singleton_isNotNull() {
    assertNotNull(StoryTypeComparator.SINGLETON);
  }

  @Test
  public void compare_booleanBeforeDouble() {
    AbstractType<?, ?, ?> boolType = JavaType.BOOLEAN_OBJECT_TYPE;
    AbstractType<?, ?, ?> doubleType = JavaType.DOUBLE_OBJECT_TYPE;
    assertTrue(StoryTypeComparator.SINGLETON.compare(boolType, doubleType) < 0);
  }

  @Test
  public void compare_doubleBeforeInteger() {
    AbstractType<?, ?, ?> doubleType = JavaType.DOUBLE_OBJECT_TYPE;
    AbstractType<?, ?, ?> intType = JavaType.INTEGER_OBJECT_TYPE;
    assertTrue(StoryTypeComparator.SINGLETON.compare(doubleType, intType) < 0);
  }

  @Test
  public void compare_integerBeforeString() {
    AbstractType<?, ?, ?> intType = JavaType.INTEGER_OBJECT_TYPE;
    AbstractType<?, ?, ?> stringType = JavaType.STRING_TYPE;
    assertTrue(StoryTypeComparator.SINGLETON.compare(intType, stringType) < 0);
  }

  @Test
  public void compare_stringBeforeSThing() {
    AbstractType<?, ?, ?> stringType = JavaType.STRING_TYPE;
    AbstractType<?, ?, ?> sThingType = JavaType.getInstance(SThing.class);
    assertTrue(StoryTypeComparator.SINGLETON.compare(stringType, sThingType) < 0);
  }

  @Test
  public void compare_sThingBeforeColor() {
    AbstractType<?, ?, ?> sThingType = JavaType.getInstance(SThing.class);
    AbstractType<?, ?, ?> colorType = JavaType.getInstance(Color.class);
    assertTrue(StoryTypeComparator.SINGLETON.compare(sThingType, colorType) < 0);
  }

  @Test
  public void compare_sameType_returnsZero() {
    AbstractType<?, ?, ?> boolType = JavaType.BOOLEAN_OBJECT_TYPE;
    assertEquals(0, StoryTypeComparator.SINGLETON.compare(boolType, boolType));
  }

  @Test
  public void compare_unknownTypes_comparesByName() {
    AbstractType<?, ?, ?> typeA = JavaType.getInstance(Runnable.class);
    AbstractType<?, ?, ?> typeB = JavaType.getInstance(Comparable.class);
    int result = StoryTypeComparator.SINGLETON.compare(typeA, typeB);
    int expected = typeA.getName().compareTo(typeB.getName());
    assertEquals(Integer.signum(expected), Integer.signum(result));
  }

  @Test
  public void compare_primitiveBeforeUnknown() {
    AbstractType<?, ?, ?> boolType = JavaType.BOOLEAN_OBJECT_TYPE;
    AbstractType<?, ?, ?> unknownType = JavaType.getInstance(Runnable.class);
    assertTrue(StoryTypeComparator.SINGLETON.compare(boolType, unknownType) < 0);
  }

  @Test
  public void compare_isConsistent() {
    AbstractType<?, ?, ?> a = JavaType.BOOLEAN_OBJECT_TYPE;
    AbstractType<?, ?, ?> b = JavaType.STRING_TYPE;
    int first = StoryTypeComparator.SINGLETON.compare(a, b);
    int second = StoryTypeComparator.SINGLETON.compare(a, b);
    assertEquals(first, second);
  }

  @Test
  public void compare_isAntiSymmetric() {
    AbstractType<?, ?, ?> a = JavaType.BOOLEAN_OBJECT_TYPE;
    AbstractType<?, ?, ?> b = JavaType.STRING_TYPE;
    int ab = StoryTypeComparator.SINGLETON.compare(a, b);
    int ba = StoryTypeComparator.SINGLETON.compare(b, a);
    assertEquals(-Integer.signum(ab), Integer.signum(ba));
  }
}
