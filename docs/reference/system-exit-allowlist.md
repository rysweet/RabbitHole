---
title: System.exit Allowlist
description: Target exact production call sites approved to terminate the JVM directly in RabbitHole.
last_updated: 2026-06-10
review_schedule: quarterly
owner: modernization
doc_type: reference
---

# System.exit allowlist

Direct JVM termination belongs only at process-boundary call sites. Reusable
RabbitHole and Alice code request termination through `ProcessTerminator`.

This reference defines the target exact call-site allowlist for the System.exit
boundary feature. The checked-in `SystemExitBoundaryTest` currently enforces
approved files and detects `System.exit(...)` text only. The feature is complete
only after that test is upgraded to enforce the exact call-site table below.

## Contents

- [Current enforcement](#current-enforcement)
- [Scope](#scope)
- [Approved call sites](#approved-call-sites)
- [Target scanner behavior](#target-scanner-behavior)
- [Changing the allowlist](#changing-the-allowlist)
- [Related documentation](#related-documentation)

## Current enforcement

`SystemExitBoundaryTest` scans production Java sources under `src/main/java`.
Today it detects files containing `System.exit(` and compares those files to
`APPROVED_SYSTEM_EXIT_FILES`.

That file-level test catches direct exits in unapproved production files and
stale approved files that no longer contain `System.exit(`. It does not yet
enforce exact call sites, method references, `Runtime.exit`, or `Runtime.halt`.

## Scope

The target exact scanner for this feature must detect:

| Termination form | Allowlist requirement |
| --- | --- |
| `System.exit(status)` | Exact call site required. |
| `System::exit` | Exact method-reference site required. |
| `Runtime.getRuntime().exit(status)` | Exact call site required. |
| `Runtime.getRuntime().halt(status)` | Exact call site required. |

Target approval is not file-level. Each approved entry includes the
repository-relative path, enclosing context, and normalized termination
expression. A new direct termination call in an approved file must fail the
boundary test until that specific call site is added.

## Approved call sites

| Path | Enclosing context | Normalized termination expression | Reason |
| --- | --- | --- | --- |
| `alice-ide/src/main/java/org/alice/stageide/EntryPoint.java` | `EntryPoint.main(String[] args)` handler installation | `ProcessTerminator.setHandler(System::exit)` | The Alice desktop entry point installs the production handler that converts reusable termination requests into JVM termination. |
| `alice-ide/src/main/java/org/alice/stageide/EntryPoint.java` | `EntryPoint.main(String[] args)` catch for `ProcessTerminationRequestedException` | `System.exit(request.getStatus())` | The Alice desktop entry point preserves the requested status if the installed handler returns instead of terminating. |
| `core/ide/src/main/java/org/alice/tools/EatmeSaveProject.java` | `EatmeSaveProject.main(String[] args)` non-zero status branch | `System.exit(status)` | The project-save command-line tool returns success normally and exits the process for existing non-zero statuses emitted by `run(...)`. |
| `core/ide/src/main/java/org/alice/tools/EatmePlaceObject.java` | `EatmePlaceObject.main(String[] args)` non-zero status branch | `System.exit(status)` | The place-object command-line tool returns success normally and exits the process for existing non-zero statuses emitted by `run(...)`. |
| `core/ide/src/main/java/org/alice/tools/EatmeEditProcedure.java` | `EatmeEditProcedure.main(String[] args)` non-zero status branch | `System.exit(status)` | The edit-procedure command-line tool returns success normally and exits the process for existing non-zero statuses emitted by `run(...)`. |
| `core/ide/src/main/java/org/alice/tools/EatmeReopenProject.java` | `EatmeReopenProject.main(String[] args)` non-zero status branch | `System.exit(status)` | The reopen-project command-line tool returns success normally and exits the process for existing non-zero statuses emitted by `run(...)`. |
| `core/ide/src/main/java/org/alice/tools/EatmeRunWorld.java` | `EatmeRunWorld.main(String[] args)` non-zero status branch | `System.exit(status)` | The run-world command-line tool returns success normally and exits the process for existing non-zero statuses emitted by `run(...)`. |

## Target scanner behavior

The exact scanner must fail closed:

1. An unapproved direct termination call fails the test.
2. A stale allowlist entry whose source call site no longer exists fails the
   test.
3. A production-source traversal or file-read error fails the test.

Diagnostics must use repository-relative paths, enclosing context, and normalized
call text so the failing call site can be reviewed without exposing local
machine paths.

## Changing the allowlist

Add an allowlist entry only when the code is a true process boundary:

1. Keep reusable behavior in a method that returns status or calls
   `ProcessTerminator.requestExit(status)`.
2. Put direct JVM termination in the entry-point `main` method or equivalent
   launcher boundary.
3. Add the exact call site to `SystemExitBoundaryTest` after the scanner is
   upgraded, or add the file to the current file-level list until that upgrade
   lands.
4. Add the same exact call site to this reference.
5. Preserve existing user-visible status codes and messages.

Do not approve direct exits in reusable IDE code, Croquet code, dialogs,
composites, exception handlers, utility classes, or tests that run in the Maven
test JVM.

## Related documentation

- [Process termination boundary](../concepts/process-termination-boundary.md)
- [Process termination API reference](./process-termination-api.md)
- [Request process termination safely](../howto/request-process-termination.md)
