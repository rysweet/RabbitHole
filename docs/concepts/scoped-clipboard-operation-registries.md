# Scoped Clipboard Operation Registries

> Status: **Feature contract** · Applies to: `core/clipboard-dnd` · Last reviewed: 2026-06-10

Clipboard and drag-and-drop operation instances are memoized by a scoped
`ClipboardOperationRegistry`, not by static mutable maps on the operation
classes.

## Contents

- [Problem solved](#problem-solved)
- [Registry scope](#registry-scope)
- [Compatibility facade](#compatibility-facade)
- [Memoization contract](#memoization-contract)
- [Behavior preservation](#behavior-preservation)
- [Isolation contract](#isolation-contract)
- [Lifetime and thread-safety](#lifetime-and-thread-safety)
- [Related documentation](#related-documentation)

## Problem solved

Alice creates reusable Croquet operations for code-editor copy, cut, paste, and
clipboard-driven drag-and-drop targets. These operations are keyed by AST
statements or insertion sites so menus and drop targets can ask for the same
operation repeatedly without creating duplicate action objects.

That memoization must not be process-global. A process-global operation cache can
retain AST nodes from previous projects, couple unrelated test cases, and let one
application window observe operation state created for another. The scoped
registry keeps the old memoization behavior inside one active context while
preventing cross-project and cross-test leakage.

## Registry scope

`ClipboardOperationRegistries.getActiveRegistry()` resolves the registry in this
order:

1. The registry installed for the current thread with
   `ClipboardOperationRegistries.useRegistry(...)`.
2. The registry associated with the nearest active Alice project.
3. The registry associated with the active Croquet application.
4. A private legacy fallback registry used only when no project or application
   context exists.

Project scope wins over application scope because clipboard and drag-and-drop
operation keys are AST objects and insertion sites from one project. The
application scope exists for UI paths that have an application context before a
project context is available. The fallback exists only to keep legacy static
callers working in headless or early-startup code.

## Compatibility facade

Existing callers keep using the static entry points:

```java
CopyToClipboardOperation.getInstance(statement);
CutToClipboardOperation.getInstance(statement);
PasteFromClipboardOperation.getInstance(blockStatementIndexPair);
CopyFromClipboardOperation.getInstance(blockStatementIndexPair);
```

Those methods are compatibility facades. They do not own static operation maps.
Each facade delegates to `ClipboardOperationRegistries.getActiveRegistry()` and
then asks that scoped registry for the operation.

New code that already has a registry should call the registry directly. New code
must not add another static operation cache.

## Memoization contract

Inside one `ClipboardOperationRegistry`, operation lookup preserves the Alice 3
baseline behavior:

| Operation | Key | Same key in same registry | Different key in same registry |
| --- | --- | --- | --- |
| `CopyToClipboardOperation` | `Statement` | Same operation instance | Different operation instance |
| `CutToClipboardOperation` | `Statement` | Same operation instance | Different operation instance |
| `PasteFromClipboardOperation` | `BlockStatementIndexPair` | Same operation instance | Different operation instance |
| `CopyFromClipboardOperation` | `BlockStatementIndexPair` | Same operation instance | Different operation instance |

`BlockStatementIndexPair` equality remains part of the compatibility contract.
Two insertion-site key objects that compare equal resolve to the same paste or
copy-from-clipboard operation inside one registry.

The registry does not merge operation types. A paste operation and a copy-from
clipboard operation for the same insertion site are separate operation objects
because they perform different edits.

## Behavior preservation

The registry changes ownership of operation memoization only. Operation UUIDs,
Croquet groups, action enablement, and `perform(...)` behavior remain compatible
with the Alice 3 baseline.

Callers should observe the same copy, cut, paste, and drag-and-drop edits they
observed before this change. The intended behavior change is scope isolation:
operation instances are no longer shared across unrelated projects,
applications, or tests.

## Isolation contract

Separate scopes produce separate operation instances even when the lookup key is
the same object:

```java
ClipboardOperationRegistry first = new ClipboardOperationRegistry();
ClipboardOperationRegistry second = new ClipboardOperationRegistry();

CopyToClipboardOperation firstOperation =
    first.getCopyToClipboardOperation(statement);
CopyToClipboardOperation secondOperation =
    second.getCopyToClipboardOperation(statement);

assert firstOperation != secondOperation;
```

This is required for tests, multiple application windows, project reloads, and
any runtime path that creates operations for more than one active project during
the same JVM lifetime.

Tests that install a registry override must use try-with-resources so the
override is restored even when the test fails:

```java
try (ClipboardOperationRegistries.RegistryScope scope =
    ClipboardOperationRegistries.useRegistry(new ClipboardOperationRegistry())) {
  CopyToClipboardOperation operation =
      CopyToClipboardOperation.getInstance(statement);
}
```

## Lifetime and thread-safety

`ClipboardOperationRegistry` owns instance-level maps. Lookup methods synchronize
on the registry instance so operation memoization remains safe when UI and test
threads request operations concurrently.

`ClipboardOperationRegistries` stores project and application registries with
weak keys. Registry lookup must not keep closed projects, disposed applications,
or their AST graphs alive. A `ClipboardOperationRegistry` must not retain the
project or application owner directly; it only owns operation maps for the
current scope.

The thread-local override is scoped to the calling thread. Closing the returned
`RegistryScope` restores the previous override. Callers should close each scope
exactly once with try-with-resources. Passing `null` to `useRegistry(...)` is
invalid and fails immediately.

## Related documentation

- [Clipboard operation registry reference](../reference/clipboard-operation-registry.md)
- [Use a scoped clipboard operation registry](../howto/use-scoped-clipboard-operation-registry.md)
- [Singleton containment strategy](../architecture/singletons.md)
