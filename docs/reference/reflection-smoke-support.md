# Reflection Smoke Support Reference

`ReflectionCoverageSupport` is test-only infrastructure for proving that a
curated class list resolves without class initialization. It is not a behavior
coverage tool.

## Supported helpers

| Module | Helper | Scope |
| --- | --- | --- |
| `core/croquet` | `org.lgna.croquet.ReflectionCoverageSupport` | Croquet class resolution and metadata sanity |
| `core/glrender` | `edu.cmu.cs.dennisc.render.gl.ReflectionCoverageSupport` | GL/render class resolution and metadata sanity |

Both helpers are `final` classes with private constructors and static methods.

## API

### `inspectClasses(String... classNames)`

```java
public static void inspectClasses(String... classNames) throws Exception
```

Resolves each named class with the helper's test class loader without
initializing the class, using the equivalent of
`Class.forName(className, false, loader)`. The method checks minimal metadata and
throws when a class cannot be resolved or when metadata sanity checks fail.

Successful inspection means:

- each supplied class name resolved to a `Class<?>` without class
  initialization;
- each class has a package;
- each class has a non-empty binary name;
- declared classes, fields, methods, constructors, annotations, interfaces, and
  superclass metadata can be queried without the helper executing behavior;
- unsafe metadata that can initialize classes, such as enum constants, is not
  queried.

Successful inspection does not mean:

- constructors work;
- instance methods work;
- static methods work;
- listeners, lifecycle hooks, factories, adapters, caches, render targets, or
  application state behave correctly;
- a GL class can create a live OpenGL resource or display surface.

Those behaviors are covered by explicit JUnit contract tests.

## Failure behavior

The helper fails fast and visibly. It does not catch and ignore failures from
class resolution or metadata inspection.

| Failure | Result |
| --- | --- |
| Bad class name | `ClassNotFoundException` or equivalent test failure |
| Missing dependency during class resolution | `LinkageError` or equivalent test failure |
| Static initializer would fail if executed | No failure; the helper must not initialize classes |
| Empty or invalid class metadata | Assertion failure |

Because the helper does not invoke constructors or methods, constructor and
method exceptions belong in explicit behavior tests, not in smoke sweeps. Static
initializer behavior also belongs in focused tests, not broad smoke sweeps.

## Non-goals

Broad reflection smoke support never does the following:

- invoke constructors;
- invoke methods;
- trigger static initialization;
- query enum constants;
- call `setAccessible(...)`;
- allocate objects with `sun.misc.Unsafe`;
- synthesize default arguments;
- recursively exercise object graphs;
- suppress invocation failures;
- scan externally supplied packages or user-provided classpath input.

## Configuration

Reflection smoke tests use the normal Maven/JUnit 4 test configuration. No
feature flag enables constructor or method exercising.

Use headless mode for local and CI module runs:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
mvn -pl core/croquet test -Djava.awt.headless=true
mvn -pl core/glrender test -Djava.awt.headless=true
```

Initialize Tweedle before broader Maven validation:

```bash
git submodule update --init tweedle-lang
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true test
```

## Examples

Croquet smoke-load test:

```java
package org.lgna.croquet;

import org.junit.Test;

public class ReflectionCoverageSupportSmokeTest {
  @Test
  public void loadsNamedCroquetClasses() throws Exception {
    ReflectionCoverageSupport.inspectClasses(
        "org.lgna.croquet.Group",
        "org.lgna.croquet.State",
        "org.lgna.croquet.data.ImmutableListData");
  }
}
```

GL/render smoke-load test:

```java
package edu.cmu.cs.dennisc.render.gl;

import org.junit.Test;

public class ReflectionCoverageSupportSmokeTest {
  @Test
  public void loadsNamedRenderClassesHeadlessly() throws Exception {
    ReflectionCoverageSupport.inspectClasses(
        "edu.cmu.cs.dennisc.render.gl.GlrRenderFactory",
        "edu.cmu.cs.dennisc.render.gl.imp.Context",
        "edu.cmu.cs.dennisc.render.gl.imp.GlResourceCache");
  }
}
```

Invalid class contract:

```java
@Test(expected = ClassNotFoundException.class)
public void invalidClassNamesFailClearly() throws Exception {
  ReflectionCoverageSupport.inspectClasses("org.lgna.croquet.DoesNotExist");
}
```

No-constructor/no-method contract:

```java
package org.lgna.croquet;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ReflectionCoverageSupportSmokeTest {
  public static final class ConstructorAndMethodTrap {
    static int constructorCalls;
    static int methodCalls;

    public ConstructorAndMethodTrap() {
      constructorCalls++;
    }

    public void invokeMe() {
      methodCalls++;
    }
  }

  @Test
  public void inspectClassesDoesNotExecuteBehavior() throws Exception {
    ReflectionCoverageSupport.inspectClasses(
        ConstructorAndMethodTrap.class.getName());

    assertEquals(0, ConstructorAndMethodTrap.constructorCalls);
    assertEquals(0, ConstructorAndMethodTrap.methodCalls);
  }
}
```

No-static-initializer contract:

```java
package org.lgna.croquet;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ReflectionCoverageSupportSmokeTest {
  private static final AtomicInteger STATIC_INITIALIZER_CALLS = new AtomicInteger();

  public static final class StaticInitializerTrap {
    static {
      STATIC_INITIALIZER_CALLS.incrementAndGet();
    }
  }

  @Test
  public void inspectClassesDoesNotInitializeClasses() throws Exception {
    ReflectionCoverageSupport.inspectClasses(StaticInitializerTrap.class.getName());

    assertEquals(0, STATIC_INITIALIZER_CALLS.get());
  }
}
```

Equivalent GL/render contract:

```java
package edu.cmu.cs.dennisc.render.gl;

import edu.cmu.cs.dennisc.render.gl.imp.RenderContext;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class GlRenderContractTest {
  @Test
  public void forgettableBindingReceivesRenderContext() {
    RecordingBinding binding = new RecordingBinding();
    RenderContext context = new RenderContext();

    binding.forget(context);

    assertSame(context, binding.lastContext);
    assertEquals(1, binding.callCount);
  }

  private static final class RecordingBinding implements ForgettableBinding {
    private RenderContext lastContext;
    private int callCount;

    @Override
    public void forget(RenderContext rc) {
      this.lastContext = rc;
      this.callCount++;
    }
  }
}
```

## Maintenance rules

- Add class names to a smoke sweep only when class resolvability itself is the
  contract.
- Add a named JUnit test when behavior matters.
- Keep broad sweeps deterministic and headless-safe.
- Keep focused reflection characterization tests separate from broad smoke
  sweeps.
- Do not reintroduce class initialization, enum constant queries, `Unsafe`,
  generated arguments, constructor invocation, or method invocation into broad
  reflection helpers.

## Related documentation

- [Reflection sweep contracts](../concepts/reflection-sweep-contracts.md)
- [Replace reflection sweeps with explicit contracts](../howto/replace-reflection-sweeps.md)
- [Testing](../testing.md)
