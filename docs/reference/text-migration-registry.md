# TextMigrationRegistry Extraction

This reference describes the `TextMigrationRegistry` class hierarchy that holds
the text migration data formerly embedded in `ProjectMigrationManager`.

## Contents

- [Package](#package)
- [Motivation](#motivation)
- [Architecture](#architecture)
- [File inventory](#file-inventory)
- [API reference](#api-reference)
- [Configuration](#configuration)
- [Compatibility rules](#compatibility-rules)
- [Examples](#examples)
- [Non-claims](#non-claims)

## Package

All new classes live in the same package as `ProjectMigrationManager`:

```text
core/story-api-migration/src/main/java/org/lgna/project/migration/
```

The classes are package-private. Only `ProjectMigrationManager` references them.

## Motivation

`ProjectMigrationManager.java` was 5702 lines, of which 97.8% was a final
`TextMigration[]` field initializer spanning lines 74–5650. The checkstyle `FileLength`
check (2000 lines) was suppressed for this file. The migration data is pure
declarative content: ordered pairs of regex patterns and replacement strings
grouped by Alice version.

Extracting the data into a dedicated registry:

1. Reduces `ProjectMigrationManager.java` to under 200 lines of behavioral code.
2. Removes the checkstyle `FileLength` suppression.
3. Makes the migration data independently navigable by version range.
4. Preserves identical runtime behavior — the same `TextMigration[]` array is
   assembled in the same order and returned from the same `getTextMigrations()`
   method.

## Architecture

The extraction splits the single 5577-line array into five classes:

```text
ProjectMigrationManager
  └─ getTextMigrations()
       └─ TextMigrationRegistry.createAll()
            ├─ TextMigrationRegistrySmallVersions.createEarly()  (mig 1–7)
            ├─ TextMigrationRegistryV3134.create()               (migration 8)
            ├─ TextMigrationRegistrySmallVersions.createMid()    (mig 9–13)
            ├─ TextMigrationRegistryV3159.create()               (migration 14)
            └─ TextMigrationRegistryLateVersions.create()        (mig 15–28)
```

Each leaf method returns a `TextMigration[]` covering a contiguous version range.
`TextMigrationRegistry.createAll()` concatenates the five sub-arrays with
`System.arraycopy` into a single array that is identical in content and ordering
to the original inline array.

The split boundaries are chosen so that every file stays under the 2000-line
checkstyle `FileLength` limit without needing a suppression.

## File inventory

| File | Lines (approx) | Content |
| --- | --- | --- |
| `TextMigrationRegistry.java` | ~75 | Assembler: concatenates sub-arrays via `System.arraycopy`. |
| `TextMigrationRegistrySmallVersions.java` | ~490 | Migrations 1–7 (versions 3.1.8–3.1.33) and 9–13 (versions 3.1.35–3.1.58). Plain string pairs only; no helper imports beyond `Version` and `TextMigration`. |
| `TextMigrationRegistryV3134.java` | ~1998 | Migration 8 (version 3.1.34): the largest single `TextMigration` entry with ~1900 regex replacement pairs for joint accessor, joint ID, biped/quadruped/flyer strings, and `NO_REPLACEMENT` sentinels. |
| `TextMigrationRegistryV3159.java` | ~1922 | Migration 14 (version 3.1.59): the second-largest entry with `createMoreSpecificFieldPattern` / `createMoreSpecificFieldReplacement` helper calls and `NO_REPLACEMENT` sentinels. |
| `TextMigrationRegistryLateVersions.java` | ~1378 | Migrations 15–28 (versions 3.1.68–3.9.0) including the `createVersion3_2_110TextMigration()` factory call. |
| `ProjectMigrationManager.java` | ~110 | Behavioral code: singleton, `getTextMigrations()` delegating to registry, `getAstMigrations()`, AST migration array. |

Total line count across all files is comparable to the original single file. The
difference is that no individual file exceeds 2000 lines.

## API reference

### `TextMigrationRegistry`

```java
// Package-private — not part of the public API.
class TextMigrationRegistry {
    static TextMigration[] createAll();
}
```

`createAll()` returns a new `TextMigration[]` array containing every text
migration in version order. The array is structurally identical to the original
`ProjectMigrationManager.textMigrations` field.

Each call allocates a fresh array. `ProjectMigrationManager` stores the result
in its instance field, so `createAll()` runs exactly once per singleton
initialization.

### Sub-registry classes

Each sub-registry follows the same pattern:

```java
class TextMigrationRegistrySmallVersions {
    static TextMigration[] createEarly();  // migrations 1–7
    static TextMigration[] createMid();    // migrations 9–13
}
```

`TextMigrationRegistryV3134`, `TextMigrationRegistryV3159`, and
`TextMigrationRegistryLateVersions` each expose a single `create()` method.

Sub-registries are package-private and have no public API. They exist solely to
respect the checkstyle file-length limit.

### `ProjectMigrationManager.getTextMigrations()`

The method signature is unchanged:

```java
@Override
protected TextMigration[] getTextMigrations() {
    return this.textMigrations;
}
```

The `textMigrations` field is now initialized from
`TextMigrationRegistry.createAll()` instead of an inline array literal.

## Configuration

### Checkstyle

The `checkstyleSuppression.xml` entry that suppressed `FileLength` for
`ProjectMigrationManager.java` is removed:

```xml
<!-- REMOVED: no longer needed after extraction -->
<!-- <suppress checks="FileLengthCheck"
             files="ProjectMigrationManager.java"/> -->
```

All files in the migration package now comply with the 2000-line limit without
suppressions.

### Build

No new build configuration is required. The new classes are in the same Maven
module (`core/story-api-migration`) and the same package. They compile
automatically.

### Validation

From a fresh checkout or worktree, initialize the grammar submodule:

```bash
git submodule update --init tweedle-lang
```

Run the focused migration characterization tests:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.migration.ProjectMigrationManagerTest \
  test
```

Run the full story API migration module:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

Run the Python contract tests:

```bash
NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest discover -s tests
```

## Compatibility rules

1. The `TextMigration[]` returned by `getTextMigrations()` is byte-for-byte
   identical in content and ordering to the original inline array.
2. No regex pattern or replacement string is reformatted, re-escaped, or
   rewritten during extraction. Patterns are copied verbatim.
3. `NO_REPLACEMENT` sentinels remain in the same positions relative to their
   migration entry.
4. The `createVersion3_2_110TextMigration()` factory call remains in
   `TextMigrationRegistryLateVersions`, preserving its position in the array.
5. Helper calls from `ProjectMigrationTextSnippets` (e.g.,
   `createJointAccessorPattern`, `createMoreSpecificFieldPattern`) remain
   unchanged and are imported by the sub-registry that uses them.
6. The split boundaries do not fall in the middle of a `TextMigration`
   constructor call. Each `TextMigration(...)` entry lives entirely within one
   sub-registry file.
7. All 11 existing `ProjectMigrationManagerTest` methods pass without
   modification.
8. The existing characterization contracts — version ordering, version-gated
   applicability, legacy dresser cascade, mixed-name rewrite — remain valid
   because the runtime migration array is unchanged.

## Examples

### Before extraction

```java
public class ProjectMigrationManager extends AbstractMigrationManager {
    private final TextMigration[] textMigrations = {
        new TextMigration(new Version("3.1.8.0.0")),
        new TextMigration(new Version("3.1.9.0.0"), "oldA", "newA"),
        // ... 5575 more lines of data ...
    };

    @Override
    protected TextMigration[] getTextMigrations() {
        return this.textMigrations;
    }
}
```

### After extraction

```java
public class ProjectMigrationManager extends AbstractMigrationManager {
    private final TextMigration[] textMigrations =
        TextMigrationRegistry.createAll();

    @Override
    protected TextMigration[] getTextMigrations() {
        return this.textMigrations;
    }
}
```

### Registry assembler

```java
class TextMigrationRegistry {
    static TextMigration[] createAll() {
        TextMigration[] early = TextMigrationRegistrySmallVersions.createEarly();
        TextMigration[] v3134 = TextMigrationRegistryV3134.create();
        TextMigration[] mid   = TextMigrationRegistrySmallVersions.createMid();
        TextMigration[] v3159 = TextMigrationRegistryV3159.create();
        TextMigration[] late  = TextMigrationRegistryLateVersions.create();

        TextMigration[] all = new TextMigration[total];
        // System.arraycopy in order: early, v3134, mid, v3159, late
        return all;
    }
}
```

The exact concatenation order preserves the original array ordering.

### Migration behavior unchanged

```java
// This still works identically:
String migrated = ProjectMigrationManager.getInstance()
    .migrate(source, new Version("3.1.19.0.0"));
// Dresser cascade: dresser.DresserCentralAsian → prop.DresserResource
```

## Non-claims

This extraction does not:

- Change any migration behavior, ordering, or version boundaries.
- Add, remove, or modify any `TextMigration` entry.
- Alter the public API of `ProjectMigrationManager`.
- Affect AST migrations (they remain in `ProjectMigrationManager`).
- Introduce new runtime dependencies or configuration.
- Require changes to any test file.
- Expose any previously package-private API.
