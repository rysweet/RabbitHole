---
title: Process Termination API Reference
description: Reference for ProcessTerminator, ProcessTerminationRequestedException, and RabbitHole process-termination configuration.
last_updated: 2026-06-10
review_schedule: quarterly
owner: modernization
doc_type: reference
---

# Process termination API reference

`ProcessTerminator` is the shared API for requesting process termination without
allowing reusable code to call `System.exit` directly.

## Contents

- [Package](#package)
- [`ProcessTerminator`](#processterminator)
- [`ProcessTerminationRequestedException`](#processterminationrequestedexception)
- [Thread-safety](#thread-safety)
- [Runtime configuration](#runtime-configuration)
- [Exit status contract](#exit-status-contract)
- [Allowlist contract](#allowlist-contract)

## Package

```java
package org.lgna.croquet;
```

The API lives in `core/croquet` because Croquet actions, dialogs, IDE handlers,
and the Alice desktop entry point all need the same termination contract.

## `ProcessTerminator`

`ProcessTerminator` is a final utility class with static process-wide handler
state.

### `requestExit`

```java
public static void requestExit(int status)
```

Requests process termination with the supplied exit status.

Behavior:

1. If a handler is installed, the handler receives `status`.
2. If the handler terminates the JVM, control does not return.
3. If the handler returns, `requestExit` throws
   `ProcessTerminationRequestedException` with the same `status`.
4. If no handler is installed, `requestExit` throws
   `ProcessTerminationRequestedException` with the supplied `status`.

This method never silently ignores a termination request.

### Handler contract

```java
@FunctionalInterface
public interface Handler {
  void requestExit(int status);
}
```

The production handler is installed by the entry point and delegates to
`System.exit(status)`.

Test handlers commonly record the status and return. Returning is allowed, but it
causes `ProcessTerminator.requestExit(status)` to throw
`ProcessTerminationRequestedException(status)` so the caller or test can observe
the request.

### Installing a handler

```java
public static Handler setHandler(Handler handler)
```

Installs the process-wide handler and returns the previous handler. Pass `null`
to remove the current handler.

Always restore the previous handler in a `finally` block:

```java
ProcessTerminator.Handler previous =
    ProcessTerminator.setHandler(System::exit);
try {
  requireGraphicalEnvironmentForDesktopLaunch(GraphicsEnvironment.isHeadless());
  // EntryPoint initializes Alice desktop services here.
  launch(args);
} catch (ProcessTerminationRequestedException request) {
  System.exit(request.getStatus());
} finally {
  ProcessTerminator.setHandler(previous);
}
```

Tests use the same restore pattern to avoid leaking global handler state into
other tests.

## `ProcessTerminationRequestedException`

```java
public class ProcessTerminationRequestedException extends RuntimeException {
  public ProcessTerminationRequestedException(int status)

  public int getStatus()
}
```

`ProcessTerminationRequestedException` carries the requested exit status when a
termination request is not consumed by a handler.

Use it only as termination control flow at known process boundaries or inside
exception-handler paths that just requested termination. Do not catch it broadly
around unrelated code.

## Thread-safety

Handler storage is process-wide and safe for calls from the desktop launcher,
Swing event dispatch thread, JavaFX thread, and uncaught-exception handler
threads. Handler updates are visible across threads, so a termination request
uses the currently installed handler even when the request is made outside the
launcher thread.

Operational rules:

1. Production code installs the handler during launcher startup before reusable
   Alice code runs.
2. Tests may install a recording handler for one test and must restore the
   previous handler in `finally`.
3. Reusable code calls `requestExit(status)` from any thread that can currently
   call `System.exit(status)`.
4. Production code should not swap handlers during normal application runtime.

## Runtime configuration

There is no environment variable, system property, preferences file, or command
line flag for process termination behavior.

Configuration is process-local Java state:

| Surface | Owner | Behavior |
| --- | --- | --- |
| Production desktop launch | `org.alice.stageide.EntryPoint` | Installs a handler that calls `System.exit(status)` and catches fallback termination exceptions at the launcher boundary. |
| Headless tool launchers | `org.alice.tools.Eatme*` main classes | Return tool-specific status from the tool runner, then call `System.exit(status)` at the launcher. |
| Reusable UI and IDE code | Croquet, IDE, dialog, and exception-handler classes | Calls `ProcessTerminator.requestExit(status)` and never calls `System.exit` directly. |
| Tests | The test that installs a handler | Installs a recording handler and restores the previous handler in `finally`. |

## Exit status contract

| Status | Meaning |
| --- | --- |
| `0` | Normal successful tool or launcher completion. |
| `-1` | Existing Alice desktop startup or exception-handler failure exit. |
| Tool-specific non-zero status | Existing headless tool argument or runtime failure status returned by that tool's `run(...)` method. |

`ProcessTerminator` preserves the exact status supplied by the caller. It does
not normalize, remap, or swallow statuses.

## Allowlist contract

`SystemExitBoundaryTest` is the current file-level code authorization boundary
for direct JVM termination. It scans repository production Java sources under
`src/main/java` across every module, including `alice-ide`, `core`, `core/ide`,
`core/util`, `core-nonfree`, and `netbeans` production roots. Test sources are
outside the production allowlist scan, but tests must not terminate the Maven
test JVM.

The checked-in test currently recognizes `System.exit(...)` by source text and
approves whole files through `APPROVED_SYSTEM_EXIT_FILES`. The feature target is
exact call-site approval: repository-relative source path, enclosing context,
and normalized call text must match the allowlist, and adding another direct
exit to an otherwise-approved file must fail until the new call site is
explicitly approved.

The target scanner must recognize direct process termination through
`System.exit(...)`, `System::exit`, `Runtime.getRuntime().exit(...)`, and
`Runtime.getRuntime().halt(...)`.

When a new production launcher truly needs direct process termination, update
the allowlist in `SystemExitBoundaryTest` and
[System.exit allowlist reference](./system-exit-allowlist.md) in the same
change that introduces the launcher. Until the exact scanner lands, the test
allowlist is file-level and the documentation table records the intended exact
call site.

Do not add `System.exit` to reusable classes, exception handlers, Croquet
operations, dialogs, composites, utilities, or tests that run in the same JVM as
the Maven test process.

See [Process termination boundary](../concepts/process-termination-boundary.md)
for the architectural rule, [System.exit allowlist reference](./system-exit-allowlist.md)
for approved direct termination sites, and
[requesting process termination](../howto/request-process-termination.md) for
examples.
