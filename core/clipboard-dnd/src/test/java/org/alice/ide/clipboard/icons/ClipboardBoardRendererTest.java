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
 * TDD contract tests for ClipboardBoardRenderer extracted from ClipboardIcon
 * (lines 119-180: board shadow + rectangle rendering).
 *
 * Verifies class structure, entry point signature, and that ClipboardIcon
 * no longer declares the board shape/composite methods.
 *
 * Pure reflection — no GUI instantiation required.
 */
public class ClipboardBoardRendererTest {

  private static final String FQCN = "org.alice.ide.clipboard.icons.ClipboardBoardRenderer";
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
      fail("ClipboardBoardRenderer class not found: " + e.getMessage());
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
    assertNull("ClipboardBoardRenderer must be top-level",
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
  public void hasPaintCompositeMethod() {
    assertNotNull("must have paintComposite method",
        findPaintComposite());
  }

  @Test
  public void paintCompositeHasCorrectSignature() {
    Method m = findPaintComposite();
    assertNotNull(m);
    Class<?>[] params = m.getParameterTypes();
    assertEquals("paintComposite must take 3 parameters", 3, params.length);
    assertEquals("first param must be Graphics2D", Graphics2D.class, params[0]);
    assertEquals("second param must be float", Float.TYPE, params[1]);
    assertEquals("third param must be DragReceptorState", stateClass, params[2]);
  }

  @Test
  public void paintCompositeReturnsVoid() {
    Method m = findPaintComposite();
    assertNotNull(m);
    assertEquals("paintComposite must return void", Void.TYPE, m.getReturnType());
  }

  // ── Board shape methods (4) extracted from ClipboardIcon ─────────

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_0_0() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_0_0");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_0_1() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_0_1");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_0_2() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_0_2");
  }

  @Test
  public void clipboardIconDoesNotDeclarePaintShapeNode_0_0_2_0_0_3() {
    assertIconDoesNotDeclare("paintShapeNode_0_0_2_0_0_3");
  }

  // ── Board composite method extracted from ClipboardIcon ──────────

  @Test
  public void clipboardIconDoesNotDeclarePaintCompositeGraphicsNode_0_0_2_0_0() {
    assertIconDoesNotDeclare("paintCompositeGraphicsNode_0_0_2_0_0");
  }

  // ── Renderer internal methods exist ──────────────────────────────

  @Test
  public void rendererHas4ShapeMethods() {
    Set<String> shapeNames = Arrays.stream(rendererClass.getDeclaredMethods())
        .map(Method::getName)
        .filter(n -> n.startsWith("paintShapeNode_0_0_2_0_0_"))
        .collect(Collectors.toSet());
    assertEquals("board renderer must have 4 shape methods", 4, shapeNames.size());
    assertTrue(shapeNames.contains("paintShapeNode_0_0_2_0_0_0"));
    assertTrue(shapeNames.contains("paintShapeNode_0_0_2_0_0_1"));
    assertTrue(shapeNames.contains("paintShapeNode_0_0_2_0_0_2"));
    assertTrue(shapeNames.contains("paintShapeNode_0_0_2_0_0_3"));
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static Method findPaintComposite() {
    return Arrays.stream(rendererClass.getDeclaredMethods())
        .filter(m -> m.getName().equals("paintComposite"))
        .findFirst()
        .orElse(null);
  }

  private static void assertIconDoesNotDeclare(String name) {
    boolean found = Arrays.stream(iconClass.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals(name));
    assertFalse("ClipboardIcon should not declare '" + name + "' (moved to ClipboardBoardRenderer)",
        found);
  }
}
