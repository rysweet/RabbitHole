package org.alice.ide.ast.sort;

import org.junit.Test;
import org.lgna.project.ast.UserMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Characterization tests for {@link AlphabeticalMemberSorter}.
 */
public class AlphabeticalMemberSorterTest {

  private UserMethod createMethod(String name) {
    UserMethod method = new UserMethod();
    if (name != null) {
      method.name.setValue(name);
    }
    return method;
  }

  @Test
  public void singleton_isNotNull() {
    assertNotNull(AlphabeticalMemberSorter.SINGLETON);
  }

  @Test
  public void singleton_implementsMemberSorter() {
    assertTrue(AlphabeticalMemberSorter.SINGLETON instanceof MemberSorter);
  }

  @Test
  public void createSortedList_emptyList_returnsEmpty() {
    List<UserMethod> empty = Collections.emptyList();
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(empty);
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void createSortedList_singleElement_returnsSingleElement() {
    UserMethod m = createMethod("alpha");
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(
        Collections.singletonList(m));
    assertEquals(1, result.size());
    assertSame(m, result.get(0));
  }

  @Test
  public void createSortedList_alreadySorted_maintainsOrder() {
    UserMethod a = createMethod("alpha");
    UserMethod b = createMethod("beta");
    UserMethod c = createMethod("gamma");
    List<UserMethod> input = Arrays.asList(a, b, c);
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(input);
    assertEquals(3, result.size());
    assertSame(a, result.get(0));
    assertSame(b, result.get(1));
    assertSame(c, result.get(2));
  }

  @Test
  public void createSortedList_reverseSorted_sortsAlphabetically() {
    UserMethod a = createMethod("alpha");
    UserMethod b = createMethod("beta");
    UserMethod c = createMethod("gamma");
    List<UserMethod> input = Arrays.asList(c, b, a);
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(input);
    assertEquals(3, result.size());
    assertSame(a, result.get(0));
    assertSame(b, result.get(1));
    assertSame(c, result.get(2));
  }

  @Test
  public void createSortedList_caseInsensitive() {
    UserMethod lower = createMethod("alpha");
    UserMethod upper = createMethod("Alpha");
    UserMethod mixed = createMethod("BETA");
    List<UserMethod> input = Arrays.asList(mixed, lower, upper);
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(input);
    assertEquals(3, result.size());
    // alpha and Alpha should sort together before BETA
    String first = result.get(0).getName();
    String second = result.get(1).getName();
    assertTrue(first.equalsIgnoreCase("alpha"));
    assertTrue(second.equalsIgnoreCase("alpha"));
    assertEquals("BETA", result.get(2).getName());
  }

  @Test
  public void createSortedList_doesNotModifyOriginal() {
    UserMethod a = createMethod("beta");
    UserMethod b = createMethod("alpha");
    List<UserMethod> original = new ArrayList<>(Arrays.asList(a, b));
    AlphabeticalMemberSorter.SINGLETON.createSortedList(original);
    assertSame(a, original.get(0));
    assertSame(b, original.get(1));
  }

  @Test
  public void createSortedList_nullNamesHandled_nullBeforeNonNull() {
    UserMethod withName = createMethod("alpha");
    UserMethod noName = createMethod(null);
    List<UserMethod> input = Arrays.asList(withName, noName);
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(input);
    assertEquals(2, result.size());
    // null name should sort before non-null
    assertNull(result.get(0).getName());
    assertEquals("alpha", result.get(1).getName());
  }

  @Test
  public void createSortedList_bothNullNames_stableOrdering() {
    UserMethod n1 = createMethod(null);
    UserMethod n2 = createMethod(null);
    List<UserMethod> input = Arrays.asList(n1, n2);
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(input);
    assertEquals(2, result.size());
  }

  @Test
  public void createSortedList_mixedNullAndNonNull() {
    UserMethod a = createMethod("charlie");
    UserMethod b = createMethod(null);
    UserMethod c = createMethod("alpha");
    List<UserMethod> input = Arrays.asList(a, b, c);
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(input);
    assertEquals(3, result.size());
    assertNull(result.get(0).getName());
  }

  @Test
  public void createSortedList_duplicateNames_allPresent() {
    UserMethod a = createMethod("same");
    UserMethod b = createMethod("same");
    UserMethod c = createMethod("same");
    List<UserMethod> input = Arrays.asList(a, b, c);
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(input);
    assertEquals(3, result.size());
  }

  @Test
  public void enumValues_containsSingleton() {
    AlphabeticalMemberSorter[] values = AlphabeticalMemberSorter.values();
    assertEquals(1, values.length);
    assertSame(AlphabeticalMemberSorter.SINGLETON, values[0]);
  }

  @Test
  public void valueOf_SINGLETON() {
    assertEquals(AlphabeticalMemberSorter.SINGLETON,
        AlphabeticalMemberSorter.valueOf("SINGLETON"));
  }

  @Test
  public void createSortedList_manyElements_sortedCorrectly() {
    UserMethod m1 = createMethod("zebra");
    UserMethod m2 = createMethod("apple");
    UserMethod m3 = createMethod("mango");
    UserMethod m4 = createMethod("banana");
    UserMethod m5 = createMethod("cherry");
    List<UserMethod> input = Arrays.asList(m1, m2, m3, m4, m5);
    List<UserMethod> result = AlphabeticalMemberSorter.SINGLETON.createSortedList(input);
    assertEquals("apple", result.get(0).getName());
    assertEquals("banana", result.get(1).getName());
    assertEquals("cherry", result.get(2).getName());
    assertEquals("mango", result.get(3).getName());
    assertEquals("zebra", result.get(4).getName());
  }
}
