# The hybrid Tweedle/XML archive format

This page describes the **within-archive hybrid** layout used by the readable
Alice archive formats — `.a3p` (project) and `.a3c` (single class) — introduced
by the Tweedle-export plan
([PLAN.md](https://github.com/asw101/alice-cmu/blob/main/tweedle-export/PLAN.md),
tracked under #973). It is the reference for the archive contents, the manifest,
the `src/*.twe` layout, and the **fallback contract** that guarantees hybrid
archives are *never worse than* the legacy XML format.

## Design goal

Let a curriculum author extract a **single class** from a project as bounded,
human-readable Tweedle source — without dragging the whole scene's object graph
into the archive — while keeping every existing Alice archive readable. The
representation is therefore *additive*: a hybrid archive carries **both** the
legacy XML AST payload **and** a per-type Tweedle representation, and the reader
prefers Tweedle but transparently falls back to XML whenever Tweedle cannot yet
represent a type faithfully.

## Archive layout

A hybrid `.a3p`/`.a3c` is a ZIP container. Entries:

| Entry | Producer | Purpose |
| --- | --- | --- |
| `manifest.json` | both | The single, XML-authoritative manifest: format/version metadata, the list of `TypeReference`s (each with its `src/*.twe` file name and, since PLAN P3, its bounded `dependencies`), and `ResourceReference`s. |
| `version.txt` | both | Archive format version. |
| `programType.xml` / `type.xml` | XML side | The **legacy XML AST payload** — the authoritative fallback. `programType.xml` for projects, `type.xml` for single-class `.a3c`. |
| `resources.xml` | XML side | The authoritative resource descriptor. The XML side owns the shared resource-binary namespace because it persists **every** resource type (the Tweedle side only carries image/audio/model resources). |
| `resources/…` | XML side | Resource binaries (images, audio, model data). |
| `src/<Type>.twe` | Tweedle side | Per-`NamedUserType` **Tweedle source**. References to other user types are emitted as **bare names**, not inlined object graphs — this is what makes single-class extraction bounded. |
| `models/…` | Tweedle side | Model data emitted by the Tweedle writer. |
| `typeSummary.xml` / `typeSummary.json` | IDE (`.a3c`) | Gallery-tile summary sidecar (type name, hierarchy, resource/procedure/function/field info). Since PLAN P4 the JSON sidecar lets the gallery render tiles **without** loading the AST; the XML sidecar remains for legacy `.a3c`. |

The manifest's image/audio references are **repointed** to the XML resource entry
names (matched by UUID) so both the Tweedle and XML read paths resolve resource
binaries from the same, single set of bytes.

## Writer

The hybrid writer (`HybridProjectIo.HybridProjectWriter`) composes the existing
`XmlProjectIo` and `JsonProjectIo` writers:

1. Each writer runs to an in-memory ZIP.
2. Their entries are merged into one archive. The **XML side is authoritative**
   for `manifest.json`, `version.txt`, `resources.xml`, and `resources/`; the
   Tweedle side contributes `src/*.twe` and `models/`.
3. Image/audio manifest references are repointed to the XML resource entries.
4. **Degrade-safe:** if the Tweedle encode step fails for any reason, the writer
   emits an **XML-only** archive. Exports are therefore never worse than the
   legacy format.

### Bounded dependency manifest (PLAN P3)

Each `TypeReference` in `manifest.json` records a `dependencies` list: the bare
names of the other user types the type references, collected by a bounded crawl
(`CrawlPolicy.INCLUDE_REFERENCES_BUT_DO_NOT_TUNNEL`, self excluded). On single-class
import the IDE resolves these against the destination project + story-API types
and prompts before importing a class whose dependencies are missing. The field is
serialized `NON_EMPTY`, so dependency-free/legacy archives stay byte-identical.

## Reader and the fallback contract

The hybrid reader (`HybridProjectIo.HybridProjectReader`) **prefers Tweedle**, but
the guarantee is that reading is *never worse than today*:

- **Tweedle path (bounded):** used only when **every** manifest-declared type
  decodes **and** every resource the archive declares is recovered. Then the XML
  AST is never decoded.
- **XML fallback:** if **any** type hits a decoder gap (see
  [`tweedle-decode-gaps.md`](tweedle-decode-gaps.md)), or the Tweedle read
  otherwise fails or drops a declared resource, the reader transparently falls
  back to the legacy XML payload **in the same archive**.

The fallback is **whole-archive, not per-type**. Mixing Tweedle-decoded and
XML-decoded types into one project cannot preserve the object identity that
cross-type references rely on, so any single gap routes the entire read through
the (loud, migration-aware) XML reader.

### Completeness gate (resource recovery)

Even when all types decode, the reader still falls back if the Tweedle read does
not recover every resource declared in `resources.xml`. Today the only concrete
`Resource` subclasses (`ImageResource`, `AudioResource`) are both recovered, so
the gate never falsely fires; it is a forward-looking guard against a future
resource subtype being silently dropped. See
[`tweedle-decode-gaps.md`](tweedle-decode-gaps.md) for details.

## Current decode coverage

The bounded Tweedle path is exercised by the characterization tests
`SilverThreadTweedleDecoderRoundTripTest` (single fixture) and
`TweedleCorpusRoundTripCharacterizationTest` (full 34-project curriculum corpus).
As of this writing, **0 %** of curriculum types decode via Tweedle — every type
falls back to XML — dominated by the constructor-body and argument-bearing
`this(...)` decoder gaps. The fallback contract means this is fully functional
today; closing the Phase 5 gaps progressively shifts types onto the bounded path
with no reader change. The live census and Phase 5 backlog live in
[`tweedle-decode-gaps.md`](tweedle-decode-gaps.md).

## Related

- [`tweedle-decode-gaps.md`](tweedle-decode-gaps.md) — decoder-gap backlog and the
  corpus census.
- `core/story-api-migration/.../io/HybridProjectIo.java` — writer/reader.
- `core/story-api-migration/.../io/JsonResourceEntryWriter.java` — Tweedle
  manifest + `dependencies` population.
- `core/ide/.../ast/export/type/TypeSummaryReader.java` — gallery-tile summary
  (JSON-first, XML-fallback).
