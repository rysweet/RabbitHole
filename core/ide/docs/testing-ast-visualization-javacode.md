# Testing: AST Visualization & Java Code Rendering (`org.alice.ide.x`, `org.alice.ide.javacode`)

> Characterization test suite for the AST i18n factory hierarchy, component views,
> croquet integration classes, and Java code rendering utilities in `core/ide`.

---

## Overview

This test suite adds **5,200+ lines** of JUnit 4 reflection-based characterization tests
across six test classes. The tests lock down the structural contracts (class hierarchy,
method signatures, modifiers, fields, inner classes) of 35 production classes without
instantiating any Swing components or triggering the IDE bootstrap sequence.

### Production packages covered

| Package | Classes | Purpose |
|---|---|---|
| `org.alice.ide.x` | 13 factory classes | AST-to-UI translation factories |
| `org.alice.ide.x.components` | 18 component views | Swing views for AST nodes |
| `org.alice.ide.x.croquet` | 1 cascade class | Scene-editor argument cascades |
| `org.alice.ide.x.croquet.edits` | 1 edit class | Property edit operations |
| `org.alice.ide.javacode.croquet` | 1 composite class | Java code frame composite |
| `org.alice.ide.javacode.croquet.views` | 1 view class | Java code rendering view |

### Test classes

| Test class | Location | Lines | Scope |
|---|---|---|---|
| `AstI18nFactoryHierarchyTest` | `core/ide/src/test/java/org/alice/ide/x/` | ~930 | 13 factory classes |
| `AstI18nFactoryComponentsTest` | `core/ide/src/test/java/org/alice/ide/x/` | ~1,260 | 18 component views |
| `AstI18nFactoryContractTest` | `core/ide/src/test/java/org/alice/ide/x/` | ~1,650 | Deep behavioral contracts |
| `AstI18nFactoryCroquetTest` | `core/ide/src/test/java/org/alice/ide/x/croquet/` | ~280 | 2 croquet classes |
| `JavaCodeViewTest` | `core/ide/src/test/java/org/alice/ide/javacode/croquet/` | ~340 | 2 javacode classes |
| `JavaCodeRenderingTest` | `core/ide/src/test/java/org/alice/ide/javacode/croquet/` | ~740 | Rendering & lifecycle |

---

## Running the tests

### Run all new tests

```bash
mvn test -pl core/ide \
  -Dtest='AstI18nFactoryHierarchyTest,AstI18nFactoryComponentsTest,AstI18nFactoryContractTest,AstI18nFactoryCroquetTest,JavaCodeViewTest,JavaCodeRenderingTest'
```

### Run a single test class

```bash
mvn test -pl core/ide -Dtest=AstI18nFactoryHierarchyTest
```

### Run a single test method

```bash
mvn test -pl core/ide -Dtest='AstI18nFactoryHierarchyTest#previewAstI18nFactory_classLoadable'
```

### Verify existing tests still pass

```bash
mvn test -pl core/ide -Dtest='JavaCodeUtilitiesTest'
```

### Run the full `core/ide` test suite

```bash
mvn test -pl core/ide
```

> **CI note:** All tests use pure reflection and do not instantiate Swing
> components, so they run successfully on headless CI runners without Xvfb.

---

## Test patterns

### Reflection-only testing (headless-safe)

All tests use `java.lang.reflect` to inspect class structure without instantiation:

```java
@Test
public void previewAstI18nFactory_classLoadable() throws ClassNotFoundException {
    Class.forName("org.alice.ide.x.PreviewAstI18nFactory");
}

@Test
public void previewAstI18nFactory_extendsImmutableAstI18nFactory() throws Exception {
    Class<?> cls = Class.forName("org.alice.ide.x.PreviewAstI18nFactory");
    assertEquals("ImmutableAstI18nFactory", cls.getSuperclass().getSimpleName());
}
```

**Why reflection-only?** Instantiating factory singletons (e.g., `PreviewAstI18nFactory.getInstance()`)
triggers the Alice IDE bootstrap sequence, which requires a running project editor,
scene graph, and OpenGL context. Reflection-based tests verify the same structural
contracts without those dependencies.

### Singleton structure verification (without invocation)

For singleton factories, tests verify the `getInstance()` method exists with correct
modifiers but **never invoke it**:

```java
@Test
public void menuIconIdeAstI18nFactory_hasStaticGetInstance() throws Exception {
    Class<?> cls = Class.forName("org.alice.ide.x.MenuIconIdeAstI18nFactory");
    Method m = cls.getDeclaredMethod("getInstance");
    assertTrue(Modifier.isStatic(m.getModifiers()));
    assertTrue(Modifier.isPublic(m.getModifiers()));
}
```

### Generic type bound verification

Component views use parameterized superclasses. Tests verify generic bounds via
`getGenericSuperclass()`:

```java
@Test
public void expressionPropertyView_genericSuperclass() throws Exception {
    Class<?> cls = Class.forName("org.alice.ide.x.components.ExpressionPropertyView");
    java.lang.reflect.Type genericSuper = cls.getGenericSuperclass();
    assertNotNull(genericSuper);
    assertTrue(genericSuper instanceof java.lang.reflect.ParameterizedType);
}
```

### Inner class discovery

For classes with inner types (e.g., `StatementListPropertyView.FeedbackJPanel`):

```java
@Test
public void statementListPropertyView_hasFeedbackJPanelInnerClass() throws Exception {
    Class<?> cls = Class.forName("org.alice.ide.x.components.StatementListPropertyView");
    Class<?>[] inner = cls.getDeclaredClasses();
    boolean found = false;
    for (Class<?> c : inner) {
        if ("FeedbackJPanel".equals(c.getSimpleName())) {
            found = true;
            break;
        }
    }
    assertTrue("Expected FeedbackJPanel inner class", found);
}
```

---

## Test class API reference

### `AstI18nFactoryHierarchyTest`

Tests for the 13 factory classes in `org.alice.ide.x`:

| Factory class | Test categories |
|---|---|
| `I18nFactory` | loadability, abstract modifier, extends `Object`, public constructors, `createComponent` methods, abstract protected methods (`createGetsComponent`, `createPropertyComponent`) |
| `AstI18nFactory` | loadability, abstract modifier, extends `I18nFactory`, method signatures (`createExpressionPane`, `createExpressionPropertyPane`, `createStatementPane`, `isCommentMutable`, `isStatementListPropertyMutable`) |
| `IdeAstI18nFactory` | loadability, abstract modifier, extends `AstI18nFactory`, constructor params, `createGetsPane` / `createLocalDeclarationPane` methods |
| `ImmutableAstI18nFactory` | loadability, abstract modifier, extends `IdeAstI18nFactory` |
| `MutableAstI18nFactory` | loadability, abstract modifier, extends `AstI18nFactory`, constructor params (`Group`) |
| `AbstractProjectEditorAstI18nFactory` | loadability, abstract modifier, extends `MutableAstI18nFactory` |
| `ProjectEditorAstI18nFactory` | loadability, extends `AbstractProjectEditorAstI18nFactory`, singleton structure |
| `SceneEditorUpdatingProjectEditorAstI18nFactory` | loadability, extends `AbstractProjectEditorAstI18nFactory`, singleton structure |
| `PreviewAstI18nFactory` | loadability, extends `ImmutableAstI18nFactory`, singleton structure |
| `TemplateAstI18nFactory` | loadability, extends `IdeAstI18nFactory`, singleton structure |
| `MenuIconIdeAstI18nFactory` | loadability, extends `ImmutableAstI18nFactory`, singleton structure |
| `DialogAstI18nFactory` | loadability, extends `MutableAstI18nFactory`, singleton structure |
| `ClipboardAstI18nFactory` | loadability, public modifier, superclass is `Object`, no declared methods, no declared fields |

#### Inheritance chain verified

```
I18nFactory (abstract)
└── AstI18nFactory (abstract)
    ├── IdeAstI18nFactory (abstract)
    │   ├── ImmutableAstI18nFactory (abstract)
    │   │   ├── MenuIconIdeAstI18nFactory
    │   │   └── PreviewAstI18nFactory
    │   └── TemplateAstI18nFactory
    └── MutableAstI18nFactory (abstract)
        ├── DialogAstI18nFactory
        └── AbstractProjectEditorAstI18nFactory (abstract)
            ├── ProjectEditorAstI18nFactory
            └── SceneEditorUpdatingProjectEditorAstI18nFactory

ClipboardAstI18nFactory (extends Object — separate class, not in hierarchy)
```

### `AstI18nFactoryComponentsTest`

Tests for 18 component views in `org.alice.ide.x.components`:

| Component class | Test categories |
|---|---|
| `AbstractExpressionView` | loadability, non-abstract (despite name prefix), superclass chain (`ExpressionLikeSubstance`) |
| `ExpressionView` | loadability, extends `AbstractExpressionView`, generic type bounds |
| `ExpressionPropertyView` | loadability, generic superclass, constructor params |
| `InfixExpressionView` | loadability, superclass, declared methods |
| `FieldAccessView` | loadability, superclass, constructor params |
| `ThisExpressionLikeView` | loadability, superclass |
| `InstanceCreationView` | loadability, superclass, constructor params |
| `InstancePropertyLabelView` | loadability, superclass |
| `ArgumentView` | loadability, abstract modifier, constructor params, superclass (`LineAxisPanel`) |
| `KeyedArgumentView` | loadability, constructor params, superclass |
| `ArgumentListPropertyView` | loadability, abstract modifier, superclass (`LineAxisPanel`) |
| `ArgumentListPropertyPane` | loadability, superclass (JPanel descendant) |
| `KeyedArgumentListPropertyView` | loadability, superclass |
| `ExpressionListPropertyPane` | loadability, superclass |
| `ListPropertyLabelsView` | loadability, superclass |
| `NodePropertyView` | loadability, generic superclass |
| `ResourcePropertyView` | loadability, superclass |
| `StatementListPropertyView` | loadability, inner classes (`FeedbackJPanel`, `BoundInformation`), declared fields, superclass |

### `AstI18nFactoryCroquetTest`

Tests for 2 classes in `org.alice.ide.x.croquet` and `org.alice.ide.x.croquet.edits`:

#### `SceneEditorUpdatingArgumentCascade`

| Test | Assertion |
|---|---|
| `classLoadable` | `Class.forName` succeeds |
| `superclass` | Extends `AbstractArgumentCascade` |
| `hasStaticMap` | Declares a static `Map` field for instance caching |
| `hasSynchronizedGetInstance` | `getInstance` method exists with `synchronized` modifier |
| `constructorAcceptsUUID` | Constructor parameter includes `java.util.UUID` |

#### `SceneEditorUpdatingExpressionPropertyEdit`

| Test | Assertion |
|---|---|
| `classLoadable` | `Class.forName` succeeds |
| `superclass` | Extends `ExpressionPropertyEdit` |
| `hasUserFieldParam` | Constructor accepts `UserField` parameter |
| `hasBinaryDecoderConstructor` | Alternate constructor with `BinaryDecoder` |
| `hasFiveParamConstructor` | Full constructor with 5 parameters |

### `JavaCodeViewTest`

Supplemental reflection tests for `JavaCodeView` and `JavaCodeFrameComposite`. **No overlap**
with the existing `JavaCodeUtilitiesTest` (which covers loadability, `getInstance`, superclass,
and `setDeclaration`).

| Test | Class under test | Assertion |
|---|---|---|
| `javaCodeView_superclassFQN` | `JavaCodeView` | Superclass fully-qualified name matches expected HtmlView |
| `javaCodeView_hasUUIDField` | `JavaCodeView` | Declares a static `UUID` field with specific value |
| `javaCodeView_hasCreateViewMethod` | `JavaCodeView` | `createView` method exists |
| `javaCodeView_hasCreateScrollPaneMethod` | `JavaCodeView` | `createScrollPane` method exists |
| `javaCodeView_hasDeclarationListenerField` | `JavaCodeView` | `declarationListener` field type is correct |
| `javaCodeFrameComposite_hasGetViewMethod` | `JavaCodeFrameComposite` | `getView` method returns expected type |
| `javaCodeFrameComposite_hasUUIDField` | `JavaCodeFrameComposite` | Static `UUID` field exists |
| `javaCodeFrameComposite_constructorAccessibility` | `JavaCodeFrameComposite` | Constructor is not public (singleton pattern) |

---

## Configuration

### No additional configuration required

These tests use only:
- **JUnit 4** — already declared in `core/ide/pom.xml`
- **`java.lang.reflect`** — standard JDK

No new dependencies, test frameworks, or build plugins are needed.

### Headless behavior

All tests use pure reflection — no Swing instantiation, no display server required.

| Environment | Behavior |
|---|---|
| Desktop (X11/Wayland) | Tests run normally |
| Headless CI (no display) | Tests run normally (reflection-only) |
| CI with Xvfb | Tests run normally |

---

## Extending the test suite

### Adding tests for a new factory class

1. Add the class to `AstI18nFactoryHierarchyTest`.
2. Follow the existing pattern — group of 3–5 `@Test` methods:

```java
// -- NewFactory structural tests ------------------------------------------

@Test
public void newFactory_classLoadable() throws ClassNotFoundException {
    Class.forName("org.alice.ide.x.NewFactory");
}

@Test
public void newFactory_extendsExpectedSuperclass() throws Exception {
    Class<?> cls = Class.forName("org.alice.ide.x.NewFactory");
    assertEquals("ExpectedSuperclass", cls.getSuperclass().getSimpleName());
}

@Test
public void newFactory_isNotAbstract() throws Exception {
    Class<?> cls = Class.forName("org.alice.ide.x.NewFactory");
    assertFalse(Modifier.isAbstract(cls.getModifiers()));
}
```

### Adding tests for a new component view

1. Add the class to `AstI18nFactoryComponentsTest`.
2. Verify at minimum: loadability, superclass, and constructor parameter types.

### Adding tests for a new javacode class

1. Add to `JavaCodeViewTest`.
2. Ensure no overlap with `JavaCodeUtilitiesTest` — check existing test methods first.

---

## Troubleshooting

### `ClassNotFoundException` in tests

**Cause:** The class was renamed, moved, or removed from `core/ide`.

**Fix:** Update the fully-qualified class name string in the test. Search for the class:

```bash
find core/ide/src/main/java -name 'ClassName.java'
```

### Tests skipped on local machine

**Cause:** Running in headless mode (no display server).

**Fix:** Either start an X server or run with Xvfb:

```bash
xvfb-run --auto-servernum -s "-screen 0 1024x768x24 -ac" \
  mvn test -pl core/ide -Dtest=AstI18nFactoryHierarchyTest
```

For the shared Xvfb command contract, see the
[JavaFX Xvfb Launcher Reference](../../../docs/reference/javafx-xvfb-launcher.md).

### `NoSuchMethodException` for singleton `getInstance()`

**Cause:** The factory switched from singleton to dependency-injected pattern.

**Fix:** Update the test to reflect the new construction pattern. Remove the
`getInstance` method check and add appropriate constructor/factory-method tests.

### Generic type assertion fails

**Cause:** A superclass type parameter was added or removed.

**Fix:** Inspect the actual generic superclass:

```java
System.out.println(cls.getGenericSuperclass());
```

Update the test to match the new parameterization.

---

## Design rationale

### Why reflection-only?

The `org.alice.ide.x` package is tightly coupled to the Alice IDE runtime:
- Factory singletons depend on a running `ProjectEditor`
- Component views extend Swing classes that require a display server
- Croquet cascades register with the global action manager

Reflection-based tests decouple structural verification from runtime dependencies,
making the tests runnable in any environment (including headless CI).

### Why `Class.forName()` instead of direct imports?

Using `Class.forName("...")` with string literals:
1. **Documents the FQN contract** — renaming a class breaks the test with a clear error
2. **Avoids import-time class loading** — some classes have static initializers that
   trigger IDE bootstrap
3. **Matches the existing pattern** — `JavaCodeUtilitiesTest` already uses this approach

### Why JUnit 4 (not JUnit 5)?

The `core/ide` module uses JUnit 4 throughout. Mixing JUnit versions in the same module
adds complexity without benefit. All new tests follow the same JUnit 4 conventions:
- `@Test` annotation
- `Assert.*` static imports

### Why separate test classes instead of one large class?

Six test classes map to distinct production sub-packages and concern axes:
- `x/` hierarchy → `AstI18nFactoryHierarchyTest`
- `x/components/` → `AstI18nFactoryComponentsTest`
- `x/` deep contracts → `AstI18nFactoryContractTest`
- `x/croquet/` + `x/croquet/edits/` → `AstI18nFactoryCroquetTest`
- `javacode/` structure → `JavaCodeViewTest`
- `javacode/` rendering & lifecycle → `JavaCodeRenderingTest`

This keeps each test class focused, enables selective test execution (run only the
tests for the package you changed), and keeps file sizes manageable.
