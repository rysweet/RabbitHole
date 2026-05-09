# Migration Hotspot Characterization

Migration hotspot characterization is the documentation and test layer that
protects existing `ProjectMigrationManager` text migration behavior before the
migration table is extracted, split, or refactored.

It locks down observable compatibility: when a saved Alice project version enters
the migration pipeline, the same final class and resource names come out. The
layer uses generated XML-string fixtures in Java source. It does not commit
binary project archives, media resources, or Git LFS payloads.

## Why it exists

Alice project files store serialized Java class names in XML attributes such as
`type` and `declaringClass`. Over many releases, resource packages were renamed,
resource classes were consolidated, fields were renamed, and accessors were
changed. `ProjectMigrationManager` applies these rewrites in version order so
that a project saved in an old Alice version opens correctly in a newer version.

That migration table is a modernization hotspot because:

1. It is a large, ordered, side-effect-free data structure that invites
   extraction into smaller modules.
2. Ordering matters — each applied migration advances the working version, so
   later migrations see earlier output.
3. Version gating matters — a migration applies only when the saved project
   version is strictly older than the migration result version.
4. Intermediate class names exist — some old names cascade through two or three
   migrations before reaching the current resource class.
5. Boundary rewrites exist — some migration entries have a strict version gate
   that must not back-apply to projects already at or beyond the boundary.

Without characterization coverage, a refactor that reorders entries, drops an
intermediate mapping, or shifts a version gate can silently break old project
loading.

## How it fits into the modernization approach

The characterization layer follows the repository convention: **add
characterization tests before refactoring behavior.** It does not rewrite
production code, add a new migration framework, or require runtime
configuration.

The durable artifact stack is:

| Layer | Artifact | Role |
| --- | --- | --- |
| Concepts (this document) | `docs/concepts/migration-hotspot-characterization.md` | Explains why the layer exists and what it protects. |
| Reference | [`docs/reference/project-migration-manager-characterization.md`](../reference/project-migration-manager-characterization.md) | Full contract, API reference, configuration, compatibility rules, and examples. |
| How-to | [`docs/howto/characterize-project-migration-manager.md`](../howto/characterize-project-migration-manager.md) | Step-by-step checklist for adding or reviewing migration characterization. |
| Tutorial | [`docs/tutorials/project-migration-manager-characterization.md`](../tutorials/project-migration-manager-characterization.md) | Guided example walking through one concrete migration seam. |
| Executable tests | `ProjectMigrationManagerTest` in `core/story-api-migration` | 12 focused Java characterization tests. |
| Repository contract | `tests/test_pr424_migration_hotspot_recovery_contract.py` | Python policy checks for diff scope, doc links, conflict markers, and test method presence. |

## What it protects

The characterization covers six kinds of observable behavior:

1. **Table ordering** — Text and AST migration result versions are valid,
   round-trippable, and strictly increasing.
2. **Version gating** — A migration with result version `3.1.20.0.0` applies to
   a `3.1.19.0.0` project but not to `3.1.20.0.0` or later.
3. **Multi-step cascades** — A legacy dresser class name that starts at
   `org.lgna.story.resources.dresser.DresserCentralAsian` reaches
   `org.lgna.story.resources.prop.DresserResource` through intermediate package
   and resource-class names in one `migrate(String, Version)` call.
4. **Boundary rewrites** — The `3.2.111.0.0` BonePile rewrite applies to
   `3.2.110.0.0` source text and is not back-applied to the selected source
   text at `3.2.111.0.0` or `3.2.112.0.0`.
5. **Field and accessor rewrites** — Selected legacy joint fields and accessor
   methods reach their current names from historical versions.
6. **Current-version guard** — The compiled current version has no pending text
   or AST migrations.

## What it does not protect

The characterization is deliberately narrow:

- It does not characterize every entry in the migration table.
- It does not exercise AST migrations (only text migrations are characterized).
- It does not use binary `.a3p`, `.a3w`, or `.a3c` archives.
- It does not prove rendering, Save, grading, Sims, installer, or lesson
  correctness.
- It does not add runtime configuration, logging levels, or diagnostics beyond
  the existing optional `TextMigration` sanity-check property.

## Relationship to other documentation

The [formal specification lane](./formal-spec-lane.md) covers save, load,
export, and backup recovery contracts using Gherkin scenarios and a TLA+ model.
Migration hotspot characterization is complementary: it protects the text
migration pipeline that runs **during** project loading, before the formal-spec
lane's archive-level contracts apply.

The [project IO corpus characterization](../reference/project-io-corpus-characterization.md)
covers generated archive round-trips. Migration hotspot characterization is
narrower: it protects individual text rewrites without requiring archive
fixtures.

## Safe refactoring rule

A refactor of `ProjectMigrationManager` is safe when the same source version
produces the same final migrated text for every characterized seam. The
executable tests enforce this by asserting both expected final names and the
absence of obsolete or intermediate names.
