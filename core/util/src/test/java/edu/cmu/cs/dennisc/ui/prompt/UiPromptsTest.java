package edu.cmu.cs.dennisc.ui.prompt;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

public class UiPromptsTest {
  @After
  public void resetBoundary() {
    UiPrompts.reset();
  }

  @Test
  public void defaultBoundaryIsNonInteractive() {
    assertSame(ResourcePromptResult.noSelection(), UiPrompts.requestResourceLocation(resourceRequest()));
    assertFalse(UiPrompts.requestEulaAcceptance(new EulaPromptRequest("title", "license", "product")));
    UiPrompts.showMessage(new MessagePromptRequest(MessageSeverity.INFO, null, "message"));
  }

  @Test
  public void installRejectsNullBoundary() {
    try {
      UiPrompts.install(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException expected) {
      assertEquals("boundary", expected.getMessage());
    }
  }

  @Test
  public void installReturnsPreviousBoundaryAndDelegatesToNewBoundary() {
    RecordingBoundary recording = new RecordingBoundary();
    UiPromptBoundary previous = UiPrompts.install(recording);

    ResourcePromptResult result = UiPrompts.requestResourceLocation(resourceRequest());
    UiPrompts.showMessage(new MessagePromptRequest(MessageSeverity.WARNING, "title", "message"));
    boolean accepted = UiPrompts.requestEulaAcceptance(new EulaPromptRequest("title", "license", "product"));

    assertSame(NonInteractiveUiPromptBoundary.INSTANCE, previous);
    assertEquals(Optional.of(new File("/selected/gallery")), result.selectedGalleryDirectory());
    assertTrue(accepted);
    assertEquals(1, recording.resourceRequests);
    assertEquals(1, recording.messageRequests);
    assertEquals(1, recording.eulaRequests);
  }

  @Test
  public void resetRestoresNonInteractiveBoundary() {
    UiPrompts.install(new RecordingBoundary());
    UiPrompts.reset();

    assertSame(NonInteractiveUiPromptBoundary.INSTANCE, UiPrompts.get());
    assertSame(ResourcePromptResult.noSelection(), UiPrompts.requestResourceLocation(resourceRequest()));
  }

  @Test
  public void requestRecordsValidateRequiredFields() {
    assertThrows(NullPointerException.class, () -> new ResourcePromptRequest(null, "resource", "assets", Collections.emptyList(), "missing"));
    assertThrows(IllegalArgumentException.class, () -> new ResourcePromptRequest("title", "", "assets", Collections.emptyList(), "missing"));
    assertThrows(NullPointerException.class, () -> new MessagePromptRequest(null, null, "message"));
    assertThrows(NullPointerException.class, () -> new EulaPromptRequest("title", null, "product"));
  }

  private static ResourcePromptRequest resourceRequest() {
    return new ResourcePromptRequest("title", "resource", "assets/resource", Collections.emptyList(), "missing");
  }

  private static class RecordingBoundary implements UiPromptBoundary {
    int resourceRequests;
    int messageRequests;
    int eulaRequests;

    @Override
    public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request) {
      resourceRequests++;
      return ResourcePromptResult.selected(new File("/selected/gallery"));
    }

    @Override
    public void showMessage(MessagePromptRequest request) {
      messageRequests++;
    }

    @Override
    public boolean requestEulaAcceptance(EulaPromptRequest request) {
      eulaRequests++;
      return true;
    }
  }
}
