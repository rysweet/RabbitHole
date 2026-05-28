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

import org.alice.ide.clipboard.DragReceptorState;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Integration tests verifying ClipboardIcon decomposition.
 * Checks: line count < 500, public API unchanged, delegate fields,
 * no leftover extracted methods, pixel-identical rendering contract.
 *
 * Pure reflection + source file checks — no GUI instantiation.
 */
public class ClipboardIconDecompositionTest {

  private static final String ICON_FQCN = "org.alice.ide.clipboard.icons.ClipboardIcon";
  private static Class<?> iconClass;

  @BeforeClass
  public static void loadClass() {
    try {
      iconClass = Class.forName(ICON_FQCN);
    } catch (ClassNotFoundException e) {
      fail("ClipboardIcon class not found: " + e.getMessage());
    }
  }

  // ── Line count requirement: under 500 ────────────────────────────

  @Test
  public void clipboardIconIsUnder500Lines() throws IOException {
    Path srcRoot = findSourceRoot();
    Path iconPath = srcRoot.resolve(
        "core/clipboard-dnd/src/main/java/org/alice/ide/clipboard/icons/ClipboardIcon.java");
    assertTrue("ClipboardIcon.java must exist at expected path", Files.exists(iconPath));
    long lineCount = Files.lines(iconPath).count();
    assertTrue("ClipboardIcon.java must be under 500 lines, was " + lineCount,
        lineCount < 500);
  }

  // ── Public API preserved (Icon interface) ────────────────────────

  @Test
  public void implementsIconInterface() {
    assertTrue("ClipboardIcon must implement javax.swing.Icon",
        Icon.class.isAssignableFrom(iconClass));
  }

  @Test
  public void isPublicClass() {
    assertTrue("ClipboardIcon must remain public",
        Modifier.isPublic(iconClass.getModifiers()));
  }

  @Test
  public void hasPaintIconMethod() {
    assertHasPublicMethod("paintIcon", Component.class, Graphics.class, Integer.TYPE, Integer.TYPE);
  }

  @Test
  public void hasPaintMethod() {
    assertHasPublicMethod("paint", Graphics2D.class);
  }

  @Test
  public void hasGetIconWidthMethod() {
    assertHasPublicMethod("getIconWidth");
  }

  @Test
  public void hasGetIconHeightMethod() {
    assertHasPublicMethod("getIconHeight");
  }

  @Test
  public void hasSetDimensionMethod() {
    assertHasPublicMethod("setDimension", Dimension.class);
  }

  @Test
  public void hasGetDragReceptorStateMethod() {
    assertHasPublicMethod("getDragReceptorState");
  }

  @Test
  public void hasSetDragReceptorStateMethod() throws NoSuchMethodException {
    Class<?> stateClass = DragReceptorState.class;
    assertHasPublicMethod("setDragReceptorState", stateClass);
  }

  @Test
  public void hasIsFullMethod() {
    assertHasPublicMethod("isFull");
  }

  @Test
  public void hasSetFullMethod() {
    assertHasPublicMethod("setFull", Boolean.TYPE);
  }

  @Test
  public void hasGetOrigXMethod() {
    assertHasPublicMethod("getOrigX");
  }

  @Test
  public void hasGetOrigYMethod() {
    assertHasPublicMethod("getOrigY");
  }

  @Test
  public void hasGetOrigWidthMethod() {
    assertHasPublicMethod("getOrigWidth");
  }

  @Test
  public void hasGetOrigHeightMethod() {
    assertHasPublicMethod("getOrigHeight");
  }

  // ── Default constructor preserved ────────────────────────────────

  @Test
  public void hasPublicNoArgConstructor() {
    try {
      var ctor = iconClass.getConstructor();
      assertTrue("no-arg constructor must be public",
          Modifier.isPublic(ctor.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("ClipboardIcon must have a public no-arg constructor");
    }
  }

  // ── Orchestration methods preserved ──────────────────────────────

  @Test
  public void hasPaintRootGraphicsNode() {
    assertIconDeclares("paintRootGraphicsNode_0");
  }

  @Test
  public void hasPaintCanvasGraphicsNode() {
    assertIconDeclares("paintCanvasGraphicsNode_0_0");
  }

  @Test
  public void hasPaintCompositeGraphicsNode_0_0_2() {
    assertIconDeclares("paintCompositeGraphicsNode_0_0_2");
  }

  @Test
  public void hasPaintCompositeGraphicsNode_0_0_2_0() {
    assertIconDeclares("paintCompositeGraphicsNode_0_0_2_0");
  }

  // ── All extracted methods removed from ClipboardIcon ─────────────

  @Test
  public void noGradientFactoryMethodsOnIcon() {
    Set<String> names = getIconMethodNames();
    assertFalse("new_LinearGradientPaint must be removed",
        names.contains("new_LinearGradientPaint"));
    assertFalse("new_RadialGradientPaint must be removed",
        names.contains("new_RadialGradientPaint"));
  }

  @Test
  public void noBoardShapeMethodsOnIcon() {
    Set<String> names = getIconMethodNames();
    assertFalse(names.contains("paintShapeNode_0_0_2_0_0_0"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_0_1"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_0_2"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_0_3"));
    assertFalse(names.contains("paintCompositeGraphicsNode_0_0_2_0_0"));
  }

  @Test
  public void noPaperShapeMethodsOnIcon() {
    Set<String> names = getIconMethodNames();
    assertFalse(names.contains("paintShapeNode_0_0_2_0_1"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_4_0"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_5_0"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_6"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_7"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_8"));
    assertFalse(names.contains("paintCompositeGraphicsNode_0_0_2_0_4"));
    assertFalse(names.contains("paintCompositeGraphicsNode_0_0_2_0_5"));
  }

  @Test
  public void noClipShapeMethodsOnIcon() {
    Set<String> names = getIconMethodNames();
    assertFalse(names.contains("paintShapeNode_0_0_2_0_9_0"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_9_1"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_9_2"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_9_3"));
    assertFalse(names.contains("paintShapeNode_0_0_2_0_9_4"));
    assertFalse(names.contains("paintCompositeGraphicsNode_0_0_2_0_9"));
  }

  // ── Delegate field existence ─────────────────────────────────────

  @Test
  public void hasBoardRendererField() {
    assertHasDelegateField("ClipboardBoardRenderer", "boardRenderer");
  }

  @Test
  public void hasPaperRendererField() {
    assertHasDelegateField("ClipboardPaperRenderer", "paperRenderer");
  }

  @Test
  public void hasClipRendererField() {
    assertHasDelegateField("ClipboardClipRenderer", "clipRenderer");
  }

  // ── Rendering identity: orig bounding box unchanged ──────────────

  @Test
  public void origBoundingBoxUnchanged() throws Exception {
    Object icon = iconClass.getConstructor().newInstance();
    Method getOrigX = iconClass.getMethod("getOrigX");
    Method getOrigY = iconClass.getMethod("getOrigY");
    Method getOrigWidth = iconClass.getMethod("getOrigWidth");
    Method getOrigHeight = iconClass.getMethod("getOrigHeight");

    assertEquals("origX must be 1", 1, getOrigX.invoke(icon));
    assertEquals("origY must be 0", 0, getOrigY.invoke(icon));
    assertEquals("origWidth must be 48", 48, getOrigWidth.invoke(icon));
    assertEquals("origHeight must be 43", 43, getOrigHeight.invoke(icon));
  }

  @Test
  public void defaultDimensionMatchesOriginal() throws Exception {
    Object icon = iconClass.getConstructor().newInstance();
    Method getWidth = iconClass.getMethod("getIconWidth");
    Method getHeight = iconClass.getMethod("getIconHeight");

    assertEquals("default width must be 48", 48, getWidth.invoke(icon));
    assertEquals("default height must be 43", 43, getHeight.invoke(icon));
  }

  @Test
  public void dragReceptorStateDefaultsToIdle() throws Exception {
    Object icon = iconClass.getConstructor().newInstance();
    Method getState = iconClass.getMethod("getDragReceptorState");
    Object state = getState.invoke(icon);
    assertEquals("default DragReceptorState must be IDLE",
        DragReceptorState.IDLE, state);
  }

  @Test
  public void isFullDefaultsToFalse() throws Exception {
    Object icon = iconClass.getConstructor().newInstance();
    Method isFull = iconClass.getMethod("isFull");
    assertEquals("default isFull must be false", false, isFull.invoke(icon));
  }

  // ── New file existence checks ────────────────────────────────────

  @Test
  public void gradientPaintFactoryFileExists() throws IOException {
    assertSourceFileExists("GradientPaintFactory.java");
  }

  @Test
  public void clipboardBoardRendererFileExists() throws IOException {
    assertSourceFileExists("ClipboardBoardRenderer.java");
  }

  @Test
  public void clipboardPaperRendererFileExists() throws IOException {
    assertSourceFileExists("ClipboardPaperRenderer.java");
  }

  @Test
  public void clipboardClipRendererFileExists() throws IOException {
    assertSourceFileExists("ClipboardClipRenderer.java");
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static Set<String> getIconMethodNames() {
    return Arrays.stream(iconClass.getDeclaredMethods())
        .map(Method::getName)
        .collect(Collectors.toSet());
  }

  private static void assertHasPublicMethod(String name, Class<?>... paramTypes) {
    try {
      Method m = iconClass.getMethod(name, paramTypes);
      assertTrue(name + " must be public", Modifier.isPublic(m.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("ClipboardIcon must have public method: " + name);
    }
  }

  private static void assertIconDeclares(String name) {
    boolean found = Arrays.stream(iconClass.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals(name));
    assertTrue("ClipboardIcon must still declare '" + name + "'", found);
  }

  private static void assertHasDelegateField(String typeName, String fieldName) {
    Field found = null;
    for (Field f : iconClass.getDeclaredFields()) {
      if (f.getName().equals(fieldName)) {
        found = f;
        break;
      }
    }
    if (found == null) {
      // Also check for alternate naming conventions
      for (Field f : iconClass.getDeclaredFields()) {
        if (f.getType().getSimpleName().equals(typeName)) {
          found = f;
          break;
        }
      }
    }
    assertNotNull("ClipboardIcon must have a delegate field of type " + typeName, found);
    assertTrue("delegate field must be of type " + typeName,
        found.getType().getSimpleName().equals(typeName));
  }

  private static Path findSourceRoot() {
    // Walk up from the test class location to find the repository root
    Path cwd = Paths.get(System.getProperty("user.dir"));
    // Check common locations
    if (Files.exists(cwd.resolve("core/clipboard-dnd/src/main/java"))) {
      return cwd;
    }
    // Try parent directories
    Path parent = cwd.getParent();
    while (parent != null) {
      if (Files.exists(parent.resolve("core/clipboard-dnd/src/main/java"))) {
        return parent;
      }
      parent = parent.getParent();
    }
    fail("Cannot find source root from " + cwd);
    return null;
  }

  private static void assertSourceFileExists(String filename) throws IOException {
    Path srcRoot = findSourceRoot();
    Path filePath = srcRoot.resolve(
        "core/clipboard-dnd/src/main/java/org/alice/ide/clipboard/icons/" + filename);
    assertTrue(filename + " must exist at " + filePath, Files.exists(filePath));
  }
}
