package edu.cmu.cs.dennisc.nebulous;

import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.Assert.*;

public class ManagerPromptBoundaryTest {
  private static final String IS_LICENSE_ACCEPTED_PREFERENCE_KEY = "isLicenseAccepted";
  private boolean originalLicenseAccepted;

  @Before
  public void isolateManagerState() throws Exception {
    Preferences preferences = Preferences.userNodeForPackage(License.class);
    originalLicenseAccepted = preferences.getBoolean(IS_LICENSE_ACCEPTED_PREFERENCE_KEY, false);
    preferences.putBoolean(IS_LICENSE_ACCEPTED_PREFERENCE_KEY, false);
    setStaticField("s_isInitialized", false);
    setStaticField("s_isLicensePromptDesired", true);
    setStaticField("lastErrorOrNotification", 0L);
  }

  @After
  public void restoreManagerState() throws Exception {
    Preferences.userNodeForPackage(License.class).putBoolean(IS_LICENSE_ACCEPTED_PREFERENCE_KEY, originalLicenseAccepted);
    setStaticField("s_isInitialized", false);
    setStaticField("s_isLicensePromptDesired", true);
    setStaticField("lastErrorOrNotification", 0L);
    UiPrompts.reset();
  }

  @Test
  public void rejectedLicenseReportsThroughBoundaryMessage() throws Exception {
    RecordingBoundary boundary = new RecordingBoundary(false);
    UiPrompts.install(boundary);

    invokeInitializationWrapper();

    assertEquals("license rejected", boundary.messages.get(0).message());
    assertEquals(1, boundary.eulaRequests.size());
  }

  @Test
  public void initializationFailureReportsThroughBoundaryMessage() throws Exception {
    RecordingBoundary boundary = new RecordingBoundary(true);
    UiPrompts.install(boundary);
    System.setProperty("org.alice.ide.simsDebugResourcePath", Files.createTempDirectory("raw-sims").toString());
    try {
      invokeInitializationWrapper();
    } finally {
      System.clearProperty("org.alice.ide.simsDebugResourcePath");
    }

    assertTrue(boundary.messages.stream().anyMatch(message -> "failed to initialize art assets".equals(message.message())));
  }

  @Test
  public void reusableManagerSourceDoesNotDirectlyImportSwingDialogs() throws Exception {
    String source = Files.readString(new java.io.File("src/main/java/edu/cmu/cs/dennisc/nebulous/Manager.java").toPath());
    assertFalse(source.contains("javax.swing.JOptionPane"));
  }

  private static void invokeInitializationWrapper() throws Exception {
    Method method = Manager.class.getDeclaredMethod("doInitializationIfNecessary");
    method.setAccessible(true);
    method.invoke(null);
  }

  private static void setStaticField(String name, Object value) throws Exception {
    Field field = Manager.class.getDeclaredField(name);
    field.setAccessible(true);
    field.set(null, value);
  }

  private static class RecordingBoundary implements UiPromptBoundary {
    private final boolean acceptEula;
    private final List<EulaPromptRequest> eulaRequests = new ArrayList<>();
    private final List<MessagePromptRequest> messages = new ArrayList<>();

    RecordingBoundary(boolean acceptEula) {
      this.acceptEula = acceptEula;
    }

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
      eulaRequests.add(request);
      return acceptEula;
    }
  }
}
