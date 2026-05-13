# Validate VirtualMachine Extraction

How to verify the VmExpressionEvaluator and VmStatementExecutor extraction
from `VirtualMachine.java` (issue #551).

## Prerequisites

- Java 17+ and Maven installed
- Tweedle grammar submodule initialized:
  ```bash
  git submodule update --init tweedle-lang
  ```

## Steps

### 1. Run the full test suite

```bash
mvn -pl core/ast -am -DfailIfNoTests=false -Dcheckstyle.skip test
```

All tests (including `VmContractTest` and the 7 existing characterization
tests) must pass with 0 failures.

### 2. Verify line counts

```bash
wc -l core/ast/src/main/java/org/lgna/project/virtualmachine/VirtualMachine.java
# Must be < 500

wc -l core/ast/src/main/java/org/lgna/project/virtualmachine/VmExpressionEvaluator.java
# Expected: ~452

wc -l core/ast/src/main/java/org/lgna/project/virtualmachine/VmStatementExecutor.java
# Expected: ~369
```

### 3. Verify no changes outside core/ast

```bash
git diff --name-only develop
# Every path must start with core/ast/
```

### 4. Verify ReleaseVirtualMachine is unchanged

```bash
git diff develop -- core/ast/src/main/java/org/lgna/project/virtualmachine/ReleaseVirtualMachine.java
# Must produce empty output
```

### 5. Verify CopyOnWriteArrayList field

The `VmContractTest` asserts this automatically, but you can also confirm
manually:

```bash
grep 'CopyOnWriteArrayList' \
  core/ast/src/main/java/org/lgna/project/virtualmachine/VirtualMachine.java
# Should show the field declaration
```

## What to check if tests fail

- If `VmContractTest` reports wrong method counts, a public or abstract
  method was added or removed on `VirtualMachine`. See
  [reference/vm-expression-evaluator-statement-executor-extraction.md](../reference/vm-expression-evaluator-statement-executor-extraction.md#troubleshooting).
- If characterization tests fail, the extraction changed behavior. The
  delegates must preserve original method semantics exactly.
- If compilation fails in `ReleaseVirtualMachine`, check that
  `evaluate(Expression)` and `execute(Statement)` wrappers still exist
  as `protected` methods on `VirtualMachine`.
