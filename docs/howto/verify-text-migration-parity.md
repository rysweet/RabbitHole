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
  -Dtest=TextMigrationJsonGeneratorTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

This is the strict text migration JSON drift check. It regenerates JSON from the
legacy registry definitions in memory, reads the committed
`core/story-api-migration/src/main/resources/migrations/text-migrations.json`
resource as UTF-8 text, compares the two UTF-8 strings exactly, and exits
non-zero when the committed JSON is stale. The strict check is read-only: it
does not modify `text-migrations.json` when JSON is current or stale.

## When to run this workflow

Run this workflow when a change touches any of these files:

```text
core/story-api-migration/src/main/java/org/lgna/project/migration/TextMigration*.java
core/story-api-migration/src/main/resources/migrations/text-migrations.json
core/story-api-migration/src/test/java/org/lgna/project/migration/TextMigration*.java
```

For broader migration work, run the strict drift check first, then run the full
text migration parity lane:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=TextMigrationRegistryTest,TextMigrationJsonLoaderTest,TextMigrationJsonGeneratorTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

Run the full module lane when the change is larger than registry parity:

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

## Verify generated migration JSON strictly

`text-migrations.json` is generated data. Do not edit it by hand.

The default `TextMigrationJsonGeneratorTest` path is the strict, read-only drift
check:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dtest=TextMigrationJsonGeneratorTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
```

The strict path performs all validation in test memory or temporary files:

1. Serialize the legacy registry sequence to deterministic pretty-printed JSON.
2. Read the committed `text-migrations.json` resource as UTF-8 text.
3. Compare generated and committed JSON with exact UTF-8 string comparison.
4. Verify generated JSON loads back to the same legacy registry definitions.
5. Verify the committed resource remains loadable through the default runtime
   JSON registry path.

When strict verification fails, the test reports drift against the repo-relative
resource path and reminds developers that the write property is only for
explicit regeneration.

## Regenerate generated migration JSON explicitly

Use the write property only when the legacy registry definitions intentionally
changed and the committed JSON must be refreshed:

```bash
export NODE_OPTIONS=--max-old-space-size=32768
git submodule update --init tweedle-lang
mvn -pl core/story-api-migration -am \
  -DincludeSims=false \
  -Dinstall4j.skip \
  -Dcheckstyle.skip \
  -Djava.awt.headless=true \
  -Dorg.lgna.project.migration.TextMigrationJsonGenerator.write=true \
  -Dtest=TextMigrationJsonGeneratorTest \
  -Dsurefire.failIfNoSpecifiedTests=false \
  test
git diff -- core/story-api-migration/src/main/resources/migrations/text-migrations.json
```

`-Dorg.lgna.project.migration.TextMigrationJsonGenerator.write=true` is the only
write gate for this resource. It writes canonical JSON generated from the legacy
registries to the committed resource path, then the same generator test verifies
the refreshed file. Keep the JSON diff only when it is the intended generated
output of a migration-content change. Formatting-only, manual, reordered, or
partial JSON edits are not valid.

## Add or change a migration safely

1. Update the authoritative legacy registry class that owns the migration
   version segment.
2. Run the strict `TextMigrationJsonGeneratorTest` command. It must fail if the
   committed generated JSON is stale and must not write files.
3. If the legacy registry change is intentional, run the explicit regeneration
   command and inspect the generated JSON diff.
4. Run `TextMigrationRegistryTest`, `TextMigrationJsonLoaderTest`, and
   `TextMigrationJsonGeneratorTest` together.
5. If `text-migrations.json` changes, confirm it changed only through generator
   output.
6. Keep the change scoped to migration parity unless the behavior change has its
   own characterization tests and review path.

## Pull request gate

Text migration registry changes target `develop`. Before merging, the strict
`TextMigrationJsonGeneratorTest` lane must pass, the full text migration parity
lane must pass when registry behavior or data changed, and required CI checks on
the pull request must be green.

## What the parity lane proves

| Test | Contract |
| --- | --- |
| `TextMigrationRegistryTest` | `TextMigrationRegistry.createAll()` keeps the expected count, version order, boundaries, sub-registry assembly order, fresh-array behavior, and ordered source/replacement pair parity with the legacy registries. |
| `TextMigrationJsonLoaderTest` | The classpath JSON produces the same ordered versions and source/replacement pairs as the concatenated legacy registries, including representative resolved pairs and loader edge-case characterization. |
| `TextMigrationJsonGeneratorTest` | The strict command serializes the legacy registries in memory, compares generated JSON with committed `text-migrations.json` using exact UTF-8 string comparison, stays read-only, and confirms generated JSON loads back to the legacy definitions. The same test writes committed JSON only when the explicit write property is supplied through the regeneration command. |

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
| Strict `TextMigrationJsonGeneratorTest` fails with generated JSON drift | Do not hand-edit the resource. If the legacy registry change is intentional, run the explicit regeneration command and keep only the generated `text-migrations.json` diff. |
| `TextMigrationJsonGeneratorTest` writes a resource diff | Confirm the command included `-Dorg.lgna.project.migration.TextMigrationJsonGenerator.write=true`; without that property the strict path is read-only. |
| `TextMigrationRegistryTest` reports an order mismatch | Check the legacy concatenation order: early small versions, `V3134`, mid small versions, `V3159`, then late versions. |
| `TextMigrationJsonLoaderTest` reports pair or version drift | Compare the JSON order and replacement pairs with the legacy registry definitions. |

## Related reference

See [Text Migration Registry Parity Reference](../reference/text-migration-registry-parity.md)
for the JSON format, registry entry points, system property, and test contracts.
