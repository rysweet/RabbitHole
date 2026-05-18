package org.alice.ide.ast.export.type;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TypeSummaryExtendedTest {

  @Test
  public void currentVersionConstantIsThreePointOne() {
    assertEquals(3.1, TypeSummary.CURRENT_VERSION, 0.0);
  }

  @Test
  public void minimumAcceptableVersionConstantIsThreePointOne() {
    assertEquals(3.1, TypeSummary.MINIMUM_ACCEPTABLE_VERSION, 0.0);
  }

  @Test
  public void fullConstructorRetainsAllProvidedValuesAndInstances() {
    List<String> hierarchy = new ArrayList<>(Arrays.asList("Parent", "GrandParent", "java.lang.Object"));
    ResourceInfo resourceInfo = new ResourceInfo("org.example.resources.HeroResource", "DEFAULT");
    List<String> procedures = new ArrayList<>(Arrays.asList("walk", "turn", "jump"));
    List<FunctionInfo> functions = new ArrayList<>(Arrays.asList(
        new FunctionInfo("java.lang.Boolean", "isReady"),
        new FunctionInfo("java.lang.Integer", "getScore")));
    List<FieldInfo> fields = new ArrayList<>(Arrays.asList(
        new FieldInfo("java.lang.String", "displayName"),
        new FieldInfo("java.lang.Double", "speed")));

    TypeSummary summary = new TypeSummary(3.1, "Hero", hierarchy, resourceInfo, procedures, functions, fields);

    assertEquals(3.1, summary.getVersion(), 0.0);
    assertEquals("Hero", summary.getTypeName());
    assertSame(hierarchy, summary.getHierarchyClassNames());
    assertSame(resourceInfo, summary.getResourceInfo());
    assertSame(procedures, summary.getProcedureNames());
    assertSame(functions, summary.getFunctionInfos());
    assertSame(fields, summary.getFieldInfos());
  }

  @Test
  public void largeHierarchyListsAreRetained() {
    List<String> hierarchy = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      hierarchy.add("org.example.Type" + i);
    }

    TypeSummary summary = new TypeSummary(3.1, "LargeHierarchyType", hierarchy, null,
        Collections.<String>emptyList(), Collections.<FunctionInfo>emptyList(), Collections.<FieldInfo>emptyList());

    assertSame(hierarchy, summary.getHierarchyClassNames());
    assertEquals(12, summary.getHierarchyClassNames().size());
    assertEquals("org.example.Type0", summary.getHierarchyClassNames().get(0));
    assertEquals("org.example.Type11", summary.getHierarchyClassNames().get(11));
  }

  @Test
  public void unicodeTypeNamesAreRetained() {
    String typeName = "类型🚀Приветمرحبا";
    List<String> hierarchy = Arrays.asList("父类型", "База", "Base🌟");

    TypeSummary summary = new TypeSummary(3.1, typeName, hierarchy, null,
        Collections.<String>emptyList(), Collections.<FunctionInfo>emptyList(), Collections.<FieldInfo>emptyList());

    assertEquals(typeName, summary.getTypeName());
    assertEquals(hierarchy, summary.getHierarchyClassNames());
  }

  @Test
  public void mixedEmptyAndPopulatedCollectionsAreRetained() {
    List<String> hierarchy = Collections.singletonList("java.lang.Object");
    List<String> procedures = Collections.emptyList();
    List<FunctionInfo> functions = Collections.singletonList(new FunctionInfo("java.lang.String", "getStatus"));
    List<FieldInfo> fields = Collections.emptyList();

    TypeSummary summary = new TypeSummary(3.1, "MixedType", hierarchy, null, procedures, functions, fields);

    assertSame(hierarchy, summary.getHierarchyClassNames());
    assertSame(procedures, summary.getProcedureNames());
    assertSame(functions, summary.getFunctionInfos());
    assertSame(fields, summary.getFieldInfos());
    assertTrue(summary.getProcedureNames().isEmpty());
    assertEquals(1, summary.getFunctionInfos().size());
    assertTrue(summary.getFieldInfos().isEmpty());
  }

  @Test
  public void zeroVersionIsRetained() {
    TypeSummary summary = new TypeSummary(0.0, "ZeroVersion", Collections.<String>emptyList(), null,
        Collections.<String>emptyList(), Collections.<FunctionInfo>emptyList(), Collections.<FieldInfo>emptyList());

    assertEquals(0.0, summary.getVersion(), 0.0);
  }

  @Test
  public void negativeVersionIsRetained() {
    TypeSummary summary = new TypeSummary(-7.25, "NegativeVersion", Collections.<String>emptyList(), null,
        Collections.<String>emptyList(), Collections.<FunctionInfo>emptyList(), Collections.<FieldInfo>emptyList());

    assertEquals(-7.25, summary.getVersion(), 0.0);
  }
}
