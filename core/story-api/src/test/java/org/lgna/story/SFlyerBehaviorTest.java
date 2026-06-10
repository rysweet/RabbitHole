package org.lgna.story;

import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.junit.After;
import org.junit.Test;
import org.lgna.story.resources.FlyerResource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class SFlyerBehaviorTest {
  @After
  public void resetPromptBoundary() {
    UiPrompts.reset();
  }

  @Test
  public void jointAccessorsExposeWingNeckAndTailFacades() {
    SFlyer flyer = new SFlyer(new JointedModelStubSupport.StubFlyerResource());

    assertSame(flyer.getRoot(), flyer.getJoint(FlyerResource.ROOT));
    assertSame(flyer.getNeck(), flyer.getNeckArray()[0]);
    assertSame(flyer.getTail(), flyer.getTailArray()[0]);
    assertSame(flyer.getLeftWingWrist(), flyer.getJoint(FlyerResource.LEFT_WING_WRIST));
    assertSame(flyer.getTail2(), flyer.getJoint(FlyerResource.TAIL_1));
    assertSame(flyer.getTail3(), flyer.getJoint(FlyerResource.TAIL_2));
  }

  @Test
  public void walkToAndTouchRequestBoundaryMessagesInHeadlessRuns() {
    RecordingBoundary boundary = new RecordingBoundary();
    UiPrompts.install(boundary);
    SFlyer flyer = new SFlyer(new JointedModelStubSupport.StubFlyerResource());

    flyer.walkTo(new SThingMarker());
    flyer.touch(new SThingMarker());

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
