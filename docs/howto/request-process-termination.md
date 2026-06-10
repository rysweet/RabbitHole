---
title: Request Process Termination Safely
description: How to request RabbitHole process termination from reusable code without calling System.exit directly.
last_updated: 2026-06-10
review_schedule: quarterly
owner: modernization
doc_type: howto
---

# Request process termination safely

Use `ProcessTerminator` when Alice code needs the process to end but is not
itself a process entry point.

## Contents

- [Before you start](#before-you-start)
- [Request exit from reusable code](#request-exit-from-reusable-code)
- [Convert a legacy direct exit](#convert-a-legacy-direct-exit)
- [Handle termination inside exception handlers](#handle-termination-inside-exception-handlers)
- [Install the entry-point handler](#install-the-entry-point-handler)
- [Add a command-line entry point](#add-a-command-line-entry-point)
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

public final class SystemExitOperation extends ActionOperation {
  @Override
  protected void perform(UserActivity activity) {
    activity.finish();
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

## Audit direct exits

Find direct process termination before changing launcher or reusable-code paths:

```bash
rg 'System\.exit|System::exit|Runtime\.getRuntime\(\)\.(exit|halt)' \
  --glob '**/src/main/java/**/*.java'
```

Every result must either be a true process boundary or be converted to
`ProcessTerminator.requestExit(status)`. The checked-in boundary test currently
uses a file-level allowlist for `System.exit(...)`; the
[System.exit allowlist](../reference/system-exit-allowlist.md) records the
target exact call sites that the scanner must enforce when the feature lands.

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
  // The exception handler must not report intentional termination as another crash.
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
public static void main(final String[] args) {
  ProcessTerminator.Handler previous = ProcessTerminator.setHandler(System::exit);
  try {
    requireGraphicalEnvironmentForDesktopLaunch(GraphicsEnvironment.isHeadless());
    // EntryPoint initializes Alice desktop services here.
    launch(args);
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

## Add a command-line entry point

Command-line tools may convert a final tool status into a process status in
their `main` method. Keep reusable tool logic in a `run` method that returns an
integer status so tests can characterize behavior without exiting the JVM.

```java
public static void main(String[] args) {
  int status = run(args, System.out, System.err);
  if (status != 0) {
    System.exit(status);
  }
}

static int run(String[] args, PrintStream out, PrintStream err) {
  PrintStream originalSystemOut = System.out;
  PrintStream silentSystemOut = new PrintStream(OutputStream.nullOutputStream());
  System.setOut(silentSystemOut);
  try {
    Arguments arguments = Arguments.parse(args);
    ProjectSave save = saveProject(arguments);
    out.println(resultJson(save));
    return 0;
  } catch (IllegalArgumentException | IOException | VersionNotSupportedException ex) {
    err.println(ex.getMessage());
    return 2;
  } catch (RuntimeException ex) {
    err.println("project save failed: " + ex.getMessage());
    return 3;
  } finally {
    System.setOut(originalSystemOut);
    silentSystemOut.close();
  }
}
```

When the tool is a real process boundary, add the exact `System.exit(status)`
call site to [System.exit allowlist reference](../reference/system-exit-allowlist.md).
Until the exact scanner is implemented, also add the tool file to
`APPROVED_SYSTEM_EXIT_FILES` in `SystemExitBoundaryTest`.

## Add characterization for a new termination path

When migrating a path from `System.exit` to `ProcessTerminator`, add or update a
test that records the requested status:

```java
@Test
public void requestExitInvokesInstalledHandlerWithRequestedStatus() {
  AtomicInteger requestedStatus = new AtomicInteger(Integer.MIN_VALUE);
  ProcessTerminator.Handler previous =
      ProcessTerminator.setHandler(requestedStatus::set);
  try {
    ProcessTerminationRequestedException request =
        assertThrows(
            ProcessTerminationRequestedException.class,
            () -> ProcessTerminator.requestExit(42));

    assertEquals(42, requestedStatus.get());
    assertEquals(42, request.getStatus());
  } finally {
    ProcessTerminator.setHandler(previous);
  }
}
```

This proves both halves of the contract: the request preserves the correct
status, and the test JVM does not exit.

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
for method details and
[System.exit allowlist reference](../reference/system-exit-allowlist.md) for
approved direct termination sites.
