package org.lgna.project.virtualmachine;

import org.junit.Test;
import org.lgna.project.ast.AbstractParameter;
import org.lgna.project.ast.BlockStatement;
import org.lgna.project.ast.UserLocal;
import org.lgna.project.ast.UserMethod;
import org.lgna.project.ast.UserParameter;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LgnaVmExceptionFormattingTest {
  @Test
  public void formattedExceptionDescriptionEscapesProjectAuthoredNames() {
    ReleaseVirtualMachine vm = new ReleaseVirtualMachine();
    UserParameter parameter = new UserParameter("parameter<script>&", String.class);
    UserLocal local = new UserLocal("local<script>&", String.class, true);

    String parameterHtml = new LgnaVmIllegalParameterAccessException(vm, parameter).getFormattedString();
    String localHtml = new LgnaVmIllegalLocalAccessException(vm, local).getFormattedString();

    assertTrue(parameterHtml.contains("parameter&lt;script&gt;&amp;"));
    assertFalse(parameterHtml.contains("parameter<script>&"));
    assertTrue(localHtml.contains("local&lt;script&gt;&amp;"));
    assertFalse(localHtml.contains("local<script>&"));
  }

  @Test
  public void formattedStackTraceEscapesNamesAndRedactsRuntimeValues() {
    ExposedReleaseVirtualMachine vm = new ExposedReleaseVirtualMachine();
    UserParameter parameter = new UserParameter("secret<param>&", String.class);
    UserMethod method = new UserMethod("run<script>&\"", Void.TYPE, new UserParameter[] {parameter}, new BlockStatement());
    String sensitiveValue = "token <img src=x onerror=alert(1)>";

    vm.pushUserMethodFrame(method, parameter, sensitiveValue);
    try {
      LgnaVmNullPointerException exception = new LgnaVmNullPointerException("boom <script>&", vm);

      String formatted = exception.getFormattedString();

      assertTrue(formatted.contains("boom &lt;script&gt;&amp;"));
      assertFalse(formatted.contains("boom <script>&"));
      assertTrue(formatted.contains("<strong>run&lt;script&gt;&amp;&quot;" + "</strong>"));
      assertFalse(formatted.contains("run<script>&\""));
      assertTrue(formatted.contains("secret&lt;param&gt;&amp;=[redacted]"));
      assertFalse(formatted.contains("token"));
      assertFalse(formatted.contains("&lt;img src=x onerror=alert(1)&gt;"));

      String trustedDebugText = exception.toString();
      assertTrue(trustedDebugText.contains("boom <script>&"));
      assertTrue(trustedDebugText.contains("run<script>&\""));
      assertTrue(trustedDebugText.contains("secret<param>&=" + sensitiveValue));
    } finally {
      vm.popExposedFrame();
    }
  }

  private static class ExposedReleaseVirtualMachine extends ReleaseVirtualMachine {
    void pushUserMethodFrame(UserMethod method, AbstractParameter parameter, Object value) {
      Map<AbstractParameter, Object> map = new LinkedHashMap<AbstractParameter, Object>();
      map.put(parameter, value);
      this.pushMethodFrame(null, method, map);
    }

    void popExposedFrame() {
      this.popFrame();
    }
  }
}
