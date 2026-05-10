# Trace the Decoder Delegate Decomposition

This tutorial walks through the decomposed Decoder architecture, showing how a
Tweedle class declaration flows through the coordinator and its three delegates.

## The entry point

All decode calls enter through `TweedleEncoderDecoder`, which creates a fresh
`Decoder` instance:

```java
// TweedleEncoderDecoder.java — unchanged
public AbstractNode decode(String document) throws VersionNotSupportedException {
    return new Decoder().decode(document);
}
```

## Decoder creates its delegates

When `Decoder` is constructed, it creates one instance of each delegate and
passes itself as the back-reference:

```java
// Decoder.java — coordinator
class Decoder {
    final ExpressionDecoder expressionDecoder;
    final StatementDecoder statementDecoder;
    final FieldDecoder fieldDecoder;

    Decoder(Set<AbstractDeclaration> terminals, boolean allowLiteralArithmeticFieldInitializers) {
        // ... terminal type map setup ...
        this.expressionDecoder = new ExpressionDecoder(this);
        this.fieldDecoder = new FieldDecoder(this, expressionDecoder);
        this.statementDecoder = new StatementDecoder(this, expressionDecoder);
    }
}
```

All three delegates are package-private with no public constructors.

## Tracing a full class decode

Given this Tweedle source:

```java
class Program extends SProgram {
    WholeNumber count <- 0;
    DecimalNumber[] values <- new DecimalNumber[3];

    WholeNumber getCount() {
        return count;
    }

    void increment(Boolean enabled) {
        if (enabled && count < 10) {
            count <- count + 1;
        }
    }
}
```

### Step 1: Decoder parses and dispatches

`Decoder.decode(String)` parses the source via `TweedleUnlinkedParser`,
confirms it is a `TweedleClass`, and calls `decodeClass(TweedleClass)`.

`decodeClass` orchestrates in this order:

1. Resolves the superclass via `resolveType("SProgram", "superclass")`.
2. Decodes each field by delegating to `fieldDecoder.decodeField(property)`.
3. Decodes each method signature itself via `decodeMethodSignature(method)`.
4. Decodes each method body by delegating to
   `statementDecoder.decodeMethodBody(...)`.
5. Decodes constructors by delegating to
   `statementDecoder.decodeConstructorBody(...)`.

### Step 2: FieldDecoder handles field declarations

For `WholeNumber count <- 0`:

1. `FieldDecoder.decodeField(property)` calls `decoder.resolveType("WholeNumber", "field")`.
   The coordinator resolves this from `TWEEDLE_TYPE_ALIASES` → `Integer.class` → `JavaType`.
2. The initializer `0` is a `TweedlePrimitiveValue`, so
   `expressionDecoder.primitiveLiteral(0)` returns an `IntegerLiteral`.
3. A `UserField("count", JavaType(Integer), IntegerLiteral(0))` is returned.

For `DecimalNumber[] values <- new DecimalNumber[3]`:

1. `FieldDecoder.decodeField(property)` resolves the array type through
   `decoder.resolveType(TweedleArrayType)`, which recursively resolves
   the component type `DecimalNumber` → `Double.class`, then calls
   `getArrayType()`.
2. The initializer is a `TweedleArrayInitializer` with size `3`, so
   `decodeSizedArrayFieldInitializer` returns an `ArrayInstanceCreation`.

### Step 3: StatementDecoder handles method bodies

For the `getCount()` method body containing `return count;`:

1. `StatementDecoder.decodeMethodBody(...)` iterates the body statements.
2. The single `ReturnStatement` with identifier `count` dispatches to
   `decodeReturnStatement(...)`.
3. The return expression `count` dispatches to
   `expressionDecoder.decodeMethodReturnExpression(...)`.
4. `ExpressionDecoder` calls `decoder.findField(fields, "count")` to resolve
   the identifier, then returns a `FieldAccess` wrapped in a
   `ReturnStatement`.

### Step 4: ExpressionDecoder handles value expressions

For the condition `enabled && count < 10` in the `if` statement:

1. `StatementDecoder.decodeIfStatement(...)` delegates the condition to
   `expressionDecoder.decodeValueExpression(...)`.
2. The top-level `LogicalAndExpression` dispatches to
   `decodeLogicalInfixExpression(...)`.
3. The left operand `enabled` resolves to a `ParameterAccess` via
   `decoder.findParameter(parameters, "enabled")`.
4. The right operand `count < 10` is a `LessThanExpression`, which dispatches
   to `decodeRelationalExpression(...)`.
5. `count` resolves to a `FieldAccess`, `10` to an `IntegerLiteral`.
6. The result is a `ConditionalInfixExpression(ParameterAccess, AND, RelationalInfixExpression)`.

### Step 5: StatementDecoder handles the if body

The `if` body contains `count <- count + 1`:

1. `decodeSimpleIfBody(...)` recognizes the assignment expression.
2. The RHS `count + 1` dispatches to
   `expressionDecoder.decodeValueExpression(...)` → `decodeBinaryNumericExpression(...)`.
3. The LHS `count` resolves to a field via `decoder.findField(fields, "count")`.
4. The result is an `AstUtilities.createFieldAssignmentStatement(field, ArithmeticInfixExpression)`.

## Collaboration summary

```text
Decoder (coordinator)
  │
  ├─ resolveType()          ← called by all three delegates
  ├─ findLocal()            ← called by ExpressionDecoder, StatementDecoder
  ├─ findParameter()        ← called by ExpressionDecoder
  ├─ findField()            ← called by all three delegates
  │
  ├─ fieldDecoder.decodeField()
  │   └─ expressionDecoder.primitiveLiteral()
  │
  ├─ statementDecoder.decodeMethodBody()
  │   ├─ expressionDecoder.decodeValueExpression()
  │   ├─ expressionDecoder.decodeMethodReturnExpression()
  │   └─ expressionDecoder.decodeAssignmentRhs()
  │
  └─ statementDecoder.decodeConstructorBody()
      └─ expressionDecoder.decodeValueExpression()
```

No delegate calls another delegate directly except through the references
provided at construction. The coordinator is the only class that knows all
three delegates exist.

## Error message preservation

Every `UnsupportedTweedleDecodeException` message is preserved
character-for-character. The error factory methods move with their owning
methods to the appropriate delegate. Characterization tests written before
the extraction assert exact exception messages, ensuring the refactor does not
change diagnostic output.

Example: the error for an unsupported method body still reads:

```text
Tweedle method bodies are not yet supported by the AST decoder: methodName
```

This message now comes from `StatementDecoder.unsupportedMethodBody(...)` rather
than `Decoder.unsupportedMethodBody(...)`, but the string is identical.

## What to read next

- [Decoder Delegate Decomposition](../reference/decoder-delegate-decomposition.md) — Full reference with class responsibilities, acceptance criteria, and security boundary.
- [Validate the Decoder Delegate Decomposition](../howto/validate-decoder-delegate-decomposition.md) — Step-by-step validation commands.
- [Decode Coverage Characterization](../reference/decode-coverage-characterization.md) — Unit-level decode coverage reference.
- [Silver Thread Tweedle Decoder Round-Trip Test](../reference/silver-thread-tweedle-decoder-round-trip-test.md) — Round-trip test reference.
