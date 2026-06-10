# Reflection Sweep Contracts

RabbitHole keeps broad reflection sweeps as non-initializing smoke-load checks
only. Behavior coverage lives in explicit JUnit contracts.

## Why reflection sweeps are narrow

Reflection sweeps are useful for finding classes that no longer load. They are
poor behavior tests because they can hide the actual contract being protected:

- constructor side effects are hard to see in a generated sweep;
- method calls use synthetic arguments that may not represent real usage;
- swallowed invocation failures can make broken behavior look covered;
- GPU, display, and singleton dependencies make broad sweeps fragile in
  headless CI.

The contract is simple: a sweep proves that listed classes can be
resolved without class initialization and have sane metadata. It does not prove
constructor, lifecycle, state, factory, listener, rendering, or data behavior.

## What counts as a reflection sweep

A reflection sweep is any broad test helper or test whose primary purpose is to
discover or exercise many classes reflectively. In this repository, that includes
the `ReflectionCoverageSupport.inspectClasses(...)` style helpers in:

```text
core/croquet/src/test/java/org/lgna/croquet/ReflectionCoverageSupport.java
core/glrender/src/test/java/edu/cmu/cs/dennisc/render/gl/ReflectionCoverageSupport.java
```

Focused tests that use reflection to reach one private state seam or difficult
integration point are not automatically sweeps. Keep those tests when they state
the behavior they protect and fail clearly when that behavior changes.

## Behavior

Broad reflection helpers:

1. Resolve each named class with the test class loader without initializing it.
2. Assert that the loaded class is not null.
3. Assert minimal metadata sanity, such as package presence and non-empty class
   names.
4. Surface class-resolution, linkage, and metadata failures as test failures.
5. Do not trigger static initialization.
6. Do not query enum constants, because that can initialize enum classes.
7. Do not instantiate classes.
8. Do not invoke constructors or methods.
9. Do not call `setAccessible(...)` for broad sweeps.
10. Do not allocate instances with `sun.misc.Unsafe`.
11. Do not synthesize constructor or method arguments.
12. Do not swallow invocation failures as incidental hidden coverage.

## Where behavior coverage belongs

Behavior previously protected only by broad reflection is covered by focused
contract tests with readable setup, action, and assertions.

| Area | Contract test style |
| --- | --- |
| Croquet data/list APIs | Verify list contents, mutation results, listener events, and invalid inputs directly. |
| Croquet state APIs | Verify initial values, transactionless updates, listener notification, and value identity directly. |
| Croquet application-facing APIs | Verify documented lifecycle and singleton interactions through test utilities or narrow fixtures. |
| GL/render lifecycle APIs | Verify headless-safe lifecycle transitions and factory/adapter decisions without requiring a live display or GPU. |
| GL/render utilities | Verify pure utility, binding, cache-key, and value-object behavior directly. |

## Sweep tests vs explicit contracts

Use this classification before changing a reflection-heavy test:

| Test shape | Keep as smoke sweep? | Add explicit contract? |
| --- | --- | --- |
| Calls `ReflectionCoverageSupport.inspectClasses(...)` with many class names | Yes, but smoke-load only | Yes, for every meaningful behavior formerly exercised indirectly |
| Scans declared constructors or methods across many classes | Convert to smoke-load only | Yes |
| Invokes methods with generated default values | No | Yes |
| Uses `Unsafe.allocateInstance(...)` to reach many classes | No | Yes |
| Uses reflection for one private field in a named contract test | Keep if it is focused and documented | Usually no, unless behavior is still hidden |

## Related documentation

- [Replace reflection sweeps with explicit contracts](../howto/replace-reflection-sweeps.md)
- [Reflection smoke support reference](../reference/reflection-smoke-support.md)
- [Testing](../testing.md)
