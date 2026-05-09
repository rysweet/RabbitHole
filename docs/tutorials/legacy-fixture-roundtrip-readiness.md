# Tutorial: Trace Legacy Fixture Round-Trip Readiness

This tutorial walks through reviewing the focused legacy fixture round-trip lane
from fixture generation to bounded PR evidence.

## Goal

Protect this current Alice archive behavior:

```text
Generated legacy-shaped fixtures for selected .a3p, .a3w, and .a3c archive
boundaries round trip through production IoUtilities when the fixture shape is
supported. Unsupported shapes fail closed at the archive I/O boundary.
```

The tutorial does not add real historical archives, Git LFS payloads, full
Tweedle support, or full player decode support.

## 1. Start from the focused suite

Open:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

Find the generated fixture tests for:

- `generatedProjectArchiveCharacterizesXmlFallbackProjectRoundTripWithoutExternalFixture`;
- `generatedWorldArchiveCharacterizesManifestProgramRoundTripWithoutExternalFixture`;
- `generatedClassArchiveCharacterizesXmlFallbackTypeRoundTripWithoutExternalFixture`;
- generated JSON `.a3w` player boundary fixtures for sibling types, resources,
  null initializers, primitive-return methods, methods returning `this.<field>`,
  constructors, literal arithmetic, simple `if`, unnamed sibling type recovery
  with a primitive-return method, and zero-argument same-type `this.method()`
  calls;
- generated JSON `.a3c` type boundary fixtures for resources, primitive-return
  methods, empty constructors, constructor assignment, and null initializers;
- `generatedXmlArchiveBeforeSupportedMigrationFloorFailsClosed`.

These tests define the supported round-trip slice and the explicit unsupported
boundaries. They are generated fixtures, not committed historical payloads.

## 2. Follow the `.a3p` project fixture

The project fixture creates a generated `ImageResource`, attaches it to a
generated `Project`, and writes:

```java
IoUtilities.writeProject(projectArchive, project);
```

The first review point is the archive shape:

```text
version.txt
manifest.json
programType.xml
resources.xml
resources/historical-project-texture.png
```

The fixture also proves this is still XML fallback behavior by asserting that no
Tweedle source entry exists:

```text
src/GeneratedHistoricalProject.twe
```

Then it reads with:

```java
Project firstRead = IoUtilities.readProject(projectArchive);
```

The readback assertions prove program metadata, resource bytes, and
`ResourceExpression` rebinding. The second write/read repeats the same contract
against the round-trip archive.

## 3. Follow the `.a3w` player fixture

The player fixture exports a simple generated project:

```java
IoUtilities.exportProject(exportArchive, project);
```

The supported player fixture is intentionally simple. It proves that a generated
program type can be emitted as Tweedle source, named by `manifest.json`, read
back through `IoUtilities.readProject`, and exported/read again.

It does not prove resource-expression player decode, arbitrary method-body
decode, runtime player launch, or desktop behavior.

## 4. Follow the `.a3c` type fixture

The type fixture writes:

```java
IoUtilities.writeType(typeArchive, generatedType);
```

The archive shape is XML fallback:

```text
version.txt
type.xml
resources.xml
resources/historical-type-texture.png
```

The fixture reads through:

```java
TypeResourcesPair firstRead = IoUtilities.readType(typeArchive);
```

The readback assertions prove the decoded type, associated resource collection,
resource bytes, and `ResourceExpression` rebinding. The second write/read repeats
the same type archive contract.

## 5. Follow an unsupported floor

The unsupported XML fixture declares an older version and minimal XML payload.
Reading it must throw:

```java
VersionNotSupportedException
```

The assertion checks the reported minimum supported version and the version from
the fixture. This is readiness evidence for fail-closed unsupported archive
handling, not migration support for that old archive.

## 6. Follow the JSON boundary fixtures

The JSON fixtures use generated `manifest.json` metadata and generated Tweedle
source entries:

```text
manifest.json
src/<GeneratedType>.twe
```

Review each JSON fixture as one small contract:

| Fixture kind | What to check |
| --- | --- |
| JSON `.a3w` supported shape | `IoUtilities.readProject` decodes the manifest-declared program or sibling type shape being characterized. |
| JSON `.a3w` unsupported neighbor | `IoUtilities.readProject` throws a checked archive-boundary failure instead of returning a partial project. |
| JSON `.a3c` supported shape | `IoUtilities.readType` decodes the manifest-declared type shape being characterized. |
| JSON `.a3c` unsupported neighbor | `IoUtilities.readType` throws a checked archive-boundary failure instead of returning a partial type. |

Do not combine several decoder expansions into one fixture. If a fixture proves
constructor assignment, keep resources, method calls, arithmetic initializers,
and unrelated statement forms out of that fixture.

The unnamed sibling type recovery fixture is one narrow JSON `.a3w`
compatibility case: it proves that a manifest-declared sibling without a source
type name can still decode when the asserted shape is only a primitive-return
method. It is not evidence for arbitrary unnamed type recovery.

## 7. Review the QA smoke

Open:

```text
qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml
```

The scenario should remain a gated command smoke for the same focused Maven
suite. The workflow value is:

```text
archive-fixture-smoke
```

The argv is allowlisted in the runner, validator, schema, and schema contract.
That keeps the desktop QA lane deny-by-default while still giving reviewers one
scenario ID for the legacy fixture round-trip lane.

## 8. Run the focused gate

From the repository root:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

The canonical evidence command uses `test`. Adding `-q` only reduces Maven log
verbosity and should be called out as such if used in PR notes.
Do not wrap the command in an external timeout helper. Reviewers need the Maven
result for the final head, not a timeout wrapper status.
The QA scenario can still use `timeoutSeconds` as runner metadata; that value
only bounds gated-smoke execution and does not become the readiness result.

Use the commit SHA that actually contains the final fixture and documentation
changes when recording PR readiness evidence.

If QA wiring changed, also run:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

To execute the gated smoke:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-archive-fixture-smoke
```

## 9. Write bounded review notes

Acceptable review wording:

```text
Focused legacy fixture round-trip readiness passes for the generated .a3p,
.a3w, and .a3c fixture lane at <commit>. The evidence covers targeted
IoUtilities archive behavior and fail-closed unsupported fixture boundaries.
It does not claim full historical archive migration, full Tweedle decode, or
full player decode.
```

If checks are pending, say so directly:

```text
Focused local evidence is complete at <commit>. CI checks <names> are still
pending, so this note does not claim all checks passed.
```

Avoid wording like:

```text
Historical Alice archives are migrated.
The player reader fully decodes Tweedle.
Legacy projects are now generally supported.
```

Those claims are outside this lane.

## 10. Trace merge-ready evidence

A merge-ready PR body for this lane ties the focused characterization to the
current integration target. The
[reference contract](../reference/legacy-fixture-roundtrip-readiness.md#merge-ready-evidence-contract)
is canonical; this tutorial only shows how to trace the same evidence. Review
the evidence in this order:

| Evidence | What the PR body should say |
| --- | --- |
| Current base | The PR branch has been updated by merging the named integration target into it, without rebasing or force-pushing. |
| GitHub PR state | GitHub `headRefOid` matches local `HEAD`, the PR is open and non-draft, the base branch is the intended integration target, and `mergeStateStatus` is `CLEAN`. |
| QA/scenario | `archive-fixture-smoke` is wired through scenario, runner, validator, schema, and schema-contract surfaces. |
| Documentation | The reference, how-to, tutorial, and docs index describe the finished bounded lane. |
| Quality audit | SEEK names the scoped fixture lane and non-claims, VALIDATE lists focused validation, and FIX names only scoped changes. |
| CI | Required checks are listed by name with successful or pending states. |
| Review decision | The PR is described as approved only when GitHub reports `reviewDecision: APPROVED`; an empty value is not approval. |
| Focused scope | The note repeats that full historical migration, full Tweedle decode, full player decode, arbitrary user archive support, and desktop UI behavior are out of scope. |

Use this finished-state wording when all required checks have completed:

```text
The PR is current with <base-branch> at <head-sha>. Focused local evidence,
QA/scenario wiring, documentation coverage, quality-audit SEEK/VALIDATE/FIX, and
required CI checks are complete for the bounded legacy fixture round-trip lane.
The evidence stays inside generated fixture round-trip readiness and does not
claim full historical archive migration, full Tweedle decode, full player decode,
arbitrary user archive support, or desktop UI behavior.
```

Use this wording while CI is still running:

```text
The PR branch is current with <base-branch> at <head-sha>. Focused local
evidence, QA/scenario wiring, documentation coverage, and quality-audit
SEEK/VALIDATE/FIX are complete for the bounded lane. CI checks <names> are still
pending, so this note does not claim strict merge-ready or all checks passed.
```

For PR #433 only, trace the current-head profile instead of replacing the
placeholders with an earlier branch head or reusing this as a generic merge
workflow. Capture the expected head from local `HEAD` and GitHub `headRefOid` at
finalization time:

```text
<current-pr-head-sha>
```

The finished evidence sequence is:

```bash
git rev-parse HEAD
gh pr view 433 --repo rysweet/RabbitHole \
  --json headRefOid,state,isDraft,baseRefName,mergeStateStatus,reviewDecision,statusCheckRollup
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
python3 -m unittest tests.test_pr433_merge_ready_contract
```

The resulting PR note stays narrow:

```text
PR #433 is current at <current-pr-head-sha>. The focused
legacy fixture round-trip Maven lane and PR #433 merge-ready contract passed for
that head, required/relevant PR checks in statusCheckRollup are successful for
the same head, and GitHub mergeability is CLEAN. This does not claim formal
approval unless GitHub reports reviewDecision: APPROVED, and it does not claim
full historical archive migration, full Tweedle decode, full player decode,
arbitrary user archive support, or desktop UI behavior.
```

Use the required no-op wording only for a finalization/evidence run that changes
no repository files, not for documentation-retcon changes:

```text
No-op justification: no repository files were changed during the finalization/evidence run because current local/GitHub head <current-pr-head-sha> matches, required/relevant PR checks in statusCheckRollup are successful for the same head, mergeability is clean, and the diff remains inside the focused legacy fixture round-trip lane.
```
