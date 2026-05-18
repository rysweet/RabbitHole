package org.alice.ide.ast.sort;

import org.junit.Test;
import org.lgna.project.ast.JavaType;
import org.lgna.project.ast.ManagementLevel;
import org.lgna.project.ast.UserMethod;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class MemberSorterTest {

  private UserMethod createMethod(String name) {
    UserMethod method = new UserMethod();
    method.name.setValue(name);
    method.returnType.setValue(JavaType.VOID_TYPE);
    method.managementLevel.setValue(ManagementLevel.NONE);
    return method;
  }

  @Test
  public void alphabeticalSorter_implementsMemberSorter() {
    MemberSorter sorter = AlphabeticalMemberSorter.SINGLETON;
    assertNotNull(sorter);
  }

  @Test
  public void memberSorter_createSortedList_returnsNewList() {
    MemberSorter sorter = AlphabeticalMemberSorter.SINGLETON;
    UserMethod a = createMethod("alpha");
    UserMethod b = createMethod("bravo");
    List<UserMethod> original = Arrays.asList(b, a);
    List<UserMethod> sorted = sorter.createSortedList(original);
    assertNotSame(original, sorted);
  }

  @Test
  public void memberSorter_createSortedList_preservesAllElements() {
    MemberSorter sorter = AlphabeticalMemberSorter.SINGLETON;
    UserMethod a = createMethod("alpha");
    UserMethod b = createMethod("bravo");
    UserMethod c = createMethod("charlie");
    List<UserMethod> sorted = sorter.createSortedList(Arrays.asList(c, a, b));
    assertEquals(3, sorted.size());
    assertTrue(sorted.contains(a));
    assertTrue(sorted.contains(b));
    assertTrue(sorted.contains(c));
  }

  @Test
  public void memberSorter_createSortedList_preservesSameNamedElements() {
    MemberSorter sorter = AlphabeticalMemberSorter.SINGLETON;
    UserMethod m1 = createMethod("same");
    UserMethod m2 = createMethod("same");
    List<UserMethod> sorted = sorter.createSortedList(Arrays.asList(m1, m2));
    assertEquals(2, sorted.size());
  }
}
