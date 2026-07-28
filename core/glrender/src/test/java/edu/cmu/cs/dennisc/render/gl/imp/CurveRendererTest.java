package edu.cmu.cs.dennisc.render.gl.imp;



import com.jogamp.math.FloatUtil;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

/**
 * Tests for {@link CurveRenderer} — validates constants and constructor.
 *
 * All draw methods require a GL context, so only static constants
 * and construction are tested here.
 */
public class CurveRendererTest {



  // ── Constructor ──

  @Test
  public void constructor_succeeds() {
    CurveRenderer renderer = new CurveRenderer();
    assertNotNull(renderer);
  }

  // ── Constants via reflection ──

  @Test
  public void stackCount_is50() throws Exception {
    int value = getStaticIntField("STACK_COUNT");
    assertEquals(50, value);
  }

  @Test
  public void sliceCount_is50() throws Exception {
    int value = getStaticIntField("SLICE_COUNT");
    assertEquals(50, value);
  }

  @Test
  public void sideCount_is50() throws Exception {
    int value = getStaticIntField("SIDE_COUNT");
    assertEquals(50, value);
  }

  @Test
  public void ringCount_is50() throws Exception {
    int value = getStaticIntField("RING_COUNT");
    assertEquals(50, value);
  }

  @Test
  public void tau_isTwoPi() throws Exception {
    float value = getStaticFloatField("TAU");
    assertEquals(2f * FloatUtil.PI, value, 0.0001f);
  }

  @Test
  public void tau_isPositive() throws Exception {
    float value = getStaticFloatField("TAU");
    assertTrue("TAU should be positive", value > 0);
  }

  @Test
  public void tau_matchesMathTwoPi() throws Exception {
    float value = getStaticFloatField("TAU");
    assertEquals((float) (2.0 * Math.PI), value, 0.001f);
  }

  @Test
  public void allCountsArePositive() throws Exception {
    assertTrue(getStaticIntField("STACK_COUNT") > 0);
    assertTrue(getStaticIntField("SLICE_COUNT") > 0);
    assertTrue(getStaticIntField("SIDE_COUNT") > 0);
    assertTrue(getStaticIntField("RING_COUNT") > 0);
  }

  // ── Context creates a CurveRenderer internally ──

  @Test
  public void context_containsCurveRenderer() throws Exception {
    // PickContext is a concrete Context subclass that doesn't need GL for construction
    PickContext pc = new PickContext(true);
    Field f = Context.class.getDeclaredField("curveRenderer");
    f.setAccessible(true);
    Object renderer = f.get(pc);
    assertNotNull("Context should contain a CurveRenderer", renderer);
    assertTrue(renderer instanceof CurveRenderer);
  }

  // ── Helpers ──

  private int getStaticIntField(String name) throws Exception {
    Field f = CurveRenderer.class.getDeclaredField(name);
    f.setAccessible(true);
    return f.getInt(null);
  }

  private float getStaticFloatField(String name) throws Exception {
    Field f = CurveRenderer.class.getDeclaredField(name);
    f.setAccessible(true);
    return f.getFloat(null);
  }
}
