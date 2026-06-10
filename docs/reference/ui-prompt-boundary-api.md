---
title: UI Prompt Boundary API Reference
description: Reference for UiPromptBoundary, UiPrompts, prompt request records, and Swing/non-interactive configuration.
last_updated: 2026-06-10
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: reference
---

# UI prompt boundary API reference

The UI prompt boundary is the shared API for prompt-producing reusable code.
It lets Alice request resource locations, messages, and EULA decisions without
depending on Swing.

## Contents

- [Packages](#packages)
- [`UiPromptBoundary`](#uipromptboundary)
- [`UiPrompts`](#uiprompts)
- [`NonInteractiveUiPromptBoundary`](#noninteractiveuipromptboundary)
- [`SwingUiPromptBoundary`](#swinguipromptboundary)
- [Prompt request and result records](#prompt-request-and-result-records)
- [Runtime configuration](#runtime-configuration)
- [Thread-safety](#thread-safety)
- [Direct dialog allowlist](#direct-dialog-allowlist)

## Packages

Shared Swing-free API:

```java
package edu.cmu.cs.dennisc.ui.prompt;
```

Swing adapter:

```java
package org.lgna.story.resourceutilities;
```

The shared API lives in `core/util` because it is used by utility code,
resource-loading code, EULA code, and nonfree initialization code. The Swing
adapter lives with the story resource UI because it owns `FindResourcesPanel`
and must remain outside Swing-free utility code.

## `UiPromptBoundary`

```java
public interface UiPromptBoundary {
  ResourcePromptResult requestResourceLocation(ResourcePromptRequest request);

  void showMessage(MessagePromptRequest request);

  boolean requestEulaAcceptance(EulaPromptRequest request);
}
```

### `requestResourceLocation`

```java
ResourcePromptResult requestResourceLocation(ResourcePromptRequest request)
```

Requests a gallery or asset directory from the active UI boundary.

Behavior:

1. The request describes the missing resource, searched directories, and
   installation-relative path.
2. The boundary either returns a selected directory or `ResourcePromptResult.noSelection()`.
3. The caller validates the selected directory before saving preferences or
   loading resources.
4. A no-selection result is not an error by itself; it means no user directory
   was provided.

### `showMessage`

```java
void showMessage(MessagePromptRequest request)
```

Reports an informational, warning, or error message through the active boundary.

The boundary must not reinterpret the message as success or failure. Reusable
callers keep their existing control flow after the message request returns.

### `requestEulaAcceptance`

```java
boolean requestEulaAcceptance(EulaPromptRequest request)
```

Requests acceptance of a license agreement.

Return values:

| Value | Meaning |
| --- | --- |
| `true` | The user accepted the EULA. |
| `false` | The user rejected the EULA, closed the prompt, or no interactive boundary is installed. |

The method does not persist preferences. `EULAUtilities` persists accepted
licenses and throws `LicenseRejectedException` on rejection.

## `UiPrompts`

```java
public final class UiPrompts {
  public static UiPromptBoundary get();

  public static UiPromptBoundary install(UiPromptBoundary boundary);

  public static void reset();

  public static ResourcePromptResult requestResourceLocation(ResourcePromptRequest request);

  public static void showMessage(MessagePromptRequest request);

  public static boolean requestEulaAcceptance(EulaPromptRequest request);
}
```

`UiPrompts` is a tiny static registry. It starts with
`NonInteractiveUiPromptBoundary` installed.

### `get`

```java
public static UiPromptBoundary get()
```

Returns the currently installed boundary. The result is never `null`.

### `install`

```java
public static UiPromptBoundary install(UiPromptBoundary boundary)
```

Installs `boundary` and returns the previous boundary. `boundary` must not be
`null`; passing `null` throws `NullPointerException`.

Use the returned value for scoped restoration:

```java
UiPromptBoundary previous = UiPrompts.install(new RecordingPromptBoundary());
try {
  StorytellingResources.INSTANCE.findAndLoadInstalledAliceResourcesIfNecessary();
} finally {
  UiPrompts.install(previous);
}
```

### `reset`

```java
public static void reset()
```

Restores the process default `NonInteractiveUiPromptBoundary`.

Use `reset` in test cleanup when the previous boundary is not needed. Prefer
restoring the previous boundary when nesting with other harnesses.

### Delegating convenience methods

The `requestResourceLocation`, `showMessage`, and `requestEulaAcceptance`
methods validate non-null request values and delegate to `get()`. They exist so
reusable callers do not need to hold the registry result.

## `NonInteractiveUiPromptBoundary`

```java
public final class NonInteractiveUiPromptBoundary implements UiPromptBoundary {
  public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request);

  public void showMessage(MessagePromptRequest request);

  public boolean requestEulaAcceptance(EulaPromptRequest request);
}
```

Default behavior:

| Request | Result |
| --- | --- |
| Resource location | `ResourcePromptResult.noSelection()` |
| Message | No UI side effect |
| EULA acceptance | `false` |

This default makes reusable code safe in headless Maven tests, command-line
tools, static analysis, and embedded library use.

## `SwingUiPromptBoundary`

```java
public final class SwingUiPromptBoundary implements UiPromptBoundary {
  public SwingUiPromptBoundary();

  public ResourcePromptResult requestResourceLocation(ResourcePromptRequest request);

  public void showMessage(MessagePromptRequest request);

  public boolean requestEulaAcceptance(EulaPromptRequest request);
}
```

`SwingUiPromptBoundary` is the only production adapter that renders these prompt
requests as Swing dialogs.

It owns:

| Prompt | Swing implementation |
| --- | --- |
| Alice gallery and Sims resource lookup | `FindResourcesPanel` |
| Informational, warning, and error messages | `JOptionPane.showMessageDialog` |
| EULA acceptance | `JEulaPane`, `JDialogBuilder`, `JDialog`, and the existing "Return to license agreement?" confirmation flow |

The adapter preserves existing text, titles, modal behavior, and option flow.
It does not validate or persist selected resource directories; callers retain
that responsibility.

## Prompt request and result records

All request/result types are immutable data carriers.

### `ResourcePromptRequest`

```java
public record ResourcePromptRequest(
    String title,
    String resourceName,
    String installRelativePath,
    List<File> searchedDirectories,
    String missingMessage,
    boolean alwaysPrompt) {
}
```

| Field | Required | Description |
| --- | --- | --- |
| `title` | Yes | UI title or logical prompt title, for example `Locate Alice Gallery`. |
| `resourceName` | Yes | Human-readable resource name, for example `Alice gallery resources` or `The Sims (TM) 2 Art Assets`. |
| `installRelativePath` | Yes | Expected path below a gallery root, for example `assets/alice` or `assets/sims`. |
| `searchedDirectories` | Yes | Directories already checked by reusable code. May be empty. |
| `missingMessage` | Yes | Base user-visible failure sentence for the missing resource, for example `Cannot find the Alice gallery resources.` |
| `alwaysPrompt` | Yes | `true` for explicit user-initiated location commands that must show the chooser even if the adapter has a previous selection; `false` for missing-resource recovery prompts. |

The record defensively copies `searchedDirectories` and rejects `null` required
fields. `missingMessage` is intentionally the base failure sentence, not the
fully composed directory report. Callers keep the exact legacy final message by
combining this base sentence with `searchedDirectories` after any selected
directory has been validated and resource loading still fails. The five-argument
constructor defaults `alwaysPrompt` to `false`.

### `ResourcePromptResult`

```java
public record ResourcePromptResult(Optional<File> selectedGalleryDirectory) {
  public static ResourcePromptResult selected(File galleryDirectory);

  public static ResourcePromptResult noSelection();
}
```

| Result | Meaning |
| --- | --- |
| `selected(file)` | The boundary returned a user-selected gallery directory. |
| `noSelection()` | No directory was selected or no interactive boundary is installed. |

The selected directory is a gallery root. Callers append the appropriate
install-relative path and validate the directory before use.

### `MessagePromptRequest`

```java
public record MessagePromptRequest(
    MessageSeverity severity,
    String title,
    String message) {
}
```

| Field | Required | Description |
| --- | --- | --- |
| `severity` | Yes | Dialog severity. |
| `title` | No | Optional title for UI adapters. `null` means the adapter uses its normal default title. |
| `message` | Yes | Exact user-visible text. |

`MessagePromptRequest` rejects `null` `severity` and `message` values. `title`
is the only nullable field in the request.

### `MessageSeverity`

```java
public enum MessageSeverity {
  INFO,
  WARNING,
  ERROR
}
```

Swing mapping:

| Severity | `JOptionPane` message type |
| --- | --- |
| `INFO` | `JOptionPane.INFORMATION_MESSAGE` |
| `WARNING` | `JOptionPane.WARNING_MESSAGE` |
| `ERROR` | `JOptionPane.ERROR_MESSAGE` |

### `EulaPromptRequest`

```java
public record EulaPromptRequest(
    String title,
    String licenseText,
    String productName) {
}
```

| Field | Required | Description |
| --- | --- | --- |
| `title` | Yes | Dialog title, for example `License Agreement (Part 1 of 2): Alice 3`. |
| `licenseText` | Yes | Full license text shown in the EULA pane. |
| `productName` | Yes | Product name used in the rejection prompt. |

The Swing adapter derives the rejection message from `productName`:

```text
You must accept the license agreement in order to use <productName>.

Would you like to return to the license agreement?
```

## Runtime configuration

There is no environment variable, system property, preferences file, or command
line flag for choosing prompt behavior.

Configuration is process-local Java state:

| Surface | Owner | Behavior |
| --- | --- | --- |
| Default reusable code | `UiPrompts` | Uses `NonInteractiveUiPromptBoundary`. |
| Desktop Alice launch | `org.alice.stageide.EntryPoint` | Installs `new SwingUiPromptBoundary()` before reusable startup flows run. |
| IDE startup | `org.alice.stageide.StageIDE` | Runs license and resource flows after the Swing boundary is installed. |
| Nonfree IDE integration | `org.alice.nonfree.IdeNonfree` | Uses the installed Swing boundary for Sims EULA/resource prompts. |
| Tests | The test that installs a boundary | Installs a recording boundary and restores the previous boundary in `finally`. |

Production desktop startup installs the Swing boundary for the full desktop
lifecycle:

```java
UiPrompts.install(new SwingUiPromptBoundary());
launchAliceDesktop(args);
```

Use scoped restoration only when the scope is guaranteed to cover every
prompt-producing callback. A `try`/`finally` around a method that merely queues
Swing or JavaFX startup work can restore the previous boundary too early, before
resource loading, EULA prompts, or nonfree initialization run.

## Thread-safety

Boundary storage is process-wide and must be visible from startup code, the Swing
event dispatch thread, JavaFX startup code, resource-loading threads, and
exception/initialization paths.

The implementation stores the boundary in a thread-safe holder such as
`AtomicReference<UiPromptBoundary>` or an equivalent visibility mechanism.

Operational rules:

1. Desktop startup installs the Swing boundary before resource loading,
   `EULAUtilities`, or nonfree initialization can prompt.
2. Tests restore the previous boundary in `finally`.
3. Reusable code does not cache the boundary long term; it calls `UiPrompts` at
   the prompt point.
4. Production code does not swap prompt boundaries during normal application
   runtime.

## Direct dialog allowlist

Production direct `JOptionPane`, `JDialog`, and `FindResourcesPanel` usage is
allowed only in explicit UI-owned code. The prompt-boundary characterization
tests scan reusable modules for forbidden direct dialog usage.

Reusable modules that must stay dialog-free include:

| Module or class | Rule |
| --- | --- |
| `core/story-api` resource flows | `StorytellingResources` and related reusable resource lookup code call `UiPrompts`, not Swing dialogs. |
| `core-nonfree/story-api-nonfree` resource flows | `NebulousStorytellingResources` and `edu.cmu.cs.dennisc.nebulous.Manager` call `UiPrompts`, not Swing dialogs. |
| `core/util` EULA flow | `EULAUtilities` delegates user decisions to `UiPrompts`, not `JDialog` or `JOptionPane`. |

See [UI prompt boundary](../concepts/ui-prompt-boundary.md) for the
architectural rule and [using the UI prompt boundary](../howto/use-ui-prompt-boundary.md)
for examples.
