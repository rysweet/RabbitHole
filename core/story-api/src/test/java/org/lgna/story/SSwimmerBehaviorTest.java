package org.lgna.story;

import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.junit.After;
import org.junit.Test;
import org.lgna.story.resources.SwimmerResource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class SSwimmerBehaviorTest {
  @After
  public void resetPromptBoundary() {
    UiPrompts.reset();
  }

  @Test
  public void jointAccessorsExposeHeadFinAndTailFacades() {
    SSwimmer swimmer = new SSwimmer(new JointedModelStubSupport.StubSwimmerResource());

    assertSame(swimmer.getRoot(), swimmer.getJoint(SwimmerResource.ROOT));
    assertSame(swimmer.getHead(), swimmer.getJoint(SwimmerResource.HEAD));
    assertSame(swimmer.getFrontLeftFin(), swimmer.getJoint(SwimmerResource.FRONT_LEFT_FIN));
    assertSame(swimmer.getTail(), swimmer.getJoint(SwimmerResource.TAIL));
    assertSame(swimmer.getRightEye(), swimmer.getJoint(SwimmerResource.RIGHT_EYE));
  }

  @Test
  public void swimToRequestsBoundaryMessageInHeadlessRuns() {
    RecordingBoundary boundary = new RecordingBoundary();
    UiPrompts.install(boundary);
    SSwimmer swimmer = new SSwimmer(new JointedModelStubSupport.StubSwimmerResource());

    swimmer.swimTo(new SThingMarker());

    assertEquals("todo: swimTo", boundary.messages.get(0).message());
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
