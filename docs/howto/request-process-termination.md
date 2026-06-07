# Request process termination safely

Use `ProcessTerminator` when Alice code needs the process to end but is not
itself a process entry point.

This how-to describes the intended migration target for the
process-termination feature. If a referenced class still calls `System.exit`
directly, that call is work to migrate, not an allowed final state.

## Contents

- [Before you start](#before-you-start)
- [Request exit from reusable code](#request-exit-from-reusable-code)
- [Convert a legacy direct exit](#convert-a-legacy-direct-exit)
- [Migrate current reusable exits](#migrate-current-reusable-exits)
- [Handle termination inside exception handlers](#handle-termination-inside-exception-handlers)
- [Install the entry-point handler](#install-the-entry-point-handler)
- [Add characterization for a new termination path](#add-characterization-for-a-new-termination-path)
- [Validate the boundary](#validate-the-boundary)

## Before you start

Read the boundary rule in
[Process termination boundary](../concepts/process-termination-boundary.md).
Only explicit launchers may call `System.exit` directly.

## Request exit from reusable code

Call `ProcessTerminator.requestExit(status)` instead of `System.exit(status)`.

```java
import org.lgna.croquet.ProcessTerminator;

public final class ProjectCloseOperation {
  public void handleUserConfirmedExit() {
    ProcessTerminator.requestExit(0);
  }
}
```

The call either reaches the entry-point handler or raises
`ProcessTerminationRequestedException` with the same status. Do not catch that
exception unless this code is a known process boundary or it is handling a
termination request it just made.

## Convert a legacy direct exit

Replace direct JVM termination in reusable code:

```java
// Before
System.exit(-1);
```

```java
// After
ProcessTerminator.requestExit(-1);
```

Keep the surrounding user-visible behavior unchanged. If the old path showed a
dialog or logged an error before exiting, keep that dialog or log message before
the `requestExit` call.

## Migrate current reusable exits

Start by finding direct exits:

```bash
rg 'System\.exit\(' --glob '*.java'
```

Only launcher files in the boundary allowlist may keep direct `System.exit`
calls. Convert reusable call sites such as:

| Class or area | Migration rule |
| --- | --- |
| `org.alice.stageide.StageIDE` | Keep the same startup failure message/status, then call `ProcessTerminator.requestExit(-1)`. |
| `org.alice.ide.croquet.models.projecturi.SystemExitOperation` | Keep the project operation behavior, but request exit instead of terminating the JVM from the operation. |
| `org.lgna.croquet.simple.SimpleApplication` | Route application close termination through `ProcessTerminator` unless an explicit launcher owns the call. |
| Dialogs including `org.lgna.croquet.views.Dialog`, `org.alice.stageide.type.croquet.OtherTypeDialog`, `org.alice.ide.upgrade.ProjectAheadDialog`, `org.alice.ide.ast.type.croquet.ImportTypeWizard`, and custom-expression dialogs | Preserve dialog behavior and request termination after the existing user-visible action. |
| `org.alice.ide.issue.DefaultExceptionHandler` and `org.alice.ide.issue.IdeUncaughtExceptionHandler` | Keep existing logging and dialogs, then request failure termination without re-reporting `ProcessTerminationRequestedException`. |
| Utility and optional UI code such as `edu.cmu.cs.dennisc.eula.swing.JEulaPane` and `org.alice.stageide.personresource.PersonResourceComposite` | Keep existing UI behavior and request termination through the shared API. |

## Handle termination inside exception handlers

Exception handlers preserve the existing dialogs and logging, then request
termination. They catch only the termination request raised by that call.

```java
JOptionPane.showMessageDialog(
    null,
    "Exception occurred before application was able to show window.  Exiting.");

try {
  ProcessTerminator.requestExit(-1);
} catch (ProcessTerminationRequestedException request) {
  if (request.getStatus() != -1) {
    throw request;
  }
}
```

The catch is narrow: it protects the handler from reporting its own termination
request as a second crash. It must not hide unrelated exceptions from the dialog,
logging, or bug-report paths.

## Install the entry-point handler

The desktop launcher owns JVM termination. It installs the production handler
before starting reusable Alice code and catches the fallback exception at the
launcher boundary.

```java
public static void main(String[] args) {
  ProcessTerminator.Handler previous =
      ProcessTerminator.setHandler(status -> System.exit(status));
  try {
    launchAliceDesktop(args);
  } catch (ProcessTerminationRequestedException request) {
    System.exit(request.getStatus());
  } finally {
    ProcessTerminator.setHandler(previous);
  }
}
```

The restore step matters because tests and embedded launch scenarios run in a
shared JVM. The handler is process-wide and must be implemented with
cross-thread visibility because requests can come from launcher, UI, and
exception-handler threads.

## Add characterization for a new termination path

When migrating a path from `System.exit` to `ProcessTerminator`, add or update a
test that records the requested status:

```java
@Test
public void requestsFailureExitWithoutTerminatingTestJvm() {
  AtomicInteger requestedStatus = new AtomicInteger(Integer.MIN_VALUE);
  ProcessTerminator.Handler previous =
      ProcessTerminator.setHandler(requestedStatus::set);
  try {
    ProcessTerminationRequestedException request =
        assertThrows(
            ProcessTerminationRequestedException.class,
            () -> operationThatRequestsExit());

    assertEquals(-1, request.getStatus());
    assertEquals(-1, requestedStatus.get());
  } finally {
    ProcessTerminator.setHandler(previous);
  }
}
```

This proves both halves of the contract: reusable code requested the correct
status, and the test JVM did not exit.

## Validate the boundary

Run the focused tests for the modules that own the behavior:

```bash
mvn -pl core/croquet -Dtest=ProcessTerminatorTest test
mvn -pl core/ide -Dtest=SystemExitBoundaryTest test
mvn -pl core/ide -Dtest=DefaultExceptionHandlerTest,IdeUncaughtExceptionHandlerTest test
```

Before broad Maven validation, initialize the Tweedle grammar submodule:

```bash
git submodule update --init tweedle-lang
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true test
```

See [Process termination API reference](../reference/process-termination-api.md)
for method details and allowlist policy.
