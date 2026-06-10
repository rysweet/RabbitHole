# Clipboard Operation Registry Reference

> Status: **Feature contract** · Applies to: `core/clipboard-dnd` · Last reviewed: 2026-06-10

`ClipboardOperationRegistry` is the scoped memoization API for Alice clipboard
and drag-and-drop operations.

## Contents

- [Package](#package)
- [`ClipboardOperationRegistry`](#clipboardoperationregistry)
- [`ClipboardOperationRegistries`](#clipboardoperationregistries)
- [Static operation facades](#static-operation-facades)
- [Behavior preservation](#behavior-preservation)
- [Configuration](#configuration)
- [Thread-safety](#thread-safety)
- [Validation commands](#validation-commands)
- [Safety rules](#safety-rules)

## Package

```java
package org.alice.ide.clipboard;
```

The API lives in `core/clipboard-dnd` with the operation classes it scopes:

- `CopyToClipboardOperation`
- `CutToClipboardOperation`
- `PasteFromClipboardOperation`
- `CopyFromClipboardOperation`

## `ClipboardOperationRegistry`

`ClipboardOperationRegistry` owns one set of operation memoization maps for one
application, project, test, or fallback scope.

```java
public final class ClipboardOperationRegistry {
  public ClipboardOperationRegistry()

  public synchronized CopyToClipboardOperation getCopyToClipboardOperation(
      Statement statement)

  public synchronized CutToClipboardOperation getCutToClipboardOperation(
      Statement statement)

  public synchronized PasteFromClipboardOperation getPasteFromClipboardOperation(
      BlockStatementIndexPair blockStatementIndexPair)

  public synchronized CopyFromClipboardOperation getCopyFromClipboardOperation(
      BlockStatementIndexPair blockStatementIndexPair)
}
```

### `getCopyToClipboardOperation`

```java
public synchronized CopyToClipboardOperation getCopyToClipboardOperation(
    Statement statement)
```

Returns the memoized copy-to-clipboard operation for `statement` in this
registry. Repeated calls with the same `Statement` object return the same
operation instance.

### `getCutToClipboardOperation`

```java
public synchronized CutToClipboardOperation getCutToClipboardOperation(
    Statement statement)
```

Returns the memoized cut-to-clipboard operation for `statement` in this registry.
Repeated calls with the same `Statement` object return the same operation
instance.

### `getPasteFromClipboardOperation`

```java
public synchronized PasteFromClipboardOperation getPasteFromClipboardOperation(
    BlockStatementIndexPair blockStatementIndexPair)
```

Returns the memoized paste operation for an insertion site. Keys use
`BlockStatementIndexPair.equals(...)` and `hashCode()`, so equivalent insertion
site objects resolve to the same operation in one registry.

### `getCopyFromClipboardOperation`

```java
public synchronized CopyFromClipboardOperation getCopyFromClipboardOperation(
    BlockStatementIndexPair blockStatementIndexPair)
```

Returns the memoized copy-from-clipboard operation for an insertion site. It uses
the same key semantics as paste, but it is stored in a separate operation map.

### Null handling

Registry lookup arguments are required. Passing `null` to any lookup method is a
programming error. Implementations must validate lookup arguments before
creating or caching an operation, so invalid calls fail at the lookup boundary.

## `ClipboardOperationRegistries`

`ClipboardOperationRegistries` resolves the active scoped registry and provides a
test override seam.

```java
public final class ClipboardOperationRegistries {
  public static ClipboardOperationRegistry getActiveRegistry()

  public static RegistryScope useRegistry(ClipboardOperationRegistry registry)

  public interface RegistryScope extends AutoCloseable {
    @Override
    void close()
  }
}
```

### `getActiveRegistry`

```java
public static ClipboardOperationRegistry getActiveRegistry()
```

Returns the registry for the current context. Resolution order is:

1. Current thread override.
2. Active project registry.
3. Active application registry.
4. Private fallback registry.

The fallback keeps legacy static callers working when no active project or
application can be resolved. It is not a general-purpose global cache for new
code.

### `useRegistry`

```java
public static RegistryScope useRegistry(ClipboardOperationRegistry registry)
```

Installs `registry` as the active registry for the current thread and returns a
scope object that restores the previous override when closed.

Use it in tests and narrow integration seams:

```java
ClipboardOperationRegistry registry = new ClipboardOperationRegistry();

try (ClipboardOperationRegistries.RegistryScope scope =
    ClipboardOperationRegistries.useRegistry(registry)) {
  CopyToClipboardOperation operation =
      CopyToClipboardOperation.getInstance(statement);
}
```

Passing `null` is invalid and fails immediately. Close each returned scope once,
preferably with try-with-resources. Repeated `close()` calls are not part of the
public contract, and callers must not depend on them for cleanup behavior.

## Static operation facades

The existing static `getInstance(...)` methods remain source-compatible:

```java
public static CopyToClipboardOperation getInstance(Statement statement)
public static CutToClipboardOperation getInstance(Statement statement)
public static PasteFromClipboardOperation getInstance(
    BlockStatementIndexPair blockStatementIndexPair)
public static CopyFromClipboardOperation getInstance(
    BlockStatementIndexPair blockStatementIndexPair)
```

Each facade delegates to the active registry:

```java
return ClipboardOperationRegistries
    .getActiveRegistry()
    .getCopyToClipboardOperation(statement);
```

The operation classes do not contain static mutable maps. Scoped registry state
is the source of truth.

## Behavior preservation

The registry feature preserves the public operation behavior that existing Alice
callers rely on:

| Surface | Compatibility requirement |
| --- | --- |
| Operation UUIDs | Keep existing operation IDs for copy, cut, paste, and copy-from-clipboard. |
| Croquet groups | Keep the same group ownership and command wiring. |
| Action enablement | Preserve existing enablement and availability decisions. |
| `perform(...)` methods | Preserve copy, cut, paste, and drag-and-drop edit semantics. |
| Static facades | Keep source-compatible `getInstance(...)` entry points. |

Only memoization ownership changes. Operation instances are owned by scoped
registries instead of process-global static maps.

## Configuration

There is no runtime preference, command-line flag, system property, or
environment variable for clipboard operation registry behavior.

Scope is configured by runtime context:

| Context | Registry owner | Use |
| --- | --- | --- |
| Active project | Project-scoped registry | Normal Alice project editing and drag-and-drop. |
| Active application without project | Application-scoped registry | Startup, shutdown, and pre-project UI paths. |
| Thread-local override | Caller-supplied registry | Tests and narrow integration seams. |
| No active context | Private fallback registry | Legacy compatibility only. |

Local automation should keep the repository memory preference when running Maven
or Node-backed tooling:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

`NODE_OPTIONS` does not change registry behavior.

## Thread-safety

`ClipboardOperationRegistry` lookup methods are synchronized. A single registry
can be used by multiple UI or test threads without creating duplicate operation
instances for the same key.

`ClipboardOperationRegistries` protects shared project and application registry
lookups. Project and application keys are weakly referenced so registry lookup
does not keep retired contexts alive. `ClipboardOperationRegistry` instances must
not retain their project or application owner directly.

The thread-local override affects only the current thread. It must be closed with
try-with-resources or an equivalent `finally` block.

## Validation commands

Focused clipboard registry validation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/clipboard-dnd -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=ClipboardOperationRegistryTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Broader affected-module validation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/clipboard-dnd -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  test
```

## Safety rules

- Keep operation memoization inside `ClipboardOperationRegistry` instances.
- Do not add static mutable operation maps to operation classes.
- Resolve project scope before application scope.
- Use weak project and application keys for scoped registry lookup.
- Do not store the project or application owner inside `ClipboardOperationRegistry`.
- Keep the legacy fallback private and compatibility-only.
- Use `ClipboardOperationRegistries.useRegistry(...)` with try-with-resources in
  tests.
- Validate that registry lookup methods reject `null` arguments before caching.
- Treat `RegistryScope.close()` as a one-time lifecycle operation.
- Preserve operation UUIDs, Croquet groups, action enablement, and `perform(...)`
  behavior while moving memoization ownership.
- Do not refactor singleton menu commands such as `CopyOperation`,
  `CutOperation`, or `PasteOperation` as part of clipboard operation registry
  changes.
- Do not log clipboard contents, serialized AST, project object graphs, or copied
  statement details from registry code.

See [Scoped Clipboard Operation Registries](../concepts/scoped-clipboard-operation-registries.md)
for the architectural contract and
[Use a scoped clipboard operation registry](../howto/use-scoped-clipboard-operation-registry.md)
for examples.
