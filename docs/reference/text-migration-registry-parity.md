# Text Migration Registry Parity Reference

This reference describes the maintained text migration registry contract in
`core/story-api-migration`. It covers the runtime JSON registry, the legacy
registry sources, the generator path, and the characterization tests that keep
them in parity.

## Components

| Component | Location | Contract |
| --- | --- | --- |
| `TextMigrationRegistry` | `core/story-api-migration/src/main/java/org/lgna/project/migration/` | Runtime assembler for all text migrations. It loads JSON by default and can use legacy registries for characterization and regeneration. |
| `TextMigrationJsonLoader` | `core/story-api-migration/src/main/java/org/lgna/project/migration/` | Loads `migrations/text-migrations.json` from the classpath and converts entries to `TextMigration` instances in file order. |
| Legacy registries | `TextMigrationRegistrySmallVersions`, `TextMigrationRegistryV3134`, `TextMigrationRegistryV3159`, `TextMigrationRegistryLateVersions` | Authoritative Java definitions used as the source for loader parity checks and JSON generation. |
| `text-migrations.json` | `core/story-api-migration/src/main/resources/migrations/` | Generated runtime migration table. It must be changed only through the approved generator path. |
| Parity test support | `TextMigrationParityTestSupport` | Test-only canonical extraction of ordered migration source/replacement pairs from runtime JSON, generated JSON, and legacy registry classes. |
| Parity tests | `TextMigrationRegistryTest`, `TextMigrationJsonLoaderTest`, `TextMigrationJsonGeneratorTest` | Characterization suite that protects order, count, version boundaries, loader pair parity, loader edge cases, and generator parity. |

## Runtime behavior

`TextMigrationRegistry.createAll()` is the production entry point used by the
story API migration pipeline. Its default behavior is:

1. Load `migrations/text-migrations.json` from the module classpath.
2. Convert each JSON entry to a `TextMigration`.
3. Return the migrations in JSON file order.

The registry returns a new array for each call. Migration ordering is part of the
compatibility contract because older Alice project text can pass through several
versioned rewrites before reaching the current class or resource name.

## Legacy registry order

The legacy registry sequence is authoritative for parity:

1. `TextMigrationRegistrySmallVersions.createEarly()`
2. `TextMigrationRegistryV3134.create()`
3. `TextMigrationRegistrySmallVersions.createMid()`
4. `TextMigrationRegistryV3159.create()`
5. `TextMigrationRegistryLateVersions.create()`

`TextMigrationRegistryTest` checks that `TextMigrationRegistry.createAll()`
preserves that order and performs the full ordered source/replacement pair
comparison against the same concatenated legacy sequence. `TextMigrationJsonLoaderTest`
performs the same classpath JSON parity check and covers loader edge cases.

## JSON format

`text-migrations.json` is an array of migration objects:

```json
[
  {
    "version": "3.1.34.0.0",
    "replacements": [
      {
        "pattern": "org.lgna.story.Program",
        "replacement": "org.lgna.story.SProgram"
      }
    ]
  }
]
```

| Field | Type | Required | Meaning |
| --- | --- | --- | --- |
| `version` | string | Yes | Alice result version passed to `new Version(...)`. |
| `replacements` | array | No | Ordered replacement entries for the version. Missing or `null` replacement arrays are treated as empty arrays. |
| `replacements[].pattern` | string | Yes | Regular expression pattern used by `TextMigration`. |
| `replacements[].replacement` | string or `null` | Yes | Replacement text. `null` maps to `MigrationManager.NO_REPLACEMENT` for no-op migrations where a pattern is recognized but the source text remains unchanged. |

The loader preserves JSON order. It does not sort, deduplicate, or silently skip
entries. Malformed JSON is surfaced as an unchecked IO failure, and a missing
classpath resource is surfaced as an illegal state.

## Configuration

Runtime Alice project loading requires no application configuration.

The registry has one package-private system property for test and regeneration
work:

```text
org.lgna.project.migration.TextMigrationRegistry.useLegacyRegistries
```

When set to `true`, `TextMigrationRegistry.createAll()` builds migrations from
the legacy Java registries instead of runtime JSON:

```bash
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dorg.lgna.project.migration.TextMigrationRegistry.useLegacyRegistries=true \
  -Dtest=TextMigrationJsonGeneratorTest \
  test
```

Use this property only for characterization and generator workflows. Do not use
it to change application behavior.

Local automation shells should keep the repository memory preference:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
```

## Test extraction

The migration tests share `TextMigrationParityTestSupport`, which uses
reflection over `TextMigration` to inspect private replacement pairs for
characterization and serialization.

The extracted data shape is:

| Field | Meaning |
| --- | --- |
| `version` | `TextMigration.getResultVersion().toString()` for the migration. |
| `pattern` | The original regular expression pattern for one replacement pair. |
| `replacement` | The original replacement string, including `null` for no-op migrations where a pattern is recognized but the source text remains unchanged. |
| Migration order | Array position from the loaded, generated, or legacy registry sequence. |
| Pair order | Array position inside a single `TextMigration`. |

The tests use that extraction for these checks:

1. Runtime JSON migrations versus the concatenated legacy registries.
2. Generated JSON versus committed `text-migrations.json`.
3. Generated JSON loaded back to the concatenated legacy registry sequence.

The generator test writes to a temporary file, compares the canonical JSON tree
with the committed resource, and verifies that generated JSON loads to the
legacy sequence. Reflection stays test-scope only. Production code should
continue to use `TextMigrationRegistry.createAll()`.

## Validation commands

Focused parity validation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=TextMigrationRegistryTest,TextMigrationJsonLoaderTest,TextMigrationJsonGeneratorTest \
  test
```

Full module validation:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  test
```

Generated JSON drift check:

```bash
git diff --exit-code -- core/story-api-migration/src/main/resources/migrations/text-migrations.json
```

## Safety rules

- Preserve Alice 3 behavior unless a separate behavior change is documented and
  characterized.
- Keep migration order stable unless the generator and parity tests prove an
  intentional legacy-registry change.
- Do not hand-edit `text-migrations.json`.
- Do not add silent loader fallbacks for malformed JSON, missing resources, or
  invalid required fields.
- Keep reflective pair extraction in test scope only.
- Keep changes inside `core/story-api-migration` unless a broader migration
  contract explicitly requires another module.
