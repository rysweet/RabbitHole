# Singleton Containment Strategy

> Status: **Active** · Applies to: all modules under `core/`

## Problem

Alice 3 accumulated ~900 singleton references across the production code,
concentrated in `core/ide` (855 call-sites) with smaller counts in
`core/story-api` (23), `core/croquet` (17), and other modules.
Singletons create hidden coupling, make testing difficult, and block modular
extraction.

## Existing Singleton Patterns

The codebase uses three patterns today:

| Pattern | Example | Count |
|---------|---------|-------|
| Eager `static final` field + `getInstance()` | `GlrRenderFactory`, `ProjectMigrationManager` | ~40 classes |
| Enum singleton (`SINGLETON`) | `ConformanceTestResults`, `Clipboard`, `RecycleBin` | ~10 classes |
| Lazy holder via `edu.cmu.cs.dennisc.pattern.Lazy<T>` | Croquet composites, launch factories | ~30 call-sites |

### Module hotspots

| Module | `getInstance()` call-sites |
|--------|---------------------------|
| `core/ide` | 855 |
| `core/story-api` | 23 |
| `core/croquet` | 17 |
| `core/util` | 9 |
| `core/model-loading` | 4 |
| `core/story-api-migration` | 3 |
| `core/glrender` | 3 |

## Containment Rules

### Rule 1 — No new singletons

New code **must not** introduce `static getInstance()`, enum `SINGLETON`, or
static holder patterns. If a shared instance is needed, pass it via constructor
or method parameter.

**Exception: ServiceLoader access points.** A single lazy-holder cache around
`ServiceLoader.load()` is permitted for SPI bridge classes (e.g.,
`IkPoserContexts`, `ClipboardProviders`) because calling `ServiceLoader.load()`
on every access has significant overhead. These must be limited to one holder
per SPI interface and must not expose a general-purpose `getInstance()` method.

### Rule 2 — Constructor injection for new classes

```java
// Good — dependency is explicit and testable
public class SceneEditor {
  private final ProjectManager projectManager;

  public SceneEditor(ProjectManager projectManager) {
    this.projectManager = projectManager;
  }
}

// Bad — hidden coupling, untestable
public class SceneEditor {
  public void doWork() {
    ProjectManager.getInstance().save();
  }
}
```

### Rule 3 — Interface extraction before injection

When refactoring an existing singleton for injection:

1. Extract an interface from the singleton's public API.
2. Make the singleton implement the interface.
3. Change callers to depend on the interface type.
4. Pass the instance via constructor or factory.

```java
// Step 1-2: Extract interface
public interface RenderFactory {
  OnscreenRenderTarget createOnscreen();
}

public class GlrRenderFactory implements RenderFactory {
  // existing singleton stays temporarily
  private static final GlrRenderFactory instance = new GlrRenderFactory();
  public static GlrRenderFactory getInstance() { return instance; }
}

// Step 3-4: Callers use the interface
public class Viewer {
  private final RenderFactory renderFactory;
  public Viewer(RenderFactory renderFactory) {
    this.renderFactory = renderFactory;
  }
}
```

### Rule 4 — Lazy migration path

Replace `edu.cmu.cs.dennisc.pattern.Lazy<T>` with `java.util.function.Supplier<T>`
backed by a thread-safe lazy wrapper when migrating Croquet composites:

```java
// Before
private final Lazy<MyComposite> lazy = new Lazy<>() {
  @Override protected MyComposite create() { return new MyComposite(); }
};

// After
private final Supplier<MyComposite> lazy =
    Suppliers.memoize(MyComposite::new);
```

Use Guava's `Suppliers.memoize()` (already on the classpath) or a simple
double-checked locking wrapper if adding a Guava dependency is not desired.

### Rule 5 — Scope containment for legacy singletons

Legacy singletons that cannot be refactored immediately must be
**contained to their module boundary**:

- A singleton defined in `core/util` may only be referenced from within `core/util`.
- Cross-module access must go through a parameter, factory method, or service
  locator that can be swapped in tests.
- When extracting a module (e.g., `core/ik-poser`), the new module must **not**
  import singletons from `core/ide`.

## Testing Guidance

### Characterization tests first

Before refactoring a singleton, add a characterization test that exercises the
singleton's current behavior. This prevents behavioral regressions during
extraction.

### Test doubles

After extracting an interface (Rule 3), tests can supply a mock or stub:

```java
@Test void sceneEditor_savesProject() {
  var mockManager = mock(ProjectManager.class);
  var editor = new SceneEditor(mockManager);
  editor.save();
  verify(mockManager).save();
}
```

### Avoid @BeforeAll singleton initialization

Prefer per-test setup (`@BeforeEach`) over shared singleton state.
Shared state between tests is a top source of flaky test failures.

## Migration Priority

Focus singleton removal on modules being extracted first:

1. **`core/ik-poser`** — `FieldFinder.getInstance()`,
   `StoryApiConfigurationManager.getInstance()`, `StorytellingSceneEditor.getInstance()`.
   Uses `IkPoserContexts` with a lazy-holder ServiceLoader cache (permitted by
   Rule 1 exception). SPI interface `IkPoserContext` lives in `core/ik-poser`;
   IDE implementation `IdeIkPoserContext` lives in `core/ide`.
2. **`core/clipboard-dnd`** — `Clipboard.SINGLETON`, `RecycleBin.SINGLETON`.
   This module is an **optional plugin** that depends on `core/ide` (not the
   reverse). It is loaded at runtime via `ServiceLoader<ClipboardProvider>`.
   The SPI interface `ClipboardProvider` is defined in `core/ide`; the
   implementation `ClipboardDnDProvider` is in `core/clipboard-dnd`. Removing
   `core/clipboard-dnd` from the build will cause a runtime
   `NoSuchElementException` if clipboard operations are invoked.
   Clipboard operation memoization uses scoped
   [`ClipboardOperationRegistry`](../reference/clipboard-operation-registry.md)
   instances instead of static mutable maps on operation classes; see
   [Scoped Clipboard Operation Registries](../concepts/scoped-clipboard-operation-registries.md).
   **Note:** After extracting clipboard classes to this module, 7 generated
   coverage tests and 9 sweep-test entries in `core/ide` that referenced the
   moved classes by FQCN were removed.
3. **`core/glrender`** — `GlrRenderFactory.getInstance()`,
   `ConformanceTestResults.SINGLETON`
4. **`core/ide`** — largest concentration; address incrementally per subsystem

## Decision Record

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-05-28 | Adopted containment strategy | Block singleton spread while planning incremental DI migration |
| — | Chose constructor injection over service locator | Simpler, compile-time verifiable, no framework dependency |
| — | Deferred full DI framework (Guice/Dagger) | Premature; extract interfaces first, then evaluate framework need |
| 2026-05-28 | Permitted ServiceLoader lazy-holder exception to Rule 1 | `ServiceLoader.load()` per-call overhead is prohibitive; one cached holder per SPI interface is acceptable |
| 2026-05-28 | Documented clipboard-dnd as plugin module | Depends on `core/ide`, not standalone; clarifies that removal breaks clipboard operations at runtime |
