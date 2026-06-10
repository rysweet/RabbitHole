# Replace Reflection Sweeps with Explicit Contracts

Use this guide when a broad reflection test covers behavior indirectly. The end
state is a small non-initializing smoke-load sweep plus explicit JUnit 4 tests
for real behavior.

## Quick start

1. Classify the test as a broad sweep, focused characterization, explicit
   contract, or unrelated utility.
2. Keep `ReflectionCoverageSupport.inspectClasses(...)` only for
   non-initializing class resolution and minimal metadata sanity.
3. Move constructor, method, lifecycle, state, and factory behavior into readable
   JUnit contract tests.
4. Run the module lane that owns the changed contracts.

## Classify the reflection test

Search for broad reflection patterns:

```bash
rg "ReflectionCoverageSupport|inspectClasses|getDeclaredMethods|getDeclaredConstructors|method.invoke|Unsafe|setAccessible" core
```

Classify each match:

| Classification | Description | Action |
| --- | --- | --- |
| Broad sweep | Reflectively loads or exercises many unrelated classes | Keep only smoke-load coverage; add explicit behavior contracts |
| Focused characterization | Uses reflection for one hard-to-reach seam | Keep if the test name and assertions describe the protected behavior |
| Explicit contract | Tests one API behavior with direct calls and assertions | Keep; no sweep conversion needed |
| Unrelated utility | Reflection is incidental to a helper or fixture | Leave alone unless it is hiding broad behavior coverage |

## Keep the smoke-load contract small

Croquet smoke sweeps use the Croquet test helper:

```java
package org.lgna.croquet;

import org.junit.Test;

public class ReflectionCoverageSupportSmokeTest {
  @Test
  public void loadsCroquetClassesWithoutExercisingBehavior() throws Exception {
    ReflectionCoverageSupport.inspectClasses(
        "org.lgna.croquet.Group",
        "org.lgna.croquet.BooleanState",
        "org.lgna.croquet.data.ImmutableListData");
  }
}
```

GL/render smoke sweeps use the GL helper and must stay headless-safe:

```java
package edu.cmu.cs.dennisc.render.gl;

import org.junit.Test;

public class ReflectionCoverageSupportSmokeTest {
  @Test
  public void loadsRenderClassesWithoutOpeningDisplayResources() throws Exception {
    ReflectionCoverageSupport.inspectClasses(
        "edu.cmu.cs.dennisc.render.gl.GlrRenderFactory",
        "edu.cmu.cs.dennisc.render.gl.imp.Context",
        "edu.cmu.cs.dennisc.render.gl.imp.GlResourceCache");
  }
}
```

The helper fails when a class cannot be resolved or safe metadata cannot
be read. It loads by name without initialization, using the equivalent of
`Class.forName(name, false, loader)`. It does not create instances, invoke
constructors or methods, allocate with `Unsafe`, query enum constants, or hide
failures from constructors and methods.

## Replace incidental coverage by area

When removing constructor or method exercising from a sweep, add explicit tests
for the behavior the sweep used to hit accidentally:

| Area | Replace incidental coverage with |
| --- | --- |
| Croquet data/list APIs | Direct tests for item count, order, mutation behavior, listener notification, and invalid input handling. |
| Croquet state APIs | Direct tests for initial value, transactionless updates, Swing model sync, listener dispatch, and value identity. |
| Croquet application-facing APIs | Direct tests using `CroquetTestUtils.ensureTestApplication()` or narrow fixtures for lifecycle, singleton lookup, and registration behavior. |
| GL/render lifecycle APIs | Headless-safe tests for lifecycle state, binding disposal, listener registration, cache cleanup, and factory decisions that do not require a live display. |
| GL/render utilities | Direct tests for pure utility results, adapter decisions, cache keys, value objects, and binding map behavior. |

## Add explicit Croquet contracts

Write contracts in the package that owns the API. Prefer direct construction and
direct assertions over reflection.

Example list-data contract:

```java
package org.lgna.croquet.data;

import org.lgna.croquet.CroquetTestUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ListDataContractTest {
  @Test
  public void immutableListDataPreservesElementOrder() {
    ImmutableListData<String> data =
        new ImmutableListData<String>(
            CroquetTestUtils.STRING_CODEC,
            new String[]{"camera", "light", "ground"});

    assertEquals(3, data.getItemCount());
    assertEquals("camera", data.getItemAt(0));
    assertEquals("light", data.getItemAt(1));
    assertEquals("ground", data.getItemAt(2));
  }
}
```

Example state contract:

```java
package org.lgna.croquet;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StateContractTest {
  private static final Group TEST_GROUP =
      Group.getInstance(UUID.fromString("00000000-0000-0000-0002-ffffffffffff"), "stateContract");

  private TestBooleanState state;

  @Before
  public void setUp() {
    state = new TestBooleanState(TEST_GROUP, false);
    CroquetTestUtils.removeItemListeners(state);
  }

  @Test
  public void transactionlessUpdateChangesCurrentValue() {
    state.setValueTransactionlessly(true);

    assertTrue(state.getValue());
    state.setValueTransactionlessly(false);
    assertFalse(state.getValue());
  }
}
```

Example application-facing contract:

```java
package org.lgna.croquet;

import org.junit.Test;
import static org.junit.Assert.assertSame;

public class ApplicationContractTest {
  @Test
  public void ensureTestApplicationInstallsActiveInstance() {
    Application<?> application = CroquetTestUtils.ensureTestApplication();

    assertSame(application, Application.getActiveInstance());
  }
}
```

## Add explicit GL/render contracts

Keep GL contracts safe for `-Djava.awt.headless=true`. Favor pure factories,
adapters, value objects, cache keys, and lifecycle state that can be verified
without a live OpenGL context.

```java
package edu.cmu.cs.dennisc.render.gl;

import edu.cmu.cs.dennisc.render.gl.imp.RenderContext;
import org.junit.Test;
import static org.junit.Assert.assertSame;

public class GlRenderContractTest {
  @Test
  public void forgettableBindingReceivesRenderContext() {
    RecordingBinding binding = new RecordingBinding();
    RenderContext context = new RenderContext();

    binding.forget(context);

    assertSame(context, binding.lastContext);
  }

  private static final class RecordingBinding implements ForgettableBinding {
    private RenderContext lastContext;

    @Override
    public void forget(RenderContext rc) {
      this.lastContext = rc;
    }
  }
}
```

Example focused render-utility contract:

```java
package edu.cmu.cs.dennisc.render.gl;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class TextureBindingContractTest {
  @Test
  public void textureBindingStartsWithNoRenderContextData() throws Exception {
    TextureBinding binding = new TextureBinding();
    Field field = TextureBinding.class.getDeclaredField("map");
    field.setAccessible(true);

    Map<?, ?> map = (Map<?, ?>) field.get(binding);

    assertTrue(map.isEmpty());
  }
}
```

If a render behavior needs a display, keep it out of the headless smoke lane and
put it in an existing headed/Xvfb validation lane with a bounded launch timeout.

## Preserve focused reflection characterization

Focused reflection is acceptable when it is the least invasive way to prove one
specific contract. Keep the test narrow: the `TextureBindingContractTest` above
reflects on one private field to prove one binding-map invariant. That is a
focused characterization, not a broad sweep.

Do not convert a focused characterization into a broad sweep. If the test needs
to inspect many unrelated classes, split it into a smoke sweep and explicit
contracts.

## Validate locally

Run the owning module first:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
mvn -pl core/croquet test -Djava.awt.headless=true
mvn -pl core/glrender test -Djava.awt.headless=true
```

Before broader Maven validation, initialize the Tweedle grammar submodule:

```bash
git submodule update --init tweedle-lang
mvn -DincludeSims=false -Dinstall4j.skip -Dcheckstyle.skip -Djava.awt.headless=true test
```

If Maven reports missing generated Tweedle parser classes, check the submodule
before changing tests:

```bash
git submodule status tweedle-lang
test -d tweedle-lang/Grammar
```

## Review checklist

- The smoke helper resolves classes without initialization and checks metadata
  only.
- Missing classes and linkage errors fail the test clearly.
- Broad reflection helpers do not initialize classes, query enum constants,
  invoke constructors, or invoke methods.
- No broad sweep uses `setAccessible(...)`, generated arguments, or
  `Unsafe.allocateInstance(...)`.
- Every removed incidental behavior path has an explicit JUnit contract.
- GL/render contracts run headlessly unless they are intentionally assigned to a
  headed/Xvfb lane.

## Related documentation

- [Reflection sweep contracts](../concepts/reflection-sweep-contracts.md)
- [Reflection smoke support reference](../reference/reflection-smoke-support.md)
- [Testing](../testing.md)
