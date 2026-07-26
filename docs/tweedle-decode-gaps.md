# Tweedle decoder-gap backlog

This backlog records the **Tweedle decode gaps** that currently force the hybrid
`.a3c`/`.a3p` reader to fall back to the legacy XML AST payload. It is the
Phase 0 census for the within-archive hybrid export/import work (see
`alice-cmu/tweedle-export/PLAN.md`) and drives the incremental decoder-closure
work (PLAN Phase 5).

Each gap below is a `NamedUserType` that **encodes** to Tweedle source but does
**not** yet **decode** back to an equivalent AST. Until a gap is closed, any
archive containing that type is read via the XML fallback — i.e. exports/imports
are *never worse than today*, but they also don't yet benefit from the bounded,
name-only Tweedle representation.

## How this census is produced

The census is the printed output of the headless characterization test
`SilverThreadTweedleDecoderRoundTripTest`
(`core/story-api-migration/src/test/java/org/lgna/project/io/SilverThreadTweedleDecoderRoundTripTest.java`),
which loads a real `.a3p`, encodes every `NamedUserType` to Tweedle, attempts to
decode it back, and documents each failure with its exact exception message. The
test passes green while gaps remain (it is a characterization test); as gaps are
closed, more types are automatically verified for structural round-trip equality.

Reproduce (headless, offline):

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-<arch> GIT_LFS_SKIP_SMUDGE=1
mvn -o -Djavafx.platform=linux -Dinstall4j.skip -Dcheckstyle.skip \
    -Djava.awt.headless=true \
    -pl core/story-api-migration surefire:test \
    -Dtest=SilverThreadTweedleDecoderRoundTripTest
```

## Census — `starters/indiaMinimum.a3p`

`0 / 6` types decode via Tweedle today. All six therefore route through the XML
fallback.

| Type | Gap category | Decoder message |
| --- | --- | --- |
| `Program` | Comments | `Tweedle comments are not yet supported by the AST decoder.` |
| `Scene` | Comments | `Tweedle comments are not yet supported by the AST decoder.` |
| `ElephantStable` | Constructor body | `Tweedle constructor bodies are not yet supported by the AST decoder: ElephantStable` |
| `Prop` | Constructor body | `Tweedle constructor bodies are not yet supported by the AST decoder: Prop` |
| `SandDunes` | Constructor body (arg-bearing `this` call) | `Tweedle argument-bearing explicit this method calls are not supported by the AST decoder: setSandDunesResource.this.setJointedModelResource` |
| `WaterTank` | Constructor body (arg-bearing `this` call) | `Tweedle argument-bearing explicit this method calls are not supported by the AST decoder: setWaterTankResource.this.setJointedModelResource` |

> **Census currency.** This table reflects the state after the resource-typed
> method-parameter gap was closed (see item 4 below). `SandDunes`/`WaterTank`
> previously failed at parameter-type resolution
> (`Unsupported Tweedle method parameter: TerrainResource`); resolving that gap
> revealed the deeper constructor-body statement gap they now report. `Program`
> and `Scene` currently fail first on the comment gap (item 5); their earlier
> reported messages (top-level parse / non-literal field initializer) sit behind
> that and will re-surface once comments are handled.

### Grouped by decoder capability to add (Phase 5 backlog)

1. **Comments** (`Program`, `Scene`, and any type containing a `Comment`
   statement). The Tweedle grammar routes `//` line comments and `/* … */` block
   comments to the lexer's hidden channel, so a `Comment` node the encoder emits
   is dropped on parse. Rather than decode to an AST that is silently missing the
   comment, the decoder rejects comment-bearing source. Closing it requires a
   first-class comment representation in `org.alice.tweedle.ast` (and grammar
   support in the `tweedle-lang` submodule), which is out of scope for the hybrid
   export work. Characterized separately by `TweedleCommentDecodeGapTest`
   (`core/ast/src/test/java/org/alice/serialization/tweedle/TweedleCommentDecodeGapTest.java`).
2. **Constructor bodies** (`ElephantStable`, `Prop`). The decoder does not yet
   reconstruct statements inside a user-type constructor body.
3. **Argument-bearing explicit `this` method calls in constructor bodies**
   (`SandDunes` → `this.setJointedModelResource(...)`, `WaterTank` likewise).
   Constructor-body decoding currently accepts only assignments and
   zero-argument `this`/same-class calls; a `this`-qualified call **with
   arguments** (as gallery models emit to set their jointed-model resource) is
   rejected. Closing this needs argument-expression decoding plus method
   resolution by name+signature on the current type (see
   `StatementDecoder.unsupportedArgumentBearingExplicitThisMethodCall`). This is
   the next-highest-leverage in-scope gap: it blocks the two resource-backed
   model types and overlaps the constructor-body work in item 2.
4. **Resource-typed method parameters** (`SandDunes` → `TerrainResource`,
   `WaterTank` → `WaterTankResource`). **CLOSED.** Gallery model resource enums
   live in per-category subpackages of `org.lgna.story.resources` (e.g.
   `org.lgna.story.resources.prop.TerrainResource`), but the Tweedle encoder
   emits them by simple name. `Decoder.JAVA_TYPE_PACKAGES` previously searched
   only `org.lgna.story.resources` (not its subpackages), so simple resource
   names failed to resolve. The decoder now also searches the resource
   subpackages (`aircraft`, `biped`, `fish`, `flyer`, `marinemammal`, `prop`,
   `quadruped`, `slitherer`, `train`, `watercraft`). This is purely additive —
   `resolveType` consults the terminals set and type aliases first, so the
   subpackage search only runs for names that would otherwise have thrown.
5. **Non-literal field initializers** (`Scene.ground`, once the comment gap is
   lifted). The decoder only accepts literal field initializers; field
   initializers that are expressions (e.g. a reference to another declaration)
   are rejected.
6. **Program type parse** (`Program`, once the comment gap is lifted). The
   top-level program type does not parse. Highest-leverage once reachable:
   closing it lets whole *projects* prefer the Tweedle read path.

## Corpus-wide census — `starter-projects/*.a3p`

PLAN Phase 6 (#992) generalises the single-fixture census above to the full,
licensed, in-repo curriculum corpus: the **34** `.a3p` starter projects checked
into `core/resources/src/application/resources/starter-projects/`. The
characterization test
`TweedleCorpusRoundTripCharacterizationTest`
(`core/story-api-migration/src/test/java/org/lgna/project/io/TweedleCorpusRoundTripCharacterizationTest.java`)
walks that directory, reads each project headlessly, encodes every
`NamedUserType` to Tweedle, attempts to decode it back, and reports per file how
many types import **bounded (Tweedle decode succeeds)** vs **fallback (decode
raises a gap → the reader routes through the XML payload)**.

Reproduce (headless, offline):

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-<arch> GIT_LFS_SKIP_SMUDGE=1
mvn -o -Djavafx.platform=linux -Dinstall4j.skip -Dcheckstyle.skip \
    -Djava.awt.headless=true \
    -pl core/story-api-migration surefire:test \
    -Dtest=TweedleCorpusRoundTripCharacterizationTest
```

### Result (current `develop`)

**29 of 34** projects are readable in a pure-headless harness; **5** are recorded
`UNREADABLE` (see below). Across the 29 readable projects:

| Metric | Value |
| --- | --- |
| Types exercised | **402** |
| Bounded (Tweedle decode) | **0** |
| Fallback (XML) | **402** |
| Bounded coverage | **0.0 %** |

Every type in the real curriculum corpus currently routes through the XML
fallback — consistent with the `indiaMinimum` census (`0 / 6`). This directly
validates the **"never worse than today"** contract: bounded Tweedle import is
not yet reachable for any curriculum type, but every archive still round-trips
via its in-archive XML payload. Bounded coverage will rise automatically as the
Phase 5 decoder gaps below are closed; the harness asserts only its own
invariants (corpus present, projects read, types exercised), never a specific
ratio, so it stays green and non-brittle as coverage improves.

### Corpus-wide decoder-gap distribution

The 402 fallbacks group into the same Phase 5 capabilities as the single-fixture
census, now weighted by real curriculum frequency:

| Count | Decoder capability to add |
| --- | --- |
| 168 | Argument-bearing explicit `this(...)` method calls |
| 165 | Constructor bodies |
| 49  | Comments |
| 11  | Top-level Tweedle type parse (`Program`) |
| 7   | Non-literal method return expressions |
| 2   | Unsupported method parameter type |

This weighting sharpens the Phase 5 priority order: **constructor bodies** and
**argument-bearing `this(...)` calls** together account for 333 / 402 (83 %) of
all curriculum fallbacks — closing those two capabilities would move the large
majority of curriculum types onto the bounded Tweedle path.

### `UNREADABLE` projects (harness limitation, not an export defect)

Five older-format projects — `iceFull`, `pacificnorthwest`, `snow`, `snowFull`,
`snowMinimum` — cannot be read by the *pure-headless* harness. They require a
model-resource migration step
(`ResourceTypeHelper.createInstanceCreation(...)`) whose helper is not wired up
outside the IDE, so the read raises `NullPointerException: ... this.helper is
null`. This is a **test-environment migration limitation**, not a hybrid-export
or decode defect: the same projects open normally in the IDE. The harness records
them as `UNREADABLE` and continues, so the census stays meaningful for the 29
projects that a headless reader can load.

**Root cause is deeper than a missing helper.** Wiring the real IDE helper
(`StorytellingResourcesTreeUtils.INSTANCE`, which lives in `core/ide` and is
unreachable from `core/story-api-migration` by a deliberate dependency wall)
into the reader — exactly as `AbstractFileProjectLoader` does at open time —
eliminates the `this.helper is null` failure but does **not** make these five
read headlessly. They then fail one step later with
`NullPointerException: ... "instantiation" is null`, because
`StorytellingResourcesTreeUtils.createInstanceCreation(...)` returns `null` when
the headless gallery tree has no node for a referenced art-gallery model
resource — the EA/Sims2 gallery assets are not resolvable in the headless,
LFS-skipped census environment. So full-corpus headless readability is gated by
**gallery-resource availability**, not merely helper wiring. This is pinned by
`StarterProjectCorpusResourceHelperReadabilityTest` (core/ide), which asserts,
non-brittly, that supplying the helper is non-regressive (still ≥ 29 readable)
and eliminates the `this.helper` cause, while tolerating the residual
gallery-resolution failures so it stays green whether or not the gallery assets
are present.

## Next fixtures to add

The PLAN Phase 0 "next fixtures" open question is now **resolved**: the licensed
curriculum corpus is checked in (34 `.a3p` starter projects) and validated by
`TweedleCorpusRoundTripCharacterizationTest` above. Any *additional* external
curriculum `.a3p` samples (see `../alice-3-curriculum-md/`) can be dropped into
`core/resources/src/application/resources/starter-projects/` and are picked up
by the same directory-walking harness automatically — no test change required.

## Relationship to the hybrid reader

The hybrid reader (`core/story-api-migration/.../io/HybridProjectIo.java`) prefers
the Tweedle representation and falls back to the in-archive XML payload whenever:

- a manifest-declared type hits any of the decode gaps above (the Tweedle read
  throws), or
- the Tweedle read succeeds but does not recover every resource the archive's
  `resources.xml` declares (e.g. generic, non-image/audio resources that the
  Tweedle side does not persist).

The fallback is whole-archive rather than per-type, because mixing
Tweedle-decoded and XML-decoded types into a single project cannot preserve the
object identity that cross-type references rely on. As the gaps in this backlog
are closed, more archives will be read entirely from Tweedle with no behavior
change required in the reader.

## Resource recovery and the completeness gate

The reader's second fallback trigger — "did not recover every declared resource"
— deserves an explicit note, because it is a *resource* gap rather than a
*decoder* gap and it will not close as the table above shrinks.

- The Tweedle reader (`JsonProjectIo.readResource`) reconstructs **only**
  `ImageReference`/`AudioReference` entries; any other `ResourceReference` yields
  `null`. The hybrid writer's `repointResourceReferences` likewise repoints only
  image/audio references.
- `XmlProjectIo.writeResources` writes **every** `org.lgna.common.Resource` in the
  project (by class name + UUID) into `resources.xml`.
- Today the only concrete `Resource` subclasses are `ImageResource` and
  `AudioResource` (`core/util/.../org/lgna/common/resources/`), and **both** are
  recovered and repointed. So for all currently-shipping resource types the
  completeness gate never falsely fires — image/audio archives can be read
  entirely from Tweedle once their types decode.
- The gate is therefore a **forward-looking guard**: if a future non-image/audio
  `Resource` subtype is added (or a generic resource reaches `resources.xml`), any
  archive carrying it is routed through the complete XML payload so the resource
  is never silently dropped. This is characterized by
  `IoUtilitiesTest.hybridProjectWithGenericResourceFallsBackToXmlYetRecoversResource`
  (using the test-only `TestResource`), which proves such an archive round-trips
  via the XML fallback and genuinely depends on the XML payload.
- Gallery *model* references (bipeds, props, terrain — e.g. `TerrainResource`,
  `WaterTankResource`) are **not** `org.lgna.common.Resource` blobs in
  `resources.xml`; they are AST types/parameters handled by the decoder, and so
  appear in this document as decoder gaps (rows above), not resource-recovery
  gaps.
