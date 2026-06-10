---
title: Process Termination Boundary
description: Explains why RabbitHole restricts direct JVM termination to exact launcher and tool call sites.
last_updated: 2026-06-10
review_schedule: quarterly
owner: modernization
doc_type: explanation
---

# Process termination boundary

Alice treats JVM termination as a top-level launcher responsibility. Reusable
Croquet, IDE, dialog, and exception-handler code request termination intent; they
do not call `System.exit` directly.

The boundary keeps the Alice desktop launcher and documented command-line tools
responsible for final process status while keeping reusable RabbitHole code
testable and embeddable.

## Contents

- [Why the boundary exists](#why-the-boundary-exists)
- [Termination flow](#termination-flow)
- [Approved `System.exit` locations](#approved-systemexit-locations)
- [Reusable code behavior](#reusable-code-behavior)
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

Production direct termination calls are limited to exact approved call sites in
the Alice desktop entry point and documented Eatme command-line tools.
`SystemExitBoundaryTest` scans production Java sources under `src/main/java` and
fails on:

1. a discovered direct termination call that is not in the exact allowlist;
2. an allowlist entry whose source call site no longer exists;
3. a traversal or source-read error while scanning production sources.

The scanner covers `System.exit(...)`, `System::exit`,
`Runtime.getRuntime().exit(...)`, and `Runtime.getRuntime().halt(...)`. Approval
is by repository-relative path, enclosing context, and normalized call text, not
by whole file.

See the [System.exit allowlist reference](../reference/system-exit-allowlist.md)
for the complete approved call-site table.

Main methods used as demos, dialogs, components, or diagnostics are not process
boundaries. They must return normally, throw a useful exception, or request exit
through `ProcessTerminator`.

## Reusable code behavior

Reusable code expresses termination intent with
`ProcessTerminator.requestExit(status)`. This includes Croquet operations,
dialogs, composites, IDE services, exception handlers, optional UI code, and
other code that can be called from tests or embedded in a larger process.

| Area | Termination behavior |
| --- | --- |
| Alice desktop launcher | Installs the production handler and owns final JVM termination. |
| Eatme command-line tools | Convert final tool status to process status in `main`. |
| Reusable IDE and Croquet code | Calls `ProcessTerminator.requestExit(status)`. |
| Dialog and operation code | Preserves the existing user-visible action, then requests termination. |
| Exception handlers | Preserve logging/dialog behavior, request termination, and consume the intentional fallback exception from that request. |

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
| `core/ide/src/test/java/org/alice/ide/SystemExitBoundaryTest.java` | Only exact allowlisted production launcher/tool call sites use direct JVM termination. |
| `core/ide/src/test/java/org/alice/ide/issue/DefaultExceptionHandlerTest.java` | Handler-initiated termination requests do not leak as uncaught failures and preserve the existing visible failure message/status. |
| `core/ide/src/test/java/org/alice/ide/issue/IdeUncaughtExceptionHandlerTest.java` | IDE uncaught handler treats `ProcessTerminationRequestedException` as intentional termination control flow. |

See [Process termination API reference](../reference/process-termination-api.md)
for the class contract, [System.exit allowlist reference](../reference/system-exit-allowlist.md)
for approved direct termination sites, and
[requesting process termination](../howto/request-process-termination.md) for
usage examples.
