# Characterize Legacy Fixture Round-Trip Readiness

Use this guide to add or review the focused legacy fixture round-trip readiness
lane in `core/story-api-migration`.

The goal is to protect targeted generated archive behavior without expanding
into full historical archive migration, full Tweedle decode, or full player
decode.

## Prerequisites

Work in:

```text
core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java
```

Initialize the grammar submodule before Maven validation in a fresh checkout:

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar
```

Keep generated fixtures temporary. Do not commit `.a3p`, `.a3w`, `.a3c`, image,
audio, user project, or recovery-log payloads for this lane.

The QA smoke for this lane is:

```text
qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml
```

It must remain a `gated-command-smoke` that runs only the focused
`HistoricalArchiveRoundTripCharacterizationTest` Maven argv when gated smokes
are explicitly enabled.

## 1. Choose the fixture boundary

Pick one production archive boundary:

| Boundary | Use when the change affects |
| --- | --- |
| Editable `.a3p` XML fallback | Project save/read archive entries, manifest metadata, project resources, or resource-expression rebinding. |
| Simple `.a3w` player round trip | Player export/read routing, simple generated Tweedle source entries, or scene-camera metadata. |
| JSON `.a3w` player boundary | Manifest-declared generated player or sibling types using one documented narrow decoder shape, such as primitive-return methods, methods returning `this.<field>`, unnamed sibling type recovery with a primitive-return method, empty constructors, literal arithmetic field initializers, simple `if`, or zero-argument same-type `this.method()` calls. |
| `.a3c` XML fallback type round trip | Type archive entries, type resources, or type-level resource-expression rebinding. |
| JSON `.a3c` type boundary | Manifest-declared generated type decode using one documented narrow shape, such as field-only resource readback, primitive-return methods, empty constructors, constructor field assignment, or null text-field initializers. |
| Unsupported shape | Migration floor handling, missing manifest-declared source entries, unresolved parent types, or unsupported Tweedle diagnostics. |

Do not combine unrelated archive concerns in one new fixture. A fixture should
make one compatibility claim and one explicit non-claim.

## 2. Generate the fixture

Build deterministic input in the JUnit temporary folder.

For resource coverage, use a tiny generated image resource:

```java
ImageResource imageResource =
    generatedImageResource("historical-project-texture.png", 0xFF996633);
```

For project/type round trips, attach the resource through an AST
`ResourceExpression` so readback proves both resource collection preservation
and expression rebinding.

For player fixtures, use simple supported Tweedle source unless the test is
specifically asserting a checked unsupported boundary.

For JSON type fixtures, keep the Tweedle source to the smallest supported shape
under review. For example, a constructor-assignment fixture should assert the
`ExpressionStatement` wrapper and `AssignmentExpression` details, but should not
also introduce resources, method bodies, or unrelated initializer behavior.

## 3. Write through production IO

Use the public API that production callers use:

```java
IoUtilities.writeProject(projectArchive, project);
IoUtilities.exportProject(playerArchive, project);
IoUtilities.writeType(typeArchive, type);
```

Do not call helper serializers as the primary evidence. Helper methods may build
the fixture, but the observable behavior must cross the `IoUtilities` boundary.

## 4. Assert archive shape

Inspect the generated zip with `ZipFile` and assert stable entries.

For `.a3p` XML fallback project fixtures:

```text
version.txt
manifest.json
programType.xml
resources.xml
resources/historical-project-texture.png
```

Also assert that `src/<Program>.twe` is absent.

For simple `.a3w` player fixtures:

```text
version.txt
manifest.json
src/<Program>.twe
```

For `.a3c` XML fallback type fixtures:

```text
version.txt
type.xml
resources.xml
resources/historical-type-texture.png
```

Also assert that `manifest.json` is absent for this XML fallback type fixture.

For JSON `.a3w` and `.a3c` fixtures, assert the manifest-declared type reference
and source entry path before readback:

```text
manifest.json
src/<GeneratedType>.twe
```

Only add resource entries when the fixture is specifically about manifest-backed
resource readback.

## 5. Read and round trip

Read through the public API:

```java
Project readProject = IoUtilities.readProject(projectArchive);
TypeResourcesPair readType = IoUtilities.readType(typeArchive);
```

Assert only the behavior the fixture proves:

- decoded type or program name;
- expected supertype;
- scene-camera metadata when applicable;
- resource id, name, original file name, content type, and bytes;
- `ResourceExpression` rebinding when the fixture includes one.

For round-trip fixtures, write the decoded object again and repeat the same
archive-shape and readback assertions against the second archive.

## 6. Assert unsupported shapes fail closed

Unsupported fixture tests should expect checked failures:

```java
IOException thrown =
    assertThrows(IOException.class, () -> IoUtilities.readProject(projectArchive));
```

or:

```java
VersionNotSupportedException thrown =
    assertThrows(VersionNotSupportedException.class, () -> IoUtilities.readProject(projectArchive));
```

Assert stable diagnostic substrings such as the manifest-declared type, decoded
type set, unsupported type name, or migration floor. Do not assert success with a
null program type unless a test explicitly documents a narrow legacy recovery
compatibility case.

Do not claim corrupt manifest or manifest/file-type mismatch coverage from this
focused command unless that fixture is added here. Cite the broader `IoUtilities`
suite separately for those reader-routing checks.

## 7. Run the focused validation

Run from the repository root:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

`test` is the canonical Maven goal for documentation and PR evidence. `test -q`
is equivalent for scope when only quieter Maven logs are desired.
Do not wrap the command in an external timeout helper. A timeout result is a
separate infrastructure signal, not a successful characterization result.
This rule does not prohibit the QA scenario's `timeoutSeconds` field; that field
is harness metadata for bounded gated-smoke execution, not canonical readiness
evidence.

If the lane changes QA scenario files, also run the relevant desktop QA schema
and contract scripts. If no QA files changed, this Maven command is the focused
fixture-readiness gate.

## 8. Keep the QA smoke allowlist synchronized

When the archive fixture smoke command changes, update all allowlist surfaces in
the same change:

```text
qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml
qa/outside-in/alice-desktop/runners/run-scenario.sh
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/schema/scenario.schema.json
qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

Then run:

```bash
qa/outside-in/alice-desktop/runners/validate-scenarios.sh
qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

To run the smoke instead of only preparing gated evidence:

```bash
ALICE_QA_RUN_GATED_SMOKES=1 \
NODE_OPTIONS=--max-old-space-size=32768 \
qa/outside-in/alice-desktop/runners/run-scenario.sh run alice-desktop-archive-fixture-smoke
```

## 9. Write bounded PR evidence

Use the exact PR head SHA from GitHub when recording evidence:

```bash
gh pr view 433 --json headRefOid --jq .headRefOid
git rev-parse HEAD
```

The two SHAs must match before local validation is described as final-head
evidence.

Before writing strict merge-ready language, also verify the final GitHub PR
state:

| Check | Required result |
| --- | --- |
| PR state | Open and non-draft. |
| Base branch | The PR targets the intended integration branch. |
| Merge state | `mergeStateStatus` is `CLEAN`. |
| Required checks | Required checks in `statusCheckRollup` are successful by name. |
| Review decision | `reviewDecision` is `APPROVED` before describing the PR as approved; an empty value is not approval. |

In PR notes, separate the state into:

| Section | What belongs there |
| --- | --- |
| Completed evidence | Exact focused command, result, and matching head SHA. |
| QA/scenario evidence | `archive-fixture-smoke` scenario, validator, runner, schema, and schema-contract wiring. |
| Documentation evidence | Links to the reference, how-to, tutorial, and docs index entries for this lane. |
| Quality-audit cycle | SEEK scope, VALIDATE commands and checks, and FIX changes made inside this bounded lane. |
| CI evidence | Required check names and whether each is successful, pending, queued, or in progress. |
| Focused-scope evidence | Explicit statement that conflicts and fixes stayed inside the legacy fixture round-trip lane. |
| Pending checks | Check names still queued, in progress, or pending. |
| Non-blocking pending CI status | Why the PR does not claim all checks passed yet. |
| Non-claims | Full historical archive migration, full Tweedle decode, full player decode, and arbitrary user archive support remain out of scope. |

Do not write "fully ready", "fully green", or "all checks passed" while any
required check is still pending.

## 10. Recover strict merge-ready wording after the target branch moves

When the integration target changes, update the PR branch by merging the current
integration target into it. Do not rebase, force-push, or merge the PR branch
into the target branch manually.

After resolving scoped conflicts and rerunning affected focused validation, use
the [merge-ready evidence contract](../reference/legacy-fixture-roundtrip-readiness.md#merge-ready-evidence-contract)
as the canonical PR body source. The how-to evidence should not introduce a
separate contract; it should point to the reference and record the final head
SHA, integration target, focused validation command, QA/scenario wiring, docs
coverage, quality-audit SEEK/VALIDATE/FIX, CI state, and focused non-claims.

If any required check is pending, the PR body should say "focused local evidence
is complete and CI is pending" rather than "strict merge-ready". Strict
merge-ready wording is reserved for a current-base branch with completed required
checks and final-head focused evidence.

## 11. Finalize PR #433 with the no-timeout current-head profile

PR #433 uses the same bounded lane with current-head evidence. This is a
PR-specific finalization profile for PR `433` in `rysweet/RabbitHole`; do not
reuse it as a generic approval or merge workflow.

The accepted head is the current PR head observed at finalization time, not a
SHA copied forward from an earlier evidence run. Capture it with the read-only
commands below and use that same value consistently in any PR note:

```text
<current-pr-head-sha>
```

Confirm local and GitHub state with read-only commands:

```bash
git rev-parse HEAD
gh pr view 433 --repo rysweet/RabbitHole \
  --json headRefOid,state,isDraft,baseRefName,mergeStateStatus,reviewDecision,statusCheckRollup
```

Proceed only when local `HEAD` and GitHub `headRefOid` both equal the captured
`<current-pr-head-sha>`, the PR is open and non-draft, `mergeStateStatus` is
`CLEAN`, and the required/relevant PR checks in `statusCheckRollup` are
successful for the same head. If `reviewDecision` is empty, say the PR was
reviewed/finalized with current evidence; do not say it is formally approved.

Refresh the focused lane without an external timeout wrapper:

```bash
NODE_OPTIONS=--max-old-space-size=32768 \
mvn -DincludeSims=false -Dinstall4j.skip \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -pl core/story-api-migration -am \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Then run the PR-specific merge-ready contract:

```bash
python3 -m unittest tests.test_pr433_merge_ready_contract
```

Use merge-ready wording only if both commands pass for the accepted head and the
diff remains inside the focused legacy fixture round-trip lane. Do not manually
merge the PR.

Use this literal no-op statement only for the finalization/evidence run when
that run edits no repository files. Do not use it for documentation-retcon work
or any other task that changes files:

```text
No-op justification: no repository files were changed during the finalization/evidence run because current local/GitHub head <current-pr-head-sha> matches, required/relevant PR checks in statusCheckRollup are successful for the same head, mergeability is clean, and the diff remains inside the focused legacy fixture round-trip lane.
```

## Review checklist

| Question | Required answer |
| --- | --- |
| Does the test run on generated temporary fixtures? | Yes. |
| Does the evidence cross `IoUtilities`? | Yes. |
| Are archive entries asserted before readback? | Yes, for the changed fixture shape. |
| Are unsupported shapes checked failures? | Yes. |
| Is the `archive-fixture-smoke` QA argv allowlist synchronized if it changed? | Yes. |
| Does the wording avoid full historical migration claims? | Yes. |
| Does the wording avoid full Tweedle or player decode claims? | Yes. |
| Are ad hoc recovery files absent from the PR? | Yes. |
| Does the PR evidence name QA/scenario, docs, quality-audit SEEK/VALIDATE/FIX, CI, and focused-scope evidence? | Yes. |
