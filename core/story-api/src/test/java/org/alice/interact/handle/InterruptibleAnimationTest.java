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
package org.alice.interact.handle;

import edu.cmu.cs.dennisc.animation.TraditionalStyle;
import edu.cmu.cs.dennisc.color.Color4f;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Behavioral tests for the extracted interruptible animation classes.
 *
 * Verifies:
 * - DoubleInterruptibleAnimation: construction, isActive, target tracking, matchesTarget, cancel
 * - Color4fInterruptibleAnimation: construction, isActive, target tracking, matchesTarget, cancel
 * - Both classes are properly abstract (require update method implementation)
 */
public class InterruptibleAnimationTest {

  private static final double EPSILON = 1e-9;

  // Concrete subclass for testing DoubleInterruptibleAnimation
  private static class TestDoubleAnimation extends DoubleInterruptibleAnimation {
    private double lastValue;

    public TestDoubleAnimation(Double d0, Double d1) {
      super(0.25, TraditionalStyle.BEGIN_AND_END_GENTLY, d0, d1);
    }

    @Override
    protected void updateValue(Double v) {
      this.lastValue = v;
    }

    public double getLastValue() {
      return lastValue;
    }
  }

  // Concrete subclass for testing Color4fInterruptibleAnimation
  private static class TestColor4fAnimation extends Color4fInterruptibleAnimation {
    private Color4f lastValue;

    public TestColor4fAnimation(Color4f c0, Color4f c1) {
      super(0.25, TraditionalStyle.BEGIN_AND_END_GENTLY, c0, c1);
    }

    @Override
    protected void updateValue(Color4f v) {
      this.lastValue = v;
    }

    public Color4f getLastValue() {
      return lastValue;
    }
  }

  // ===== DoubleInterruptibleAnimation =====

  @Test
  public void doubleAnimation_initiallyActive() {
    TestDoubleAnimation anim = new TestDoubleAnimation(0.0, 1.0);
    assertTrue("Newly created animation should be active", anim.isActive());
  }

  @Test
  public void doubleAnimation_targetMatchesConstructor() {
    TestDoubleAnimation anim = new TestDoubleAnimation(0.0, 1.0);
    assertEquals("Target should match d1 from constructor", 1.0, anim.getTarget(), EPSILON);
  }

  @Test
  public void doubleAnimation_matchesTarget_exactMatch() {
    TestDoubleAnimation anim = new TestDoubleAnimation(0.0, 1.0);
    assertTrue("matchesTarget should return true for exact target value",
        anim.matchesTarget(1.0));
  }

  @Test
  public void doubleAnimation_matchesTarget_differentValue() {
    TestDoubleAnimation anim = new TestDoubleAnimation(0.0, 1.0);
    assertFalse("matchesTarget should return false for different value",
        anim.matchesTarget(0.5));
  }

  @Test
  public void doubleAnimation_cancel_deactivates() {
    TestDoubleAnimation anim = new TestDoubleAnimation(0.0, 1.0);
    anim.cancel();
    assertFalse("Animation should not be active after cancel", anim.isActive());
  }

  @Test
  public void doubleAnimation_cancel_setsTargetToNaN() {
    TestDoubleAnimation anim = new TestDoubleAnimation(0.0, 1.0);
    anim.cancel();
    assertTrue("Target should be NaN after cancel", Double.isNaN(anim.getTarget()));
  }

  @Test
  public void doubleAnimation_matchesTarget_falseAfterCancel() {
    TestDoubleAnimation anim = new TestDoubleAnimation(0.0, 1.0);
    anim.cancel();
    assertFalse("matchesTarget should return false after cancel (NaN != NaN)",
        anim.matchesTarget(1.0));
    assertFalse("matchesTarget should return false for NaN target",
        anim.matchesTarget(-1.0));
  }

  // ===== Color4fInterruptibleAnimation =====

  @Test
  public void color4fAnimation_initiallyActive() {
    TestColor4fAnimation anim = new TestColor4fAnimation(Color4f.BLACK, Color4f.WHITE);
    assertTrue("Newly created animation should be active", anim.isActive());
  }

  @Test
  public void color4fAnimation_targetMatchesConstructor() {
    TestColor4fAnimation anim = new TestColor4fAnimation(Color4f.BLACK, Color4f.WHITE);
    assertEquals("Target should match c1 from constructor", Color4f.WHITE, anim.getTarget());
  }

  @Test
  public void color4fAnimation_matchesTarget_exactMatch() {
    TestColor4fAnimation anim = new TestColor4fAnimation(Color4f.BLACK, Color4f.WHITE);
    assertTrue("matchesTarget should return true for exact target color",
        anim.matchesTarget(Color4f.WHITE));
  }

  @Test
  public void color4fAnimation_matchesTarget_differentColor() {
    TestColor4fAnimation anim = new TestColor4fAnimation(Color4f.BLACK, Color4f.WHITE);
    assertFalse("matchesTarget should return false for different color",
        anim.matchesTarget(Color4f.RED));
  }

  @Test
  public void color4fAnimation_matchesTarget_nullTarget() {
    TestColor4fAnimation anim = new TestColor4fAnimation(Color4f.BLACK, Color4f.WHITE);
    anim.cancel();
    assertFalse("matchesTarget should return false when target is null (after cancel)",
        anim.matchesTarget(Color4f.WHITE));
  }

  @Test
  public void color4fAnimation_cancel_deactivates() {
    TestColor4fAnimation anim = new TestColor4fAnimation(Color4f.BLACK, Color4f.WHITE);
    anim.cancel();
    assertFalse("Animation should not be active after cancel", anim.isActive());
  }

  @Test
  public void color4fAnimation_cancel_nullifiesTarget() {
    TestColor4fAnimation anim = new TestColor4fAnimation(Color4f.BLACK, Color4f.WHITE);
    anim.cancel();
    assertNull("Target should be null after cancel", anim.getTarget());
  }
}
