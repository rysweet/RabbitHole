# Tutorial: Trace the Encoder Delegate Decomposition

A guided walkthrough showing how a Tweedle encode request flows through
`TweedleEncoder` and its four delegates. Use this to understand the delegation
pattern before extending encoder behavior.

## Prerequisites

- Familiarity with the visitor pattern used by `SourceCodeGenerator`
- Access to the `core/ast` source in `org.alice.serialization.tweedle`
- Optional: read the [Decoder Delegate Decomposition](../reference/decoder-delegate-decomposition.md)
  tutorial for the mirror pattern on the decode side

## Overview

When a caller encodes an Alice AST node to Tweedle source, the request passes
through five classes:

```text
TweedleEncoderDecoder.encode(node)
  └── TweedleEncoder.encode(node)        ← coordinator
      ├── EncoderMappings.*              ← static lookup tables
      ├── StatementEncoder.*             ← statement encoding
      ├── ExpressionEncoder.*            ← expression encoding
      └── ResourceStructureEncoder.*     ← resource class generation
```

## Step 1: Entry point — TweedleEncoderDecoder

Open `TweedleEncoderDecoder.java`. Find the `encode` method:

```java
public String encode(ProcessableNode node) {
  return new TweedleEncoder().encode(node);
}
```

This is the only public entry point. It creates a fresh `TweedleEncoder` and
calls `encode`. The encoder is single-use per encode operation.

## Step 2: Coordinator setup — TweedleEncoder constructor

Open `TweedleEncoder.java`. The constructor wires up the four delegates:

```java
TweedleEncoder(Set<AbstractDeclaration> terminals) {
  super(EncoderMappings.codeOrganizerDefinitionMap,
        CodeOrganizer.defaultCodeOrganizer);
  terminalNodes = terminals;
}
```

The `super()` call passes code organizer definitions from `EncoderMappings` to
`SourceCodeGenerator`. The coordinator creates delegate instances as fields.

**Key insight:** `EncoderMappings` holds all the static rename maps that
previously lived in the 184-line `static {}` block. This data is loaded once
at class initialization and shared across all encode operations.

## Step 3: Visitor dispatch — processMethod

When the AST visitor encounters a method node, `SourceCodeGenerator` calls
`processMethod(UserMethod)`. Trace this in `TweedleEncoder`:

```java
@Override
public void processMethod(UserMethod method) {
  super.processMethod(method);
  appendNewLine();
}
```

The `super.processMethod()` call in `SourceCodeGenerator` walks the method's
body, which triggers further visitor callbacks into `TweedleEncoder`.

## Step 4: Statement delegation — processLocalDeclaration

When the visitor encounters a local variable declaration inside a method body,
it calls `processLocalDeclaration`. Follow the delegation:

```java
// In TweedleEncoder.java
@Override
public void processLocalDeclaration(LocalDeclarationStatement stmt) {
  statementEncoder.processLocalDeclaration(stmt, this);
}
```

Now open `StatementEncoder.java`:

```java
void processLocalDeclaration(LocalDeclarationStatement stmt,
                             TweedleEncoder encoder) {
  encoder.processSingleStatement(stmt, () -> {
    UserLocal localVar = stmt.local.getValue();
    encoder.processTypeName(localVar.getValueType());
    encoder.appendSpace();
    encoder.processVariableIdentifier(localVar);
    encoder.appendAssignmentOperator();
    encoder.processExpression(stmt.initializer.getValue());
  });
}
```

**Key insight:** The delegate calls back to `encoder` (the coordinator) for
shared formatting methods like `processTypeName`, `appendSpace`, and
`processExpression`. The delegate owns the _logic_ of local declaration
encoding; the coordinator owns the _formatting primitives_.

## Step 5: Expression delegation — processInstantiation

When the visitor encounters an `InstanceCreation` node, trace the delegation:

```java
// In TweedleEncoder.java
@Override
public void processInstantiation(InstanceCreation creation) {
  if (!expressionEncoder.processInstantiation(creation, this)) {
    super.processInstantiation(creation);
  }
}
```

Now open `ExpressionEncoder.java` and find the method. It handles three special
cases:

1. **PersonResource** — evaluates via `ReleaseVirtualMachine` to get a summary
   string, then emits `new PersonResource(name: "Person/Summary")`
2. **Double boxing** — rewrites `new Double(n)` to `$DecimalNumber.from(wholeNumber: n)`
3. **Dynamic resources** — rewrites `new DynamicXxxResource(a, "Variant")` to
   `VariantResource.DEFAULT`

If none match, the method returns `false` and the coordinator falls through to
`super.processInstantiation(creation)`.

**Key insight:** The boolean-return pattern is a design decision introduced by
the extraction. The original code uses early `return` statements inside
`processInstantiation`. The delegate cannot call `super.processInstantiation()`
(only the coordinator can), so the delegate returns a boolean to signal whether
it handled the call. This is the same "try-delegate" pattern used in the Decoder
decomposition.

## Step 6: Argument labeling — processArgument

One of the more complex flows involves parameter labeling. Trace
`processArgument`:

```java
// In TweedleEncoder.java
@Override
public void processArgument(AbstractParameter parameter,
                            AbstractArgument argument) {
  expressionEncoder.processArgument(parameter, argument, this);
}
```

In `ExpressionEncoder`, `processArgument` calls `getParameterLabel` which
performs a multi-step resolution:

1. Check `EncoderMappings.constructorsWithRelabeledParams` for constructor
   parameter renames (e.g., `leftToRight` → `width` for `Size`)
2. Try `identifierName(parameter)` and apply
   `EncoderMappings.methodParamsToRelabel`
3. Look up `EncoderMappings.methodsMissingParameterNames` for Java methods
   that lack parameter name metadata
4. Fall back to type name with an error dialog

After labeling, `appendWrappedArg` checks `EncoderMappings.methodsWithWrappedArgs`
to see if the argument needs wrapping (e.g., `duration` → `new Duration(seconds: ...)`).

## Step 7: Resource class generation — processResourceType

When the encoder encounters a resource type, trace the delegation:

```java
// In TweedleEncoder.java
@Override
public void processResourceType(String jointedModelResource) {
  resourceStructureEncoder.processResourceType(jointedModelResource, this);
}
```

In `ResourceStructureEncoder`, the method uses `Class.forName` to reflect over
the resource class, then generates:

1. Class header with superclass
2. Constructor with superclass-specific parameters
3. Static fields for joint IDs and arrays (via `Field.get` reflection)
4. Resource instances (enum constants for enum resource classes)
5. Class footer

**Key insight:** All reflective code is consolidated in
`ResourceStructureEncoder`. This makes security review straightforward — only
one class uses `Class.forName` and `Field.get`.

Note that `ResourceStructureEncoder` also implements the logic for public
methods like `appendNewJointId`, `appendNewPose`, and
`appendNewJointTransformation`. These are called by AST nodes during
`encodeDefinition(this)`, so `TweedleEncoder` keeps public bridge methods that
delegate to the resource encoder.

## Step 8: Static maps — EncoderMappings

Open `EncoderMappings.java`. This class holds no methods — only static final
maps populated in a `static {}` initializer. Examples:

- `typesToRename`: `"Double"` → `"DecimalNumber"`, `"String"` → `"TextString"`
- `membersToRename`: `"rint"` → `"round"`, `"nextBoolean"` → `"boolean"`
- `methodsWithWrappedArgs`: `"delay"` → wrap `duration` in `new Duration(seconds: ...)`
- `systemIdentifiers`: `"args"`, `"index"`, `"event"`, `"myScene"`, etc.

All delegates and the coordinator read from these maps via
`EncoderMappings.fieldName`. No writes occur after class initialization.

## Step 9: Verify with a test

Run a single encoder test to watch the delegation in action:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=TweedleEncoderTest test -q
```

Each test in `TweedleEncoderTest` builds an AST fragment, calls
`encoder.encode(node)`, and asserts the exact Tweedle string output. The tests
exercise all four delegates without knowing about them — the public API
(`encode`) is unchanged.

## Summary

| Concern | Class | Pattern |
| --- | --- | --- |
| Visitor `@Override` stubs | TweedleEncoder | Delegates to appropriate encoder |
| `super.method()` calls | TweedleEncoder | Cannot be forwarded; stays on coordinator |
| AST-callback bridges | TweedleEncoder | Public methods delegated to ResourceStructureEncoder; called by `encodeDefinition(this)` |
| Statement logic | StatementEncoder | Calls back to coordinator for shared methods |
| Expression logic | ExpressionEncoder | Calls back to coordinator; reads from EncoderMappings |
| Resource reflection | ResourceStructureEncoder | Consolidates all `Class.forName` / `Field.get` |
| Static rename data | EncoderMappings | Read-only maps; no instance state |

## What to do when extending the encoder

1. **New statement type:** Add a method to `StatementEncoder`, add an
   `@Override` stub in `TweedleEncoder` that delegates to it.
2. **New expression type:** Add a method to `ExpressionEncoder`, add an
   `@Override` stub in `TweedleEncoder`.
3. **New type/member rename:** Add an entry to the appropriate map in
   `EncoderMappings`.
4. **New resource class shape:** Modify `ResourceStructureEncoder`.
5. **New formatting primitive:** Add to `TweedleEncoder` (the coordinator owns
   `appendString`, `appendSpace`, `openBlock`, etc.).

Always run the full test suite after changes:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -pl core/ast -am -DfailIfNoTests=false test -q
```
