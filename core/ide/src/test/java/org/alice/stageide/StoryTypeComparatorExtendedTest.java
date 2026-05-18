package org.alice.stageide;

import org.junit.Test;
import org.lgna.project.ast.AbstractType;
import org.lgna.project.ast.JavaType;
import org.lgna.story.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class StoryTypeComparatorExtendedTest {

  @Test
  public void primitiveTypes_orderedCorrectly() {
    List<AbstractType<?, ?, ?>> types = new ArrayList<>();
    types.add(JavaType.STRING_TYPE);
    types.add(JavaType.BOOLEAN_OBJECT_TYPE);
    types.add(JavaType.INTEGER_OBJECT_TYPE);
    types.add(JavaType.DOUBLE_OBJECT_TYPE);
    Collections.sort(types, StoryTypeComparator.SINGLETON);
    assertEquals(JavaType.BOOLEAN_OBJECT_TYPE, types.get(0));
    assertEquals(JavaType.DOUBLE_OBJECT_TYPE, types.get(1));
    assertEquals(JavaType.INTEGER_OBJECT_TYPE, types.get(2));
    assertEquals(JavaType.STRING_TYPE, types.get(3));
  }

  @Test
  public void colorBeforePaint() {
    AbstractType<?, ?, ?> colorType = JavaType.getInstance(Color.class);
    AbstractType<?, ?, ?> paintType = JavaType.getInstance(Paint.class);
    assertTrue(StoryTypeComparator.SINGLETON.compare(colorType, paintType) < 0);
  }

  @Test
  public void positionBeforeOrientation() {
    AbstractType<?, ?, ?> posType = JavaType.getInstance(Position.class);
    AbstractType<?, ?, ?> oriType = JavaType.getInstance(Orientation.class);
    assertTrue(StoryTypeComparator.SINGLETON.compare(posType, oriType) < 0);
  }

  @Test
  public void orientationBeforeVantagePoint() {
    AbstractType<?, ?, ?> oriType = JavaType.getInstance(Orientation.class);
    AbstractType<?, ?, ?> vpType = JavaType.getInstance(VantagePoint.class);
    assertTrue(StoryTypeComparator.SINGLETON.compare(oriType, vpType) < 0);
  }

  @Test
  public void sjoint_hasHighPriority() {
    AbstractType<?, ?, ?> jointType = JavaType.getInstance(SJoint.class);
    AbstractType<?, ?, ?> stringType = JavaType.STRING_TYPE;
    assertTrue(StoryTypeComparator.SINGLETON.compare(jointType, stringType) > 0);
  }

  @Test
  public void twoUnknownTypes_sortedByName() {
    AbstractType<?, ?, ?> typeA = JavaType.getInstance(Appendable.class);
    AbstractType<?, ?, ?> typeB = JavaType.getInstance(Readable.class);
    int result = StoryTypeComparator.SINGLETON.compare(typeA, typeB);
    int expected = typeA.getName().compareTo(typeB.getName());
    assertEquals(Integer.signum(expected), Integer.signum(result));
  }

  @Test
  public void compare_transitivity() {
    AbstractType<?, ?, ?> a = JavaType.BOOLEAN_OBJECT_TYPE;
    AbstractType<?, ?, ?> b = JavaType.getInstance(SThing.class);
    AbstractType<?, ?, ?> c = JavaType.getInstance(Color.class);
    int ab = StoryTypeComparator.SINGLETON.compare(a, b);
    int bc = StoryTypeComparator.SINGLETON.compare(b, c);
    int ac = StoryTypeComparator.SINGLETON.compare(a, c);
    if (ab < 0 && bc < 0) {
      assertTrue(ac < 0);
    }
  }

  @Test
  public void sameType_alwaysZero() {
    AbstractType<?, ?, ?> type = JavaType.getInstance(Position.class);
    for (int i = 0; i < 10; i++) {
      assertEquals(0, StoryTypeComparator.SINGLETON.compare(type, type));
    }
  }
}
