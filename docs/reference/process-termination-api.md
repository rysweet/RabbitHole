# Process termination API reference

`ProcessTerminator` is the shared API for requesting process termination without
allowing reusable code to call `System.exit` directly.

This reference describes the intended API contract for the
process-termination feature. Until the implementation lands, matching classes,
tests, and migration call sites may not exist in every branch.

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
    ProcessTerminator.setHandler(status -> System.exit(status));
try {
  runApplication(args);
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
public final class ProcessTerminationRequestedException extends RuntimeException {
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

Handler storage is process-wide and must be safe for calls from the desktop
launcher, Swing event dispatch thread, JavaFX thread, and uncaught-exception
handler threads.

The implementation must make handler updates visible across threads. Use a
thread-safe holder such as `AtomicReference<ProcessTerminator.Handler>` or an
equivalent visibility guarantee; do not store the handler in an unsynchronized
plain static field.

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
| Tool-specific non-zero status | Headless tool argument or runtime failure, as documented by that tool. |

`ProcessTerminator` preserves the exact status supplied by the caller. It does
not normalize, remap, or swallow statuses.

## Allowlist contract

`SystemExitBoundaryTest` is the code-level authorization boundary for direct JVM
termination. It scans repository production Java sources under `src/main/java`
across every module, including `alice-ide`, `core`, `core/ide`, `core/util`,
`core-nonfree`, and `netbeans` production roots. Test sources are outside the
production allowlist scan, but tests must not terminate the Maven test JVM.

When a new production launcher truly needs to call `System.exit`, update the
explicit allowlist in that test in the same change that introduces the launcher.

Do not add `System.exit` to reusable classes, exception handlers, Croquet
operations, dialogs, composites, utilities, or tests that run in the same JVM as
the Maven test process.

See [Process termination boundary](../concepts/process-termination-boundary.md)
for the architectural rule and [requesting process termination](../howto/request-process-termination.md)
for examples.
