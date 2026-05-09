# Tutorial: Assemble PR #463 Recovery Evidence

This tutorial walks through assembling the structured evidence JSON that the
PR #463 recovery gate evaluates. Follow along after a focused repair of the
`feat/issue-462-restart-rabbithole-archiveplayer-boundary-lane-thr` branch.

## 1. Confirm you are on the right branch

```bash
git branch --show-current
```

Expected output:

```text
feat/issue-462-restart-rabbithole-archiveplayer-boundary-lane-thr
```

If you are not on this branch, the gate rejects the evidence with
`wrong-authoritative-branch`.

## 2. Confirm a clean worktree

```bash
git status --porcelain
```

Expected: no output. Any uncommitted changes produce `dirty-worktree`.

## 3. Initialize the tweedle-lang submodule

```bash
git submodule update --init tweedle-lang
test -d tweedle-lang/Grammar && echo "submodule ready"
```

The gate checks `tweedleLangInitialized: true`.

## 4. Capture SHA values

```bash
HEAD_SHA=$(git rev-parse HEAD)
DEVELOP_SHA=$(git rev-parse origin/develop)
echo "head:    $HEAD_SHA"
echo "develop: $DEVELOP_SHA"
```

Use these exact values in the evidence JSON. Placeholder SHAs cause
`missing-pr-head-evidence` or `missing-develop-base-sha` blockers.

## 5. Run focused Python contract tests

```bash
export NODE_OPTIONS=--max-old-space-size=32768

python3 -m unittest \
  tests.test_pr463_owner_free_recovery_gate \
  tests.test_pr463_archive_player_boundary_contract -v
```

Record the validation:

```json
{
  "name": "python-pr463-contracts",
  "command": "NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest tests.test_pr463_owner_free_recovery_gate tests.test_pr463_archive_player_boundary_contract",
  "outcome": "passed",
  "headSha": "<HEAD_SHA>"
}
```

## 6. Run QA scenario and schema validation

```bash
bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh
bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh
```

Record the validation:

```json
{
  "name": "alice-desktop-scenario-catalog",
  "command": "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh && NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
  "outcome": "passed",
  "headSha": "<HEAD_SHA>"
}
```

## 7. Run Maven archive fixture characterization

```bash
mvn -pl core/story-api-migration -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest \
  test
```

Record the validation:

```json
{
  "name": "story-api-migration-characterization",
  "command": "NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/story-api-migration -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest test",
  "outcome": "passed",
  "headSha": "<HEAD_SHA>"
}
```

## 8. Run Maven Tweedle decoder boundary tests

```bash
mvn -pl core/ast -am \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+zeroArgumentThisMethodCallDecodeRejectsChainedCall \
  test
```

Record the validation:

```json
{
  "name": "core-ast-decoder-boundary",
  "command": "NODE_OPTIONS=--max-old-space-size=32768 mvn -pl core/ast -am -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+... test",
  "outcome": "passed",
  "headSha": "<HEAD_SHA>"
}
```

## 9. Assemble the evidence JSON

Combine all the pieces:

```json
{
  "repository": "rysweet/RabbitHole",
  "prNumber": 463,
  "branch": "feat/issue-462-restart-rabbithole-archiveplayer-boundary-lane-thr",
  "baseRef": "develop",
  "developBaseSha": "<DEVELOP_SHA>",
  "headSha": "<HEAD_SHA>",
  "localHeadSha": "<HEAD_SHA>",
  "prHeadSha": "<HEAD_SHA>",
  "mergeable": "MERGEABLE",
  "mergeStateStatus": "CLEAN",
  "worktreeClean": true,
  "recoveryMode": "focused-archive-player-repair",
  "scope": "archive/player-boundary",
  "manualMergePerformed": false,
  "replacementPullRequestCreated": false,
  "noOpModeUsed": false,
  "repairRequired": false,
  "pushedRepair": false,
  "tweedleLangInitialized": true,
  "nodeOptions": "--max-old-space-size=32768",
  "archivePlayerEvidenceSurfaces": [
    "docs/reference/player-archive-unsupported-tweedle-diagnostics.md",
    "docs/howto/characterize-player-archive-unsupported-tweedle-diagnostics.md",
    "docs/tutorials/player-archive-unsupported-this-call-diagnostic.md",
    "qa/outside-in/alice-desktop/scenarios/archive-fixture-smoke.yaml",
    "qa/outside-in/alice-desktop/scenarios/tweedle-decoder-boundary-smoke.yaml",
    "core/story-api-migration/src/test/java/org/lgna/project/io/HistoricalArchiveRoundTripCharacterizationTest.java",
    "core/ast/src/test/java/org/alice/serialization/tweedle/TweedleEncoderDecoderTest.java"
  ],
  "boundaryEvidence": {
    "headSha": "<HEAD_SHA>",
    "referenceDocCurrent": true,
    "howtoCurrent": true,
    "tutorialCurrent": true,
    "archiveScenarioCurrent": true,
    "tweedleScenarioCurrent": true,
    "characterizationTestsCurrent": true,
    "nonclaims": [
      "full Tweedle/player decode",
      "historical archive migration completeness",
      "full UI automation",
      "visible rendering correctness",
      "grading",
      "Save/Open guarantees",
      "lesson completion"
    ],
    "forbiddenClaims": [],
    "generatedArchivesCommitted": [],
    "binaryCorpusPayloadsCommitted": []
  },
  "qaScenarioContracts": {
    "validated": true,
    "workflows": {
      "archive-fixture-smoke": {
        "allowlistedInValidator": true,
        "allowlistedInRunner": true,
        "listedInSchema": true,
        "automationMode": "gated-command-smoke",
        "argv": ["mvn", "-DincludeSims=false", "-Dinstall4j.skip", "-DfailIfNoTests=false", "-Dsurefire.failIfNoSpecifiedTests=false", "-pl", "core/story-api-migration", "-am", "-Dtest=org.lgna.project.io.HistoricalArchiveRoundTripCharacterizationTest", "test"]
      },
      "tweedle-decoder-boundary-smoke": {
        "allowlistedInValidator": true,
        "allowlistedInRunner": true,
        "listedInSchema": true,
        "automationMode": "gated-command-smoke",
        "argv": ["mvn", "-DincludeSims=false", "-Dinstall4j.skip", "-DfailIfNoTests=false", "-Dsurefire.failIfNoSpecifiedTests=false", "-pl", "core/ast", "-am", "-Dtest=org.alice.serialization.tweedle.TweedleEncoderDecoderTest#zeroArgumentThisMethodCallDecodeRejectsArgumentBearingCall+zeroArgumentThisMethodCallDecodeRejectsOptionalParameterTargetMethod+zeroArgumentThisMethodCallDecodeRejectsUnknownMethod+zeroArgumentThisMethodCallDecodeRejectsDuplicateTargetMethodName+zeroArgumentThisMethodCallDecodeRejectsNonThisTarget+zeroArgumentThisMethodCallDecodeRejectsStaticTargetMethod+zeroArgumentThisMethodCallDecodeRejectsChainedCall", "test"]
      }
    }
  },
  "validations": [
    { "name": "python-pr463-contracts", "command": "...", "outcome": "passed", "headSha": "<HEAD_SHA>" },
    { "name": "alice-desktop-scenario-catalog", "command": "...", "outcome": "passed", "headSha": "<HEAD_SHA>" },
    { "name": "story-api-migration-characterization", "command": "...", "outcome": "passed", "headSha": "<HEAD_SHA>" },
    { "name": "core-ast-decoder-boundary", "command": "...", "outcome": "passed", "headSha": "<HEAD_SHA>" }
  ],
  "githubActions": {
    "headSha": "<HEAD_SHA>",
    "checks": []
  },
  "prEvidence": {
    "headSha": "<HEAD_SHA>",
    "currentHeadEvidence": true,
    "mergeReadyCriteriaUpdated": true,
    "boundedArchivePlayerClaimsOnly": true,
    "blockers": []
  },
  "commands": [
    "git fetch origin develop",
    "git submodule update --init tweedle-lang",
    "NODE_OPTIONS=--max-old-space-size=32768 python3 -m unittest tests.test_pr463_owner_free_recovery_gate tests.test_pr463_archive_player_boundary_contract",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/runners/validate-scenarios.sh",
    "NODE_OPTIONS=--max-old-space-size=32768 bash qa/outside-in/alice-desktop/tests/test-schema-contract.sh",
    "..."
  ]
}
```

Replace every `<HEAD_SHA>` and `<DEVELOP_SHA>` with the actual values from
step 4. Replace `"..."` placeholders in `validations[].command` and `commands`
with the actual commands you ran. The `commands` array must not be empty; the
gate reports `missing-command-evidence` when no commands are recorded.

## 10. Run the gate

```bash
python3 scripts/pr463_recovery_gate.py evidence.json --pretty --verbose
```

### Expected output for a passing gate

```json
{
  "allowedRepairPaths": [],
  "blockers": [],
  "headSha": "b67969f41ccf61d85cefc2a8b2ee133a2fa1ac38",
  "mayUseNoOpJustification": false,
  "recoveryMode": "focused-archive-player-repair",
  "repairRequired": false,
  "status": "MERGE_READY",
  "summary": "PR #463 is merge-ready after focused archive/player repair: ..."
}
```

Exit code is `0`.

### With live GitHub evidence

```bash
python3 scripts/pr463_recovery_gate.py evidence.json \
  --refresh-github --pretty --verbose
```

This overlays live PR state (mergeability, head SHA, check results) from
`gh pr view` before evaluation. Use this mode to catch remote/local evidence
drift.

## 11. Fix common problems

**Stale validation SHAs.** If you made additional commits after running
validations, the gate reports `validation-stale-head`. Re-run validations at
the final HEAD and rebuild the evidence JSON.

**Submodule not initialized.** The gate reports `tweedle-lang-not-initialized`.
Run `git submodule update --init tweedle-lang` and set the field to `true`.

**Missing nonclaims.** If `boundaryEvidence.nonclaims` is incomplete, the gate
reports `archive-player-nonclaims-missing`. Include all seven nonclaims listed
in [PR #463 Recovery Gate Reference](../reference/pr463-recovery-gate.md).

**GitHub Actions still running.** The gate reports
`github-actions-not-complete`. Wait for CI to finish before collecting evidence.

## What this does not prove

This tutorial produces structured evidence that PR #463 recovery is complete
and bounded. It does not prove:

- full Tweedle/player decode
- historical archive migration completeness
- full UI automation
- visible rendering correctness
- grading
- Save/Open guarantees
- lesson completion

The gate validates evidence structure and currency. Human review of the actual
characterization test behavior is still required.
