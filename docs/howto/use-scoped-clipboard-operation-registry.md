# Use a Scoped Clipboard Operation Registry

> Status: **Feature contract** · Applies to: `core/clipboard-dnd` · Last reviewed: 2026-06-10

Use `ClipboardOperationRegistry` when clipboard or drag-and-drop operation
memoization must be isolated to one Alice project, application, or test.

## Contents

- [Before you start](#before-you-start)
- [Keep legacy operation calls working](#keep-legacy-operation-calls-working)
- [Use an explicit registry in new code](#use-an-explicit-registry-in-new-code)
- [Isolate a test with `useRegistry`](#isolate-a-test-with-useregistry)
- [Verify paste and drop-site key behavior](#verify-paste-and-drop-site-key-behavior)
- [Avoid global operation caches](#avoid-global-operation-caches)
- [Validate registry behavior](#validate-registry-behavior)

## Before you start

Read the API contract in
[Clipboard operation registry reference](../reference/clipboard-operation-registry.md).

The registry scopes these operation classes:

- `CopyToClipboardOperation`
- `CutToClipboardOperation`
- `PasteFromClipboardOperation`
- `CopyFromClipboardOperation`

It does not replace the singleton menu commands `CopyOperation`, `CutOperation`,
or `PasteOperation`.

## Keep legacy operation calls working

Existing UI code can continue to call the static operation facades:

```java
CopyToClipboardOperation copy =
    CopyToClipboardOperation.getInstance(statement);
CutToClipboardOperation cut =
    CutToClipboardOperation.getInstance(statement);
PasteFromClipboardOperation paste =
    PasteFromClipboardOperation.getInstance(blockStatementIndexPair);
CopyFromClipboardOperation copyFromClipboard =
    CopyFromClipboardOperation.getInstance(blockStatementIndexPair);
```

These calls use the active scoped registry. They are safe for existing callers
because the operation class still owns the public static entry point, but the
operation class no longer owns process-global mutable maps.

## Use an explicit registry in new code

When new code already has a registry, ask it for operations directly:

```java
public final class ClipboardActionFactory {
  private final ClipboardOperationRegistry registry;

  public ClipboardActionFactory(ClipboardOperationRegistry registry) {
    this.registry = java.util.Objects.requireNonNull(registry);
  }

  public CopyToClipboardOperation copyOperationFor(Statement statement) {
    return registry.getCopyToClipboardOperation(statement);
  }

  public PasteFromClipboardOperation pasteOperationFor(
      BlockStatementIndexPair blockStatementIndexPair) {
    return registry.getPasteFromClipboardOperation(blockStatementIndexPair);
  }
}
```

This makes the scope visible to the caller and avoids adding another static
access path.

Resolve the active registry only at a boundary that already depends on ambient
Alice UI context, then pass it inward:

```java
ClipboardActionFactory factory =
    new ClipboardActionFactory(ClipboardOperationRegistries.getActiveRegistry());
```

Avoid calling `ClipboardOperationRegistries.getActiveRegistry()` from lower-level
logic when a registry can be passed as a parameter or constructor dependency.

## Isolate a test with `useRegistry`

Tests that need deterministic registry state install a fresh registry for the
current thread:

```java
@Test
public void copyOperationIsMemoizedOnlyInsideOneRegistry() {
  ClipboardOperationRegistry registry = new ClipboardOperationRegistry();

  try (ClipboardOperationRegistries.RegistryScope scope =
      ClipboardOperationRegistries.useRegistry(registry)) {
    CopyToClipboardOperation first =
        CopyToClipboardOperation.getInstance(statement);
    CopyToClipboardOperation second =
        CopyToClipboardOperation.getInstance(statement);

    assertSame(first, second);
  }
}
```

Use try-with-resources for every override. The scope restores the previous
registry when it closes, so a failing test does not leak registry state into the
next test.

To prove two scopes do not share cached operations, use two registry instances:

```java
ClipboardOperationRegistry firstRegistry = new ClipboardOperationRegistry();
ClipboardOperationRegistry secondRegistry = new ClipboardOperationRegistry();

CopyToClipboardOperation first =
    firstRegistry.getCopyToClipboardOperation(statement);
CopyToClipboardOperation second =
    secondRegistry.getCopyToClipboardOperation(statement);

assertNotSame(first, second);
```

## Verify paste and drop-site key behavior

Paste and copy-from-clipboard operations use `BlockStatementIndexPair` keys. The
registry preserves equality-based lookup for equivalent insertion-site keys:

```java
PasteFromClipboardOperation first =
    registry.getPasteFromClipboardOperation(originalSite);
PasteFromClipboardOperation equivalent =
    registry.getPasteFromClipboardOperation(equivalentSite);

assertEquals(originalSite, equivalentSite);
assertSame(first, equivalent);
```

The same insertion site does not merge different operation types:

```java
PasteFromClipboardOperation paste =
    registry.getPasteFromClipboardOperation(blockStatementIndexPair);
CopyFromClipboardOperation copyFromClipboard =
    registry.getCopyFromClipboardOperation(blockStatementIndexPair);

assertNotSame(paste, copyFromClipboard);
```

## Avoid global operation caches

Do not add static maps or long-lived fields that store clipboard operation
instances:

```java
// Do not do this.
private static final Map<Statement, CopyToClipboardOperation> COPY_OPERATIONS =
    new HashMap<>();
```

Use one of these instead:

```java
CopyToClipboardOperation operation =
    registry.getCopyToClipboardOperation(statement);
```

or, at a UI boundary that must bridge from ambient context:

```java
ClipboardOperationRegistry registry =
    ClipboardOperationRegistries.getActiveRegistry();
CopyToClipboardOperation operation =
    registry.getCopyToClipboardOperation(statement);
```

In legacy code that already depends on the static facade, keep the facade instead
of adding a new static cache:

```java
CopyToClipboardOperation operation =
    CopyToClipboardOperation.getInstance(statement);
```

## Validate registry behavior

Run the focused registry tests after changing clipboard or drag-and-drop
operation memoization:

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

See [Scoped Clipboard Operation Registries](../concepts/scoped-clipboard-operation-registries.md)
for the design rationale.
