---
title: Tutorial: Add a UI Prompt Boundary Adapter
description: Walks through adding or updating a prompt-producing flow so it is testable without Swing and interactive through desktop startup.
last_updated: 2026-06-10
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: tutorial
---

# Tutorial: Add a UI prompt boundary adapter

This tutorial walks through a complete prompt-boundary migration for an Alice
flow that needs to ask the user for input.

## What you will build

You will convert a resource lookup flow from direct Swing prompting to:

1. an immutable prompt request in reusable code;
2. a recording boundary test that runs headlessly;
3. the existing Swing dialog behavior through `SwingUiPromptBoundary`.

## Prerequisites

- Read [UI prompt boundary](../concepts/ui-prompt-boundary.md).
- Know which module owns the reusable behavior.
- Know the exact legacy prompt text and option flow.

## Step 1: Characterize the existing prompt

Before changing the implementation, write down the observable behavior in the
module test that owns the flow.

For a missing Alice gallery path, characterize:

```text
resourceName = Alice gallery resources
installRelativePath = assets/alice
missingMessage = Cannot find the Alice gallery resources.
```

For a missing Sims art asset path, characterize:

```text
resourceName = The Sims (TM) 2 Art Assets
installRelativePath = assets/sims
missingMessage = Cannot find The Sims (TM) 2 Art Assets.
```

The characterization should assert prompt request data, not click Swing UI.

## Step 2: Install a recording boundary in the test

Create a small test boundary that records requests and returns safe defaults.

```java
final class RecordingPromptBoundary implements UiPromptBoundary {
  final List<ResourcePromptRequest> resourceRequests = new ArrayList<>();
  final List<MessagePromptRequest> messages = new ArrayList<>();

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
    return false;
  }
}
```

Use scoped installation so global state does not leak:

```java
RecordingPromptBoundary recording = new RecordingPromptBoundary();
UiPromptBoundary previous = UiPrompts.install(recording);
try {
  runMissingResourceFlow();
} finally {
  UiPrompts.install(previous);
}
```

## Step 3: Replace direct Swing calls in reusable code

Build a request where the old code opened the dialog.

```java
ResourcePromptRequest request =
    new ResourcePromptRequest(
        "Locate Alice Gallery",
        "Alice gallery resources",
        "assets/alice",
        resourcePaths,
        "Cannot find the Alice gallery resources.");

ResourcePromptResult result = UiPrompts.requestResourceLocation(request);
File galleryDir = result.selectedGalleryDirectory().orElse(null);
```

If a directory is selected, validate it before saving preferences or retrying
resource loading. If no directory is selected, continue to the same failure
message path the legacy code used after the user cancelled or closed the prompt.

## Step 4: Keep the Swing behavior in the adapter

`SwingUiPromptBoundary` owns the Swing implementation. For resource prompts, it
uses the same `FindResourcesPanel` flow:

```java
FindResourcesPanel.getInstance().show(null);
File galleryDir = FindResourcesPanel.getInstance().getGalleryDir();
return galleryDir == null
    ? ResourcePromptResult.noSelection()
    : ResourcePromptResult.selected(galleryDir);
```

Do not move validation or preference writes into the adapter. The reusable
resource owner keeps those rules so non-interactive and Swing paths behave the
same after a result is returned.

## Step 5: Install Swing at the UI entry point

Install the adapter before Alice startup can load resources, show EULAs, or
initialize nonfree assets. Keep it installed across the desktop lifecycle so
prompt-producing work queued on Swing or JavaFX threads still reaches the Swing
boundary.

```java
UiPrompts.install(new SwingUiPromptBoundary());
launchAliceDesktop(args);
```

`EntryPoint` is the normal desktop owner. `StageIDE` and `IdeNonfree` may rely on
that installed boundary when they run license and nonfree initialization flows.
Use scoped restoration only in tests or harnesses where the scope blocks until
all prompt-producing callbacks have finished.

## Step 6: Assert the headless result

The test should prove the reusable path asks for the prompt without requiring a
display.

```java
assertEquals(1, recording.resourceRequests.size());

ResourcePromptRequest request = recording.resourceRequests.get(0);
assertEquals("Alice gallery resources", request.resourceName());
assertEquals("assets/alice", request.installRelativePath());
assertEquals("Cannot find the Alice gallery resources.", request.missingMessage());
```

For EULA flows, assert rejection remains rejection:

```java
LicenseRejectedException rejected =
    assertThrows(
        LicenseRejectedException.class,
        () -> EULAUtilities.promptUserToAcceptEULAIfNecessary(
            License.class,
            "isLicenseAccepted",
            "License Agreement (Part 1 of 2): Alice 3",
            License.TEXT,
            "Alice"));

assertNotNull(rejected);
```

## Step 7: Check for forbidden direct dialogs

Search reusable code for direct dialog usage:

```bash
rg 'JOptionPane|JDialog|FindResourcesPanel' \
  core/util/src/main/java \
  core/story-api/src/main/java \
  core-nonfree/story-api-nonfree/src/main/java
```

Direct matches are allowed only in approved UI owners such as
`SwingUiPromptBoundary` or documented UI dialog wrappers called from UI-owned
code. Resource, library, and utility flows should depend on `UiPrompts` instead.

## Finished behavior

The flow is complete when:

1. headless reusable code returns no resource selection, rejects EULAs, and does
   not open windows;
2. desktop Alice still shows the same resource, message, and license dialogs;
3. tests assert prompt request data and restore `UiPrompts` state;
4. direct dialogs remain only in explicit UI-owned code.

See [Use the UI prompt boundary](../howto/use-ui-prompt-boundary.md) for common
migration recipes and [UI prompt boundary API reference](../reference/ui-prompt-boundary-api.md)
for the exact API contract.
