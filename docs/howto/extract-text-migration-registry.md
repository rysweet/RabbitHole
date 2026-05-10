# Extract Text Migration Data from ProjectMigrationManager

Use this guide to understand, verify, or extend the text migration registry
extraction from `ProjectMigrationManager`.

## Contents

- [Prerequisites](#prerequisites)
- [Understand the split strategy](#understand-the-split-strategy)
- [Verify the extraction](#verify-the-extraction)
- [Add a new text migration](#add-a-new-text-migration)
- [Review a sub-registry change](#review-a-sub-registry-change)
- [Troubleshoot checkstyle failures](#troubleshoot-checkstyle-failures)

## Prerequisites

Work in the migration package:

```text
core/story-api-migration/src/main/java/org/lgna/project/migration/
```

Initialize the grammar submodule from a fresh checkout or worktree:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

## Understand the split strategy

The original `ProjectMigrationManager.java` was 5702 lines. The checkstyle
`FileLength` limit is 2000 lines. The text migration data alone was ~5577
lines — too large for any single file.

The data splits into five files based on natural version boundaries:

| File | Migrations | Why this boundary |
| --- | --- | --- |
| `TextMigrationRegistrySmallVersions` | 1–7 (3.1.8–3.1.33) and 9–13 (3.1.35–3.1.58) | Small entries with plain string pairs and no helper imports. |
| `TextMigrationRegistryV3134` | 8 (3.1.34) | Single largest migration (~1951 data lines) with joint accessor/ID patterns and biped/quadruped/flyer strings. |
| `TextMigrationRegistryV3159` | 14 (3.1.59) | Second-largest migration (~1869 data lines) with `createMoreSpecificFieldPattern` helper calls and `NO_REPLACEMENT` sentinels. |
| `TextMigrationRegistryLateVersions` | 15–28 (3.1.68–3.9.0) | Later versions including the `createVersion3_2_110TextMigration()` factory call. |
| `TextMigrationRegistry` | (assembler) | Concatenates the five sub-arrays into a single `TextMigration[]`. |

The early migrations (1–7) and mid-range migrations (9–13) are grouped in
`SmallVersions` because they are all short entries that collectively fit under
500 lines. Migration 8 and migration 14 each need their own file because they
individually approach the 2000-line limit.

## Verify the extraction

Run the 11 existing characterization tests that guard migration behavior:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.migration.ProjectMigrationManagerTest \
  test
```

The tests verify:

| Test | What it guards |
| --- | --- |
| Text migration version ordering | Entries are strictly increasing by result version. |
| AST migration version ordering | AST entries follow the same invariant. |
| Version-gated applicability | A migration does not back-apply to its own threshold version. |
| Legacy name cascade | The dresser resource name migrates through intermediate names to the final `DresserResource`. |
| Mixed-name rewrite | Representative legacy story/resource names rewrite from `3.1.19.0.0`. |
| Already-at-threshold unchanged | Text at the threshold version is not modified. |

If all 11 tests pass, the extraction preserves migration behavior.

Run the full module tests to check for compilation and downstream effects:

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  test
```

## Add a new text migration

When adding a new `TextMigration` entry to the registry:

1. **Choose the correct sub-registry.** New versions after 3.9.0 go in
   `TextMigrationRegistryLateVersions`. If that file approaches 2000 lines,
   create a new sub-registry (e.g., `TextMigrationRegistryV40Plus`) and update
   `TextMigrationRegistry.createAll()` to include it.

2. **Append to the array.** Add the new `TextMigration(...)` as the last element
   in the sub-registry's `create()` method. Text migrations must be ordered by
   strictly increasing result version.

3. **Update the assembler count.** If the total entry count changes,
   `TextMigrationRegistry.createAll()` does not need manual count updates — the
   `System.arraycopy` sizes are computed from the sub-array lengths.

4. **Run tests.** The version-ordering test will catch out-of-order entries. The
   existing cascade tests will catch regressions in known migration paths.

## Review a sub-registry change

When reviewing a pull request that modifies a sub-registry:

1. **Diff only the changed sub-registry.** Each sub-registry covers a disjoint
   version range, so changes to one file cannot affect migrations in another
   version range.

2. **Check regex fidelity.** Patterns must be copied byte-for-byte from the
   original source. No re-escaping, no whitespace normalization.

3. **Check `NO_REPLACEMENT` sentinels.** Some migration entries include
   `TextMigration.NO_REPLACEMENT` as a replacement value. These must remain in
   their original positions.

4. **Check helper call imports.** Each sub-registry imports only the helpers it
   uses. `TextMigrationRegistryV3134` uses joint accessor/ID and biped/flyer/
   quadruped helpers. `TextMigrationRegistryV3159` uses more-specific-field
   helpers. `TextMigrationRegistrySmallVersions` uses no helpers.

5. **Run the focused tests.** The 11 characterization tests are the primary
   regression guard.

## Troubleshoot checkstyle failures

If a sub-registry exceeds the 2000-line `FileLength` limit:

1. **Trim decorative blank lines.** Each `TextMigration(...)` constructor call
   may have blank lines between entries that can be removed without changing
   behavior.

2. **Use wildcard imports.** The checkstyle configuration does not enforce
   `AvoidStarImport`. Replace 9+ individual static imports with a wildcard
   import to save lines in large sub-registries.

3. **Split the sub-registry.** If trimming is not enough, split along a version
   boundary and add the new sub-array to `TextMigrationRegistry.createAll()`.

The `checkstyleSuppression.xml` file no longer contains a `FileLength`
suppression for any file in the migration package. Do not add one back; split
the file instead.
