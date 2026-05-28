/*******************************************************************************
 * Copyright (c) 2006, 2015, Carnegie Mellon University. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Products derived from the software may not be called "Alice", nor may
 *    "Alice" appear in their name, without prior written permission of
 *    Carnegie Mellon University.
 *
 * 4. All advertising materials mentioning features or use of this software must
 *    display the following acknowledgement: "This product includes software
 *    developed by Carnegie Mellon University"
 *
 * 5. The gallery of art assets and animations provided with this software is
 *    contributed by Electronic Arts Inc. and may be used for personal,
 *    non-commercial, and academic use only. Redistributions of any program
 *    source code that utilizes The Sims 2 Assets must also retain the copyright
 *    notice, list of conditions and the disclaimer contained in
 *    The Alice 3.0 Art Gallery License.
 *
 * DISCLAIMER:
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 * ANY AND ALL EXPRESS, STATUTORY OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY,  FITNESS FOR A
 * PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT ARE DISCLAIMED. IN NO EVENT
 * SHALL THE AUTHORS, COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING FROM OR OTHERWISE RELATING TO
 * THE USE OF OR OTHER DEALINGS WITH THE SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
package org.alice.ide.clipboard.icons;

import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Graphics2D;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * TDD contract tests for ClipboardPaperRenderer extracted from ClipboardIcon
 * (lines 182-300 shape methods + lines 430-465 orchestration).
 *
 * Paper rendering includes: shadow, fill, stroke, fold triangle, fold gradient,
 * inner-fold gradient, and highlight stroke — 7 shape methods plus 2 composites
 * plus orchestration alpha composites moved from paintCompositeGraphicsNode_0_0_2_0.
 *
 * Pure reflection — no GUI instantiation required.
 */
public class ClipboardPaperRendererTest {

  private static final String FQCN = "org.alice.ide.clipboard.icons.ClipboardPaperRenderer";
  private static final String ICON_FQCN = "org.alice.ide.clipboard.icons.ClipboardIcon";
  private static final String STATE_FQCN = "org.alice.ide.clipboard.DragReceptorState";
  private static Class<?> rendererClass;
  private static Class<?> iconClass;
  private static Class<?> stateClass;

  @BeforeClass
  public static void loadClasses() {
    try {
      rendererClass = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("ClipboardPaperRenderer class not found: " + e.getMessage());
    }
    try {
      iconClass = Class.forName(ICON_FQCN);
    } catch (ClassNotFoundException e) {
      fail("ClipboardIcon class not found: " + e.getMessage());
    }
    try {
      stateClass = Class.forName(STATE_FQCN);
    } catch (ClassNotFoundException e) {
      fail("DragReceptorState class not found: " + e.getMessage());
    }
  }

  // ── Class structure ──────────────────────────────────────────────

  @Test
  public void isTopLevelClass() {
    assertNull("ClipboardPaperRenderer must be top-level",
        rendererClass.getEnclosingClass());
  }

  @Test
  public void isPackagePrivate() {
    int mods = rendererClass.getModifiers();
    assertFalse("must not be public", Modifier.isPublic(mods));
    assertFalse("must not be private", Modifier.isPrivate(mods));
    assertFalse("must not be protected", Modifier.isProtected(mods));
  }

  @Test
  public void isSamePackageAsClipboardIcon() {
    assertEquals("must be in same package as ClipboardIcon",
        iconClass.getPackage(), rendererClass.getPackage());
  }

  // ── Entry point method ───────────────────────────────────────────

  @Test
  public void hasPaintAllMethod() {
    assertNotNull("must have paintAll method", findPaintAll());
  }

  @Test
  public void paintAllHasCorrectSignature() {
    Method m = findPaintAll();
    assertNotNull(m);
    Class<?>[] params = m.getParameterTypes();
    assertEquals("paintAll must take 3 parameters", 3, params.length);
    assertEquals("first param must be Graphics2D", Graphics2D.class, params[0]);
    assertEquals("second param must be float", Float.TYPE, params[1]);
    assertEquals("third param must be DragReceptorState", stateClass, params[2]);
  }

  @Test
  public void paintAllReturnsVoid() {
    Method m = findPaintAll();
    assertNotNull(m);
    assertEquals("paintAll must return void", Void.TYPE, m.getReturnType());
  }

  // ── Paper shape methods extracted from ClipboardIcon ──────────────

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_1() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_1");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_4_0() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_4_0");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_5_0() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_5_0");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_6() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_6");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_7() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_7");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_8() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_8");
  }

  // ── Paper composite wrapper methods extracted from ClipboardIcon ──

  @Test
  public void clipboardIconDoesNotDeclarePaintCompositeGraphicsNode_0_0_2_0_4() {
    assertIconDoesNotDeclare("paintCompositeGraphicsNode_0_0_2_0_4");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintCompositeGraphicsNode_0_0_2_0_5() {
    assertIconDoesNotDeclare("paintCompositeGraphicsNode_0_0_2_0_5");
  }

  // ── Renderer has the shape methods ───────────────────────────────

  @Test
  public void rendererHasPaperShadowMethod() {
    assertRendererHasMethod("paintShapeNode_0_0_2_0_1");
  }

  @Test
  public void rendererHasPaperFillMethod() {
    assertRendererHasMethod("paintShapeNode_0_0_2_0_4_0");
  }

  @Test
  public void rendererHasPaperStrokeMethod() {
    assertRendererHasMethod("paintShapeNode_0_0_2_0_5_0");
  }

  @Test
  public void rendererHasFoldTriangleMethod() {
    assertRendererHasMethod("paintShapeNode_0_0_2_0_6");
  }

  @Test
  public void rendererHasFoldGradientMethod() {
    assertRendererHasMethod("paintShapeNode_0_0_2_0_7");
  }

  @Test
  public void rendererHasHighlightStrokeMethod() {
    assertRendererHasMethod("paintShapeNode_0_0_2_0_8");
  }

  // ── Renderer has composite wrappers ──────────────────────────────

  @Test
  public void rendererHasFillCompositeWrapper() {
    assertRendererHasMethod("paintCompositeGraphicsNode_0_0_2_0_4");
  }

  @Test
  public void rendererHasStrokeCompositeWrapper() {
    assertRendererHasMethod("paintCompositeGraphicsNode_0_0_2_0_5");
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static Method findPaintAll() {
    return Arrays.stream(rendererClass.getDeclaredMethods())
        .filter(m -> m.getName().equals("paintAll"))
        .findFirst()
        .orElse(null);
  }

  private static void assertRendererHasMethod(String name) {
    boolean found = Arrays.stream(rendererClass.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals(name));
    assertTrue("ClipboardPaperRenderer must declare '" + name + "'", found);
  }

  private static void assertIconDoesNotDeclare(String name) {
    boolean found = Arrays.stream(iconClass.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals(name));
    assertFalse("ClipboardIcon should not declare '" + name + "' (moved to ClipboardPaperRenderer)",
        found);
  }
}
