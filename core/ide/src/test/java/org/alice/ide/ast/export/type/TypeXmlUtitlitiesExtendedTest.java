package org.alice.ide.ast.export.type;

import org.junit.Test;
import org.lgna.project.VersionNotSupportedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class TypeXmlUtitlitiesExtendedTest {

  @Test
  public void roundTripWithLargeListsPreservesAllEntries() throws VersionNotSupportedException {
    List<String> hierarchy = Arrays.asList(
        "org.example.Type0", "org.example.Type1", "org.example.Type2", "org.example.Type3",
        "org.example.Type4", "org.example.Type5", "org.example.Type6", "org.example.Type7",
        "org.example.Type8", "org.example.Type9", "org.example.Type10", "org.example.Type11");
    List<String> procedures = new ArrayList<>();
    List<FunctionInfo> functions = new ArrayList<>();
    List<FieldInfo> fields = new ArrayList<>();
    for (int i = 0; i < 15; i++) {
      procedures.add("procedure" + i);
      functions.add(new FunctionInfo("org.example.Return" + i, "function" + i));
      fields.add(new FieldInfo("org.example.Value" + i, "field" + i));
    }

    TypeSummary original = new TypeSummary(3.1, "LargeRoundTrip",
        hierarchy,
        new ResourceInfo("org.example.resources.LargeResource", "DEFAULT"),
        procedures,
        functions,
        fields);

    TypeSummary decoded = TypeXmlUtitlities.decode(TypeXmlUtitlities.encode(original));

    assertEquals(original.getVersion(), decoded.getVersion(), 0.0);
    assertEquals(original.getTypeName(), decoded.getTypeName());
    assertEquals(hierarchy, decoded.getHierarchyClassNames());
    assertEquals(procedures, decoded.getProcedureNames());
    assertEquals(15, decoded.getFunctionInfos().size());
    assertEquals(15, decoded.getFieldInfos().size());
    assertEquals("function0", decoded.getFunctionInfos().get(0).getName());
    assertEquals("org.example.Return14", decoded.getFunctionInfos().get(14).getReturnClassName());
    assertEquals("field0", decoded.getFieldInfos().get(0).getName());
    assertEquals("org.example.Value14", decoded.getFieldInfos().get(14).getValueClassName());
  }

  @Test
  public void roundTripWithNullResourceInfoPreservesNull() throws VersionNotSupportedException {
    TypeSummary original = new TypeSummary(3.1, "NoResource",
        Arrays.asList("BaseType", "java.lang.Object"),
        null,
        Arrays.asList("walk", "stop"),
        Collections.singletonList(new FunctionInfo("java.lang.Boolean", "isReady")),
        Collections.singletonList(new FieldInfo("java.lang.String", "name")));

    TypeSummary decoded = TypeXmlUtitlities.decode(TypeXmlUtitlities.encode(original));

    assertNull(decoded.getResourceInfo());
    assertEquals(original.getHierarchyClassNames(), decoded.getHierarchyClassNames());
    assertEquals(original.getProcedureNames(), decoded.getProcedureNames());
    assertEquals("isReady", decoded.getFunctionInfos().get(0).getName());
    assertEquals("name", decoded.getFieldInfos().get(0).getName());
  }

  @Test
  public void roundTripWithEmptyStringsPreservesEmptyStrings() throws VersionNotSupportedException {
    TypeSummary original = new TypeSummary(3.1, "",
        Arrays.asList("", "base"),
        new ResourceInfo("", ""),
        Collections.singletonList(""),
        Collections.singletonList(new FunctionInfo("", "")),
        Collections.singletonList(new FieldInfo("", "")));

    TypeSummary decoded = TypeXmlUtitlities.decode(TypeXmlUtitlities.encode(original));

    assertEquals("", decoded.getTypeName());
    assertEquals("", decoded.getHierarchyClassNames().get(0));
    assertEquals("base", decoded.getHierarchyClassNames().get(1));
    assertNotNull(decoded.getResourceInfo());
    assertEquals("", decoded.getResourceInfo().getClassName());
    assertEquals("", decoded.getResourceInfo().getFieldName());
    assertEquals("", decoded.getProcedureNames().get(0));
    assertEquals("", decoded.getFunctionInfos().get(0).getReturnClassName());
    assertEquals("", decoded.getFunctionInfos().get(0).getName());
    assertEquals("", decoded.getFieldInfos().get(0).getValueClassName());
    assertEquals("", decoded.getFieldInfos().get(0).getName());
  }

  @Test
  public void roundTripPreservesVersionExactly() throws VersionNotSupportedException {
    double version = 3.141592653589793;
    TypeSummary original = new TypeSummary(version, "PreciseVersion",
        Collections.singletonList("java.lang.Object"),
        null,
        Collections.<String>emptyList(),
        Collections.<FunctionInfo>emptyList(),
        Collections.<FieldInfo>emptyList());

    TypeSummary decoded = TypeXmlUtitlities.decode(TypeXmlUtitlities.encode(original));

    assertEquals(version, decoded.getVersion(), 0.0);
  }

  @Test
  public void roundTripPreservesHierarchyOrder() throws VersionNotSupportedException {
    List<String> hierarchy = Arrays.asList("Gamma", "Alpha", "Omega", "Beta");
    TypeSummary original = new TypeSummary(3.1, "OrderedType",
        hierarchy,
        null,
        Collections.<String>emptyList(),
        Collections.<FunctionInfo>emptyList(),
        Collections.<FieldInfo>emptyList());

    TypeSummary decoded = TypeXmlUtitlities.decode(TypeXmlUtitlities.encode(original));

    assertEquals(hierarchy, decoded.getHierarchyClassNames());
  }

  @Test
  public void multipleSuccessiveEncodeDecodeCallsRemainStable() throws VersionNotSupportedException {
    TypeSummary current = new TypeSummary(3.1, "StableType",
        Arrays.asList("FirstBase", "SecondBase"),
        new ResourceInfo("org.example.resources.StableResource", "PRIMARY"),
        Arrays.asList("spin", "wave"),
        Arrays.asList(new FunctionInfo("java.lang.Integer", "getCount"), new FunctionInfo("java.lang.String", "getLabel")),
        Arrays.asList(new FieldInfo("java.lang.Boolean", "visible"), new FieldInfo("java.lang.Double", "speed")));

    for (int i = 0; i < 3; i++) {
      current = TypeXmlUtitlities.decode(TypeXmlUtitlities.encode(current));
    }

    assertEquals(3.1, current.getVersion(), 0.0);
    assertEquals("StableType", current.getTypeName());
    assertEquals(Arrays.asList("FirstBase", "SecondBase"), current.getHierarchyClassNames());
    assertNotNull(current.getResourceInfo());
    assertEquals("org.example.resources.StableResource", current.getResourceInfo().getClassName());
    assertEquals("PRIMARY", current.getResourceInfo().getFieldName());
    assertEquals(Arrays.asList("spin", "wave"), current.getProcedureNames());
    assertEquals("getCount", current.getFunctionInfos().get(0).getName());
    assertEquals("java.lang.String", current.getFunctionInfos().get(1).getReturnClassName());
    assertEquals("visible", current.getFieldInfos().get(0).getName());
    assertEquals("java.lang.Double", current.getFieldInfos().get(1).getValueClassName());
  }
}
