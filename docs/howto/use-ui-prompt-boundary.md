---
title: Use the UI Prompt Boundary
description: Shows how to request prompts, install Swing UI ownership, and test non-interactive prompt behavior.
last_updated: 2026-06-10
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: howto
---

# Use the UI prompt boundary

Use `UiPrompts` when reusable Alice code needs a resource location, user-visible
message, or EULA decision.

## Contents

- [Before you start](#before-you-start)
- [Request a missing resource location](#request-a-missing-resource-location)
- [Report a message from reusable code](#report-a-message-from-reusable-code)
- [Request EULA acceptance](#request-eula-acceptance)
- [Install the Swing boundary at desktop startup](#install-the-swing-boundary-at-desktop-startup)
- [Test non-interactive behavior](#test-non-interactive-behavior)
- [Convert a legacy dialog call](#convert-a-legacy-dialog-call)
- [Validate the boundary](#validate-the-boundary)

## Before you start

Read the architectural rule in
[UI prompt boundary](../concepts/ui-prompt-boundary.md). Reusable code must not
import `JOptionPane`, `JDialog`, or `FindResourcesPanel` unless it is an
approved UI owner.

## Request a missing resource location

Build a `ResourcePromptRequest` with the resource name, expected install path,
searched directories, and base missing-resource message. Then ask `UiPrompts`
for a gallery directory.

```java
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;

import java.io.File;
import java.util.List;

public final class AliceGalleryLookup {
  private static final String ALICE_RESOURCE_INSTALL_PATH = "assets/alice";

  public ResourcePromptResult askForGallery(List<File> searchedDirectories) {
    String message = "Cannot find the Alice gallery resources.";
    ResourcePromptRequest request =
        new ResourcePromptRequest(
            "Locate Alice Gallery",
            "Alice gallery resources",
            ALICE_RESOURCE_INSTALL_PATH,
            searchedDirectories,
            message);

    return UiPrompts.requestResourceLocation(request);
  }
}
```

Use the six-argument constructor with `alwaysPrompt = true` only for explicit UI
commands such as "locate gallery" where the chooser should open even when the
adapter already has a selected directory.

Use the returned directory only after validating it:

```java
ResourcePromptResult result = askForGallery(resourcePaths);
if (result.selectedGalleryDirectory().isPresent()) {
  File galleryDirectory = result.selectedGalleryDirectory().get();
  File aliceResources = new File(galleryDirectory, "assets/alice");
  if (aliceResources.isDirectory()) {
    StorytellingResources.INSTANCE.setGalleryResourceDirs(
        new String[] {galleryDirectory.getAbsolutePath()});
  }
}
```

Do not save a directory only because a UI boundary returned it. The reusable
resource owner still validates and persists the path.

## Report a message from reusable code

Use `MessagePromptRequest` for existing user-visible messages that used to be
shown with `JOptionPane.showMessageDialog`.

```java
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessageSeverity;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;

public final class NonfreeInitializationReporter {
  public void reportLicenseRejected() {
    UiPrompts.showMessage(
        new MessagePromptRequest(
            MessageSeverity.INFO,
            null,
            "license rejected"));
  }

  public void reportInitializationFailure() {
    UiPrompts.showMessage(
        new MessagePromptRequest(
            MessageSeverity.ERROR,
            null,
            "failed to initialize art assets"));
  }
}
```

Keep the caller's control flow unchanged. Showing a message does not imply
retry, recovery, or success.

## Request EULA acceptance

`EULAUtilities` owns preference behavior. It asks the boundary only for the
accept/reject decision.

```java
import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;

boolean accepted =
    UiPrompts.requestEulaAcceptance(
        new EulaPromptRequest(
            "License Agreement (Part 1 of 2): Alice 3",
            License.TEXT,
            "Alice"));

if (accepted) {
  userPreferences.putBoolean("isLicenseAccepted", true);
} else {
  throw new LicenseRejectedException();
}
```

The non-interactive default returns `false`, so EULA code must continue to treat
that result as rejection.

## Install the Swing boundary at desktop startup

Install `SwingUiPromptBoundary` from an explicit UI owner before startup flows
can load resources or prompt for licenses. Keep it installed for the full
desktop lifecycle, including work queued onto Swing or JavaFX startup threads.

```java
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;
import org.lgna.story.resourceutilities.SwingUiPromptBoundary;

public final class EntryPoint {
  public static void main(String[] args) {
    UiPrompts.install(new SwingUiPromptBoundary());
    launchAliceDesktop(args);
  }
}
```

`EntryPoint`, `StageIDE`, and `IdeNonfree` are UI-owned startup surfaces. They
may install or rely on the installed Swing boundary. Reusable modules below
those surfaces do not install Swing themselves.

Use `try`/`finally` restoration only for tests or harnesses where the scoped call
blocks until all prompt-producing work has finished. Do not restore immediately
after scheduling asynchronous Swing or JavaFX startup work.

## Test non-interactive behavior

Install a recording boundary and assert the request instead of displaying UI.

```java
import edu.cmu.cs.dennisc.ui.prompt.EulaPromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.MessagePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptRequest;
import edu.cmu.cs.dennisc.ui.prompt.ResourcePromptResult;
import edu.cmu.cs.dennisc.ui.prompt.UiPromptBoundary;
import edu.cmu.cs.dennisc.ui.prompt.UiPrompts;

import java.util.ArrayList;
import java.util.List;

final class RecordingPromptBoundary implements UiPromptBoundary {
  final List<ResourcePromptRequest> resourceRequests = new ArrayList<>();
  final List<MessagePromptRequest> messages = new ArrayList<>();
  final List<EulaPromptRequest> eulaRequests = new ArrayList<>();

  @Override
  public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request) {
    resourceRequests.add(request);
    return ResourcePromptResult.noSelection();
  }

  @Override
  public void showMessage(MessagePromptRequest request) {
    messages.add(request);
  }

  @Override
  public boolean requestEulaAcceptance(EulaPromptRequest request) {
    eulaRequests.add(request);
    return false;
  }
}
```

Use scoped installation:

```java
RecordingPromptBoundary recording = new RecordingPromptBoundary();
UiPromptBoundary previous = UiPrompts.install(recording);
try {
  StorytellingResources.INSTANCE.findAndLoadInstalledAliceResourcesIfNecessary();

  assertEquals(1, recording.resourceRequests.size());
  assertEquals(
      "Alice gallery resources",
      recording.resourceRequests.get(0).resourceName());
} finally {
  UiPrompts.install(previous);
}
```

This proves the reusable code can run without opening a dialog. The test should
not require a display, click a modal window, or pre-accept a license unless it is
testing a real UI owner.

## Convert a legacy dialog call

Replace direct Swing usage in reusable code:

```java
// Before
JOptionPane.showMessageDialog(null, "failed to initialize art assets");
```

```java
// After
UiPrompts.showMessage(
    new MessagePromptRequest(
        MessageSeverity.ERROR,
        null,
        "failed to initialize art assets"));
```

Replace resource-picker access:

```java
// Before
FindResourcesPanel.getInstance().show(null);
File galleryDir = FindResourcesPanel.getInstance().getGalleryDir();
```

```java
// After
ResourcePromptResult result =
    UiPrompts.requestResourceLocation(resourcePromptRequest);
File galleryDir = result.selectedGalleryDirectory().orElse(null);
```

Keep surrounding behavior the same: clear stale preferences at the same point,
retry resource loading only after a selected directory validates, and show the
same failure text if resources are still missing.

## Validate the boundary

Run the focused modules that own the prompt behavior:

```bash
mvn -pl core/util -Dtest=UiPromptsTest,NonInteractiveUiPromptBoundaryTest,EULAUtilitiesPromptBoundaryTest test
mvn -pl core/story-api -Dtest=StorytellingResourcesPromptBoundaryTest test
mvn -pl core-nonfree/story-api-nonfree -Dtest=NebulousStorytellingResourcesPromptBoundaryTest,ManagerPromptBoundaryTest test
```

For broad validation, initialize Tweedle grammar first:

```bash
git submodule update --init tweedle-lang
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true test
```

See [UI prompt boundary API reference](../reference/ui-prompt-boundary-api.md)
for method and configuration details.
