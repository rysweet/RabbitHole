---
title: UI Prompt Boundary
description: Explains how Alice keeps reusable code non-interactive while preserving desktop prompts at UI entry points.
last_updated: 2026-06-10
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: explanation
---

# UI prompt boundary

Alice routes reusable prompt requests through a small UI boundary. Runtime,
resource, EULA, and utility code describe the prompt they need; explicit desktop
UI owners decide whether that request becomes a Swing dialog.

## Contents

- [Why the boundary exists](#why-the-boundary-exists)
- [Boundary responsibilities](#boundary-responsibilities)
- [Prompt flow](#prompt-flow)
- [Prompt types](#prompt-types)
- [Approved direct dialog owners](#approved-direct-dialog-owners)
- [Reusable code rules](#reusable-code-rules)
- [Characterization coverage](#characterization-coverage)

## Why the boundary exists

Reusable Alice modules run in both desktop and non-interactive contexts. Direct
`JOptionPane`, `JDialog`, or resource-picker calls from those modules make
headless tests block, make library behavior depend on a display, and hide the
actual dependency on user input.

The UI prompt boundary keeps those responsibilities separate:

| Layer | Responsibility |
| --- | --- |
| Reusable resource/library/utility code | Builds immutable prompt requests and calls `UiPrompts`. |
| Default runtime behavior | Uses `NonInteractiveUiPromptBoundary`, which does not open windows or accept licenses. |
| Desktop UI entry points | Install `SwingUiPromptBoundary` before resource loading, EULA, or nonfree initialization can prompt. |
| Swing adapter | Owns `FindResourcesPanel`, `JOptionPane`, `JDialog`, `JEulaPane`, and equivalent modal UI. |
| Tests | Install recording boundaries and assert request data instead of displaying Swing UI. |

## Boundary responsibilities

`UiPromptBoundary` is intentionally small. It has one job: convert prompt
requests into explicit results.

The shared boundary supports three request families:

1. **Resource prompts** ask the user to locate missing Alice gallery or Sims art
   asset resources.
2. **Message prompts** report existing informational, warning, or error
   messages.
3. **EULA prompts** ask whether a user accepts a license agreement.

The boundary is not a dependency-injection framework. It is process-local Java
state with a safe non-interactive default and an explicit install step for real
desktop UI startup.

## Prompt flow

```text
Reusable code
  builds ResourcePromptRequest, MessagePromptRequest, or EulaPromptRequest
    |
    v
UiPrompts delegates to installed UiPromptBoundary
    |
    +-- no Swing boundary installed
    |     NonInteractiveUiPromptBoundary returns no selection, rejects EULA,
    |     and records no message side effects
    |
    +-- Swing boundary installed by EntryPoint, StageIDE, or IdeNonfree
          SwingUiPromptBoundary shows the same Alice dialogs and returns the
          user's selection or acceptance decision
```

The default is deliberately safe. Headless code never gains a resource
directory, accepts a license, or blocks for input unless a UI owner has installed
a boundary that can actually ask the user.

## Prompt types

### Missing Alice gallery resources

`org.lgna.story.resourceutilities.StorytellingResources` uses a resource prompt
when the Alice gallery cannot be found from the installed paths or saved
preferences. Under desktop startup, the Swing boundary shows the existing
`FindResourcesPanel` flow. In non-interactive code, the request returns no
directory and resource loading continues to the same failure-reporting path.

The failure message remains:

```text
Cannot find the Alice gallery resources.
```

When no directories were detected, the message also includes:

```text
No gallery directories were detected. Make sure Alice is properly installed and has been run at least once.
```

When directories were searched, the message still lists those paths and asks the
user to verify that the directory or directories exist and that Alice is
properly installed.

### Missing Sims art assets

`org.lgna.story.resourceutilities.NebulousStorytellingResources` uses the same
resource prompt boundary for The Sims (TM) 2 Art Assets. The Swing boundary uses
the existing gallery-location UI and the reusable nonfree module stays
headless-safe.

The failure message remains:

```text
Cannot find The Sims (TM) 2 Art Assets.
```

Directory reporting keeps the current "No gallery directories were detected"
and "Searched in ..." variants.

### Nonfree initialization messages

`edu.cmu.cs.dennisc.nebulous.Manager` reports license rejection and native art
asset initialization failures through `MessagePromptRequest`.

The visible messages remain:

```text
license rejected
failed to initialize art assets
```

The throttling behavior for repeated initialization failures remains owned by
`Manager`; only the display mechanism moves behind the boundary.

### EULA acceptance

`edu.cmu.cs.dennisc.eula.EULAUtilities` still owns preference lookup,
preference clearing for `org.alice.clearAllPreferences`, persistence, and
`LicenseRejectedException`. It delegates only the user acceptance decision to
`UiPrompts`.

The Swing boundary preserves the existing modal license pane and the rejection
confirmation message:

```text
You must accept the license agreement in order to use Alice.

Would you like to return to the license agreement?
```

The product name changes with the requested license, for example
`The Sims (TM) 2 Art Assets`.

## Approved direct dialog owners

Production direct `JOptionPane`, `JDialog`, and `FindResourcesPanel` ownership is
limited to explicit UI code:

| Owner | Allowed reason |
| --- | --- |
| `org.lgna.story.resourceutilities.SwingUiPromptBoundary` | The adapter that renders prompt-boundary requests as Swing UI. |
| `org.alice.stageide.EntryPoint` | Desktop launcher and startup UI boundary. |
| `org.alice.stageide.StageIDE` | Desktop IDE owner that installs or relies on the installed Swing prompt boundary before prompt-producing flows run. |
| `org.alice.nonfree.IdeNonfree` | Desktop nonfree integration owner for Sims EULA and resource startup flows. |
| Documented UI dialog wrappers | Existing UI-package helpers whose public purpose is to show dialogs, such as desktop `Dialogs` helpers or Swing dialog builders. They are allowed only when called from UI-owned code, not as a bypass from reusable resource, library, or utility modules. |

Reusable resource, library, and utility code does not directly open dialogs.
This includes `StorytellingResources`, `NebulousStorytellingResources`,
`edu.cmu.cs.dennisc.nebulous.Manager`, and `EULAUtilities`.

## Reusable code rules

Reusable code follows these rules:

1. Build immutable request data.
2. Call `UiPrompts`.
3. Treat a no-selection resource result as "the user did not provide a
   directory."
4. Treat EULA rejection as rejection, never implicit acceptance.
5. Keep resource directory validation and preference persistence outside the UI
   adapter.
6. Do not catch and hide prompt failures as success.
7. Do not import Swing dialog classes unless the class is an approved UI owner.

## Characterization coverage

The boundary is protected by characterization tests at the module that owns each
behavior:

| Test | Contract |
| --- | --- |
| `core/util/src/test/java/edu/cmu/cs/dennisc/ui/prompt/UiPromptsTest.java` | Installed boundary delegation, previous-boundary restoration, null rejection, and reset to non-interactive default. |
| `core/util/src/test/java/edu/cmu/cs/dennisc/ui/prompt/NonInteractiveUiPromptBoundaryTest.java` | Default resource requests return no selection, EULA requests reject, and messages do not require UI. |
| `core/story-api/src/test/java/org/lgna/story/resourceutilities/StorytellingResourcesPromptBoundaryTest.java` | Missing Alice gallery behavior emits resource/message prompt requests instead of opening Swing dialogs. |
| `core-nonfree/story-api-nonfree/src/test/java/org/lgna/story/resourceutilities/NebulousStorytellingResourcesPromptBoundaryTest.java` | Missing Sims art asset behavior emits resource/message prompt requests without requiring UI. |
| `core-nonfree/story-api-nonfree/src/test/java/edu/cmu/cs/dennisc/nebulous/ManagerPromptBoundaryTest.java` | License rejection and initialization failures report the same messages through the boundary. |
| `core/util/src/test/java/edu/cmu/cs/dennisc/eula/EULAUtilitiesPromptBoundaryTest.java` | EULA acceptance, rejection, persistence, and exception behavior match the legacy flow without displaying Swing UI. |

See [UI prompt boundary API reference](../reference/ui-prompt-boundary-api.md)
for class details and [using the UI prompt boundary](../howto/use-ui-prompt-boundary.md)
for migration examples.
