package org.alice.ide.ast.export.type;

import org.junit.Test;
import static org.junit.Assert.*;

public class FunctionInfoTest {

  @Test
  public void constructor_setsName() {
    FunctionInfo info = new FunctionInfo("java.lang.Double", "getSpeed");
    assertEquals("getSpeed", info.getName());
  }

  @Test
  public void constructor_setsReturnClassName() {
    FunctionInfo info = new FunctionInfo("java.lang.Boolean", "isReady");
    assertEquals("java.lang.Boolean", info.getReturnClassName());
  }

  @Test
  public void getName_matchesConstructorArg() {
    FunctionInfo info = new FunctionInfo("java.lang.Integer", "compute");
    assertEquals("compute", info.getName());
  }

  @Test
  public void getReturnClassName_matchesConstructorArg() {
    FunctionInfo info = new FunctionInfo("java.lang.String", "getValue");
    assertEquals("java.lang.String", info.getReturnClassName());
  }

  @Test
  public void multipleFunctions_differentInfos() {
    FunctionInfo i1 = new FunctionInfo("java.lang.Double", "getA");
    FunctionInfo i2 = new FunctionInfo("java.lang.Integer", "getB");
    assertNotEquals(i1.getName(), i2.getName());
    assertNotEquals(i1.getReturnClassName(), i2.getReturnClassName());
  }

  @Test
  public void nullReturnClassName_allowed() {
    FunctionInfo info = new FunctionInfo(null, "fn");
    assertNull(info.getReturnClassName());
  }

  @Test
  public void nullName_allowed() {
    FunctionInfo info = new FunctionInfo("java.lang.Object", null);
    assertNull(info.getName());
  }

  @Test
  public void emptyStrings_allowed() {
    FunctionInfo info = new FunctionInfo("", "");
    assertEquals("", info.getReturnClassName());
    assertEquals("", info.getName());
  }
}
