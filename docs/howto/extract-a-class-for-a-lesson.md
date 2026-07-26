---
title: Extract a Class for a Lesson
description: Shows a curriculum author how to export a single Alice class as a bounded, readable hybrid .a3c and import it into a fresh project without pulling in the whole scene.
last_updated: 2026-07-26
review_schedule: quarterly
owner: rabbithole-maintainers
doc_type: howto
---

# Extract a class for a lesson

Use single-class extraction when you want to lift **one** class out of a
curriculum project — a custom character behavior, a helper procedure type, a
puzzle controller — and hand it to students in a fresh project, **without**
dragging the whole scene's object graph along with it.

This is the core user story behind the within-archive hybrid Tweedle/XML format
([PLAN.md](https://github.com/asw101/alice-cmu/blob/main/tweedle-export/PLAN.md),
#973). The exported `.a3c` is *bounded*: it carries the extracted class as
compact, readable Tweedle source plus an authoritative XML fallback, and it
records only the class's declared dependencies — never the entire project.

## Contents

- [Before you start](#before-you-start)
- [Export the class](#export-the-class)
- [What the archive contains](#what-the-archive-contains)
- [Import into a fresh project](#import-into-a-fresh-project)
- [Resolve missing dependencies](#resolve-missing-dependencies)
- [Verify the extraction is bounded](#verify-the-extraction-is-bounded)
- [Troubleshooting](#troubleshooting)

## Before you start

| Requirement | Value |
| --- | --- |
| Source | A curriculum project (`.a3p`) open in Alice that contains the class you want |
| Target | A separate, fresh project to receive the class |
| Class | A `NamedUserType` you authored (not a built-in story-API type) |
| Output | A writable directory for the exported `.a3c` (defaults to the IDE types directory) |

You do not need to prepare the scene. Extraction deliberately leaves the scene
behind.

## Export the class

1. In the source project, open the class in the declarations editor so it is the
   active type.
2. Choose **Export Type…** (the export action backed by
   `ExportTypeToFileDialogOperation`, in the IDE export group).
3. Pick a destination and file name. The default name is `<ClassName>.a3c`.
4. Save.

The IDE writes a hybrid single-class archive via
`IoUtilities.writeType(file, type, …)`, attaching both the XML and JSON
gallery-tile summary sidecars so the class renders as a gallery tile without a
full AST load.

## What the archive contains

The `.a3c` is a ZIP with an **additive** hybrid layout (full reference:
[the hybrid Tweedle/XML archive format](../tweedle-hybrid-format.md)):

| Entry | Role |
| --- | --- |
| `src/<ClassName>.twe` | The extracted class as bounded Tweedle source. References to other user types are emitted as **bare names**, not inlined graphs — this is what keeps the export bounded. |
| `type.xml` | The authoritative legacy XML AST payload — the fallback the reader uses if any type hits a decoder gap. |
| `manifest.json` | Format metadata plus the class's `TypeReference`, including its bounded `dependencies` list (PLAN P3). |
| `typeSummary.json` / `typeSummary.xml` | Gallery-tile summary (name, hierarchy, procedure/function/field info). |
| `resources.xml`, `resources/…` | Any image/audio/model resources the class binds. |

The `dependencies` list is computed by a bounded crawl
(`CrawlPolicy.INCLUDE_REFERENCES_BUT_DO_NOT_TUNNEL`, self excluded): it names the
other user types the class references **without** bundling their source.

### Archive size

The hybrid archive carries both the Tweedle and XML payloads, so it is larger
than a hypothetical XML-only class file — but only modestly, because the compact
Tweedle source and the small JSON/XML summaries are dwarfed by the verbose XML
AST they accompany. For a representative small class the additive Tweedle +
summary overhead is on the order of **~15–25 %** of the XML payload
(≈ 1.25× total, uncompressed); ZIP compression narrows the gap further. This is
measured and guarded by `HybridArchiveSizeOverheadCharacterizationTest`
(see [archive size overhead](../tweedle-hybrid-format.md#archive-size-overhead)).

## Import into a fresh project

1. Open (or create) the destination project.
2. Open the Gallery browser and import the `.a3c` you exported.
3. The class appears as a gallery tile (rendered from the JSON summary — no AST
   load required) and is added to the destination project.

Only the extracted class comes across. The source project's scene, camera, and
unrelated classes are **not** imported.

## Resolve missing dependencies

If the class references other user types (recorded in its manifest
`dependencies`), the IDE resolves each name against the destination project and
the story-API types on import:

- **All dependencies present** → the class imports cleanly.
- **Some dependencies missing** → the IDE prompts before importing, so you can
  decide whether to continue (the missing references become placeholders you
  resolve later) or cancel and add the dependencies first.

The archive advertises exactly the same dependency set the import prompt checks —
this parity is locked by `ImportDependencyExportParityTest`, so an archive can
never claim a different dependency set than the prompt validates.

## Verify the extraction is bounded

To confirm an extraction stays bounded (only the declared dependencies cross,
never the whole graph), the behavior is protected headlessly by:

- `ImportDependencyExportParityTest` (`core/ide`) — the persisted manifest
  `dependencies` equal the import resolver crawl, the referenced type's source is
  **not** bundled, and importing into an empty project flags exactly the declared
  dependency.
- `TweedleCorpusRoundTripCharacterizationTest` (`core/story-api-migration`) — the
  round-trip census across the curriculum corpus.

## Troubleshooting

| Symptom | Cause | What to do |
| --- | --- | --- |
| Import prompts about missing types | The class declares dependencies not present in the destination | Continue with placeholders, or cancel and add the dependencies first |
| Class opens but some code reads as XML-migrated | The class hit a Tweedle **decoder gap**, so the reader used the authoritative XML fallback | Expected and safe — see [decoder gaps](../tweedle-decode-gaps.md). The class is fully functional; the bounded Tweedle path activates as gaps close |
| Exported archive is XML-only (no `src/*.twe`) | The Tweedle encode step failed, so the writer degraded to XML-only | Also safe — hybrid exports are never worse than legacy. Report the class so the encode gap can be closed |

## Related

- [The hybrid Tweedle/XML archive format](../tweedle-hybrid-format.md)
- [Tweedle decoder gaps and the corpus census](../tweedle-decode-gaps.md)
