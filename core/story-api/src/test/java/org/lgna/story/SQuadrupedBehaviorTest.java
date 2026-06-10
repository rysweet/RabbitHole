package org.lgna.story;

import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.junit.After;
import org.junit.Test;
import org.lgna.story.resources.QuadrupedResource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class SQuadrupedBehaviorTest {
  @After
  public void resetPromptBoundary() {
    UiPrompts.reset();
  }

  @Test
  public void jointAccessorsExposeTailAndLegFacades() {
    SQuadruped quadruped = new SQuadruped(new JointedModelStubSupport.StubQuadrupedResource());

    assertSame(quadruped.getRoot(), quadruped.getJoint(QuadrupedResource.ROOT));
    assertSame(quadruped.getFrontLeftFoot(), quadruped.getJoint(QuadrupedResource.FRONT_LEFT_FOOT));
    assertSame(quadruped.getBackRightToe(), quadruped.getJoint(QuadrupedResource.BACK_RIGHT_TOE));
    assertSame(quadruped.getTail(), quadruped.getTailArray()[0]);
  }

  @Test
  public void deprecatedTailAliasesStillPointAtTailSegments() {
    SQuadruped quadruped = new SQuadruped(new JointedModelStubSupport.StubQuadrupedResource());

    assertSame(quadruped.getTail(), quadruped.getTail1());
    assertSame(quadruped.getTail2(), quadruped.getJoint(QuadrupedResource.TAIL_1));
    assertSame(quadruped.getTail3(), quadruped.getJoint(QuadrupedResource.TAIL_2));
    assertSame(quadruped.getTail4(), quadruped.getJoint(QuadrupedResource.TAIL_3));
  }

  @Test
  public void walkToAndTouchRequestBoundaryMessagesInHeadlessRuns() {
    RecordingBoundary boundary = new RecordingBoundary();
    UiPrompts.install(boundary);
    SQuadruped quadruped = new SQuadruped(new JointedModelStubSupport.StubQuadrupedResource());

    quadruped.walkTo(new SThingMarker());
    quadruped.touch(new SThingMarker());

    assertEquals("todo: walkTo", boundary.messages.get(0).message());
    assertEquals("todo: touch", boundary.messages.get(1).message());
  }

  private static class RecordingBoundary implements UiPromptBoundary {
    private final List<MessagePromptRequest> messages = new ArrayList<>();

    @Override
    public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request) {
      return ResourcePromptResult.noSelection();
    }

    @Override
    public void showMessage(MessagePromptRequest request) {
      messages.add(request);
    }

    @Override
    public boolean requestEulaAcceptance(EulaPromptRequest request) {
      return false;
    }
  }
}
