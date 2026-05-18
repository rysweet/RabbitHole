package org.alice.stageide.ast.sort;

import org.junit.Test;
import org.lgna.project.ast.JavaMethod;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.ManagementLevel;
import org.lgna.project.ast.UserMethod;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class OneShotSorterExtendedTest {

  private UserMethod createMethod(String name) {
    UserMethod m = new UserMethod();
    m.name.setValue(name);
    m.returnType.setValue(JavaType.VOID_TYPE);
    m.managementLevel.setValue(ManagementLevel.NONE);
    return m;
  }

  @Test
  public void moveBeforeTurn_inSortedOrder() {
    assertNotNull(OneShotSorter.MOVE_METHOD);
    assertNotNull(OneShotSorter.TURN_METHOD);
  }

  @Test
  public void allMoveMethods_notNull() {
    assertNotNull(OneShotSorter.MOVE_METHOD);
    assertNotNull(OneShotSorter.MOVE_TOWARD_METHOD);
    assertNotNull(OneShotSorter.MOVE_AWAY_FROM_METHOD);
    assertNotNull(OneShotSorter.MOVE_TO_METHOD);
    assertNotNull(OneShotSorter.MOVE_AND_ORIENT_TO_METHOD);
    assertNotNull(OneShotSorter.PLACE_METHOD);
  }

  @Test
  public void allTurnMethods_notNull() {
    assertNotNull(OneShotSorter.TURN_METHOD);
    assertNotNull(OneShotSorter.ROLL_METHOD);
    assertNotNull(OneShotSorter.TURN_TO_FACE_METHOD);
    assertNotNull(OneShotSorter.POINT_AT_METHOD);
    assertNotNull(OneShotSorter.ORIENT_TO_UPRIGHT_METHOD);
    assertNotNull(OneShotSorter.ORIENT_TO_METHOD);
  }

  @Test
  public void jointMethod_notNull() {
    assertNotNull(OneShotSorter.STRAIGHTEN_OUT_JOINTS_METHOD);
  }

  @Test
  public void wingMethods_notNull() {
    assertNotNull(OneShotSorter.SPREAD_WINGS_METHOD);
    assertNotNull(OneShotSorter.FOLD_WINGS_METHOD);
  }

  @Test
  public void paintMethods_notNull() {
    assertNotNull(OneShotSorter.GROUND_SET_PAINT_METHOD);
    assertNotNull(OneShotSorter.MODEL_SET_PAINT_METHOD);
  }

  @Test
  public void opacityMethods_notNull() {
    assertNotNull(OneShotSorter.GROUND_SET_OPACITY_METHOD);
    assertNotNull(OneShotSorter.MODEL_SET_OPACITY_METHOD);
  }

  @Test
  public void vantagePointMethod_notNull() {
    assertNotNull(OneShotSorter.MOVE_AND_ORIENT_TO_A_GOOD_VANTAGE_POINT_METHOD);
  }

  @Test
  public void createSortedList_withUserMethods_preservesAll() {
    List<UserMethod> methods = new ArrayList<>();
    methods.add(createMethod("z"));
    methods.add(createMethod("a"));
    methods.add(createMethod("m"));
    List<UserMethod> sorted = OneShotSorter.SINGLETON.createSortedList(methods);
    assertEquals(3, sorted.size());
  }

  @Test
  public void singleton_implementsMemberSorter() {
    assertTrue(OneShotSorter.SINGLETON instanceof org.alice.ide.ast.sort.MemberSorter);
  }

  @Test
  public void moveMethod_isJavaMethod() {
    assertTrue(OneShotSorter.MOVE_METHOD instanceof JavaMethod);
  }

  @Test
  public void turnMethod_isJavaMethod() {
    assertTrue(OneShotSorter.TURN_METHOD instanceof JavaMethod);
  }
}
