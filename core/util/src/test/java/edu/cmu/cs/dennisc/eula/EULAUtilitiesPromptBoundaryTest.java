package edu.cmu.cs.dennisc.eula;

import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.prefs.Preferences;

import static org.junit.Assert.*;

public class EULAUtilitiesPromptBoundaryTest {
  private static final String KEY = "uiPromptBoundaryEulaAccepted";
  private Preferences preferences;

  @Before
  public void clearPreference() {
    preferences = Preferences.userNodeForPackage(EULAUtilitiesPromptBoundaryTest.class);
    preferences.putBoolean(KEY, false);
  }

  @After
  public void resetBoundaryAndPreference() {
    UiPrompts.reset();
    preferences.putBoolean(KEY, false);
  }

  @Test
  public void rejectedBoundaryDecisionThrowsAndDoesNotPersistAcceptance() {
    RecordingBoundary boundary = new RecordingBoundary(false);
    UiPrompts.install(boundary);

    try {
      EULAUtilities.promptUserToAcceptEULAIfNecessary(EULAUtilitiesPromptBoundaryTest.class, KEY, "License Title", "license text", "Test Product");
      fail("Expected LicenseRejectedException");
    } catch (LicenseRejectedException expected) {
      assertEquals("License Title", boundary.request.title());
      assertEquals("license text", boundary.request.licenseText());
      assertEquals("Test Product", boundary.request.productName());
      assertFalse(preferences.getBoolean(KEY, false));
    }
  }

  @Test
  public void acceptedBoundaryDecisionPersistsAcceptance() throws Exception {
    RecordingBoundary boundary = new RecordingBoundary(true);
    UiPrompts.install(boundary);

    EULAUtilities.promptUserToAcceptEULAIfNecessary(EULAUtilitiesPromptBoundaryTest.class, KEY, "License Title", "license text", "Test Product");

    assertEquals(1, boundary.eulaRequests);
    assertTrue(preferences.getBoolean(KEY, false));
  }

  @Test
  public void existingAcceptedPreferenceSkipsBoundaryPrompt() throws Exception {
    preferences.putBoolean(KEY, true);
    RecordingBoundary boundary = new RecordingBoundary(false);
    UiPrompts.install(boundary);

    EULAUtilities.promptUserToAcceptEULAIfNecessary(EULAUtilitiesPromptBoundaryTest.class, KEY, "License Title", "license text", "Test Product");

    assertEquals(0, boundary.eulaRequests);
    assertTrue(preferences.getBoolean(KEY, false));
  }

  private static class RecordingBoundary implements UiPromptBoundary {
    private final boolean acceptEula;
    private int eulaRequests;
    private EulaPromptRequest request;

    RecordingBoundary(boolean acceptEula) {
      this.acceptEula = acceptEula;
    }

    @Override
    public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request) {
      return ResourcePromptResult.noSelection();
    }

    @Override
    public void showMessage(MessagePromptRequest request) {
    }

    @Override
    public boolean requestEulaAcceptance(EulaPromptRequest request) {
      eulaRequests++;
      this.request = request;
      return acceptEula;
    }
  }
}
