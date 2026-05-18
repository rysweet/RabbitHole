package org.alice.ide.ast.sort;

import org.junit.Test;
import org.lgna.project.ast.UserMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class MemberSorterContractTest {

  private final MemberSorter sorter = AlphabeticalMemberSorter.SINGLETON;

  private UserMethod createMethod(String name) {
    UserMethod method = new UserMethod();
    if (name != null) {
      method.name.setValue(name);
    }
    return method;
  }

  @Test
  public void createSortedListWithNullElementsInList() {
    List<UserMethod> input = Arrays.asList(createMethod("beta"), null, createMethod("alpha"));

    List<UserMethod> result = sorter.createSortedList(input);

    assertEquals(3, result.size());
    assertNull(result.get(0));
    assertEquals("alpha", result.get(1).getName());
    assertEquals("beta", result.get(2).getName());
  }

  @Test
  public void createSortedListWithSingleNullNameMember() {
    UserMethod unnamedMethod = createMethod(null);

    List<UserMethod> result = sorter.createSortedList(Collections.singletonList(unnamedMethod));

    assertEquals(1, result.size());
    assertSame(unnamedMethod, result.get(0));
    assertNull(result.get(0).getName());
  }

  @Test
  public void createSortedListPreservesListSize() {
    List<UserMethod> input = Arrays.asList(createMethod("beta"), createMethod(null), null, createMethod("alpha"));

    List<UserMethod> result = sorter.createSortedList(input);

    assertEquals(input.size(), result.size());
  }

  @Test
  public void createSortedListReturnsNewListReference() {
    List<UserMethod> input = new ArrayList<>(Arrays.asList(createMethod("beta"), createMethod("alpha")));

    List<UserMethod> result = sorter.createSortedList(input);

    assertNotSame(input, result);
    assertEquals("beta", input.get(0).getName());
    assertEquals("alpha", input.get(1).getName());
  }
}
