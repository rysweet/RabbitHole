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

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * TDD contract tests for GradientPaintFactory extracted from ClipboardIcon.
 * Verifies class structure, method signatures, and that ClipboardIcon
 * no longer declares the gradient factory methods.
 *
 * Pure reflection — no GUI instantiation required.
 */
public class GradientPaintFactoryTest {

  private static final String FQCN = "org.alice.ide.clipboard.icons.GradientPaintFactory";
  private static final String ICON_FQCN = "org.alice.ide.clipboard.icons.ClipboardIcon";
  private static Class<?> factoryClass;
  private static Class<?> iconClass;

  @BeforeClass
  public static void loadClasses() {
    try {
      factoryClass = Class.forName(FQCN);
    } catch (ClassNotFoundException e) {
      fail("GradientPaintFactory class not found: " + e.getMessage());
    }
    try {
      iconClass = Class.forName(ICON_FQCN);
    } catch (ClassNotFoundException e) {
      fail("ClipboardIcon class not found: " + e.getMessage());
    }
  }

  // ── Class structure ──────────────────────────────────────────────

  @Test
  public void isTopLevelClass() {
    assertNull("GradientPaintFactory must be top-level (no enclosing class)",
        factoryClass.getEnclosingClass());
  }

  @Test
  public void isPackagePrivate() {
    int mods = factoryClass.getModifiers();
    assertFalse("must not be public", Modifier.isPublic(mods));
    assertFalse("must not be private", Modifier.isPrivate(mods));
    assertFalse("must not be protected", Modifier.isProtected(mods));
  }

  @Test
  public void isFinalUtilityClass() {
    assertTrue("GradientPaintFactory should be final",
        Modifier.isFinal(factoryClass.getModifiers()));
  }

  @Test
  public void hasPrivateConstructor() {
    try {
      var ctor = factoryClass.getDeclaredConstructor();
      assertTrue("no-arg constructor must be private",
          Modifier.isPrivate(ctor.getModifiers()));
    } catch (NoSuchMethodException e) {
      fail("GradientPaintFactory must have a private no-arg constructor");
    }
  }

  // ── new_LinearGradientPaint ──────────────────────────────────────

  @Test
  public void hasLinearGradientPaintMethod() {
    assertNotNull("must have new_LinearGradientPaint",
        findMethod("new_LinearGradientPaint"));
  }

  @Test
  public void linearGradientPaintIsStatic() {
    Method m = findMethod("new_LinearGradientPaint");
    assertNotNull(m);
    assertTrue("new_LinearGradientPaint must be static",
        Modifier.isStatic(m.getModifiers()));
  }

  @Test
  public void linearGradientPaintReturnsPaint() {
    Method m = findMethod("new_LinearGradientPaint");
    assertNotNull(m);
    assertEquals("new_LinearGradientPaint must return Paint",
        Paint.class, m.getReturnType());
  }

  @Test
  public void linearGradientPaintHasCorrectParameters() {
    Method m = findMethod("new_LinearGradientPaint");
    assertNotNull(m);
    Class<?>[] params = m.getParameterTypes();
    assertEquals("new_LinearGradientPaint must take 5 parameters", 5, params.length);
    assertEquals(Point2D.class, params[0]);
    assertEquals(Point2D.class, params[1]);
    assertEquals(float[].class, params[2]);
    assertEquals(Color[].class, params[3]);
    assertEquals(AffineTransform.class, params[4]);
  }

  // ── new_RadialGradientPaint ──────────────────────────────────────

  @Test
  public void hasRadialGradientPaintMethod() {
    assertNotNull("must have new_RadialGradientPaint",
        findMethod("new_RadialGradientPaint"));
  }

  @Test
  public void radialGradientPaintIsStatic() {
    Method m = findMethod("new_RadialGradientPaint");
    assertNotNull(m);
    assertTrue("new_RadialGradientPaint must be static",
        Modifier.isStatic(m.getModifiers()));
  }

  @Test
  public void radialGradientPaintReturnsPaint() {
    Method m = findMethod("new_RadialGradientPaint");
    assertNotNull(m);
    assertEquals("new_RadialGradientPaint must return Paint",
        Paint.class, m.getReturnType());
  }

  @Test
  public void radialGradientPaintHasCorrectParameters() {
    Method m = findMethod("new_RadialGradientPaint");
    assertNotNull(m);
    Class<?>[] params = m.getParameterTypes();
    assertEquals("new_RadialGradientPaint must take 6 parameters", 6, params.length);
    assertEquals(Point2D.class, params[0]);
    assertEquals(Float.TYPE, params[1]);
    assertEquals(Point2D.class, params[2]);
    assertEquals(float[].class, params[3]);
    assertEquals(Color[].class, params[4]);
    assertEquals(AffineTransform.class, params[5]);
  }

  // ── ClipboardIcon no longer declares gradient methods ────────────

  @Test
  public void clipboardIconDoesNotDeclareLinearGradientPaint() {
    assertIconDoesNotDeclare("new_LinearGradientPaint");
  }

  @Test
  public void clipboardIconDoesNotDeclareRadialGradientPaint() {
    assertIconDoesNotDeclare("new_RadialGradientPaint");
  }

  // ── Functional: methods produce valid Paint ──────────────────────

  @Test
  public void linearGradientPaintProducesNonNullPaint() throws Exception {
    Method m = findMethod("new_LinearGradientPaint");
    assertNotNull(m);
    m.setAccessible(true);
    Object result = m.invoke(null,
        new Point2D.Double(0, 0),
        new Point2D.Double(10, 10),
        new float[]{0.0f, 1.0f},
        new Color[]{Color.RED, Color.BLUE},
        new AffineTransform());
    assertNotNull("new_LinearGradientPaint must return non-null Paint", result);
    assertTrue("result must implement Paint", result instanceof Paint);
  }

  @Test
  public void radialGradientPaintProducesNonNullPaint() throws Exception {
    Method m = findMethod("new_RadialGradientPaint");
    assertNotNull(m);
    m.setAccessible(true);
    Object result = m.invoke(null,
        new Point2D.Double(5, 5),
        10.0f,
        new Point2D.Double(5, 5),
        new float[]{0.0f, 1.0f},
        new Color[]{Color.WHITE, Color.BLACK},
        new AffineTransform());
    assertNotNull("new_RadialGradientPaint must return non-null Paint", result);
    assertTrue("result must implement Paint", result instanceof Paint);
  }

  // ── Helpers ──────────────────────────────────────────────────────

  private static Method findMethod(String name) {
    return Arrays.stream(factoryClass.getDeclaredMethods())
        .filter(m -> m.getName().equals(name))
        .findFirst()
        .orElse(null);
  }

  private static void assertIconDoesNotDeclare(String name) {
    boolean found = Arrays.stream(iconClass.getDeclaredMethods())
        .anyMatch(m -> m.getName().equals(name));
    assertFalse("ClipboardIcon should not declare '" + name + "' (moved to GradientPaintFactory)",
        found);
  }
}
