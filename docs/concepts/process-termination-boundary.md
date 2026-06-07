# Process termination boundary

Alice treats JVM termination as a top-level launcher responsibility. Reusable
Croquet, IDE, dialog, and exception-handler code request termination intent; they
do not call `System.exit` directly.

This document is the target-state contract for the process-termination feature.
Direct exits that exist today outside the allowlist are migration targets, not
approved exceptions.

## Contents

- [Why the boundary exists](#why-the-boundary-exists)
- [Termination flow](#termination-flow)
- [Approved `System.exit` locations](#approved-systemexit-locations)
- [Reusable exit migration targets](#reusable-exit-migration-targets)
- [Exception-handler behavior](#exception-handler-behavior)
- [Characterization coverage](#characterization-coverage)

## Why the boundary exists

`System.exit` stops the whole JVM immediately. That is correct for a process
launcher that has reached a terminal result, but it is unsafe inside reusable UI
code because it bypasses normal control flow, makes tests hard to isolate, and
can surface termination exceptions as unexpected user-visible crashes.

The process termination boundary keeps those responsibilities separate:

| Layer | Responsibility |
| --- | --- |
| Entry points and launchers | Convert a final exit status into `System.exit(status)`. |
| Reusable application code | Call `ProcessTerminator.requestExit(status)` when it needs the process to end. |
| Exception handlers | Preserve the existing dialog and logging behavior, then consume intentional termination requests instead of reporting them as new crashes. |
| Tests | Enforce the explicit `System.exit` allowlist and characterize the termination control flow. |

## Termination flow

```text
Reusable code
  calls ProcessTerminator.requestExit(status)
    |
    v
ProcessTerminator invokes the installed handler
    |
    v
If the handler exits the JVM, control stops at the entry boundary
    |
    v
If the handler returns, ProcessTerminator throws
ProcessTerminationRequestedException(status)
    |
    v
The entry boundary catches the exception and exits with the same status
```

The fallback exception is intentional control flow. It prevents a termination
request from being silently ignored when no handler is installed or when a test
handler records the request and returns.

## Approved `System.exit` locations

Production `System.exit` calls are limited to explicit process entry points and
headless tool launchers. `SystemExitBoundaryTest` scans repository production
Java sources under `src/main/java` and fails if any other file calls
`System.exit`. The scan covers every production module in this repository,
including `alice-ide`, `core`, `core/ide`, `core/util`, `core-nonfree`, and
`netbeans` production source roots.

The allowlist contains:

| File | Reason |
| --- | --- |
| `alice-ide/src/main/java/org/alice/stageide/EntryPoint.java` | Desktop Alice launcher and JavaFX/Swing process boundary. |
| `core/ide/src/main/java/org/alice/tools/EatmeSaveProject.java` | Headless project-save tool launcher. |
| `core/ide/src/main/java/org/alice/tools/EatmePlaceObject.java` | Headless place-object tool launcher. |
| `core/ide/src/main/java/org/alice/tools/EatmeEditProcedure.java` | Headless edit-procedure tool launcher. |
| `core/ide/src/main/java/org/alice/tools/EatmeReopenProject.java` | Headless reopen-project tool launcher. |
| `core/ide/src/main/java/org/alice/tools/EatmeRunWorld.java` | Headless run-world tool launcher. |

Main methods used as demos, dialogs, components, or diagnostics are not process
boundaries. They must return normally, throw a useful exception, or request exit
through `ProcessTerminator`.

## Reusable exit migration targets

The implementation must migrate reusable direct-exit call sites to
`ProcessTerminator.requestExit(status)`. Representative migration targets
include:

| Current area | Required target behavior |
| --- | --- |
| `org.alice.stageide.StageIDE` | Preserve startup failure behavior and request failure termination instead of exiting from IDE code. |
| `org.alice.ide.croquet.models.projecturi.SystemExitOperation` | Keep the user operation behavior, but request termination through the shared boundary. |
| Croquet and IDE dialogs such as `org.lgna.croquet.views.Dialog`, `org.alice.stageide.type.croquet.OtherTypeDialog`, `org.alice.ide.upgrade.ProjectAheadDialog`, and import/custom-expression dialogs | Keep dialog-visible behavior, then request termination through `ProcessTerminator`. |
| `org.lgna.croquet.simple.SimpleApplication` | Treat application-level close behavior as reusable Croquet code unless called from an explicit launcher. |
| `org.alice.ide.issue.DefaultExceptionHandler` and `org.alice.ide.issue.IdeUncaughtExceptionHandler` | Preserve logging and dialogs, then request termination without reporting the request as another crash. |
| Utility and nonfree UI code such as `edu.cmu.cs.dennisc.eula.swing.JEulaPane` and `org.alice.stageide.personresource.PersonResourceComposite` | Keep user-facing behavior and route termination through the shared boundary. |

## Exception-handler behavior

Alice keeps the existing visible error behavior:

- exceptions are still logged through the existing logging path;
- bug-report and multiple-exception dialogs remain user-visible;
- the pre-window-startup failure message still says
  `Exception occurred before application was able to show window.  Exiting.`;
- the requested status is still `-1` for startup or handler paths that previously
  exited with failure.

The difference is where termination happens. `DefaultExceptionHandler` and
`IdeUncaughtExceptionHandler` request termination through `ProcessTerminator`.
When that request raises `ProcessTerminationRequestedException`, the handler
treats it as intentional termination control flow and does not re-report it as
another uncaught exception.

## Characterization coverage

The boundary is protected by tests at the module that owns each behavior:

| Test | Contract |
| --- | --- |
| `core/croquet/src/test/java/org/lgna/croquet/ProcessTerminatorTest.java` | Default request behavior, handler invocation, fallback exception when a handler returns, status propagation, and handler cleanup. |
| `core/ide/src/test/java/org/alice/ide/SystemExitBoundaryTest.java` | Only allowlisted production entry-point files call `System.exit`. |
| `core/ide/src/test/java/org/alice/ide/issue/DefaultExceptionHandlerTest.java` | Handler-initiated termination requests do not leak as uncaught failures and preserve the existing visible failure message/status. |
| `core/ide/src/test/java/org/alice/ide/issue/IdeUncaughtExceptionHandlerTest.java` | IDE uncaught handler treats `ProcessTerminationRequestedException` as intentional termination control flow. |

See [Process termination API reference](../reference/process-termination-api.md)
for the class contract and [requesting process termination](../howto/request-process-termination.md)
for migration examples.
