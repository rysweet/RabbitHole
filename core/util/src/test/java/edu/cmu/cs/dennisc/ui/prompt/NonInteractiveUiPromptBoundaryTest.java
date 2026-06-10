package edu.cmu.cs.dennisc.ui.prompt;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class NonInteractiveUiPromptBoundaryTest {
  @Test
  public void nonInteractiveBoundaryNeverSelectsResourcesAcceptsEulasOrShowsMessages() {
    NonInteractiveUiPromptBoundary boundary = NonInteractiveUiPromptBoundary.INSTANCE;

    assertSame(ResourcePromptResult.noSelection(), boundary.requestResourceLocation(new ResourcePromptRequest("title", "resource", "assets/resource", Collections.emptyList(), "missing")));
    assertFalse(boundary.requestEulaAcceptance(new EulaPromptRequest("title", "license", "product")));
    boundary.showMessage(new MessagePromptRequest(MessageSeverity.ERROR, "title", "message"));
  }

  @Test
  public void nonInteractiveBoundaryRejectsNullRequests() {
    NonInteractiveUiPromptBoundary boundary = NonInteractiveUiPromptBoundary.INSTANCE;

    assertThrows(NullPointerException.class, () -> boundary.requestResourceLocation(null));
    assertThrows(NullPointerException.class, () -> boundary.showMessage(null));
    assertThrows(NullPointerException.class, () -> boundary.requestEulaAcceptance(null));
  }
}
