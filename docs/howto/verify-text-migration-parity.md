# Verify Text Migration Registry Parity

Use this workflow when changing `core/story-api-migration` text migration
registries, the migration JSON resource, or the tests that characterize them.
The goal is safety only: preserve Alice 3 migration behavior while keeping the
JSON registry and legacy registry classes provably equivalent.

## Quick start

From the repository root:

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
git diff --exit-code -- core/story-api-migration/src/main/resources/migrations/text-migrations.json
```

The focused Maven command runs the parity lane. The final `git diff` command
confirms the committed generated JSON was not hand-edited or dirtied by local
verification.

## When to run this workflow

Run the parity lane when a change touches any of these files:

```text
core/story-api-migration/src/main/java/org/lgna/project/migration/TextMigration*.java
core/story-api-migration/src/main/resources/migrations/text-migrations.json
core/story-api-migration/src/test/java/org/lgna/project/migration/TextMigration*.java
```

For broader migration work, run the focused parity lane first, then run the full
module lane:

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

## Verify generated migration JSON safely

`text-migrations.json` is generated data. Do not edit it by hand.

When the legacy registry definitions intentionally change, run the approved
generator validation path. It writes canonical JSON to a temporary file, compares
that output with the committed resource, then proves the generated JSON still
loads to the legacy registry definitions:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=TextMigrationJsonGeneratorTest \
  test
git diff -- core/story-api-migration/src/main/resources/migrations/text-migrations.json
```

The command should leave the resource unchanged for parity-only work. If the
resource must change in a separate migration-content change, accept the JSON diff
only when it is generated output and the registry, loader, and generator tests
pass together. Formatting-only, manual, reordered, or partial JSON edits are not
valid.

## Add or change a migration safely

1. Update the authoritative legacy registry class that owns the migration
   version segment.
2. Run `TextMigrationJsonGeneratorTest` to regenerate canonical JSON.
3. Run `TextMigrationRegistryTest`, `TextMigrationJsonLoaderTest`, and
   `TextMigrationJsonGeneratorTest` together.
4. If `text-migrations.json` changes, confirm it changed only through generator
   output.
5. Keep the change scoped to migration parity unless the behavior change has its
   own characterization tests and review path.

## What the parity lane proves

| Test | Contract |
| --- | --- |
| `TextMigrationRegistryTest` | `TextMigrationRegistry.createAll()` keeps the expected count, version order, boundaries, sub-registry assembly order, fresh-array behavior, and ordered source/replacement pair parity with the legacy registries. |
| `TextMigrationJsonLoaderTest` | The classpath JSON produces the same ordered versions and source/replacement pairs as the concatenated legacy registries, including representative resolved pairs and loader edge-case characterization. |
| `TextMigrationJsonGeneratorTest` | The generator validation path serializes the legacy registries to temporary canonical JSON, compares it with committed `text-migrations.json`, and confirms generated JSON loads back to the legacy definitions. |

## Boundaries of this lane

The focused lane is a parity guard, not a behavior-change lane. It includes
malformed JSON, duplicate entry, empty list, and required-field characterization
for the current loader behavior, but it does not change loader error handling or
add fallback behavior. Shared reflection-based pair extraction lives in
`TextMigrationParityTestSupport` and stays test-scope only.

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| Maven reports missing Tweedle parser classes | Run `git submodule update --init tweedle-lang`, then confirm `tweedle-lang/Grammar` exists. |
| `TextMigrationJsonGeneratorTest` writes a resource diff | Review the `text-migrations.json` diff and confirm the legacy registry change is intentional. |
| `TextMigrationRegistryTest` reports an order mismatch | Check the legacy concatenation order: early small versions, `V3134`, mid small versions, `V3159`, then late versions. |
| `TextMigrationJsonLoaderTest` reports pair or version drift | Compare the JSON order and replacement pairs with the legacy registry definitions. |

## Related reference

See [Text Migration Registry Parity Reference](../reference/text-migration-registry-parity.md)
for the JSON format, registry entry points, system property, and test contracts.
