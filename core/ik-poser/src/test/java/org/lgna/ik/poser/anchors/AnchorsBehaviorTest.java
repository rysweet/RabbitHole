package org.lgna.ik.poser.anchors;

import org.junit.Test;
import org.lgna.ik.poser.anchors.events.AnchorEvent;
import org.lgna.ik.poser.anchors.events.AnchorListener;
import org.lgna.story.resources.JointId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class AnchorsBehaviorTest {
  @Test
  public void changingAnchorPublishesPreviousAndNextJointIds() {
    Anchors anchors = new Anchors();
    RecordingAnchorListener listener = new RecordingAnchorListener();
    JointId shoulder = new JointId(null, null);
    JointId elbow = new JointId(shoulder, null);
    anchors.addAnchorListener(listener);

    anchors.setLeftArm(shoulder);
    anchors.setLeftArm(elbow);

    assertEquals(2, listener.leftArmChangeCount);
    assertNull(listener.firstLeftArmEvent.getPreviousValue());
    assertSame(shoulder, listener.firstLeftArmEvent.getNextValue());
    assertSame(shoulder, listener.secondLeftArmEvent.getPreviousValue());
    assertSame(elbow, listener.secondLeftArmEvent.getNextValue());
    assertSame(elbow, anchors.getLeftArm());
  }

  @Test
  public void reassigningSameAnchorAndRemovingListenerSuppressesFurtherNotifications() {
    Anchors anchors = new Anchors();
    RecordingAnchorListener listener = new RecordingAnchorListener();
    JointId foot = new JointId(null, null);
    anchors.addAnchorListener(listener);

    anchors.setRightLeg(foot);
    anchors.setRightLeg(foot);
    anchors.removeAnchorListener(listener);
    anchors.setRightLeg(new JointId(foot, null));

    assertEquals(1, listener.rightLegChangeCount);
    assertNull(listener.rightLegEvent.getPreviousValue());
    assertSame(foot, listener.rightLegEvent.getNextValue());
  }

  private static final class RecordingAnchorListener implements AnchorListener {
    private int leftArmChangeCount;
    private int rightLegChangeCount;
    private AnchorEvent firstLeftArmEvent;
    private AnchorEvent secondLeftArmEvent;
    private AnchorEvent rightLegEvent;

    @Override
    public void leftArmChanged(AnchorEvent e) {
      this.leftArmChangeCount++;
      if (this.leftArmChangeCount == 1) {
        this.firstLeftArmEvent = e;
      } else {
        this.secondLeftArmEvent = e;
      }
    }

    @Override
    public void rightArmChanged(AnchorEvent e) {
    }

    @Override
    public void leftLegChanged(AnchorEvent e) {
    }

    @Override
    public void rightLegChanged(AnchorEvent e) {
      this.rightLegChangeCount++;
      this.rightLegEvent = e;
    }
  }
}
